JAVAC = javac

SOURCES = C45ModelSelection.java EntropyBasedSplitCrit.java \
	C45PruneableClassifierTree.java GainRatioSplitCrit.java \
	SplitCriterion.java C45Split.java InfoGainSplitCrit.java \
	ClassifierSplitModel.java J48.java Stats.java \
	ClassifierTree.java Distribution.java \
	ModelSelection.java MakeDecList.java ClassifierDecList.java \
	C45PruneableDecList.java PART.java EntropySplitCrit.java \
	PruneableClassifierTree.java PruneableDecList.java \
	BinC45ModelSelection.java BinC45Split.java

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


