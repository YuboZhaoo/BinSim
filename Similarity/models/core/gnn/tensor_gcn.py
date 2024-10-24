from torch_geometric.nn import GATConv, SAGPooling, GCNConv
from torch_geometric.nn import global_mean_pool as gap, global_max_pool as gmp
from torch_geometric.nn import MessagePassing
from torch_geometric.nn.conv.gcn_conv import GCNConv
from embedding_layer import EmbeddingLayer
from mlp_layer import MLPLayer

import torch.nn as nn
import torch
import torch.nn.functional as F
from typing import List, Tuple

class Tensor_GCN(MessagePassing):
    def __init__(self,
                 num_edge_types,
                 in_features,
                 out_features,
                 embedding_out_features,
                 embedding_num_classes,
                 dropout=0,
                 max_node_per_graph=50,
                 add_self_loops=False,
                 bias=True,
                 aggr="mean",
                 device="cpu"):
        super(Tensor_GCN, self).__init__(aggr=aggr)
        # params set
        self.num_edge_types = num_edge_types
        self.device = device
        self.dropout = dropout
        self.max_node_per_graph=max_node_per_graph
        # value embedding layer for variable name.
        self.value_embeddingLayer = EmbeddingLayer(embedding_num_classes,
                                                   in_features,
                                                   embedding_out_features,
                                                   device=device)

        self.MessagePassingNN = nn.ModuleList(
            [
                 MLPLayer(in_features=embedding_out_features,
                    out_features=out_features,device=device) for _ in range(self.num_edge_types)
            ]
        )

        self.gru_cell = torch.nn.GRUCell(input_size=embedding_out_features, hidden_size=out_features)
        

        self.lin = nn.Linear(out_features, out_features)
        self.conv1 = GATConv(out_features, out_features//8, add_self_loops=True, heads=8, concat=True)

    
    def forward(self, x, edge_list: List[torch.tensor], **kwargs):
        print("input node feature")
        print(x.shape)
        
        x_embedding = self.value_embeddingLayer(x)
        print("embedding node feature")
        print(x_embedding.shape)
        # Tensor GGNN 
        last_node_states = x_embedding


        '''
        each graph message passing
        record the tmp result
        sum the result
        give to the gru
        '''

        for _ in range(8):
            out_list = []
            cur_node_states = F.dropout(last_node_states, self.dropout, training=self.training)
            for i in range(len(edge_list)):
                edge = edge_list[i]
                print("edge")
                print(edge)
                if edge.shape[0] != 0 :
                    out_list.append(self.MessagePassingNN[i](cur_node_states, edge))
            print(len(out_list))
            print(out_list[0].shape)
            print("help")
            cur_node_states = sum(out_list)
            print(cur_node_states.shape)
            print("one epoch\n\n")
            new_node_states = self.gru_cell(cur_node_states, last_node_states)  # input:states, hidden
            last_node_states = new_node_states

        ggnn_out = last_node_states # shape: V, D
        print("ggnn output")
        print(ggnn_out.shape)



        # tensor GCN:
        cur_x = torch.cat([ggnn_out for _ in range(self.num_edge_types)], dim=0)  # 4V, D
        print("cur_x")
        print(cur_x.shape)
        print("edge_list")
        print(len(edge_list))
        print(edge_list[0].shape)
        loop_edge_list = self.matrix_loop_new(edge_list) # 4V, 4V
        print("loop_edge_list")
        print(loop_edge_list.shape)
        print(loop_edge_list)

        out = self.conv1(cur_x, loop_edge_list) # 4V, D
        print("out")
        print(out.shape)

        out = out.view(self.num_edge_types, x_embedding.shape[0], out.shape[-1])  # 4, V, D
        print("out2")
        print(out.shape)

        out = torch.sum(out, dim=0)  # V, D
        print("out3")
        print(out.shape)

        out = F.relu(out)
        print("out4")
        print(out.shape)

        out = self.lin(out)
        print("out5")
        print(out.shape)
    

        return out


    def matrix_transfer(self, edge, i, j):
        # edge: [[i],[j]]
        edge_new = edge.detach().clone()
        edge_new[0]+=i
        edge_new[1]+=j
        return edge_new

    def matrix_loop(self, edge_list):
        # do tensor dot
        assert len(edge_list) == 4
        A1, A2, A3, A4 = edge_list
        n = self.max_node_per_graph
        loop_edge_list = []
        loop_edge_list.append(A1)
        loop_edge_list.append(self.matrix_transfer(A2, n, 0))
        loop_edge_list.append(self.matrix_transfer(A3, 2*n, 0))
        loop_edge_list.append(self.matrix_transfer(A4, 3*n, 0))

        loop_edge_list.append(self.matrix_transfer(A4, 0, n))
        loop_edge_list.append(self.matrix_transfer(A1, n, n))
        loop_edge_list.append(self.matrix_transfer(A2, 2*n, n))
        loop_edge_list.append(self.matrix_transfer(A3, 3*n, n))

        loop_edge_list.append(self.matrix_transfer(A3, 0, 2*n))
        loop_edge_list.append(self.matrix_transfer(A4, n, 2*n))
        loop_edge_list.append(self.matrix_transfer(A1, 2*n, 2*n))
        loop_edge_list.append(self.matrix_transfer(A2, 3*n, 2*n))

        loop_edge_list.append(self.matrix_transfer(A2, 0, 3*n))
        loop_edge_list.append(self.matrix_transfer(A3, n, 3*n))
        loop_edge_list.append(self.matrix_transfer(A4, 2*n, 3*n))
        loop_edge_list.append(self.matrix_transfer(A1, 3*n, 3*n))

        return torch.cat(loop_edge_list, dim=1)

    def matrix_loop_new(self, edge_list):
        n = self.max_node_per_graph
        edge_nums = len(edge_list)
        loop_edge_list = []
        edge_start = 0
        for j in range(edge_nums):
            cur_edge_start = edge_start    
            for i in range(edge_nums):
                cur_edge_start %= edge_nums
                loop_edge_list.append(self.matrix_transfer(edge_list[cur_edge_start], i*n, j*n))
                cur_edge_start += 1
            edge_start-=1
            edge_start
        return torch.cat(loop_edge_list, dim=1)


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
        if(_ == 0):
            src = torch.randint(0, num_nodes, (2, 3), dtype=torch.long, device=device)
        else:
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