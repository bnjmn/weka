JAVAC = javac

SOURCES = DecisionStump.java Id3.java UserClassifier.java


CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVAC) $< 

.PHONY: all debug optimized clean

#--------------------------------------------------------------------

all :   $(CLASSFILES) 
	(cd adtree; make all JAVAC=$(JAVAC))
	(cd j48; make all JAVAC=$(JAVAC))
	(cd m5; make all JAVAC=$(JAVAC))


debug : clean
	$(JAVAC) -g $(SOURCES)
	(cd adtree; make debug JAVAC=$(JAVAC))
	(cd j48; make debug JAVAC=$(JAVAC))
	(cd m5; make debug JAVAC=$(JAVAC))


optimized : clean
	$(JAVAC) -O $(SOURCES)
	(cd adtree; make optimized JAVAC=$(JAVAC))
	(cd j48; make optimized JAVAC=$(JAVAC))
	(cd m5; make optimized JAVAC=$(JAVAC))


clean : 
	$(RM) *.class *~
	(cd adtree; make clean)
	(cd j48; make clean)
	(cd m5; make clean)


