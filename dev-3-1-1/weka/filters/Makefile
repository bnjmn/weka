JAVA = javac

SOURCES = Filter.java NullFilter.java AllFilter.java AddFilter.java \
	DiscretizeFilter.java InstanceFilter.java AttributeFilter.java \
	MergeTwoValuesFilter.java SwapAttributeValuesFilter.java \
	MakeIndicatorFilter.java ReplaceMissingValuesFilter.java \
	NumericTransformFilter.java FirstOrderFilter.java \
	NominalToBinaryFilter.java SplitDatasetFilter.java \
	CopyAttributesFilter.java TimeSeriesDeltaFilter.java \
	TimeSeriesTranslateFilter.java \
	AttributeSelectionFilter.java

# New Filters Done:
# CopyAttributesFilter.java      -- create a copy of selected attributes
# TimeSeriesTranslateFilter.java -- replace attribute values with values from
#                                   historic instances 
# TimeSeriesDeltaFilter.java     -- replace attribute values with first order
#                                   difference from historic instances

# Filters To Do:
# TimeSeriesSmoothFilter.java    -- replace attribute values with values
#                                   smoothed over historic instances 
#                                   (average / decay)

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

