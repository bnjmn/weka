JAVAC = javac

SOURCES = Classifier.java DistributionClassifier.java \
	Evaluation.java  UpdateableClassifier.java CheckClassifier.java \
	ZeroR.java IB1.java Id3.java Prism.java NaiveBayesSimple.java \
	KernelDensity.java OneR.java LinearRegression.java DecisionStump.java \
	NaiveBayes.java IBk.java SMO.java Logistic.java LWR.java \
	DecisionTable.java VotedPerceptron.java HyperPipes.java \
	Bagging.java LogitBoost.java AdaBoostM1.java Stacking.java \
	MultiClassClassifier.java MultiScheme.java CVParameterSelection.java \
	BVDecompose.java RegressionByDiscretization.java \
	CostSensitiveClassifier.java MetaCost.java \
	ClassificationViaRegression.java FilteredClassifier.java \
	Sourcable.java UserClassifier.java

CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVAC) $< 

.PHONY: all debug optimized clean

#--------------------------------------------------------------------

all :   $(CLASSFILES) 
	(cd j48; make all JAVAC=$(JAVAC))
	(cd m5; make all JAVAC=$(JAVAC))
	(cd kstar; make all JAVAC=$(JAVAC))

debug : clean
	$(JAVAC) -g $(SOURCES)
	(cd j48; make debug JAVAC=$(JAVAC))
	(cd m5; make debug JAVAC=$(JAVAC))
	(cd kstar; make debug JAVAC=$(JAVAC))

optimized : clean
	$(JAVAC) -O $(SOURCES)
	(cd j48; make optimized JAVAC=$(JAVAC))
	(cd m5; make optimized JAVAC=$(JAVAC))
	(cd kstar; make optimized JAVAC=$(JAVAC))

clean : 
	$(RM) *.class *~
	(cd j48; make clean)
	(cd m5; make clean)
	(cd kstar; make clean)

