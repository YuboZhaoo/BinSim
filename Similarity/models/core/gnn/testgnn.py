
from tensor_gcn import Tensor_GCN

import torch

def main():
    # 配置参数
    num_edge_types = 4
    in_features = 16
    out_features = 32
    embedding_out_features = 32
    embedding_num_classes = 70
    dropout = 0.5
    max_node_per_graph = 10
    device = 'cpu'

    # 创建模型实例
    model = Tensor_GCN(
        num_edge_types=num_edge_types,
        in_features=in_features,
        out_features=out_features,
        embedding_out_features=embedding_out_features,
        embedding_num_classes=embedding_num_classes,
        dropout=dropout,
        max_node_per_graph=max_node_per_graph,
        device=device
    ).to(device)

    # 创建假数据
    num_nodes = max_node_per_graph
    num_edges = num_edge_types

    # 随机生成节点特征（整数索引），这里假设每个节点特征是一维的
    # 维度应为 [num_nodes]，每个值代表一个类别索引
    x = torch.randint(0, embedding_num_classes, (num_nodes,in_features), dtype=torch.long, device=device)
    print(x)

    # 随机生成边列表
    edge_list = []
    for _ in range(num_edge_types):
        src = torch.randint(0, num_nodes, (2, num_nodes), dtype=torch.long, device=device)
        edge_list.append(src)
    print("edge init")
    print(edge_list)

    # 将边列表转换为边索引
    edge_indices = [edge_list[i] for i in range(num_edge_types)]

    # 执行前向传播
    model.eval()
    with torch.no_grad():
        output = model(x, edge_indices)

    print("Model output:")
    print(output.shape)

if __name__ == "__main__":
    main()
