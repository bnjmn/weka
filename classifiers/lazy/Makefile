JAVAC = javac

SOURCES = IB1.java IBk.java LBR.java LWL.java



CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVAC) $< 

.PHONY: all debug optimized clean

#--------------------------------------------------------------------

all :   $(CLASSFILES) 
	(cd kstar; make all JAVAC=$(JAVAC))


debug : clean
	$(JAVAC) -g $(SOURCES)
	(cd kstar; make debug JAVAC=$(JAVAC))


optimized : clean
	$(JAVAC) -O $(SOURCES)
	(cd kstar; make optimized JAVAC=$(JAVAC))


clean : 
	$(RM) *.class *~
	(cd kstar; make clean)


