
import pandas as pd

from .graph_factory_base import *
from collections import defaultdict
from random import Random
from tqdm import tqdm


import torch
import torch.nn as nn
from torch.nn.utils.rnn import pack_padded_sequence, pad_packed_sequence

import logging
log = logging.getLogger('gnn')


class GraphFactoryTraining(GraphFactoryBase):

    def __init__(self, func_path, feat_path, batch_size,
                 max_num_nodes, max_num_edges, n_sim_funcs,
                 used_subgraphs, edge_feature_dim):
        """
            Args:
                func_path: CSV file with function pairs
                feat_path: JSON file with function features
                batch_size: size of the batch for each iteration
        """
        super(GraphFactoryTraining, self).__init__(
            GraphFactoryMode.TRAIN, feat_path, batch_size, used_subgraphs, edge_feature_dim)

        assert (n_sim_funcs & (n_sim_funcs - 1)) == 0
        self._n_sim_funcs = n_sim_funcs
        self._load_data(func_path)

        self._random = Random()
        self._random.seed(11)

        # Initialize the iterator
        self._get_next_pair_it = self._get_next_pair()

        # Number of pairs for the positive or negative DF.
        # Since this is a random batch generator, this number must be defined.
        self._num_funcs_in_epoch = 200000
        log.info("Tot num functions (training): {}".format(
            self._num_funcs_in_epoch))

        num_funcs_one_iter = self._batch_size * self._n_sim_funcs
        self._num_batches_in_epoch = self._num_funcs_in_epoch // num_funcs_one_iter
        log.info("Num batches in epoch (training): {}".format(
            self._num_batches_in_epoch))

        self._num_funcs_in_epoch = self._num_batches_in_epoch * num_funcs_one_iter
        log.info("Tot num functions per epoch (training): {}".format(
            self._num_funcs_in_epoch))
        self._num_pairs_in_epoch = self._num_funcs_in_epoch // 2
        self.epoh_counter = 0

        self._max_num_nodes = max_num_nodes
        self._max_num_edges = max_num_edges
        return

    def reset_seed(self, seed):
        self._random.seed(seed)

    def step(self):
        self.epoh_counter += 1

    def _load_data(self, func_path):
        """
        Load the training data (functions and features)

        Args
            func_path: CSV file with training functions
        """
        # Load CSV with the list of functions
        log.debug("Reading {}".format(func_path))
        # Read the CSV and reset the index
        self._df_func = pd.read_csv(func_path, index_col=0)
        self._df_func.reset_index(drop=True, inplace=True)

        # Get the list of indexes associated to each function name
        self._func_name_dict = defaultdict(list)
        for i, f in enumerate(tqdm(self._df_func.func_name)):
            self._func_name_dict[f].append(i)

        # Get the list of unique function name
        self._func_name_list = list(self._func_name_dict.keys())
        log.info("Found {} different functions".format(
            len(self._func_name_list)))

        self._pairable_func_name_dict = dict(
            [(k, v) for k, v in self._func_name_dict.items() if len(v) >= 2])
        self._pairable_func_name_list = list(
            self._pairable_func_name_dict.keys())
        log.info("Found {} pairable functions".format(
            len(self._pairable_func_name_list)))
        log.info("{} among them more than {} variants".format(
            len([(k, v) for k, v in self._func_name_dict.items()
                 if len(v) >= self._n_sim_funcs]), self._n_sim_funcs))

    def _select_random_function_pairs(self):
        """
        Return
            a tuple (pos_p, neg_p) where pos_p and neg_g are a tuple
            like (['idb_path_1', 'fva_1'], ['idb_path_2', 'fva_2'])
        """
        func_poll_one, func_poll_two = set(), set()

        while (1):
            # Get two random function names
            fn1, fn3 = self._random.sample(self._func_name_list, k=2)

            # Select functions with the same name
            func_poll_one = self._func_name_dict[fn1]

            # Select other functions with the same name
            func_poll_two = self._func_name_dict[fn3]

            # WARNING: there must be at least two binary functions for each
            #  function name, otherwise this will be an infinite loop.
            if len(func_poll_one) >= 2 and len(func_poll_two) >= 1:
                break

        idx1, idx2 = self._random.sample(func_poll_one, k=2)
        idx3 = self._random.sample(func_poll_two, k=1)[0]

        f1 = self._df_func.iloc[idx1][['idb_path', 'fva']]
        f2 = self._df_func.iloc[idx2][['idb_path', 'fva']]
        f3 = self._df_func.iloc[idx3][['idb_path', 'fva']]

        # Create the positive and the negative pairs
        pos_p = f1, f2
        neg_p = f1, f3
        return pos_p, neg_p

    def _graph_size(self, g):
        edge_size = len(g[0])
        node_size = g[3]
        return node_size, edge_size

    def _get_next_pair(self):
        """The function implements an infinite loop over the input data."""
        while True:
            log.info("Re-initializing the pair generation")

            for _ in range(self._num_pairs_in_epoch):
                while True:
                    ll = list()
                    pairs = self._select_random_function_pairs()
                    # Pairs contain a positive and a negative pair of functions
                    for pair in pairs:
                        g_list, f_list = list(), list()
                        # Each pair contain a left and right function
                        g_list = [self._fdict[idb][fva]['graph']
                                  for idb, fva in pair]
                        f_list = [self._fdict[idb][fva][self._features_type]
                                  for idb, fva in pair]
                        ll.append(tuple(g_list))
                        ll.append(tuple(f_list))
                    else:
                        break

                yield tuple(ll)

    def _batch_iter_wrap(self, get_batch_data):
        nlarge_batch = 0
        numIter = self._num_batches_in_epoch
        for _ in tqdm(range(numIter), total=numIter):
            '''
            batch_graphs[ [[from], [to], [edge_type], node_num, node_num]], [[from], [to], [edge_type], node_num, node_num]]]
            batch_features [ [[nodefeatures]], [[nodefeatures]] ]
            batch_labels [1,1,.....] len 40
            node_size  nodes num in batch
            '''
            batch_graphs, batch_features, batch_labels, node_size, edge_size = get_batch_data()
            # print("hahaha")
            # print(len(batch_graphs))
            # print(batch_graphs[0])

            # for gg in batch_graphs[0]:
            #     if isinstance(gg, int):
            #         print("  "+str(gg))
            #     else: print("      "+str(len(gg)))

            # for graph in batch_graphs:
            #     print(len(graph))
            #     for gg in graph:
            #         if isinstance(gg, int):
            #             print(gg)
            #         else: print("      "+str(len(gg)))
            # i = 0
            # for bb in batch_features:
            #     i+=len(bb)
            # print(i)
            # print(batch_features[0])
            # print(len(batch_features[0]))
            # print(len(batch_features))
            # print(batch_labels)
            # print(len(batch_labels))

            # print(node_size)
            # print(batch_labels)
            ## Limits tuned for 10GB GPU Memory
            while node_size > self._max_num_nodes or edge_size > self._max_num_edges:
                nlarge_batch += 1
                log.debug(
                    f"Skip {nlarge_batch}-th batch with size {(node_size, edge_size)})")
                batch_graphs, batch_features, batch_labels, node_size, edge_size = get_batch_data()

            packed_graphs = self._pack_batch(batch_graphs, batch_features)


            labels = torch.tensor(batch_labels, dtype=torch.int32)
            # batch_features = [torch.tensor(seq.astype(np.int32)) for seq in batch_features]

            # yield packed_graphs, labels, batch_features
            yield packed_graphs, labels

        log.info(f"{100 * nlarge_batch / numIter}% Large Batch Skipped. ")

    def _batch_triplets(self):
        k = self._n_sim_funcs

        def get_batch_data():
            def checked_get(func_idx):
                idb, fva = self._df_func.iloc[func_idx][['idb_path', 'fva']]
                try:
                    g = self._fdict[idb][fva]
                except:
                    log.debug(idb, fva)
                    return None
                node_size, edge_size = self._graph_size(g['graph'])
                if node_size == 0 or edge_size == 0:
                    return None
                return g

            def sample_variants(fn):
                lst = self._pairable_func_name_dict[fn]
                if len(lst) >= k:
                    return self._random.sample(lst, k=k)
                else:
                    return self._random.choices(lst, k=k)

            batch_graphs, batch_features = list(), list()
            node_size, edge_size = 0, 0
            fns = self._random.sample(
                self._pairable_func_name_list, self._batch_size)
            used_fns = set(fns)
            for fn in fns:
                idxs = sample_variants(fn)
                gs = [checked_get(i) for i in idxs]
                while any([g is None for g in gs]):
                    fn = self._random.choice(self._pairable_func_name_list)
                    while fn in used_fns:
                        fn = self._random.choice(self._pairable_func_name_list)
                    used_fns.add(fn)
                    idxs = sample_variants(fn)
                    gs = [checked_get(i) for i in idxs]
                for g in gs:
                    batch_graphs.append(g['graph'])
                    batch_features.append(g[self._features_type])
            batch_labels = [1 for _ in range(self._batch_size)]

            graph_sizes = [self._graph_size(g) for g in batch_graphs]
            node_size = sum([n for n, _ in graph_sizes])
            edge_size = sum([e for _, e in graph_sizes])
            return batch_graphs, batch_features, batch_labels, node_size, edge_size

        return self._batch_iter_wrap(get_batch_data)

    def _batch_triplets_rnn(self):
        k = self._n_sim_funcs

        def get_batch_data():
            def checked_get(func_idx):
                idb, fva = self._df_func.iloc[func_idx][['idb_path', 'fva']]
                try:
                    g = self._fdict[idb][fva]
                except:
                    log.debug(idb, fva)
                    return None
                node_size, edge_size = self._graph_size(g['graph'])
                if node_size == 0 or edge_size == 0:
                    return None
                return g

            def sample_variants(fn):
                lst = self._pairable_func_name_dict[fn]
                if len(lst) >= k:
                    return self._random.sample(lst, k=k)
                else:
                    return self._random.choices(lst, k=k)

            batch_graphs, batch_features = list(), list()
            node_size, edge_size = 0, 0
            fns = self._random.sample(
                self._pairable_func_name_list, self._batch_size)
            used_fns = set(fns)
            for fn in fns:
                idxs = sample_variants(fn)
                gs = [checked_get(i) for i in idxs]
                while any([g is None for g in gs]):
                    fn = self._random.choice(self._pairable_func_name_list)
                    while fn in used_fns:
                        fn = self._random.choice(self._pairable_func_name_list)
                    used_fns.add(fn)
                    idxs = sample_variants(fn)
                    gs = [checked_get(i) for i in idxs]
                for g in gs:
                    batch_graphs.append(g['graph'])
                    batch_features.append(g[self._features_type])
            batch_labels = [1 for _ in range(self._batch_size)]

            graph_sizes = [self._graph_size(g) for g in batch_graphs]
            node_size = sum([n for n, _ in graph_sizes])
            edge_size = sum([e for _, e in graph_sizes])
            return batch_graphs, batch_features, batch_labels, node_size, edge_size

        return self._batch_iter_wrap(get_batch_data)
