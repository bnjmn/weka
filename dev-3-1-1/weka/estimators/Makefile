JAVA = javac


SOURCES = DiscreteEstimator.java NormalEstimator.java PoissonEstimator.java \
	KernelEstimator.java Estimator.java DDConditionalEstimator.java \
	NDConditionalEstimator.java DNConditionalEstimator.java \
	KDConditionalEstimator.java DKConditionalEstimator.java \
	KKConditionalEstimator.java NNConditionalEstimator.java \
	MahalanobisEstimator.java ConditionalEstimator.java

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

