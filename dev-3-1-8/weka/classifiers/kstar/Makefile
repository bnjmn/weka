JAVAC = javac

SOURCES = KStar.java KStarCache.java \
	KStarConstants.java KStarNominalAttribute.java \
	KStarNumericAttribute.java KStarWrapper.java \
	LightHashTable.java 

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

