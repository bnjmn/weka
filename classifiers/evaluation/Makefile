JAVAC = javac

SOURCES = Prediction.java NominalPrediction.java NumericPrediction.java \
	TwoClassStats.java EvaluationUtils.java \
	ThresholdCurve.java MarginCurve.java ConfusionMatrix.java \
	CostCurve.java


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

