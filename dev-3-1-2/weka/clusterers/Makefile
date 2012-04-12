JAVA = javac

SOURCES = ClusterEvaluation.java Clusterer.java Cobweb.java \
	  DistributionClusterer.java EM.java

CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVA) $< 

.PHONY: all debug optimized clean

#--------------------------------------------------------------------

all :   $(CLASSFILES)

debug : clean
	$(JAVA) -g $(SOURCES)

optimized : clean
	$(JAVA) -O $(SOURCES)

clean : 
	$(RM) *.class *~

