JAVAC = javac
JAVACDOC = javadoc
SOURCES = \
	ClassifierPanel.java \
	Explorer.java \
	PreprocessPanel.java \
	AttributeSelectionPanel.java \
	AssociationsPanel.java \
	ClustererPanel.java \
	VisualizePanel.java

CLASSFILES = $(SOURCES:.java=.class)
TOPDIR = ../../..
DOCDIR = $(TOPDIR)/doc
PACKAGEDIR = weka/gui/explorer
PACKAGE = weka.gui.explorer

%.class : %.java
	$(JAVAC) $< 

.PHONY: doc

#----------------------------------------------------------------------------#

all :	$(CLASSFILES)
	(cd treevisualizer; make all JAVAC=$(JAVAC))

debug : clean
	$(JAVAC) -g $(SOURCES)
	(cd treevisualizer; make debug JAVAC=$(JAVAC))

optimized : clean
	$(JAVAC) -O $(SOURCES)
	(cd treevisualizer; make optimized JAVAC=$(JAVAC))

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

