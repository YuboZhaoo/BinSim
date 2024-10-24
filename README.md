# 基于程序依赖与异构图学习的二进制相似性分析


## 二进制分析部分：用于产生好的程序图表示

### 环境准备

* Java 11

* 安装Ghidra 9.2.2（高版本API发生变化，建图模块会产生问题）
    * https://github.com/NationalSecurityAgency/ghidra/releases?page=4
    
* Ghidra脚本开发环境：建议使用IDEA+Ghidra插件
    * https://plugins.jetbrains.com/plugin/18086-ghidra
    
* 第三方库

    * error_prone_annotations-2.10.0.jar
    
    * javimmutable-collections-2.4.1.jar

### 运行方法
 
有两种模式，GUI模式和headless模式

* GUI模式是在ghidra中直接运行脚本，主要是为了开发和观察效果

* headless模式通过命令行运行，主要是为了批量生成数据用于实验（在相似性分析部分介绍）

#### GUI模式

GUI模式是在ghidra中直接运行脚本，主要是为了开发和观察效果

* 首先在Ghidra中打开一个二进制文件

* 将项目的src路径加入到ghidra脚本路径中

    ![](https://notes.sjtu.edu.cn/uploads/upload_46b4eb3c65b4a1d309526a48a24649a1.png)

    ![](https://notes.sjtu.edu.cn/uploads/upload_4ec59329752cf2dc6c4392bc43a901df.png)

* 点击一个函数

* 运行图生成脚本BuildGraph.java

    ![](https://notes.sjtu.edu.cn/uploads/upload_a3babdd0c561947a0131f7d3c3fcc819.png)
    
    ![](https://notes.sjtu.edu.cn/uploads/upload_a5aa84e2526ce6d5adc0493fd0f92b63.png)
    
* 运行区间分析IntervalAnalysis.java

    ![](https://notes.sjtu.edu.cn/uploads/upload_07cd64b887433cc957a0ec298ca9a2e1.png)

#### Headless模式

headless模式通过命令行运行，主要是为了批量生成数据。会把一个二进制文件中需要的函数的图输出到json中。具体的数据集准备和运行方法，在相似性分析部分进行详细介绍。


### 代码结构与功能解释

主要的功能是分析和建图

* src/ 主要外部脚本，也是你要直接运行的

    * Analysis.java 脚本基类，其他脚本需要extends它
    
    * BuildGraph.java 用于生成各种程序图表示
    
    * IntervalAnalysis.java 区间分析（目前支持的P-code指令有限）

* src/com.bai.graph 图模块 

    * 所有图都继承GraphBasic类
    
    * 每种图可选三种粒度：基本块级别、指令级别、OP级别（操作符和操作数）。需要哪种粒度就从GraphBasic类继承对应的内部类

    * 每种图可选两种模式输出：可视化（ghidra内浏览）和json输出（用于批量生成）。根据输出需要继承GraphDisplay类和GraphJson类
    
    * 目前完善的程序图表示有5种，控制流图CFG、数据依赖图DDG、CDFG（CFG+DDG）、控制依赖图CDG、程序依赖图PDG（CDG+DDG）

    * 两个初步实现，还需要完善的图表示
    
        * 带长距离依赖的数据依赖图LDDG（DDG+long dependency）
        
        * 带概率的程序依赖图PPDG（PDG+probability）
        
* src/com.bai.analysis 内部静态分析模块

    * 图模块会调用这些分析功能，把实现并封装好的分析脚本放进来
    
* src/com.bai.env 环境模块，分析模块会调用里面的抽象域和解释器

    * src/com.bai.env.domain 抽象域模块，所有抽象域继承AbsDomain类。重点在于实现抽象域的join与基本操作
    
      * Interval.java 目前只实现了区间域
      
      * IntervalSet.java 

    * src/com.bai.env.semantic 抽象语义模块，所有抽象解释器继承Interpreter类。重点在于将各种指令解释为基本操作
    
        * IntervalInterpreter.java 目前只支持20种指令的区间域抽象解释
        
    * src/com.bai.env.region 抽象内存模块，分为Global、Heap、Local、Reg、Unique五种
    
        * 参考BinAbsInspector的实现，可以参考这篇博客
            https://www.wolai.com/nocbtm/aSHsPHF3QG8RueRMT7xiKQ

    * src/com.bai.env.ALoc 抽象内存位置，是对region的封装，包括region、偏移量和长度
    
        * 二进制分析与源码分析不同。在二进制层面不再有变量，只有寄存器和内存，因此需要内存抽象
    
    * src/com.bai.env.AbsEnv 抽象程序状态模块，抽象解释的本质是抽象程序状态的转换
    
        * AbsEnv是从ALoc到AbsDomain的Map
        
            * 抽象解释的实质是计算抽象程序状态以及状态间的转换
            
            * 因此二进制程序分析，本质上是从一种抽象内存状态转换为另一种内存状态
        
* src/com.bai.utils 工具模块 

    * GlobalState.java 用于在脚本间共享反编译器的全局配置，尤其是输出到ghidra控制台需要它
    
    * Architecture.java 用于存储二进制文件的架构信息



## 相似性分析部分：利用AI模型计算图相似性



### 数据集

#### Sec22数据集介绍

* 该数据集来自Usenix Security 22的论文How Machine Learning Is Solving the Binary Function Similarity Problem

    https://www.usenix.org/system/files/sec22-marcelli.pdf
    https://github.com/Cisco-Talos/binary_function_similarity/tree/main

* 包含clamav，curl、nmap、openssl、unrar、z3、zlib七个项目中的二进制程序

#### 数据集下载

* 需要下载二进制文件、csv索引和features文件

    * 二进制文件，下载Dataset-1
    
        https://drive.google.com/drive/folders/1g9P0KKSwqdFt0K6dDeKKhWfmhqiQHQqU
        
        放置在BinSim/Similarity/IDBs/Dataset1
        
        ![](https://notes.sjtu.edu.cn/uploads/upload_dba7b341343bb809f32173c0f3bdc752.png)
        
    * csv索引文件和features文件，下载Dataset-1
    
        https://drive.google.com/drive/folders/1uqZb0geb4CgDe9XEczZhNcyfBQM1TusG
        
        放置在BinSim/Similarity/DBs/Dataset-1
        
        ![](https://notes.sjtu.edu.cn/uploads/upload_890d9b53ca899836b8475721cf6300d4.png)

### 环境配置

* 与Hermes的环境配置相同

    https://github.com/NSSL-SJTU/HermesSim/blob/main/requirements.txt
    ```
    conda create -n hermessim python=3.10
    conda activate hermessim
    pip install -r ./requirements.txt \
        --extra-index-url https://download.pytorch.org/whl/cu116 \
        -f https://data.pyg.org/whl/torch-1.13.1+cu116.html
    ```

### 运行方法

这部分是在Usenix Security 24的HermesSim上修改的，可以直接参考他们的readme与实现
https://github.com/NSSL-SJTU/HermesSim

#### 数据预处理

* 分析与建图，运行BinSim/Similarity/preprocess/lifter.py

    * 需要修改路径，包括ghidra安装路径、脚本路径、输出路径

    * 该代码首先读取DBs/Dataset-1/features中的json文件，确定数据集中每个二进制文件中的目标函数列表
    
        * 目标函数列表的txt文件会输出到BinSim/Similarity/DBs/Dataset-1/index下
    
    * 随后以headless模式多进程运行ghidra脚本BuildGraph.java，生成目标函数的图表示
    
        * 各个二进制的图文件会输出到BinSim/Similarity/DBs/输出文件夹（如1023）/index下
        
        * 错误日志中会记录一些ghidra识别失败的函数
        
            * 失败有两种原因，一种是该函数无法被ghidra识别，另一种是二进制文件太大ghidra反编译失败

* 图编码，运行BinSim/Similarity/preprocess/preprocess.py

    * 输入为上一步的图文件（修改对应路径），输出为编码好的图文件（指定输出路径），用于后续模型学习
    
    * 修改gtype为你想要的图类别，例如['PDG_INST', 'PDG_OP']
    
    * 输出位置为BinSim/Similarity/DBs/Dataset-1/inputs/指定的输出文件夹/。子文件夹中包含编码好的各种图，pkl文件是后续模型读取的，json文件是给人看的
    
    * 这一步首先从训练集中计算各操作的数量，然后根据频率为训练集、验证集、测试集进行编码。目前只支持INST和OP级别的图

* 过滤索引文件，运行BinSim/Similarity/preprocess/csv_filter.py

    * 输入为上一步的pkl文件（修改对应路径）和BinSim/Similarity/DBs/Dataset-1/下的csv文件，会将过滤后的csv文件输出到当前文件夹下
    
    * 需要将输出的csv文件移动到BinSim/Similarity/inputs/Dataset-1/下
    
    * 这一步的目的是删除掉csv中lift失败的函数

#### 模型训练部分

* 训练模型

    * 运行命令

        ```    
        python models/main.py     --inputdir inputs     --config ./models/config/ddg_op.json     --dataset=ddg_op
        ```

    * 输入为编码好的pkl图文件、json配置文件、csv索引文件。会训练并保存模型，得到zlib验证集的结果，并将测试集的embedding结果以pkl文件输出

    * json配置文件在BinSim/Similarity/models/config下
    
        * 更多的参考 https://github.com/NSSL-SJTU/HermesSim/tree/main/model/configures

    * 会输出zlib上Recall、MRR等指标
    
    * 加入新的模型，可以在 BinSim/Similarity/models/core 中实现新的GNN，然后在 BinSim/Similarity/models/gnn_model的_model_initialize 函数中进行替换
    
        * 目前还未实现适合PDG的模型，以及后续用于LDDG和PPDG的模型

    

* 模型推理评估部分还未实现
    * 可以参考 https://github.com/NSSL-SJTU/HermesSim 的4. Result Analysis部分

### 参考资料

本项目主要参考了BinAbsInspector、SigmaDiff和Hermes的实现

* BinAbsInspector
    * https://github.com/KeenSecurityLab/BinAbsInspector
    * Ghidra分析部分参考了他们的内存模型

* SigmaDiff
    * https://github.com/yijiufly/SigmaDiff/
    * Ghidra分析部分参考了他们的分析脚本

* Hermes
    * https://github.com/NSSL-SJTU/HermesSim
    * Python模型部分的代码基本和他们是一致的，只是在其基础上替换模型，修改预处理
    * https://github.com/sgfvamll/gsat

关于Ghidra脚本开发

* P-code：Ghidra内部使用的IR，分为两级，分析中使用优化过的High IR

    * P-code参考手册：
    https://fossies.org/linux/ghidra/GhidraDocs/languages/html/pcoderef.html
    
    * P-code笔记
    https://www.wolai.com/nocbtm/wL33fEw2ci4Yi5DSCDd7F4

    
