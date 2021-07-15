set terminal png
set output ARG2
unset key
set title "Occurrence of namespaces in documents"
set ylabel "Number of namespaces"
set xlabel "Number of documents with the namespace"
set style fill solid border -1
set logscale y
set xtics rotate by 30 right
set xrange [-0.5:6.5]
set boxwidth 0.61
plot ARG1 using 1:3:xtic(2) with boxes
