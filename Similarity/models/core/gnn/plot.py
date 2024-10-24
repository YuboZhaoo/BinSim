import matplotlib.pyplot as plt

# 两组数据，每组包含20个数据点
recall1 = [0.4860, 0.5607, 0.6075, 0.5981, 0.5888, 0.6449, 0.6168, 0.6542, 0.6168, 0.6822, 0.6542, 0.6355, 0.6355, 0.6449, 0.6355, 0.5981, 0.6449, 0.5888, 0.6449, 0.5794]
recall2 = [0.5140, 0.5327, 0.5607, 0.6168, 0.6355, 0.6262, 0.6075, 0.6449, 0.6168, 0.6449, 0.6636, 0.6449, 0.6822, 0.6822, 0.6729, 0.6729, 0.6822, 0.6636, 0.6449, 0.6636]

# 生成 1 到 20 的 epoch
epochs = list(range(1, 21))

# 创建折线图
plt.figure(figsize=(10, 6))
plt.plot(epochs, recall1, marker='o', label='GGNN')
plt.plot(epochs, recall2, marker='s', label='RNN+GGNN')

# 添加标题和标签
plt.title('Recall@1 on zlib, poolsize 10000')
plt.xlabel('Epoch')
plt.ylabel('Recall@1')

plt.xticks(epochs)

# 添加图例
plt.legend()



# 显示图表
plt.grid(True)
plt.show()
