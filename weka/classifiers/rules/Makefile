JAVAC = javac

SOURCES = DecisionTable.java OneR.java Prism.java ZeroR.java \
	Ridor.java ConjunctiveRule.java JRip.java RuleStats.java



CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVAC) $< 

.PHONY: all debug optimized clean

#--------------------------------------------------------------------

all :   $(CLASSFILES) 
	(cd part; make all JAVAC=$(JAVAC))


debug : clean
	$(JAVAC) -g $(SOURCES)
	(cd part; make debug JAVAC=$(JAVAC))


optimized : clean
	$(JAVAC) -O $(SOURCES)
	(cd part; make optimized JAVAC=$(JAVAC))


clean : 
	$(RM) *.class *~
	(cd part; make clean)


