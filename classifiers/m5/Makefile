JAVAC = javac 

SOURCES = M5Prime.java Options.java M5Utils.java Errors.java Node.java SplitInfo.java \
	Impurity.java Values.java Matrix.java Ivector.java Dvector.java \
	Measures.java Function.java

CLASSFILES = $(SOURCES:.java=.class)

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




