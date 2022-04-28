set terminal png
set output ARG2
set logscale
unset key
set title "Amount of triples in documents"
set xlabel "Amount of triples"
set ylabel "Number of documents"
set xrange [1:]
set yrange [1:]
plot ARG1 using 1:2 with points
