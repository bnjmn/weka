JAVAC = javac

SOURCES = Filter.java NullFilter.java AllFilter.java AddFilter.java \
	DiscretizeFilter.java InstanceFilter.java AttributeFilter.java \
	MergeTwoValuesFilter.java SwapAttributeValuesFilter.java \
	MakeIndicatorFilter.java ReplaceMissingValuesFilter.java \
	NumericTransformFilter.java FirstOrderFilter.java \
	NominalToBinaryFilter.java SplitDatasetFilter.java \
	CopyAttributesFilter.java AbstractTimeSeriesFilter.java \
	TimeSeriesDeltaFilter.java \
	TimeSeriesTranslateFilter.java RandomizeFilter.java \
	AttributeSelectionFilter.java NormalizationFilter.java \
	ResampleFilter.java AttributeTypeFilter.java \
	StringToNominalFilter.java ObfuscateFilter.java \
	NonSparseToSparseFilter.java SparseToNonSparseFilter.java \
	UselessAttributeFilter.java SpreadSubsampleFilter.java \
	StringToWordVectorFilter.java \
	AttributeExpressionFilter.java AddNoiseFilter.java \
	ClassOrderFilter.java MisclassificationFilter.java

# Filters To Do:
# TimeSeriesSmoothFilter.java    -- replace attribute values with values
#                                   smoothed over historic instances 
#                                   (average / decay)

CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVAC) $< 

.PHONY: all debug optimized clean

#----------------------------------------------------------------------------#

all :	$(CLASSFILES)

debug : clean
	$(JAVAC) -g $(SOURCES)

optimized : clean
	$(JAVAC) -O $(SOURCES)

binaries : all
	(cd ..; \
	jar -cvfm archive/$(PACKAGE)-bin.`date +%d%b%Y`.jar \
	$(PACKAGE)/*.class)

clean : 
	$(RM) *.class *~

