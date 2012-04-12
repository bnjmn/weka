JAVAC = javac

SOURCES = ClusterEvaluation.java Clusterer.java Cobweb.java \
	  DistributionClusterer.java EM.java SimpleKMeans.java \
	  DistributionMetaClusterer.java

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

