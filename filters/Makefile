JAVAC = javac

SOURCES = Filter.java NullFilter.java AllFilter.java \
	StreamableFilter.java SupervisedFilter.java UnsupervisedFilter.java

CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVAC) $< 

.PHONY: all debug optimized clean

#----------------------------------------------------------------------------#

all :	$(CLASSFILES)
	(cd supervised; make all JAVAC=$(JAVAC))
	(cd unsupervised; make all JAVAC=$(JAVAC))

debug : clean
	$(JAVAC) -g $(SOURCES)
	(cd supervised; make debug JAVAC=$(JAVAC))
	(cd unsupervised; make debug JAVAC=$(JAVAC))

optimized : clean
	$(JAVAC) -O $(SOURCES)
	(cd supervised; make optimized JAVAC=$(JAVAC))
	(cd unsupervised; make optimized JAVAC=$(JAVAC))

clean : 
	$(RM) *.class *~
	(cd supervised; make clean JAVAC=$(JAVAC))
	(cd unsupervised; make clean JAVAC=$(JAVAC))
