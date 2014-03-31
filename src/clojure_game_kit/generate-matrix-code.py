for i in range(0, 4):
    for j in range(0, 4):
        print "(set! (. mat m{0}{1}) e{1}{0})".format(i, j)
