JAVAC = javac

SOURCES = AbstractTimeSeries.java Add.java AddCluster.java \
	AddExpression.java AddNoise.java Copy.java Discretize.java \
	FirstOrder.java MakeIndicator.java MergeTwoValues.java \
	NominalToBinary.java Normalize.java NumericToBinary.java \
	NumericTransform.java Obfuscate.java Remove.java RemoveType.java \
	RemoveUseless.java ReplaceMissingValues.java StringToNominal.java \
	StringToWordVector.java SwapValues.java TimeSeriesDelta.java \
	TimeSeriesTranslate.java

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

clean : 
	$(RM) *.class *~
