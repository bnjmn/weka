JAVA = javac

SOURCES = Filter.java NullFilter.java AllFilter.java AddFilter.java \
	DiscretizeFilter.java SelectFilter.java DeleteFilter.java \
	MergeTwoValuesFilter.java SwapAttributeValuesFilter.java \
	MakeIndicatorFilter.java ReplaceMissingValuesFilter.java \
	NumericTransformFilter.java FirstOrderFilter.java \
	NominalToBinaryFilter.java SplitDatasetFilter.java \
	AttributeSelectionFilter.java

# Example filters:
# NullFilter       : Allows no instances through
# AllFilter        : Allows all instances through unaltered
# AddFilter        : Add new (blank) columns
# DeleteFilter     : Delete columns
# DiscretiseFilter : Change numeric columns to enumerated
# SelectFilter     : Blocks instances matching some criterion
# DegradeFilter    : Replace values with missing or noise
# RelabelFilter    : Change enumerated value labels
# FillFilter       : Replace missing values with predicted or calculated values

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

manifest.tmp: $(CLASSFILES)
	@/bin/rm -f manifest.tmp
	echo "Name: filterPackage/NullFilter.class" >> manifest.tmp
	echo "Java-Bean: True" >> manifest.tmp
	echo "" >> manifest.tmp
	echo "Name: filterPackage/AllFilter.class" >> manifest.tmp
	echo "Java-Bean: True" >> manifest.tmp
	echo "" >> manifest.tmp
	echo "Name: filterPackage/AddFilter.class" >> manifest.tmp
	echo "Java-Bean: True" >> manifest.tmp
	echo "" >> manifest.tmp
	echo "Name: filterPackage/DeleteFilter.class" >> manifest.tmp
	echo "Java-Bean: True" >> manifest.tmp
	echo "" >> manifest.tmp
	echo "Name: filterPackage/DiscretiseFilter.class" >> manifest.tmp
	echo "Java-Bean: True" >> manifest.tmp
	echo "" >> manifest.tmp
	echo "Name: filterPackage/SelectFilter.class" >> manifest.tmp
	echo "Java-Bean: True" >> manifest.tmp
	echo "" >> manifest.tmp
	echo "Name: filterPackage/NominalToBinaryFilter.class" >> manifest.tmp
	echo "Java-Bean: True" >> manifest.tmp
	echo "" >> manifest.tmp
	echo "Name: filterPackage/MergeTwoValuesFilter.class" >> manifest.tmp
	echo "Java-Bean: True" >> manifest.tmp
	echo "" >> manifest.tmp
	echo "Name: filterPackage/SwapAttributeValuesFilter.class" >> manifest.tmp
	echo "Java-Bean: True" >> manifest.tmp
	echo "" >> manifest.tmp
	echo "Name: filterPackage/MakeIndicatorFilter.class" >> manifest.tmp
	echo "Java-Bean: True" >> manifest.tmp
	echo "" >> manifest.tmp
	echo "Name: filterPackage/ReplaceMissingValuesFilter.class" >> manifest.tmp
	echo "Java-Bean: True" >> manifest.tmp
	echo "Name: filterPackage/UnaryNumericTransformFilter.class" >> manifest.tmp
	echo "Java-Bean: True" >> manifest.tmp

binaries : manifest.tmp
	(cd ..; \
	jar -cvfm archive/$(PACKAGE)-bin.`date +%d%b%Y`.jar \
	$(PACKAGE)/manifest.tmp $(PACKAGE)/*.class)

clean : 
	$(RM) *.class *~

