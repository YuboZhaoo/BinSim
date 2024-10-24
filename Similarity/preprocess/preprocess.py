import click
import json
import networkx as nx
import numpy as np
import os
import pickle

from collections import Counter
from collections import defaultdict
from tqdm import tqdm

'''
graph: from1,*::to,*::edgetype,*::nodenum::nodenum
opc:[xx,xx]
{
    "IDBs/Dataset-1/zlib/mips64-gcc-4.8-O3_libz.so.1.2.11.i64":{
            "0xd030":{
                        "graph": "0;1::1,0::1,2::2,2"
                        "opc": [0,1]
            },
            "0x5380":{
                        "graph": "0;1::1,0::1,2::2,2"
                        "opc": [0,1]
            }
    }
}
'''

class PreprocessGraph():
    def __init__(self, input_dir, training, freq_mode, opcodes_json, output_dir, dataset, out_format, gtype):
        self.inputPath = input_dir
        self.outputPath = output_dir

        self.training = training
        self.freq_mode = freq_mode
        # self.gtype = gtype
        self.GRAPH_TYPES = gtype
        self.opcodes_json = opcodes_json
        self.out_format = out_format
        self.dataset = dataset

    def coo2tuple(self, coo_mat):
        return (coo_mat.row, coo_mat.col, coo_mat.data, *coo_mat.shape)
    def coo_matrix_to_str(self, cmat):
        """
        Convert the Numpy matrix in input to a Scipy sparse matrix.

        Args:
            np_mat: a Numpy matrix

        Return
            str: serialized matrix
        """
        # Custom string serialization
        row_str = ';'.join([str(x) for x in cmat.row])
        col_str = ';'.join([str(x) for x in cmat.col])
        data_str = ';'.join([str(x) for x in cmat.data])
        n_row = str(cmat.shape[0])
        n_col = str(cmat.shape[1])
        mat_str = "::".join([row_str, col_str, data_str, n_row, n_col])
        return mat_str
    def handle_ndarray(self, obj):
        if isinstance(obj, np.ndarray):
            return obj.tolist()  # 将ndarray转换为列表
        raise TypeError(f"Object of type {obj.__class__.__name__} is not JSON serializable")
    def get_sub_dir(self, output_dir, gtype, dataset=None):
        if dataset is not None:
            sub_dir = os.path.join(output_dir, f'pcode_{gtype.lower()}', dataset)
        else:
            sub_dir = os.path.join(output_dir, f'pcode_{gtype.lower()}')
        if not os.path.exists(sub_dir):
            os.makedirs(sub_dir)
        return sub_dir




    # for the op-level graph
    def parse_nxopr(self, pcode_asm):
        s = pcode_asm.find('(')
        e = pcode_asm.find(')')
        return tuple(pcode_asm[s + 1:e].split(', ')), pcode_asm[e + 1:].strip()

    # for the inst-level graph
    def parse_pcode(self, pcode_asm):
        '''
        Examples:
        (register, 0x20, 4) COPY (const, 0x0, 4)
        (unique, 0x8380, 4) INT_ADD (register, 0x4c, 4) , (const, 0xfffffff0, 4)
         ---  STORE (STORE, 0x1a1, 0) , (unique, 0x8280, 4) , (register, 0x20, 4)
         ---  BRANCH (ram, 0x22128, 4)
        '''
        NOP_OPERAND = ' --- '
        dst_opr = None
        if pcode_asm.startswith(NOP_OPERAND):
            # no output varnode
            pcode_asm = pcode_asm[len(NOP_OPERAND) + 1:]
        else:
            # get the output varnode, and the remain pcode
            dst_opr, pcode_asm = self.parse_nxopr(pcode_asm)
        opc_e = pcode_asm.find(' ')
        if opc_e != -1:
            opc, pcode_asm = pcode_asm[:opc_e], pcode_asm[opc_e:].strip()
        else:
            opc, pcode_asm = pcode_asm, ""
        oprs = [] if dst_opr is None else [dst_opr, ]
        while len(pcode_asm) != 0:
            src_opr, pcode_asm = self.parse_nxopr(pcode_asm)
            oprs.append(src_opr)

        # output: (COPY, [(register, 0x20, 4), (const, 0x0, 4)]
        return (opc, oprs)

    def normalize_pcode_opr(self, opr, arch):
        if opr[0] in ['register']:
            return f'{arch}_reg', arch + '_' + '_'.join(opr)
        elif opr[0] in ['STORE', 'const']:
            return 'val', '_'.join(opr[:-1])  # omit dummy size field
        elif opr[0] in ['unique', 'NewUnique', 'ram', 'stack', 'VARIABLE']:
            return 'val', opr[0]
        else:
            raise Exception(f"Unkown operand type {opr[0]}. FULL: {opr}. ")

    def normalize_pcode(self, pcode, arch):
        normalized_pcode = [('opc', pcode[0])]
        for opr in pcode[1]:
            normalized_pcode.append(self.normalize_pcode_opr(opr, arch))
        return normalized_pcode

    def token_mapping(self, input_folder, output_dir, freq_mode=True):
        print("[i] Freq_mode: ", freq_mode)
        idmaps, opc_counters, opc_occurs = {}, {}, {}
        cached = {}
        num_func = 0

        # Try loading caches
        for gtype in self.GRAPH_TYPES:
            sub_dir = self.get_sub_dir(output_dir, gtype)
            counter_path = os.path.join(sub_dir, "opc_counter.json")
            occurs_path = os.path.join(sub_dir, "opc_occurs.json")
            if os.path.exists(counter_path) and os.path.exists(occurs_path):
                with open(counter_path, "r") as f:
                    opc_counters[gtype] = json.load(f)
                with open(occurs_path, "r") as f:
                    opc_occurs[gtype] = json.load(f)
                    num_func = opc_occurs[gtype]["num_funcs"]
                cached[gtype] = True
            else:
                opc_counters[gtype], opc_occurs[gtype] = (dict([
                    ('opc', Counter()),
                    ('val', Counter()),
                    *[(f'{arch}_reg', Counter()) for arch in ['mips', 'arm', 'x']],
                ]) for _ in range(2))
                cached[gtype] = False

        any_cached = sum(cached[gtype] for gtype in self.GRAPH_TYPES) != 0
        not_cached_graph_types = list(g for g in self.GRAPH_TYPES if not cached[g])

        if len(not_cached_graph_types) != 0:
            # Collect opc stats info
            for f_json in tqdm(os.listdir(input_folder)):
                if not f_json.endswith(".json"):
                    continue

                json_path = os.path.join(input_folder, f_json)
                with open(json_path) as f_in:
                    jj = json.load(f_in)

                arch = f_json.split('-')[0][:-2]
                idb_path = list(jj.keys())[0]
                j_data = jj[idb_path]
                for key in ['arch']:
                    if key in j_data:
                        del j_data[key]

                # Iterate over each function
                for fva in j_data:
                    for gtype in not_cached_graph_types:
                        opc_sets = defaultdict(set)
                        fva_data = j_data[fva][gtype]
                        # Iterate over each basic-block
                        for bb in fva_data['nverbs']:
                            nverb = fva_data['nverbs'][bb]
                            # print(nverb)
                            # important
                            for ty, opc in self.process_nverb(gtype, [nverb], arch):
                                opc_counters[gtype][ty].update([opc])
                                opc_sets[ty].add(opc)
                        for ty, opc_set in opc_sets.items():
                            opc_occurs[gtype][ty].update(opc_set)
                if not any_cached:
                    num_func += len(j_data)

            # Cache results
            for gtype in not_cached_graph_types:
                sub_dir = self.get_sub_dir(output_dir, gtype)
                output_path = os.path.join(sub_dir, "opc_counter.json")
                with open(output_path, "w") as f:
                    json.dump(opc_counters[gtype], f)
                output_path = os.path.join(sub_dir, "opc_occurs.json")
                opc_occurs[gtype]["num_funcs"] = num_func
                with open(output_path, "w") as f:
                    json.dump(opc_occurs[gtype], f)

        # Assigning each word an ID
        print(f"num funcs: {num_func}")
        for gtype in self.GRAPH_TYPES:
            idmaps[gtype] = {'padding': 0} if gtype.endswith("_INST") else {}
        for gtype, opc_cnts in opc_counters.items():
            print(f"[D] Processing {gtype}. ")
            # TODO what is it mean
            ths = dict([
                ('opc', 55),
                ('val', 0.01),
                *[(f'{arch}_reg', 0.01) for arch in ['mips', 'arm', 'x']],
            ])  # thresholds
            # TODO what is it mean
            for ty, opc_cnt in opc_cnts.items():
                if ty.endswith("_occur"):
                    continue
                if not isinstance(opc_cnt, dict):
                    opc_cnt = [(k, v) for k, v in opc_cnt.most_common()]
                else:
                    opc_cnt = sorted(list(opc_cnt.items()), key=lambda k: k[1], reverse=True)
                mapped_cnt = 0
                tot_cnt = sum([v for _, v in opc_cnt])
                idmaps[gtype][ty] = len(idmaps[gtype])
                start_id = len(idmaps[gtype])
                for i, (k, v) in enumerate(opc_cnt):
                    idmaps[gtype][k] = i + start_id
                    mapped_cnt += v
                    if isinstance(ths[ty], float):
                        if not freq_mode and mapped_cnt / tot_cnt > ths[ty]:
                            break
                        elif freq_mode and v / num_func < ths[ty]:
                            break
                    elif isinstance(ths[ty], int) and i + 1 >= ths[ty]:
                        break
                # TODO what is it mean
                print("[D] Found: {} mnemonics.".format(len(opc_cnt)))
                print("[D] Num of mnemonics mapped: {}".format(len(idmaps[gtype]) - start_id))
            print("[D] Tot Num of mnemonics mapped: {}".format(len(idmaps[gtype])))
        return idmaps

    def create_functions_dict(self, input_folder, opc_dicts, dump_str, dump_pkl):
        """
        Convert each function into a graph with embedded features.

        Args:
            input_folder: a folder with JSON files from ghidra analyzer
            opc_dict: dictionary that maps most common opcodes to their ranking.
            dump_str: bool
            dump_pkl: bool

        Return
            dict: map each function to a graph and features matrix
        """
        str_func_dict = {g: defaultdict(dict) for g in self.GRAPH_TYPES} if dump_str else {}
        pkl_func_dict = {g: defaultdict(dict) for g in self.GRAPH_TYPES} if dump_pkl else {}
        args = []

        # get all json files' paths
        for f_json in os.listdir(input_folder):
            if not f_json.endswith(".json"):
                continue
            json_path = os.path.join(input_folder, f_json)
            args.append((json_path, opc_dicts, dump_str, dump_pkl))

        # call process_one_file for all the jsons
        for idb_path, str_func_one, pkl_func_one in \
                tqdm(map(self.process_one_file, args), total=len(args)):
            if dump_str:
                for gtype, data in str_func_one.items():
                    str_func_dict[gtype][idb_path] = data
            if dump_pkl:
                for gtype, data in pkl_func_one.items():
                    pkl_func_dict[gtype][idb_path] = data
        return str_func_dict, pkl_func_dict



    '''
    main function
    intput_dir: A directory that contains JSON-formed SOG/ISCG/TSCG/ACFG. 
    training: In training mode, this script generates a new token mapping. 
    freq_mode: In frequency mode, the number of tokens to map is determined by the frequency of occurrence of the token, rather than by a predefined number/ratio. 
    opcodes_json: opcodes_dict.json, Token mapping result file name.
    
    two key function: 
        token_mapping
        create_functions_dict
    '''
    def processGraph(self):  # main
        input_dir = self.inputPath
        training = self.training
        freq_mode = self.freq_mode
        opcodes_json = self.opcodes_json
        output_dir = self.outputPath
        dataset = self.dataset
        out_format = self.out_format


        # Create output directory if it doesn't exist
        if not os.path.isdir(output_dir):
            os.makedirs(output_dir)

        # token mapping
        opc_dicts = {}
        if training:
            # Conduct token mapping and save results.
            # node attr to index, such as "LOAD" -> "1"
            ''' important'''
            opc_dicts = self.token_mapping(
                input_dir, output_dir, freq_mode)

            # iter for each graph type
            for gtype in self.GRAPH_TYPES:
                sub_dir = self.get_sub_dir(output_dir, gtype)
                output_path = os.path.join(sub_dir, opcodes_json)
                with open(output_path, "w") as f_out:
                    json.dump(opc_dicts[gtype], f_out)
        else:
            # Load previous token mapping results.
            for gtype in self.GRAPH_TYPES:
                sub_dir = self.get_sub_dir(output_dir, gtype)
                json_path = os.path.join(sub_dir, opcodes_json)
                if not os.path.isfile(json_path):
                    print("[!] Error loading {}".format(json_path))
                    return
                with open(json_path) as f_in:
                    opc_dict = json.load(f_in)
                opc_dicts[gtype] = opc_dict

        # dump embedded graph file in json (for read) or pkl (for model training)
        dump_str = out_format == "json" or out_format == "both"
        dump_pkl = out_format == "pkl" or out_format == "both"

        # get embedded graph
        ''' important'''
        str_dict, pkl_dict = self.create_functions_dict(
            input_dir, opc_dicts, dump_str, dump_pkl)

        # dump into json
        for gtype, g_str_dict in str_dict.items():
            o_json = "graph_func_dict_opc_{}.json".format(freq_mode)  # output json name
            sub_dir = self.get_sub_dir(output_dir, gtype, dataset)
            output_path = os.path.join(sub_dir, o_json)
            with open(output_path, 'w') as f_out:
                # print(g_str_dict)
                json.dump(g_str_dict, f_out, default=self.handle_ndarray)

        # dump into pkl
        for gtype, g_pkl_dict in pkl_dict.items():
            o_json = "graph_func_dict_opc_{}.pkl".format(freq_mode)
            sub_dir = self.get_sub_dir(output_dir, gtype, dataset)
            output_path = os.path.join(sub_dir, o_json)
            with open(output_path, 'wb') as f_out:
                pickle.dump(g_pkl_dict, f_out)



