JAVAC = javac

SOURCES = SMOset.java Kernel.java PolyKernel.java NormalizedPolyKernel.java RBFKernel.java SMO.java SMOreg.java 

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


