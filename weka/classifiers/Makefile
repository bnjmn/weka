JAVAC = javac

SOURCES = BVDecompose.java CheckClassifier.java Classifier.java \
	CostMatrix.java DistributionClassifier.java Evaluation.java \
	IterativeClassifier.java Sourcable.java UpdateableClassifier.java \
	SequentialClassifier.java DistributionSequentialClassifier.java \
	SequentialEvaluation.java


CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVAC) $< 

.PHONY: all debug optimized clean

#--------------------------------------------------------------------

all :   $(CLASSFILES) 
	(cd bayes; make all JAVAC=$(JAVAC))
	(cd evaluation; make all JAVAC=$(JAVAC))
	(cd functions; make all JAVAC=$(JAVAC))
	(cd lazy; make all JAVAC=$(JAVAC))
	(cd meta; make all JAVAC=$(JAVAC))
	(cd misc; make all JAVAC=$(JAVAC))
	(cd rules; make all JAVAC=$(JAVAC))
	(cd trees; make all JAVAC=$(JAVAC))

debug : clean
	$(JAVAC) -g $(SOURCES)
	(cd bayes; make debug JAVAC=$(JAVAC))
	(cd evaluation; make debug JAVAC=$(JAVAC))
	(cd functions; make debug JAVAC=$(JAVAC))
	(cd lazy; make debug JAVAC=$(JAVAC))
	(cd meta; make debug JAVAC=$(JAVAC))
	(cd misc; make debug JAVAC=$(JAVAC))
	(cd rules; make debug JAVAC=$(JAVAC))
	(cd trees; make debug JAVAC=$(JAVAC))

optimized : clean
	$(JAVAC) -O $(SOURCES)
	(cd bayes; make optimized JAVAC=$(JAVAC))
	(cd evaluation; make optimized JAVAC=$(JAVAC))
	(cd functions; make optimized JAVAC=$(JAVAC))
	(cd lazy; make optimized JAVAC=$(JAVAC))
	(cd meta; make optimized JAVAC=$(JAVAC))
	(cd misc; make optimized JAVAC=$(JAVAC))
	(cd rules; make optimized JAVAC=$(JAVAC))
	(cd trees; make optimized JAVAC=$(JAVAC))

clean : 
	$(RM) *.class *~
	(cd bayes; make clean)
	(cd evaluation; make clean)
	(cd functions; make clean)
	(cd lazy; make clean)
	(cd meta; make clean)
	(cd misc; make clean)
	(cd rules; make clean)
	(cd trees; make clean)

