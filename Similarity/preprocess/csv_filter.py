import pickle
import pandas as pd

def load_pkl(fp):
    with open(fp, "rb") as f:
        return pickle.load(f)

def get_funcdict_from_pkl(pkl_path):
    pkl = load_pkl(pkl_path)
    # print(x)
    func_dict = {}
    for idb_path in pkl:
        func_dict[idb_path] = []
        for fva in pkl[idb_path]:
            func_dict[idb_path].append(fva)
    return func_dict

def filter_csv(csv_path, pkl_path, output_path):
    func_dict = get_funcdict_from_pkl(pkl_path)
    # print(func_dict)
    print("csv_path: "+csv_path)
    print("pkl_path: "+pkl_path)
    print("output_path: "+output_path)

    func_path = csv_path
    func_list = pd.read_csv(func_path, index_col=0)

    # 定义过滤条件函数
    def should_keep_row(row):
        idb_path = row['idb_path']
        fva = row['fva']
        # print(func_dict[idb_path])
        return idb_path in func_dict and fva in func_dict[idb_path]

    # 使用条件函数过滤DataFrame
    filtered_df = func_list[func_list.apply(should_keep_row, axis=1)]
    filtered_df.reset_index(drop=True, inplace=True)
    print(filtered_df)

    # 将过滤后的DataFrame保存到新的CSV文件
    filtered_df.to_csv(output_path, index=True)

def filter_csv_pairs(csv_path, pkl_path, output_path):
    func_dict = get_funcdict_from_pkl(pkl_path)
    # print(func_dict)
    print("csv_path: "+csv_path)
    print("pkl_path: "+pkl_path)
    print("output_path: "+output_path)

    func_path = csv_path
    func_list = pd.read_csv(func_path, index_col=0)

    # 定义过滤条件函数
    def should_keep_row(row):
        idb_path = row['idb']
        fva = row['fva']
        # print(f'idb_path type: {type(idb_path)}, value: {idb_path}')
        # print(f'fva type: {type(fva)}, value: {fva}')
        # print(func_dict[idb_path])
        return idb_path in func_dict and fva in func_dict[idb_path]

    # 使用条件函数过滤DataFrame
    filtered_df = func_list[func_list.apply(should_keep_row, axis=1)]
    filtered_df.reset_index(drop=True, inplace=True)
    print(filtered_df)

    # 将过滤后的DataFrame保存到新的CSV文件
    filtered_df.to_csv(output_path, index=True)

if __name__ == '__main__':

    # 0711
    # filter_csv("../inputs/Dataset-1/training_Dataset-1.csv",
    #                  "../inputs/lddg_0711/pcode_lddg/Dataset-1_training/graph_func_dict_opc_True.pkl",
    #                  "./training_Dataset-1.csv")

    # filter_csv("../inputs/Dataset-1/validation_Dataset-1.csv",
    #            "../inputs/lddg_0711/pcode_lddg/Dataset-1_validation/graph_func_dict_opc_True.pkl",
    #            "./validation_Dataset-1.csv")

    name = "0918"

    # filter_csv_pairs("../inputs/Dataset-1/pairs/validation/validation_functions.csv",
    #            "../inputs/"+ name +"/pcode_pdg_inst/Dataset-1_validation/graph_func_dict_opc_True.pkl",
    #         "./validation_functions.csv")

    filter_csv("../inputs/Dataset-1/training_Dataset-1.csv",
                     "../inputs/" + name + "/pcode_pdg_inst/Dataset-1_training/graph_func_dict_opc_True.pkl",
                     "./training_functions.csv")


