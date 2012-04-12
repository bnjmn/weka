JAVA = javac

SOURCES = Classifier.java DistributionClassifier.java \
	Evaluation.java  UpdateableClassifier.java CheckClassifier.java \
	ZeroR.java IB1.java Id3.java Prism.java NaiveBayesSimple.java \
	KernelDensity.java OneR.java LinearRegression.java DecisionStump.java \
	NaiveBayes.java IBk.java SMO.java Logistic.java LWR.java \
	DecisionTable.java VotedPerceptron.java \
	Bagging.java LogitBoost.java AdaBoostM1.java Stacking.java \
	MultiClassClassifier.java MultiScheme.java CVParameterSelection.java \
	BVDecompose.java RegressionByDiscretization.java

CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVA) $< 

.PHONY: all debug optimized clean

#--------------------------------------------------------------------

all :   $(CLASSFILES) 
	(cd j48; make all)
	(cd m5; make all)

debug : clean
	$(JAVA) -g $(SOURCES)
	(cd j48; make debug)
	(cd m5; make debug)

optimized : clean
	$(JAVA) -O $(SOURCES)
	(cd j48; make optimized)
	(cd m5; make optimized)

clean : 
	$(RM) *.class *~
	(cd j48; make clean)
	(cd m5; make clean)

