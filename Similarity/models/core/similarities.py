import torch

def euclidean_distance(x, y):
    """Compute the squared Euclidean distance."""
    return torch.sum((x - y)**2, dim=-1)

def pairwise_cosine_similarity(x, y, eps: float=1e-8):
    """Compute the cosine similarity between x and y.

    Args:
      x: NxD float tensor.
      y: MxD float tensor.

    Returns:
      sim_mt: NxM float tensor, the pairwise cosine similarity.
    """
    x_n, y_n = x.norm(2, dim=1).unsqueeze(1), y.norm(2, dim=1).unsqueeze(1)
    x_norm = x / torch.max(x_n, eps * torch.ones_like(x_n))
    y_norm = y / torch.max(y_n, eps * torch.ones_like(y_n))
    sim_mt = torch.mm(x_norm, y_norm.transpose(0, 1))
    return sim_mt

def compute_similarity(loss_type: str, x, y):
    """Compute the distance between x and y vectors.

    The distance will be computed based on the training loss type.

    Args:
      config: a config dict.
      x: [n_examples, feature_dim] float tensor.
      y: [n_examples, feature_dim] float tensor.
      or 
      x: [n_examples, feature_dim] float tensor.
      y: [feature_dim] float tensor.

    Returns:
      dist: [n_examples] float tensor.

    Raises:
      ValueError: if loss type is not supported.
    """
    if loss_type == 'margin':
        # similarity is negative distance
        return -euclidean_distance(x, y)
    elif loss_type == 'cosine':
        return torch.cosine_similarity(x, y)
    else:
        raise ValueError('Unknown loss type %s' % loss_type)
