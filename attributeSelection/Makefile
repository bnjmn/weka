JAVAC = javac

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
	  ConsistencySubsetEval.java StartSetHandler.java \
	  RankSearch.java PrincipalComponents.java \
	  ErrorBasedMeritEvaluator.java HoldOutSubsetEvaluator.java \
	  RaceSearch.java ClassifierSubsetEval.java \
	  ChiSquaredAttributeEval.java

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

