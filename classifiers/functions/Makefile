JAVAC = javac

SOURCES = LeastMedSq.java LinearRegression.java Logistic.java \
	VotedPerceptron.java Winnow.java UnivariateLinearRegression.java


CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVAC) $< 

.PHONY: all debug optimized clean

#--------------------------------------------------------------------

all :   $(CLASSFILES) 
	(cd neural; make all JAVAC=$(JAVAC))
	(cd pace; make all JAVAC=$(JAVAC))
	(cd supportVector; make all JAVAC=$(JAVAC))
debug : clean
	$(JAVAC) -g $(SOURCES)
	(cd neural; make debug JAVAC=$(JAVAC))
	(cd pace; make debug JAVAC=$(JAVAC))
	(cd supportVector; make debug JAVAC=$(JAVAC))

optimized : clean
	$(JAVAC) -O $(SOURCES)
	(cd neural; make optimized JAVAC=$(JAVAC))
	(cd pace; make optimized JAVAC=$(JAVAC))
	(cd supportVector; make optimized JAVAC=$(JAVAC))

clean : 
	$(RM) *.class *~
	(cd neural; make clean)
	(cd pace; make clean)
	(cd supportVector; make clean)

