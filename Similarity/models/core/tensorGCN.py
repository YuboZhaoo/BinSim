from typing import Optional
from torch_geometric.nn import GATConv, SAGPooling, GCNConv
from torch_geometric.nn import global_mean_pool as gap, global_max_pool as gmp
from torch_geometric.nn import MessagePassing
from torch_geometric.nn.conv.gcn_conv import GCNConv
import torch.nn as nn
import torch
import torch.nn.functional as F
from typing import List


from torch import Tensor
from torch import nn
from torch_geometric.nn import Linear
from torch.nn import Parameter as Param

from torch_geometric.nn.conv import MessagePassing
from torch_geometric.typing import Adj
from torch_geometric.nn.aggr.utils import (
    MultiheadAttentionBlock,
)

from .mlp import *

# class MLPLayer_v0(MessagePassing):
#     def __init__(self, in_features, out_features):
#         """MPLayer  norm Message Passing layer for gnn.
#
#         Args:
#             in_feature ([type]): in_feature
#             out_features ([type]): out_features
#             device ([type]): device
#         """
#         super(MLPLayer_v0, self).__init__(aggr='add')  # "Add" aggregation (Step 5).
#         self.linear_0 = nn.Linear(in_features, out_features)
#         self.linear = nn.Linear(out_features, out_features)
#
#     def forward(self, x, edge_index):
#         x = self.linear_0(x)
#         x = self.linear(x)
#         return self.propagate(edge_index, x=x)
#
#     @torch.jit.ignore
#     def reset_parameters(self):
#         self.linear_0.reset_parameters()
#         self.linear.reset_parameters()

class MessageNet(MessagePassing):
    def __init__(self, out_channels: int, n_message_net_layers: int,
                 aggr: str = 'add', aggr_kwargs={}, bias: bool = True, **kwargs):
        super().__init__(aggr=aggr, aggr_kwargs=aggr_kwargs, **kwargs)

        meg_channels = out_channels * 2

        self.message_net = MLP(n_message_net_layers, meg_channels, acti_fini=False)
        self.rev_message_net = MLP(n_message_net_layers, meg_channels, acti_fini=False)

    @torch.jit.ignore
    def reset_parameters(self):
        self.message_net.reset_parameters()
        self.rev_message_net.reset_parameters()

    def forward(self, x: Tensor, edge_index: Adj, edge_feat: Tensor) -> Tensor:
        """"""
        nnodes = x.shape[0]
        # propagate_type: (x: Tensor, edge_feat: Tensor)
        # x = self.propagate(edge_index, x=x, size=(nnodes, nnodes), edge_feat=edge_feat)
        x = self.propagate(edge_index, x=x, size=(nnodes, nnodes), edge_feat=edge_feat)

        return x

    def message(self, x_i: Tensor, x_j: Tensor, edge_feat: Tensor):
        return torch.stack(
            (self.message_net(torch.cat((x_i, x_j, edge_feat), dim=1)),
             self.rev_message_net(torch.cat((x_j, x_i, edge_feat), dim=1))))

    def aggregate(self, inputs: Tensor, edge_index_i: Tensor, edge_index_j: Tensor,
                  ptr: Optional[Tensor] = None,
                  dim_size: Optional[int] = None) -> Tensor:
        return self.aggr_module(inputs[0], edge_index_i, ptr=ptr, dim_size=dim_size,
                                dim=self.node_dim) + \
            self.aggr_module(inputs[1], edge_index_j, ptr=ptr, dim_size=dim_size,
                             dim=self.node_dim)


class TensorGraphConv(MessagePassing):

    def __init__(self, out_channels: int, n_message_net_layers: int,
                 aggr: str = 'add', aggr_kwargs={}, bias: bool = True, **kwargs):
        super().__init__(aggr=aggr, aggr_kwargs=aggr_kwargs, **kwargs)

        meg_channels = out_channels * 2
        self.num_edge_types = 2

        self.message_net_1 = MessageNet(out_channels, n_message_net_layers, aggr, aggr_kwargs).jittable()
        self.message_net_2 = MessageNet(out_channels, n_message_net_layers, aggr, aggr_kwargs).jittable()


        self.rnn = torch.nn.GRUCell(meg_channels, out_channels, bias=bias)

        self.out_channels = out_channels
        self.reset_parameters()

    @torch.jit.ignore
    def reset_parameters(self):
        # self.message_net.reset_parameters()
        # self.rev_message_net.reset_parameters()

        self.message_net_1.reset_parameters()
        self.message_net_2.reset_parameters()
        self.rnn.reset_parameters()

        # for i in range(self.num_edge_types):
        #     self.MessagePassingNN[i].reset_parameters()

    def forward(self, x: Tensor, edge_index_list: List[Tensor],
                edge_feat_list: List[Tensor]) -> Tensor:
        """"""
        # nnodes = x.shape[0]
        # propagate_type: (x: Tensor, edge_feat: Tensor)
        # m = self.propagate(edge_index, x=x, size=(nnodes, nnodes), edge_feat=edge_feat)
        # x = self.rnn(m, x)

        # print(edge_feat_list)
        # print(edge_feat_list[0].shape)
        # print(edge_feat_list[1].shape)
        # exit(0)

        last_node_states = x
        out_list = []
        cur_node_states = F.dropout(last_node_states, 0.1, training=True)
        for i in range(len(edge_index_list)):
            edge = edge_index_list[i]
            if edge.shape[0] != 0:
                if i == 0:
                    out_list.append(self.message_net_1(cur_node_states, edge, edge_feat_list[i]))
                else:
                    out_list.append(self.message_net_2(cur_node_states, edge, edge_feat_list[i]))
                # out_list.append(self.propagate(edge, x=x, size=(nnodes, nnodes), edge_feat=edge_feat))

        cur_node_states = torch.sum(torch.stack(out_list), dim=0)
        new_node_states = self.rnn(cur_node_states, last_node_states)  # input:states, hidden

        x = new_node_states

        return x

    def __repr__(self) -> str:
        return (f'{self.__class__.__name__}({self.out_channels})')


