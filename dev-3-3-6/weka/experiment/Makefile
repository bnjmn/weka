JAVAC = javac
JAVACDOC = javadoc
SOURCES = \
	AveragingResultProducer.java \
	CSVResultListener.java \
	ClassifierSplitEvaluator.java \
	CostSensitiveClassifierSplitEvaluator.java \
	CrossValidationResultProducer.java \
	DatabaseResultListener.java \
	DatabaseResultProducer.java \
	DatabaseUtils.java \
	Experiment.java \
	InstanceQuery.java \
	InstancesResultListener.java \
	OutputZipper.java \
	PairedStats.java \
	PairedTTester.java \
	PropertyNode.java \
	RandomSplitResultProducer.java \
	RegressionSplitEvaluator.java \
	ResultListener.java \
	ResultProducer.java \
	SplitEvaluator.java \
	Stats.java \
#	LearningRateResultProducer.java \
#	TreeMatchingEvaluator.java \
	RemoteEngine.java \
	Compute.java \
	Task.java \
	RemoteExperiment.java \
	RemoteExperimentListener.java \
	RemoteExperimentEvent.java \
	RemoteExperimentSubTask.java \
	TaskStatusInfo.java \
	PairedStatsCorrected.java \
	PairedCorrectedTTester.java

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
# generate the _Stub.class and _Skel.class for remote engine. The behaviour
# of rmic between jdk1.1 and jdk1.2 is different. Replace -d ../../ with 
# -d . for jdk1.1
	rmic -d ../../ weka.experiment.RemoteEngine

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

