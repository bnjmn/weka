JAVAC = javac

SOURCES = BayesNet.java BayesNetB.java BayesNetB2.java \
	DiscreteEstimatorBayes.java ParentSet.java \
	Scoreable.java NaiveBayes.java NaiveBayesSimple.java \
	NaiveBayesUpdateable.java BayesNetK2.java

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


