'''
    a script to generate p-code graph from binary
        envs: ghidra scripts in ghidra_scripts
        input:
            a folder with jsons -> a folder with txt (target fun address in it)

'''

import os
import json
from datetime import datetime
import sys
from tqdm import tqdm
import numpy as np
import multiprocessing


ghidra_home = "/home/yuboz/work/Programming/Bin/ghidra/ghidra_9.2.2_PUBLIC_20201229/ghidra_9.2.2_PUBLIC/"
# script_path = "/home/yuboz/work/Research/Binsim/GNN/moon0225/apollo/scripts/ghidra_scripts_apollo/"
script_path = "/home/yuboz/work/Research/Binsim/BinaryAnalysis/src/"
script_name = "BuildGraph.java"
output_path = "../DBs/Dataset-1/" + "0918" + '/'


def get_filenames(path):
    return os.listdir(path)


# path must end with "/"
def mod2path(mod = "train", err = "mod2path"):
    if mod == "train":
        mod = "training/"
    elif mod == "test":
        mod = "testing/"
    elif mod == "valid":
        mod = "validation/"
    elif mod == "debug":
        mod = "debug/"
    else:
        print("error in " + err)
        return None
    return mod


# read jsons from dataset1, output the target fun lists in txt
def get_funlists(jsons_path="../DBs/Dataset-1/features/training/acfg_disasm_Dataset-1_training/",
                      output_path="../DBs/Dataset-1/index/training/"):
    js_namelist = get_filenames(jsons_path)
    if not os.path.exists(output_path):
        os.makedirs(output_path)
    with open(output_path + "name_path.txt", "w") as map:
        for js_name in tqdm(js_namelist):
            with open(jsons_path + js_name, "r") as f:
                js = json.load(f)
                bin_path = ""
                target_funs = []
                for bin in js:
                    bin_path = bin
                    # if bin[-4:] != ".i64" :
                    #     print("error")
                    for fun in js[bin]:
                        if fun != "arch":
                            target_funs.append(fun)
                            # print(fun)
                    target_bin_path = "../" + bin[:-4]
                print(target_bin_path)
                print(target_funs)

                target_info_name = target_bin_path.split("/")[-1] + ".txt"
                with open(output_path + target_info_name, 'w') as tmp_f:
                    map.write(target_info_name+" "+bin_path + "\n")
                    for fun in target_funs:
                        # tmp_f.write(fun)
                        tmp_f.write(fun + "\n")


def get_funlists_driver(mod="train"):
    mod = mod2path(mod, "get_funlists_driver")
    if mod is None: return

    if not os.path.exists("../DBs/Dataset-1/index/"):
        os.makedirs("../DBs/Dataset-1/index/")
    if not os.path.exists("../DBs/Dataset-1/index/"+mod):
        os.makedirs("../DBs/Dataset-1/index/"+mod)

    get_funlists("../DBs/Dataset-1/features/"+mod+"acfg_disasm_Dataset-1_"+mod,
                 "../DBs/Dataset-1/index/"+mod+"/")


def run_ghidra(ctt, output_path, index_path, prj_path, ghidra_home, script_path, graph_type):
    for line in tqdm(ctt):
        txt_name = line.split(" ")[0]
        funlist_path = index_path + txt_name
        bin_path = "../" + line.split(" ")[1][:-5]
        idb_path = line.split(" ")[1][:-1]
        print(txt_name, " ", bin_path)
        ghidra_headless_cmd = ghidra_home + "support/analyzeHeadless " + prj_path + " " + "test_dataset1_0911 " + \
                              "-import " + bin_path + " -overwrite -scriptPath " + script_path + \
                              " -postScript  "+script_name+ " "+ \
                              txt_name[:-4] + " " + funlist_path + " " + output_path+ " " + idb_path + " " + graph_type
        print(ghidra_headless_cmd)
        os.system(ghidra_headless_cmd)
        if not os.path.exists(output_path + "output/" + txt_name[:-4] + ".json"):
            with open(output_path + "python_error.txt", 'a') as errlog:
                errlog.write("ghidra failed in " + txt_name + " \n")


