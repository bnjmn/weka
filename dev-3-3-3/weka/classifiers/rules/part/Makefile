JAVAC = javac

SOURCES = C45PruneableDecList.java ClassifierDecList.java \
	MakeDecList.java PART.java PruneableDecList.java




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


