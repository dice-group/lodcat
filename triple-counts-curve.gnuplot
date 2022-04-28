set terminal png
set output ARG2
set logscale y
unset key
set title "Amount of triples in documents"
set xlabel "Documents sorted by amount of triples"
set ylabel "Amount of triples"
set xrange [1:]
set yrange [1:]
plot ARG1 with linespoints
