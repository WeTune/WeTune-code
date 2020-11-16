#! env python3

with open("operations.csv", "r") as f_op:
    ops = f_op.readlines()

with open("features.csv", "r") as f_feat:
    features = f_feat.readlines()

if len(ops) != len(features):
    print('Warning: lines mismatch')

out_lines = []
for i in range(len(ops)):
    idx, op = ops[i].rstrip().split(',', 1)
    feature = features[i].rstrip()
    out_lines.append("{},{},{}\n".format(idx, feature, op))

    #  op = [x for x in ops[i].rstrip().split(',') if x != '']
    #  idx, op = op[0], op[1:]
    #  feature = features[i].rstrip()
    #  for j in range(len(op)):
    #  out_lines.append("{},{},Dist{},{}\n".format(idx, feature, j + 1,
    #  op[j]))

with open("data.csv", "w") as f_data:
    f_data.writelines(out_lines)

# vim:sw=4 ts=4
