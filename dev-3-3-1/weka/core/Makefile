JAVAC = javac

SOURCES = Attribute.java Instance.java Instances.java Utils.java \
	ContingencyTables.java SpecialFunctions.java Statistics.java \
	Option.java OptionHandler.java Matrix.java \
	Tag.java SelectedTag.java \
	FastVector.java Copyable.java Queue.java Range.java \
	WeightedInstancesHandler.java CheckOptionHandler.java \
	Drawable.java Matchable.java Summarizable.java AttributeStats.java \
	SparseInstance.java AdditionalMeasureProducer.java \
	ProtectedProperties.java

CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVAC) $< 

.PHONY: all debug optimized clean

#--------------------------------------------------------------------

all :   $(CLASSFILES)
	(cd converters; make all JAVAC=$(JAVAC))

debug : clean
	$(JAVAC) -g $(SOURCES)
	(cd converters; make debug JAVAC=$(JAVAC))

optimized : clean
	$(JAVAC) -O $(SOURCES)
	(cd converters; make optimized JAVAC=$(JAVAC))

clean : 
	$(RM) *.class *~
	(cd converters; make clean)

