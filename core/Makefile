JAVAC = javac

SOURCES = Attribute.java Instance.java Instances.java Utils.java \
	ContingencyTables.java SpecialFunctions.java Statistics.java \
	Option.java OptionHandler.java Matrix.java \
	Tag.java SelectedTag.java \
	FastVector.java Copyable.java Queue.java Range.java \
	WeightedInstancesHandler.java CheckOptionHandler.java \
	Drawable.java Matchable.java Summarizable.java AttributeStats.java

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

