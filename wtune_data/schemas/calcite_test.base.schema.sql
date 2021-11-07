CREATE TABLE `account`
(
    `acctno`  int,
    `type`    varchar(20),
    `balance` varchar(20)
);

CREATE TABLE `bonus`
(
    `ename` int,
    `job`   varchar(20),
    `sal`   varchar(20),
    `comm`  varchar(20)
);

CREATE TABLE `dept`
(
    `deptno` int,
    `name`   varchar(20)
);

CREATE TABLE `emp`
(
    `empno`    int,
    `ename`    varchar(20),
    `job`      varchar(20),
    `mgr`      int,
    `hiredate` int,
    `comm`     int,
    `sal`      int,
    `deptno`   int,
    `slacker`  int
);

CREATE TABLE `T`
(
    `F0_C0` int,
    `F0_C1` int,
    `F1_C0` int,
    `F1_C2` int,
    `F2_C3` int,
    `F1_A0` int,
    `F2_A0` int,
    `K0`    int,
    `C1`    int
);
