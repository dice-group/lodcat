set terminal png
set output ARG2
unset key
set title "Amount of triples in documents"
set xlabel "Amount of triples"
set ylabel "Number of documents"
set style fill solid border -1
set logscale y
set xtics rotate by 30 right
set xrange [-0.5:7.5]
set boxwidth 0.61
plot ARG1 using 1:3:xtic(2) with boxes
