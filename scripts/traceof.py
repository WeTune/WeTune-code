#! env python3

import sys

located = False
for line in sys.stdin:
    if located:
        if line[0] == ' ':
            print(line.strip())
        else:
            break
    if line.strip() == sys.argv[1]:
        located = True
