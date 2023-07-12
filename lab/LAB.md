## 基于WeTune的查询改写实验

### 实验介绍

WeTune[[paper](https://dl.acm.org/doi/10.1145/3514221.3526125)]是一个查询改写规则（Query Rewrite Rule）生成器，它能自动化地发现和验证能优化查询性能的查询改写规则。

WeTune的工作流程主要包含三个阶段：规则枚举、规则验证、查询改写。

1. 规则枚举阶段通过暴力枚举的方式生成给定条件下所有可能的**查询模板**，并对所有的模板对（两个不同的模板）枚举所有可能的约束条件；
2. 规则验证阶段使用基于SMT的验证器来验证规则中包含的两个查询模板在给定约束条件下是否等价；
3. 查询改写阶段接受外部输入的一条SQL查询，首先用各个规则的源查询模板和约束条件进行匹配，然后按照规则中的目标查询模板替换查询语句中相应的结构。

本实验主要包含两个部分：代码补全（50'）、查询改写（50'）

### 目录结构

```
|-- lib/             # 外部库
|-- common/          # 通用工具
|-- sql/             # 对SQL语句及其执行计划的解析
|-- superopt/        # WeTune的核心算法
    |-- fragment/    # 查询模板及其枚举
    |-- constraint/  # 约束枚举及验证
    |-- runner/      # 各个功能的程序入口
    |-- logic/       # 基于SMT的验证器
    |-- optimizer/   # 查询改写
|-- testbed/         # 测试框架
|-- wtune_data/      # 数据输入和输出目录
    |-- schemas/     # 数据表Schemas
    |-- wtune.db     # 保存了待改写SQL的数据库
```

### 1 代码补全 （50'）

在此部分中，你需要阅读代码，并补全缺失的部分代码。

#### 1.1 规则枚举（20'）

##### 介绍

WeTune中的“规则”是一个三元组，包含：①源查询模板、②目标查询模板、③约束条件，按以下格式存储为字符串：

```
源查询模板|目标查询模板|约束条件1;约束条件2;……
```

例如，这是一个具体的规则：

```
Proj<a0 s0>(Input<t0>)|Proj<a1 s1>(Input<t1>)|
AttrsSub(a0,t0);TableEq(t1,t0);AttrsEq(a1,a0);SchemaEq(s1,s0)
```

该规则的第一行是两个**查询模板**，该规则的第二行是一些约束条件（详见论文）。查询模板是一个树状结构（字符串中用圆括号表示节点父子关系，括号前是父节点括号内是子节点），每个节点表示一个运算符，例如，下表中给出了各运算符的定义：

|          运算符表达式          | 孩子数量 |                             描述                             |
| :----------------------------: | :------: | :----------------------------------------------------------: |
|           $Input<t>$           |    0     |                将表$t$的所有元组作为初始输入                 |
|         $Proj<a,s>(R)$         |    1     |                     仅保留表$R$中的$a$列                     |
|        $Proj*<a,s>(R)$         |    1     |         仅保留表$R$中的$a$列，并去除$a$列重复的元组          |
|        $Filter<p,a>(R)$        |    1     |          丢弃$R$中所有$a$列不满足谓词条件$p$的元组           |
|   $InSubFilter<a>(R_1, R_2)$   |    2     |           丢弃$R_1$中，$a$列不在$R_2$中出现的元组            |
| $InnerJoin<a_1, a_2>(R_1,R_2)$ |    2     | 计算$R_1,R_2$的笛卡尔积，丢弃$R_1.a_1$与$R_2.a_2$不同的元组  |
| $LeftJoin<a_1, a_2>(R_1,R_2)$  |    2     | 计算$R_1,R_2$的笛卡尔积，将$R_1.a_1$与$R_2.a_2$不同的元组的$R_2.a2$列置为NULL |

利用上述不同的运算符可以组合成大量查询模板。例如，下面是一些查询模板及它们对应的一个可能的查询语句

```sql
Proj<a0 s0>(Input<t0>)
SELECT a0 FROM t0

InnerJoin<k0 k1>(Input<t0>,Input<t1>)
SELECT * FROM t0 JOIN t1 ON t0.k0 = t1.k1

InSubFilter<k2>(Input<t2>,Proj<k3 s3>(Input<t3>))
SELECT * FROM t2 WHERE t2.k2 IN (SELECT k3 FROM t3)

Proj<a1 s1>(LeftJoin<a2 a3>(Input<t1>,Proj*<a4 s4>(Input<t2>))
SELECT a1 FROM (SELECT * FROM t1 JOIN (SELECT a4 FROM t2) as t3 ON t1.a2 = t3.a3))
```

其中，第三、第四个模板对应的树形结构如下：

```
InSubFilter<k2>
|-- Input<t2>
|-- Proj<k3 s3>
    |-- Input<t3>
    
Proj<a1 s1>
|-- LeftJoin<a2 a3>
    |-- Input<t1>
    |-- Proj*<a4 s4>
        |-- Input<t2>
```

##### 实现要求

在1.1节，你需要构造指定条件下所有可能的查询模板，枚举需要注意以下几个事项：

1. 每种运算符都有指定的孩子数量
2. 所有叶子节点都是Input运算符
3. 为了限制枚举范围，除Input运算符外的其它运算符的总个数需小于等于FragmentEnumerator.maxOps

请在FragmentEnumerator.java中，enumerateFragmentSet()函数。该函数首先枚举所有的查询模板，并返回包含这些模板的集合。

提示：①可以在FragmentEnumerator类中自己定义工具函数，用递归的方式实现枚举。②可以先枚举树的骨架，最后填充节点运算符。

##### 评估方式

完成上述代码后，可以通过EnumTest测试：

```bash
gradle :superopt:test --tests "wtune.lab.EnumTest"
```

#### 1.2 规则验证 （30'）

1.1中枚举的规则需要经过等价性验证，方可用于改写查询。WeTune首先把一条规则中的两个查询模板转换为两个**U表达式（U-expression）**，从而把查询模板的等价性问题转换为U表达式的等价问题。随后，WeTune用一阶逻辑（first-order logic, FOL）表示其等价性，最后交由SMT solver进行证明。

U表达式$f(t)$可以表示元组$t$在查询$f$的结果中出现的次数。其中主要包含以下几种基本运算符：

1. $[[R]](x)$表示$x$在表$R$中出现的次数，中括号可以省略
2. $[b]$当布尔值$b$为真时返回1，否则返回0
3. $||e||$当$e>0$时返回1，否则返回0
4. $not(e)$当$e>0$时返回0，否则返回1
5. $\Sigma{\{t\}}f(t)$表示对所有可能的元组$t$，$f(t)$的值的总和

具体而言，WeTune从根节点开始遍历模板树，按下表规则翻译各类运算符：

|          运算符表达式          |                       翻译后的U表达式                        |
| :----------------------------: | :----------------------------------------------------------: |
|           $Input<t>$           |                         $f(x)=t(x)$                          |
|         $Proj<a,s>(R)$         |             $f(x)=\Sigma{\{t\}}(R(t)*[x=a(t)])$              |
|        $Proj*<a,s>(R)$         |           $f(x)=\Sigma{\{t\}}(||R(t)*[x=a(t)]||)$            |
|        $Filter<p,a>(R)$        |                    $f(x)=R(x)*[p(a(x))]$                     |
|   $InSubFilter<a>(R_1, R_2)$   |       $f(x)=R_1(x)*||R_2(a(x))*not([isNull(a(x))])||$        |
| $InnerJoin<a_1, a_2>(R_1,R_2)$ | $f(x)=\Sigma{\{t_1，t_2\}}(R_1(t_1)*R_2(t_2)*[a_1(t_1)=a_2(t_2)]*not(isNull(a_1(t_1))))$ |
| $LeftJoin<a_1, a_2>(R_1,R_2)$  |                              略                              |

##### 实现要求

1. 你需要补全把查询模板转换为U表达式的代码。请在UExprTranslator.java中补全trSimpleFilter()、trJoin()函数。这两个函数能把运算符转化为对应的U表达式。

   提示：可以参考trInput()、trInSubFilter()、trExistsFilter()、trProj()的实现方式。

2. 你需要补全U表达式转为FOL的代码。请在LogicProver.java中补全proveEq0()函数。该函数中先处理简单情况E=E‘（待补全），再处理一些复杂情况。

完成上述代码后，可以通过UExprTest和VeriTest两个测试：

```bash
gradle :superopt:test --tests "wtune.lab.UExprTest"
gradle :superopt:test --tests "wtune.lab.VeriTest"
```

### 2 查询改写（50'）

在此部分中，你需要改写给定的几条查询语句，提高他们的执行效率。

#### 2.1

#### 2.2
