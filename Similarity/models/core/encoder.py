import torch.nn as nn
import torch
import numpy as np
from torch.nn.utils.rnn import pad_sequence, pack_padded_sequence, pad_packed_sequence

from .mlp import *

class EmbeddingEncoder(torch.nn.Module):
    def __init__(self, n_node_feat_dim, n_edge_feat_dim, n_node_attr, n_edge_attr, n_pos_enc):
        super().__init__()
        self.node_encoder = nn.Embedding(n_node_attr, n_node_feat_dim)
        self.edge_encoder = nn.Embedding(n_edge_attr, n_edge_feat_dim)
        self.pos_encoder = nn.Embedding(n_pos_enc, n_edge_feat_dim)
        self._n_pos_enc = n_pos_enc
        self._n_edge_attr = n_edge_attr

    def reset_parameters(self):
        reset_subnet(self.node_encoder)
        reset_subnet(self.edge_encoder)

    def forward(self, x, e, idx):
        x = self.node_encoder(x)
        if self._n_pos_enc > 0:
            # FIXME so strange, although n_edge_attr = 1, still two
            p = self.pos_encoder(e // self._n_edge_attr)
            e = self.edge_encoder(e % self._n_edge_attr)
            e += p
        else:
            e = self.edge_encoder(e)
        return x, e


'''
batch_inputs, label, batch_features = next(iter(training_set))
batch_inputs 5
        node features torch.Size([64943])
        edge torch.Size([2, 72799])
        edge features torch.Size([72799])
        graph id torch.Size([64943])
        len 160
'''
class GruEncoder(nn.Module):
    def __init__(self, c):
        super().__init__()
        self.word_embedding = nn.Embedding(c["embed_size"], c["embed_dim"], 0)
        self.edge_encoder = nn.Embedding(c["edge_attr"], c["edge_feat_dim"])
        self.rnn = nn.GRU(input_size=c["embed_dim"],
                                hidden_size=c["hidden_dim"],
                                num_layers=c["layers"],
                                bidirectional=True)

    def forward(self, node_features, edge_feat, graph_ids):

        e = self.edge_encoder(edge_feat)

        num_nodes = node_features.size(0)
        num_graphs = graph_ids.max().item() + 1

        # 将节点特征进行嵌入
        embedded_features = self.word_embedding(node_features)  # shape: (num_nodes, embedding_dim)

        # 对每个图的节点特征进行 padding
        graph_node_list = []
        for graph_id in range(int(num_graphs)):
            # 获取属于当前图的节点索引
            node_indices = torch.nonzero(graph_ids == graph_id).squeeze(1)
            # 取出对应的嵌入特征并加入列表
            graph_node_list.append(embedded_features[node_indices])

        # 对所有图的节点序列进行 padding
        padded_sequences = pad_sequence(graph_node_list, batch_first=True)

        # 获取每个图的节点序列长度
        lengths = torch.tensor([seq.size(0) for seq in graph_node_list])

        # 打包序列
        packed_sequences = pack_padded_sequence(padded_sequences, lengths, batch_first=True, enforce_sorted=False)

        # RNN 前向传播
        packed_output, _ = self.rnn(packed_sequences)

        # 解包 RNN 输出
        rnn_output, _ = pad_packed_sequence(packed_output, batch_first=True)

        # 还原节点特征，去除 padding
        valid_outputs = []
        for i, length in enumerate(lengths):
            valid_outputs.append(rnn_output[i, :length])

        # 将编码后的节点特征拼接成一个大张量
        encoded_features = torch.cat(valid_outputs, dim=0)

        return encoded_features, e



# class GruEncoder(nn.Module):
#     def __init__(self):
#         super().__init__()
#         self.word_embedding = nn.Embedding(521, 32, 0)
#         self.rnn = nn.GRU(input_size=32,
#                                 hidden_size=16,
#                                 num_layers=2,
#                                 bidirectional=True)
#
#     def forward(self, batch_inputs, batch_features):
#         # get ori lengths
#         lengths = [len(seq) for seq in batch_features]
#         lengths = torch.tensor(lengths)
#         # turn into tensor (no needed)
#         # input = [torch.tensor(seq.astype(np.int32)) for seq in batch_features]
#
#         # padding sequences
#         padded_sequences = torch.nn.utils.rnn.pad_sequence(batch_features, batch_first=True)
#         # print(padded_sequences.shape)
#
#         # embedding
#         padded_sequences = self.word_embedding(padded_sequences)
#         # print(padded_sequences.shape)
#
#         # packing
#         packed_sequences = pack_padded_sequence(padded_sequences, lengths, batch_first=True, enforce_sorted=False)
#
#         out, _ = self.rnn(packed_sequences)
#
#         # unpacking
#         out, _ = pad_packed_sequence(out, batch_first=True)
#
#         valid_outputs = []
#         for i, length in enumerate(lengths):
#             valid_outputs.append(out[i, :length])  # 只保留有效的时间步（去掉 padding）
#
#         # unfold
#         valid_outputs = torch.cat(valid_outputs, dim=0)
#
#         # fixme TypeError: 'tuple' object does not support item assignment
#         batch_inputs_list = list(batch_inputs)
#         batch_inputs_list[0] = valid_outputs
#
#         # print(batch_inputs_list)
#         # print(batch_inputs_list[0].shape)
#
#         return tuple(batch_inputs_list)