JAVA = javac

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
	UserClassifier.java \
	Sourcable.java 

CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVA) $< 

.PHONY: all debug optimized clean

#--------------------------------------------------------------------

all :   $(CLASSFILES) 
	(cd j48; make all JAVA=$(JAVA))
	(cd m5; make all JAVA=$(JAVA))
	(cd kstar; make all JAVA=$(JAVA))

debug : clean
	$(JAVA) -g $(SOURCES)
	(cd j48; make debug JAVA=$(JAVA))
	(cd m5; make debug JAVA=$(JAVA))
	(cd kstar; make debug JAVA=$(JAVA))

optimized : clean
	$(JAVA) -O $(SOURCES)
	(cd j48; make optimized JAVA=$(JAVA))
	(cd m5; make optimized JAVA=$(JAVA))
	(cd kstar; make optimized JAVA=$(JAVA))

clean : 
	$(RM) *.class *~
	(cd j48; make clean)
	(cd m5; make clean)
	(cd kstar; make clean)

