JAVAC = javac

SOURCES = NonSparseToSparse.java Randomize.java RemoveFolds.java \
	RemoveMisclassified.java RemovePercentage.java RemoveRange.java \
	RemoveWithValues.java Resample.java SparseToNonSparse.java

CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVAC) $< 

.PHONY: all debug optimized clean

#----------------------------------------------------------------------------#

all :	$(CLASSFILES)

debug : clean
	$(JAVAC) -g $(SOURCES)

optimized : clean
	$(JAVAC) -O $(SOURCES)

clean : 
	$(RM) *.class *~
