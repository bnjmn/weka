JAVA = javac
JAVADOC = javadoc
SOURCES = \
	AttributeSelectionPanel.java \
	AttributeSummaryPanel.java \
	ExtensionFileFilter.java \
	FileEditor.java \
	GenericArrayEditor.java \
	GenericObjectEditor.java \
	GUIChooser.java \
	InstancesSummaryPanel.java \
	ListSelectorDialog.java \
	LogPanel.java \
	Logger.java \
	PropertyDialog.java \
	PropertyPanel.java \
	PropertySelectorDialog.java \
	PropertySheetPanel.java \
	PropertyText.java \
	PropertyValueSelector.java \
	ResultHistoryPanel.java \
	SelectedTagEditor.java \
	SetInstancesPanel.java \
	SysErrLog.java \
	TaskLogger.java \
	WekaTaskMonitor.java

CLASSFILES = $(SOURCES:.java=.class)
TOPDIR = ../..
DOCDIR = $(TOPDIR)/doc
PACKAGEDIR = weka/gui
PACKAGE = weka.gui

%.class : %.java
	$(JAVA) $< 

.PHONY: doc

#----------------------------------------------------------------------------#

all :	$(CLASSFILES)
	(cd experiment; make all JAVA=$(JAVA))
	(cd explorer; make all JAVA=$(JAVA))
	(cd streams; make all JAVA=$(JAVA))

debug : clean
	$(JAVA) -g $(SOURCES)
	(cd experiment; make debug JAVA=$(JAVA))
	(cd explorer; make debug JAVA=$(JAVA))
	(cd streams; make debug JAVA=$(JAVA))

optimized : clean
	$(JAVA) -O $(SOURCES)
	(cd experiment; make optimized JAVA=$(JAVA))
	(cd explorer; make optimized JAVA=$(JAVA))
	(cd streams; make optimized JAVA=$(JAVA))

fulljar : all
	(cd $(TOPDIR) ; \
	jar cvf archive/$(PACKAGE).`date +%d%b%Y`.jar \
	$(PACKAGEDIR); \
	rm -f $(PACKAGE).jar; \
	ln -s archive/$(PACKAGE).`date +%d%b%Y`.jar $(PACKAGE).jar)

classjar :  all
	(cd $(TOPDIR) ;\
	jar cvf archive/$(PACKAGE)-bin.`date +%d%b%Y`.jar \
	$(PACKAGEDIR)/*.class \
	$(PACKAGEDIR)/*.props \
	$(PACKAGEDIR)/experiment/*.class \
	$(PACKAGEDIR)/explorer/*.class \
	$(PACKAGEDIR)/streams/*.class; \
	rm -f $(PACKAGE).jar; \
	ln -s archive/$(PACKAGE)-bin.`date +%d%b%Y`.jar $(PACKAGE).jar)

doc : all
	$(JAVADOC) -public -author -version -d $(DOCDIR) $(PACKAGEDIR)

clean : 
	$(RM) *.class *~
	(cd explorer; make clean)
	(cd experiment; make clean)
	(cd streams; make clean)

