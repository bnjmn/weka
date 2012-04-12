JAVA = javac

SOURCES = Attribute.java Instance.java Instances.java Utils.java \
	ContingencyTables.java SpecialFunctions.java Statistics.java \
	Option.java OptionHandler.java Matrix.java \
	Tag.java SelectedTag.java \
	FastVector.java Copyable.java Queue.java Range.java \
	WeightedInstancesHandler.java CheckOptionHandler.java \
	Drawable.java Matchable.java Summarizable.java

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

