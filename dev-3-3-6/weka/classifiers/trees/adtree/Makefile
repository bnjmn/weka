JAVAC = javac

SOURCES = ADTree.java PredictionNode.java Splitter.java \
	TwoWayNominalSplit.java TwoWayNumericSplit.java \
	ReferenceInstances.java

CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVAC) $< 

.PHONY: all debug optimized clean

#--------------------------------------------------------------------

all :   $(CLASSFILES)

debug : clean
	$(JAVAC) -g $(SOURCES)

optimized : clean
	$(JAVAC) -O $(SOURCES)

clean : 
	$(RM) *.class *~