def getGraphMultithread(mod="train", p_num=16, graph_type='PDG'):
    mod = mod2path(mod, "get_"+graph_type+"_wt_list")
    if mod is None: return

    global output_path
    output_path = output_path + mod

    prj_path = output_path + "prj/"
    index_path = "../DBs/Dataset-1/index/"+mod+"/"

    if not os.path.exists(output_path):
        os.makedirs(output_path)
    if not os.path.exists(output_path+"output/"):
        os.makedirs(output_path+"output/")
    if not os.path.exists(prj_path):
        os.makedirs(prj_path)

    with open("../DBs/Dataset-1/index/"+mod+"/name_path.txt", 'r') as txt:
        ctt = txt.readlines()
        ctt_spl = np.array_split(ctt, p_num)
        print(ctt_spl)
        pool = multiprocessing.Pool(processes=p_num)
        for i in range(p_num):
            trd_path = prj_path+"thread"+str(i)+"/"
            if not os.path.exists(trd_path):
                os.makedirs(trd_path)
            pool.apply_async(run_ghidra, args=(ctt_spl[i], output_path, index_path, trd_path, ghidra_home, script_path, graph_type))
        # 关闭进程池，不再接受新的任务
        pool.close()
        # 等待所有进程完成
        pool.join()


# used for test graph build script
def getGraphSingle(mod="train", name = "mips64-gcc-5-O2_libz.so.1.2.11", graph_type="PDG_INST+PDG_OP+CFG_INST", folder = "openssl"):
    mod = mod2path(mod, "get_pdg_wt_list")
    if mod is None: return

    output_path = "../DBs/Dataset-1/test/" + mod
    prj_path = output_path + "prj/"


    if not os.path.exists(output_path):
        os.makedirs(output_path)
    if not os.path.exists(output_path + "output/"):
        os.makedirs(output_path + "output/")
    if not os.path.exists(prj_path):
        os.makedirs(prj_path)


    bin_path = "../IDBs/Dataset-1/"+folder +"/"+name
    txt_name = name+".txt"
    index_path = "../DBs/Dataset-1/index/"+mod+"/"
    funclist_path = index_path + txt_name
    idb_path = "IDBs/Dataset-1/"+ folder +"/"+name+".i64"

    ghidra_headless_cmd = (ghidra_home + "support/analyzeHeadless " + prj_path + " " + "test_dataset1_0405 " +
                           "-import " + bin_path + " -overwrite -scriptPath " + script_path +" -postScript " +script_name+ " " +
                           txt_name[:-4] + " " + funclist_path + " " + output_path + " " + idb_path + " " + graph_type)
    print(ghidra_headless_cmd)
    os.system(ghidra_headless_cmd)
    if not os.path.exists(output_path + "output/" + txt_name[:-4] + ".json"):
        with open(output_path + "error.txt", 'a') as errlog:
            errlog.write("ghidra failed in "+txt_name+" idb: "+idb_path+" \n")

def reGen():
    with open("../DBs/Dataset-1/0918/"+"testing"+"/python_error.txt", 'r') as txt:
        lines = txt.readlines()

    for line in lines:
        filename = line.split(" ")[-2][:-4]
        print(filename)
        if(filename.endswith("z3")):
            getGraphSingle("test", filename, "PDG_INST+PDG_OP+CFG_INST", "z3")
        else:
            getGraphSingle("test", filename, "PDG_INST+PDG_OP+CFG_INST", "nmap")

if __name__ == '__main__':
    # get_funlists_driver("train")

    # getGraphMultithread("train", 16, 'PDG_INST+PDG_OP+CFG_INST')
    # getGraphMultithread("valid", 16, 'PDG_INST+PDG_OP+CFG_INST')
    # getGraphMultithread("test", 16, 'PDG_INST+PDG_OP+CFG_INST')

    # getGraphSingle("valid", "arm64-gcc-5-O1_minigzip64","PDG_INST+PDG_OP+CFG_INST", "zlib")
    # getGraphSingle("train", "x86-clang-7-O3_libssl.so.3", "PDG_INST+PDG_OP+CFG_INST", "openssl")
    getGraphSingle("train", "mips64-gcc-4.8-O3_ossltest.so", "PDG_INST+PDG_OP+CFG_INST", "openssl")

    # reGen()
