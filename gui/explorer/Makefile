JAVA = javac
JAVADOC = javadoc
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
	$(JAVA) $< 

.PHONY: doc

#----------------------------------------------------------------------------#

all :	$(CLASSFILES)
	(cd treevisualizer; make all JAVA=$(JAVA))

debug : clean
	$(JAVA) -g $(SOURCES)
	(cd treevisualizer; make debug JAVA=$(JAVA))

optimized : clean
	$(JAVA) -O $(SOURCES)
	(cd treevisualizer; make optimized JAVA=$(JAVA))

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

