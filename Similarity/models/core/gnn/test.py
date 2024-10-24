import torch
import torch.nn as nn

# 超参数
vocab_size = 1  # 词汇表的大小
embedding_dim = 4  # 嵌入向量的维度

# 创建嵌入层
embedding = nn.Embedding(vocab_size, embedding_dim)

# 示例输入：一批索引序列
input_indices = torch.tensor([1, 1, 0, 0])  # 假设这些是词汇表中单词的索引

# 通过嵌入层获取嵌入向量
embedded_vectors = embedding(input_indices)

# 打印结果
print(f"Input indices: {input_indices}")
print(f"Embedded vectors:\n{embedded_vectors}")
