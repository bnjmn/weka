JAVA = javac

SOURCES = ASEvaluation.java ASSearch.java AttributeEvaluator.java \
	  AttributeSelection.java \
	  BestFirst.java CfsSubsetEval.java GainRatioAttributeEval.java \
	  InfoGainAttributeEval.java OneRAttributeEval.java \
	  RankedOutputSearch.java Ranker.java SubsetEvaluator.java \
	  SymmetricalUncertAttributeEval.java  \
	  UnsupervisedAttributeEvaluator.java \
	  UnsupervisedSubsetEvaluator.java WrapperSubsetEval.java \
	  ReliefFAttributeEval.java ForwardSelection.java \
	  GeneticSearch.java RandomSearch.java \
	  ConsistencySubsetEval.java StartSetHandler.java

CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVA) $< 

.PHONY: all debug optimized clean

#--------------------------------------------------------------------

all :   $(CLASSFILES) 

debug : clean
	$(JAVA) -g $(SOURCES)

optimized : clean
	$(JAVA) -O $(SOURCES)

clean : 
	$(RM) *.class *~

