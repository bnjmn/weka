JAVA = javac

SOURCES = Filter.java NullFilter.java AllFilter.java AddFilter.java \
	DiscretizeFilter.java InstanceFilter.java AttributeFilter.java \
	MergeTwoValuesFilter.java SwapAttributeValuesFilter.java \
	MakeIndicatorFilter.java ReplaceMissingValuesFilter.java \
	NumericTransformFilter.java FirstOrderFilter.java \
	NominalToBinaryFilter.java SplitDatasetFilter.java \
	AttributeSelectionFilter.java

# CopyAttributesFilter.java
# TimeSeriesDeltaFilter.java
# TimeSeriesSmoothFilter.java

CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVA) $< 

.PHONY: all debug optimized clean

#-------------------------------------------------------------------------------#

all :	$(CLASSFILES)

debug : clean
	$(JAVA) -g $(SOURCES)

optimized : clean
	$(JAVA) -O $(SOURCES)

binaries : all
	(cd ..; \
	jar -cvfm archive/$(PACKAGE)-bin.`date +%d%b%Y`.jar \
	$(PACKAGE)/*.class)

clean : 
	$(RM) *.class *~

