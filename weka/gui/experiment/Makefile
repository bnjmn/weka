JAVAC = javac
JAVACDOC = javadoc
SOURCES = \
	DatasetListPanel.java \
	Experimenter.java \
	GeneratorPropertyIteratorPanel.java \
	ResultsPanel.java \
	RunNumberPanel.java \
	RunPanel.java \
	SetupPanel.java \
	DistributeExperimentPanel.java \
	HostListPanel.java \
	SimpleSetupPanel.java \
	SetupModePanel.java \
	AlgorithmListPanel.java

CLASSFILES = $(SOURCES:.java=.class)
TOPDIR = ../../..
DOCDIR = $(TOPDIR)/doc
PACKAGEDIR = weka/gui/experiment
PACKAGE = weka.gui.experiment

%.class : %.java
	$(JAVAC) $< 

.PHONY: doc

#----------------------------------------------------------------------------#

all :	$(CLASSFILES)

debug : clean
	$(JAVAC) -g $(SOURCES)

optimized : clean
	$(JAVAC) -O $(SOURCES)

fulljar : all
	(cd $(TOPDIR) ; \
	jar cvf archive/$(PACKAGE).`date +%d%b%Y`.jar \
	$(PACKAGEDIR); \
	rm -f $(PACKAGE).jar; \
	ln -s archive/$(PACKAGE).`date +%d%b%Y`.jar $(PACKAGE).jar)

classjar : all
	(cd $(TOPDIR) ;\
	jar cvf archive/$(PACKAGE)-bin.`date +%d%b%Y`.jar \
	$(PACKAGEDIR)/*.class; \
	rm -f $(PACKAGE).jar; \
	ln -s archive/$(PACKAGE)-bin.`date +%d%b%Y`.jar $(PACKAGE).jar)

doc : all
	$(JAVACDOC) -public -author -version -d $(DOCDIR) $(PACKAGEDIR)

clean : 
	$(RM) *.class *~

