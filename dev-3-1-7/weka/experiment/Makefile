JAVAC = javac
JAVACDOC = javadoc
SOURCES = InstanceQuery.java \
	ResultListener.java \
	ResultProducer.java \
	PropertyNode.java \
	Experiment.java \
	SplitEvaluator.java \
	ClassifierSplitEvaluator.java \
	RegressionSplitEvaluator.java \
	RandomSplitResultProducer.java \
	CrossValidationResultProducer.java \
	AveragingResultProducer.java \
	CSVResultListener.java \
	DatabaseUtils.java \
	DatabaseResultListener.java \
	DatabaseResultProducer.java \
	PairedTTester.java \
	CostSensitiveClassifierSplitEvaluator.java
#	LearningRateResultProducer.java \
#	TreeMatchingEvaluator.java

CLASSFILES = $(SOURCES:.java=.class)
TOPDIR = ../..
DOCDIR = $(TOPDIR)/doc
PACKAGEDIR = weka/experiment
PACKAGE = weka.experiment

%.class : %.java
	$(JAVAC) $< 

.PHONY: doc

#-----------------------------------------------------------------------------#

all :	$(CLASSFILES)

debug : clean
	$(JAVAC) -g $(SOURCES)

optimized : clean
	$(JAVAC) -O $(SOURCES)

fulljar : all
	(cd $(TOPDIR) ;\
	jar cvf archive/$(PACKAGE).`date +%d%b%Y`.jar \
	$(PACKAGEDIR) \
	rm -f $(PACKAGE).jar; \
	ln -s archive/$(PACKAGE).`date +%d%b%Y`.jar $(PACKAGE).jar
	)

classjar : all
	(cd $(TOPDIR) ;\
	jar cvf archive/$(PACKAGE)-bin.`date +%d%b%Y`.jar \
	$(PACKAGEDIR)/*.class; \
	$(PACKAGEDIR)/*.props; \
	rm -f $(PACKAGE).jar; \
	ln -s archive/$(PACKAGE)-bin.`date +%d%b%Y`.jar $(PACKAGE).jar)

doc : all
	$(JAVACDOC) -public -author -version -d $(DOCDIR) $(PACKAGE)

clean : 
	$(RM) *.class *~

