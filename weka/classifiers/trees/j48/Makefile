JAVAC = javac

SOURCES = BinC45ModelSelection.java BinC45Split.java \
	C45ModelSelection.java C45PruneableClassifierTree.java \
	C45Split.java ClassifierSplitModel.java ClassifierTree.java \
	Distribution.java EntropyBasedSplitCrit.java \
	EntropySplitCrit.java GainRatioSplitCrit.java \
	InfoGainSplitCrit.java J48.java ModelSelection.java \
	NoSplit.java PruneableClassifierTree.java SplitCriterion.java \
	Stats.java



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


