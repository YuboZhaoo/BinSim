
import torch 
from torch.nn.functional import cosine_similarity
from .similarities import euclidean_distance


def pairwise_loss(x, y, labels, loss_type: str='margin', margin: float=1.0):
    """Compute pairwise loss.

    Args:
      x: [N, D] float tensor, representations for N examples.
      y: [N, D] float tensor, representations for another N examples.
      labels: [N] int tensor, with values in -1 or +1.  labels[i] = +1 if x[i]
        and 1 are similar, and -1 otherwise.
      loss_type: margin or hamming.
      margin: float scalar, margin for the margin loss.

    Returns:
      loss: [N] float tensor.  Loss for each pair of representations.
    """
    labels = labels.to(device=x.device, dtype=x.dtype)
    if loss_type == 'margin':
        return torch.nn.functional.relu(margin - labels * (1 - euclidean_distance(x, y)))
    else:
        raise ValueError('Unknown loss_type %s' % loss_type)


def triplet_loss(x_1, y, x_2, z, loss_type: str='margin', margin: float=1.0):
    """Compute triplet loss.

    This function computes loss on a triplet of inputs (x, y, z).
    A similarity or distance value is computed for each pair of
    (x, y) and (x, z).  Since the representations for x can be
    different in the two pairs (like our matching model)
    we distinguish the two x representations by x_1 and x_2.

    Args:
      x_1: [N, D] float tensor.
      y: [N, D] float tensor.
      x_2: [N, D] float tensor.
      z: [N, D] float tensor.
      loss_type: margin or hamming.
      margin: float scalar, margin for the margin loss.

    Returns:
      loss: [N] float tensor.  Loss for each pair of representations.
    """
    if loss_type == 'margin':
        return torch.nn.functional.relu(
            margin + euclidean_distance(x_1, y) - euclidean_distance(x_2, z))
    elif loss_type == 'cosine':
        return torch.nn.functional.relu(
            margin - cosine_similarity(x_1, y) + cosine_similarity(x_2, z))
    else:
        raise ValueError('Unknown loss_type %s' % loss_type)
