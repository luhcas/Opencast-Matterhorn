rm -rf /tmp/graph
rm -rf /tmp/graph2
!dstat -cdm --thermal --load --output /tmp/graph 1 120

graph=!cat graph

while graph[0]!= '':
    del graph[0]

del graph[0]
headers=""

while graph[0].startswith('"'):
	#just pick the most specific of the header labels
	headers = graph.pop(0)

headers=headers.replace('"','')
headers=headers.split(",")

i=0
for line in graph:
    line = line.replace(","," ")
    echo $i $line
    i=i+1
    echo $i,$line>>graph2

plot_cmds=[]
for item in headers:
	plot='"/tmp/graph2" u 1:' + str(len(plot_cmds)+1) + ' w l t "' + item + '"'
	plot_cmds.append(plot)

overall_cmd = "plot "
for cmd in plot_cmds:
	overall_cmd = overall_cmd+cmd+","

import sys, os
fil = open("/tmp/graph.gp","w")
i=0
for cmd in plot_cmds:
	fil.write( "set terminal png" + "\n")
	fil.write( "set output '" + str(i) + ".png'\n")
	i=i+1
	fil.write( "plot " + str(cmd) + "\n")
	
fil.close()

!gnuplot -persist /tmp/graph.gp

output="<html><body>\n"
for i in range(0,len(plot_cmds)):
	output = output + "<img src='" + str(i) + ".png'/><p>\n"

output=output+"</body></html>"
fil = open("/tmp/results.html","w")
fil.write(output)
fil.close()