class PreprocessGraphWrap(PreprocessGraph):
    def __init__(self, input_dir, training, freq_mode, opcodes_json, output_dir, dataset, out_format, gtype):
        # super(PreprocessGraph, self).__init__()
        super().__init__(input_dir, training, freq_mode, opcodes_json, output_dir, dataset, out_format, gtype)


    # a dispatcher to deal with the nverbs in json
    # in inst level, a nverb is like (register, 0x65, 1) INT_NOTEQUAL (register, 0x20, 4) , (const, 0x0, 4)
    # in op-level, it is (register, 0x65, 1) or INT_NOTEQUAL
    # fixme nverb is a list in hermes, but in inst or op graph is just a string
    # fixme [(register, 0x65, 1)], (register, 0x65, 1)
    def process_nverb(self, gtype, nverb: list, arch):
        assert isinstance(nverb, list)
        if len(nverb) == 0:
            return []
        if gtype == 'PDG_INST' or gtype == 'CFG_INST':
            parsed = self.parse_pcode(nverb[0])
            # parsed: (COPY, [(register, 0x20, 4), (const, 0x0, 4)]
            return self.normalize_pcode(parsed, arch)
            # return [('opc', COPY), ('val', ...), ('val', ...)]
        elif gtype == 'PDG_OP':
            # varnode
            if '(' == nverb[0][0]:
                ty, opc = self.normalize_pcode_opr(self.parse_nxopr(nverb[0])[0], arch)
            # opcode
            else:
                ty, opc = 'opc', nverb[0]
            return [(ty, opc), ]
        else:
            assert "Unkown Graph Type"

    def create_graph(self, fva_data, fva, path):

        nodes, edges = fva_data['nodes'], fva_data['edges']
        # print(fva + "\n" + path)
        G = nx.MultiDiGraph()
        for node in nodes:
            # if(node is None):
            #     print(fva+"\n"+path)
            G.add_node(node)
        for edge in edges:
            # fixme bug in ghidra cfg graph builder
            if(edge[0] is None):
                G.add_edge(edge[1], edge[1], weight=edge[2])
            else:
                G.add_edge(edge[0], edge[1], weight=edge[2])

        nodelist = list(G.nodes())
        adj_mat = nx.to_scipy_sparse_array(
            G, nodelist=nodelist, dtype=np.int8, format='coo')

        # TODO POS encode in hermes

        return adj_mat, nodelist

    def process_one_file(self, args):
        json_path, opc_dicts, dump_str, dump_pkl = args
        with open(json_path) as f_in:
            jj = json.load(f_in)
        f_json = os.path.basename(json_path)
        arch = f_json.split('-')[0][:-2]
        idb_path = list(jj.keys())[0]
        # print("[D] Processing: {}".format(idb_path))
        str_func_dict, pkl_func_dict = defaultdict(dict), defaultdict(dict)
        j_data = jj[idb_path]
        for key in ['arch', 'failed_functions', 'overrange_functions', 'underrange_functions']:
            if key in j_data:
                del j_data[key]

        # Iterate over each function
        # fixme gtype and GRAPH_TYPES
        for fva in j_data:
            for gtype in self.GRAPH_TYPES:
                fva_data = j_data[fva][gtype]
                g_coo_mat, nodes = self.create_graph(fva_data, fva, json_path)
                f_list = self.create_features_matrix(
                    nodes, fva_data, opc_dicts[gtype], gtype, arch)
                if not fva.startswith("0x"):
                    fva = hex(int(fva, 10))
                if dump_str:
                    str_func_dict[gtype][fva] = {
                        'graph': self.coo_matrix_to_str(g_coo_mat),
                        'opc': f_list
                    }
                if dump_pkl:
                    pkl_func_dict[gtype][fva] = {
                        'graph': self.coo2tuple(g_coo_mat),
                        'opc': f_list
                    }

        return idb_path, str_func_dict, pkl_func_dict

    def create_features_matrix(self, node_list, fva_data, opc_dict, gtype, arch):
        """
        Create the matrix with numerical features.

        Args:
            node_list: list of basic-blocks addresses
            fva_data: dict with features associated to a function
            opc_dict: selected opcodes.

        Return
            np.matrix: Numpy matrix with selected features.
        """
        # assert gtype in ['SOG', 'TSCG', 'ISCG', 'ACFG']
        if gtype.endswith("_OP"):
            opcs = []
            for node_fva in node_list:
                assert str(node_fva) in fva_data['nverbs']
                node_data = fva_data['nverbs'][str(node_fva)]
                for ty, nopc in self.process_nverb(gtype, [node_data], arch):
                    if nopc in opc_dict:
                        opcs.append(opc_dict[nopc])
                    else:
                        opcs.append(opc_dict[ty])
            asms = opcs
        elif gtype.endswith("_INST"):
            asms = []
            I_SIZE = 12
            PADDING = opc_dict['padding']
            assert PADDING == 0
            for node_fva in node_list:
                opcs = np.zeros(I_SIZE, dtype=np.uint16)
                node_data = fva_data['nverbs'].get(str(node_fva), [])
                for i, (ty, nopc) in enumerate(self.process_nverb(gtype, [node_data], arch)):
                    if i >= I_SIZE:
                        break
                    if nopc in opc_dict:
                        opcs[i] = opc_dict[nopc]
                    else:
                        opcs[i] = opc_dict[ty]
                asms.append(opcs)

        return np.array(asms, dtype=np.uint16)


if __name__ == '__main__':

    dataset_folder = "0918"

    # preprocessor = PreprocessGraphWrap("../DBs/Dataset-1/" + dataset_folder + "/training/output/", True, True,
    #                           "opcodes_dict.json", "../inputs/" + dataset_folder + "/",
    #                           "Dataset-1_training", "both", ["PDG_INST", "PDG_OP"])
    #
    preprocessor = PreprocessGraphWrap("../DBs/Dataset-1/" + dataset_folder + "/validation/output/", False, True,
                              "opcodes_dict.json", "../inputs/" + dataset_folder + "/",
                              "Dataset-1_validation", "both", ["PDG_INST", "PDG_OP", "CFG_INST"])

    # preprocessor = PreprocessGraphWrap("../DBs/Dataset-1/" + dataset_folder + "/training/output/", True, True,
    #                           "opcodes_dict.json", "../inputs/" + dataset_folder + "/",
    #                           "Dataset-1_training", "both", ["PDG_INST", "PDG_OP", "CFG_INST"])


    preprocessor.processGraph()


