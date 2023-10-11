JAVAC=javac
CC = sparc-linux-gcc

.SUFFIXES	:

default		:	compiler

compiler	:	compile.jar runtime.o assemble compile
	chmod a+x assemble
	chmod a+x compile

runtime.o	:
	$(CC) -c runtime.c

compile.jar	:
	${JAVACC} -OUTPUT_DIRECTORY:parser/source -DEBUG_PARSER parser/scanner.jj
	$(JAVAC) -cp .:${SUPPORT} main/*.java parser/source/*.java global/*.java global/IRTree/*.java
	jar cfm $@ META-INF/MANIFEST.MF parser/source/*.class main/*.class global/*.class global/IRTree/*.class

clean		:
	-/bin/rm compile.jar main/*.class parser/source/* debug/verbose/* global/*.class global/IRTree/*.class runtime.o
