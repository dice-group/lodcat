set terminal png
set output ARG2
set grid
unset key
set title "C_P quality of topics in the model"
set xlabel "Topics"
set ylabel "C_P topic quality"
set xrange [1:ARG3]
set yrange [-1:1]
plot ARG1 with linespoints
