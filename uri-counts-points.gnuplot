set terminal png
set output ARG2
set logscale
unset key
set title "Occurrence of namespaces in documents"
set xlabel "Number of documents with the namespace"
set ylabel "Number of namespaces"
set xrange [0.5:1000000]
set yrange [0.5:]
plot ARG1 using 1:2 with points
