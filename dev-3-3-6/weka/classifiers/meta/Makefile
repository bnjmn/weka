JAVAC = javac

SOURCES = AdaBoostM1.java AdditiveRegression.java \
	AttributeSelectedClassifier.java Bagging.java \
	CVParameterSelection.java ClassificationViaRegression.java \
	CostSensitiveClassifier.java \
	DistributionMetaClassifier.java FilteredClassifier.java \
	LogitBoost.java MetaCost.java MultiClassClassifier.java \
	MultiBoostAB.java MultiScheme.java OrdinalClassClassifier.java \
	RacedIncrementalLogitBoost.java \
	RegressionByDiscretization.java Stacking.java \
	ThresholdSelector.java StackingC.java Grading.java Vote.java



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


