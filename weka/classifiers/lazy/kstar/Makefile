JAVA = javac

SOURCES = KStar.java KStarCache.java \
	KStarConstants.java KStarNominalAttribute.java \
	KStarNumericAttribute.java KStarWrapper.java \
	LightHashTable.java 

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

