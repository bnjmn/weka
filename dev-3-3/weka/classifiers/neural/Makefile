JAVAC = javac

SOURCES = LinearUnit.java NeuralMethod.java NeuralNode.java \
	NeuralConnection.java NeuralNetwork.java SigmoidUnit.java

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


