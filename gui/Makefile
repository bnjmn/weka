JAVA = javac
JAVADOC = javadoc
SOURCES = \
	PropertyText.java \
	PropertyValueSelector.java \
	PropertySelectorDialog.java \
	PropertyPanel.java \
	PropertyDialog.java \
	PropertySheetPanel.java \
	ListSelectorDialog.java \
	GenericArrayEditor.java \
	GenericObjectEditor.java \
	SelectedTagEditor.java

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
	(cd experiment; make all)
	(cd streams; make all)

debug : clean
	$(JAVA) -g $(SOURCES)
	(cd experiment; make debug)
	(cd streams; make debug)

optimized : clean
	$(JAVA) -O $(SOURCES)
	(cd experiment; make optimized)
	(cd streams; make optimized)

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
	$(PACKAGEDIR)/experiment/*.class \
	$(PACKAGEDIR)/streams/*.class; \
	rm -f $(PACKAGE).jar; \
	ln -s archive/$(PACKAGE)-bin.`date +%d%b%Y`.jar $(PACKAGE).jar)

doc : all
	$(JAVADOC) -public -author -version -d $(DOCDIR) $(PACKAGEDIR)

clean : 
	$(RM) *.class *~
	(cd experiment; make clean)
	(cd streams; make clean)

