JAVAC = javac 

SOURCES = CorrelationSplitInfo.java M5Base.java M5P.java Rule.java RuleNode.java \
	Impurity.java Values.java SplitEvaluate.java YongSplitInfo.java \

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




