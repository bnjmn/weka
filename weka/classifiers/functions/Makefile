JAVAC = javac

SOURCES = LeastMedSq.java LinearRegression.java Logistic.java SMO.java \
	VotedPerceptron.java


CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVAC) $< 

.PHONY: all debug optimized clean

#--------------------------------------------------------------------

all :   $(CLASSFILES) 
	(cd neural; make all JAVAC=$(JAVAC))


debug : clean
	$(JAVAC) -g $(SOURCES)
	(cd neural; make debug JAVAC=$(JAVAC))


optimized : clean
	$(JAVAC) -O $(SOURCES)
	(cd neural; make optimized JAVAC=$(JAVAC))


clean : 
	$(RM) *.class *~
	(cd neural; make clean)


