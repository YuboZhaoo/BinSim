

from .graph_factory_testing import GraphFactoryTesting
from .graph_factory_inference import GraphFactoryInference
from .graph_factory_training import GraphFactoryTraining

import logging
log = logging.getLogger('gnn')

import torch
from torch.utils import data

class DatasetWrap(data.IterableDataset):
    def __init__(self, training_gen, mode):
        self.gen = training_gen
        self.mode = mode
    
    def __iter__(self):
        if self.mode == 'pair':
            bg = self.gen.pairs()
        elif self.mode == 'triplet':
            bg = self.gen.triplets()
        elif self.mode.startswith('batch'):
            bg = self.gen.batch_triplets()
        else:
            raise Exception(f'Unkown train mode {self.gen}')
        return bg

    def step(self):
        return self.gen.step()

    def reset_seed(self, seed):
        return self.gen.reset_seed(seed)

def build_train_validation_generators(config):
    """Utility function to build train and validation batch generators.

    Args
      config: global configuration
    """
    training_gen = DatasetWrap(GraphFactoryTraining(
        func_path=config['training']['df_train_path'],
        feat_path=config['training']['features_train_path'],
        batch_size=config['training']['batch_size'],
        max_num_nodes=config['training']['max_num_nodes'],
        max_num_edges=config['training']['max_num_edges'],
        n_sim_funcs=config['training']['n_sim_funcs'], 
        used_subgraphs=config['used_subgraphs'], 
        edge_feature_dim=config['edge_feature_dim'],
    ), config['training']['mode'])

    validation_gen = GraphFactoryTesting(
        func_info_path=config['validation']['func_info_csv_path'],
        feat_path=config['validation']['features_validation_path'],
        batch_size=config['batch_size'],
        used_subgraphs=config['used_subgraphs'], 
        edge_feature_dim=config['edge_feature_dim'],
    )

    return training_gen, validation_gen


def build_testing_generator(config, csv_path):
    """Build a batch_generator from the CSV in input.

    Args
      config: global configuration
      csv_path: CSV input path
    """
    testing_gen = DatasetWrap(GraphFactoryInference(
        func_path=csv_path,
        feat_path=config['testing']['features_testing_path'],
        batch_size=config['batch_size'],
        used_subgraphs=config['used_subgraphs'], 
        edge_feature_dim=config['edge_feature_dim'],
    ), 'pair')

    return testing_gen



