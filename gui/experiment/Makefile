JAVA = javac
JAVADOC = javadoc
SOURCES = \
	DatasetListPanel.java \
	GeneratorPropertyIteratorPanel.java \
	RunNumberPanel.java \
	SetupPanel.java \
	RunPanel.java \
	ResultsPanel.java \
	Main.java

CLASSFILES = $(SOURCES:.java=.class)
TOPDIR = ../../..
DOCDIR = $(TOPDIR)/doc
PACKAGEDIR = weka/gui/experiment
PACKAGE = weka.gui.experiment

%.class : %.java
	$(JAVA) $< 

.PHONY: doc

#----------------------------------------------------------------------------#

all :	$(CLASSFILES)

debug : clean
	$(JAVA) -g $(SOURCES)

optimized : clean
	$(JAVA) -O $(SOURCES)

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
	$(JAVADOC) -public -author -version -d $(DOCDIR) $(PACKAGEDIR)

clean : 
	$(RM) *.class *~

