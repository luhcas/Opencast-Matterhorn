CC=gcc
LIBS=`pkg-config gstreamer-0.10 --libs`
CFLAGS=`pkg-config gstreamer-0.10 --cflags`

h_wo_c: h_wo_c.c
	$(CC) -Wall $(LIBS) $(CFLAGS) h_wo_c.c -o h_wo_c

clean:
	rm h_wo_c
