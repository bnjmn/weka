JAVAC = javac

SOURCES = 

CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVAC) $< 

.PHONY: all debug optimized clean

#----------------------------------------------------------------------------#

all :	$(CLASSFILES)
	(cd attribute; make all JAVAC=$(JAVAC))
	(cd instance; make all JAVAC=$(JAVAC))

debug : clean
	$(JAVAC) -g $(SOURCES)
	(cd attribute; make debug JAVAC=$(JAVAC))
	(cd instance; make debug JAVAC=$(JAVAC))

optimized : clean
	$(JAVAC) -O $(SOURCES)
	(cd attribute; make optimized JAVAC=$(JAVAC))
	(cd instance; make optimized JAVAC=$(JAVAC))

clean : 
	$(RM) *.class *~
	(cd attribute; make clean JAVAC=$(JAVAC))
	(cd instance; make clean JAVAC=$(JAVAC))
