JAVAC = javac
JAVACDOC = javadoc
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
	WekaTaskMonitor.java \
	SaveBuffer.java \
	VisualizePanelEvent.java \
	VisualizePanelListener.java \
	VisualizePanel.java

CLASSFILES = $(SOURCES:.java=.class)
TOPDIR = ../..
DOCDIR = $(TOPDIR)/doc
PACKAGEDIR = weka/gui
PACKAGE = weka.gui

%.class : %.java
	$(JAVAC) $< 

.PHONY: doc

#----------------------------------------------------------------------------#

all :	$(CLASSFILES)
	(cd experiment; make all JAVAC=$(JAVAC))
	(cd explorer; make all JAVAC=$(JAVAC))
	(cd streams; make all JAVAC=$(JAVAC))
	(cd treevisualizer; make all JAVAC=$(JAVAC))

debug : clean
	$(JAVAC) -g $(SOURCES)
	(cd experiment; make debug JAVAC=$(JAVAC))
	(cd explorer; make debug JAVAC=$(JAVAC))
	(cd streams; make debug JAVAC=$(JAVAC))
	(cd treevisualizer; make debug JAVAC=$(JAVAC))

optimized : clean
	$(JAVAC) -O $(SOURCES)
	(cd experiment; make optimized JAVAC=$(JAVAC))
	(cd explorer; make optimized JAVAC=$(JAVAC))
	(cd streams; make optimized JAVAC=$(JAVAC))
	(cd treevisualizer; make optimized JAVAC=$(JAVAC))	

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
	$(PACKAGEDIR)/treevisualizer/*.class \
	$(PACKAGEDIR)/streams/*.class; \
	rm -f $(PACKAGE).jar; \
	ln -s archive/$(PACKAGE)-bin.`date +%d%b%Y`.jar $(PACKAGE).jar)

doc : all
	$(JAVACDOC) -public -author -version -d $(DOCDIR) $(PACKAGEDIR)

clean : 
	$(RM) *.class *~
	(cd explorer; make clean)
	(cd experiment; make clean)
	(cd streams; make clean)
	(cd treevisualizer; make clean)