class GTCN(torch.nn.Module):
    def __init__(self, encoder, aggr, n_node_feat_dim, n_edge_feat_dim, layer_groups, n_message_net_layers, skip_mode,
                 output_mode, num_query, n_atte_layers, layer_aggr="add", layer_aggr_kwargs={}, concat_skip=1):
        super().__init__()
        # Message Passing layers
        self.convs = torch.nn.ModuleList()
        self.skips = torch.nn.ParameterList()
        for num_layers in layer_groups:
            group_convs = torch.nn.ModuleList()
            for i in range(num_layers):
                if i == 0:
                    conv = TensorGraphConv(n_node_feat_dim,
                                          n_message_net_layers,
                                          layer_aggr,
                                          layer_aggr_kwargs).jittable()
                else:
                    conv = group_convs[0]
                group_convs.append(conv)
            self.convs.append(group_convs)
            self.skips.append(Param(torch.Tensor(1 if skip_mode == 2 else 0)))

        self.softmax = nn.Softmax(dim=-1)
        if output_mode == 2:
            self.pos_embeds = nn.Embedding(len(layer_groups) + 1, n_node_feat_dim)
            self.mab = MultiheadAttentionBlock(
                n_node_feat_dim, 1, True, 0.)
        else:
            self.pos_embeds = None
            self.mab = None

        self.encoder = encoder
        self.aggr = aggr
        # Misc
        self._skip_mode = skip_mode
        self._output_mode = output_mode
        self._concat_skip = concat_skip
        # init
        self.reset_parameters()
        return

    @torch.jit.ignore
    def reset_parameters(self):
        for group_convs, skip_beta in zip(self.convs, self.skips):
            group_convs[0].reset_parameters()
            skip_beta.data.fill_(0.0)

    def forward(self, x, edge_index, edge_feat, graph_idx, batch_size: int):
        x, e = self.encoder(x, edge_feat, graph_idx)

        # new for gtcn
        edge_type_0_indices = torch.nonzero(edge_feat == 0).squeeze(1)
        edge_type_1_indices = torch.nonzero(edge_feat == 1).squeeze(1)

        edge_index_0 = edge_index[:, edge_type_0_indices]
        edge_feat_0 = e[edge_type_0_indices, :]

        edge_index_1 = edge_index[:, edge_type_1_indices]
        edge_feat_1 = e[edge_type_1_indices, :]

        # del e
        # del edge_type_0_indices
        # del edge_type_1_indices

        edge_index_list = [edge_index_0, edge_index_1]
        edge_feat_list = [edge_feat_0, edge_feat_1]
        #

        out_feats = [x]
        for group_convs, skip_beta in zip(self.convs, self.skips):
            skip_input = x
            for conv in group_convs:
                x = conv(x, edge_index_list, edge_feat_list)
            if self._output_mode > 0:
                out_feats.append(x)
            if self._skip_mode == 2:
                alpha = skip_beta.sigmoid()
                x = x * (1 - alpha) + skip_input * alpha
            elif self._skip_mode == 1:
                x += skip_input
        if self.pos_embeds is not None and self.mab is not None:
            x = torch.stack(out_feats[1:], dim=1)
            # x: n_node x n_layer x n_channel
            pos = torch.tensor(list(range(x.shape[1])),
                               dtype=torch.int, device=x.device)
            x = x + self.pos_embeds(pos)
            x = self.mab(out_feats[0].unsqueeze(1), x, x_mask=None, y_mask=None).squeeze(1)
        elif self._output_mode == 1:
            x = torch.cat(out_feats[self._concat_skip:], dim=-1)
        return self.aggr(x, graph_idx, batch_size)
