JAVAC = javac
JAVACDOC = javadoc
SOURCES = \
	InstanceEvent.java \
	InstanceProducer.java \
	InstanceListener.java \
	InstanceLoader.java \
	InstanceCounter.java \
	InstanceViewer.java \
	InstanceTable.java \
	InstanceJoiner.java \
	InstanceSavePanel.java

#	FilterPanel.java 

# For a beans-style linking of instance streaming type interface:
#	InstanceEvent.java        # Instance produced event
#	InstanceProducer.java     # Interface for instance producers (IP)
#	InstanceListener.java     # Interface for instance listeners (IL)
#	InstanceLoader.java       # IP that loads from a file
#	InstanceCounter.java      # IL that counts instances
#	InstanceTable.java        # IL that displays instances in a table
#	InstanceViewer.java       # IL that displays instances as text output
#	InstanceClassifier.java   # IL that builds classifiers
#	InstanceSaver.java        # IL that saves to a file
#	InstanceFilter.java       # IL/IP that filters instances
#	InstanceJoiner.java       # IL/IP that merges two instance streams
#	ModelViewer.java          # For viewing classifier toString
#	GraphViewer.java          # For viewing decision trees
#	PerformanceViewer.java    # For viewing classifier performance

CLASSFILES = $(SOURCES:.java=.class)
TOPDIR = ../../..
DOCDIR = $(TOPDIR)/doc
PACKAGEDIR = weka/gui/streams
PACKAGE = weka.gui.streams

%.class : %.java
	$(JAVAC) $< 

.PHONY: doc

#----------------------------------------------------------------------------#

all :	$(CLASSFILES)

debug : clean
	$(JAVAC) -g $(SOURCES)

optimized : clean
	$(JAVAC) -O $(SOURCES)

manifest:
	(cd $(TOPDIR) ;\
	echo "" > manifest.tmp ;\
	echo "Name: "$(PACKAGEDIR)"/InstanceLoader.class" >> manifest.tmp ;\
	echo "Java-Bean: True" >> manifest.tmp ;\
	echo "" >> manifest.tmp ;\
	echo "Name: "$(PACKAGEDIR)"/InstanceCounter.class" >> manifest.tmp ;\
	echo "Java-Bean: True" >> manifest.tmp ;\
	echo "" >> manifest.tmp ;\
	echo "Name: "$(PACKAGEDIR)"/InstanceViewer.class" >> manifest.tmp ;\
	echo "Java-Bean: True" >> manifest.tmp ;\
	echo "" >> manifest.tmp ;\
	echo "Name: "$(PACKAGEDIR)"/InstanceSaver.class" >> manifest.tmp ;\
	echo "Java-Bean: True" >> manifest.tmp ;\
	echo "" >> manifest.tmp ;\
	echo "Name: "$(PACKAGEDIR)"/InstanceJoiner.class" >> manifest.tmp ;\
	echo "Java-Bean: True" >> manifest.tmp ;\
	echo "" >> manifest.tmp ;\
	echo "Name: "$(PACKAGEDIR)"/InstanceTable.class" >> manifest.tmp ;\
	echo "Java-Bean: True" >> manifest.tmp ;\
	echo "" >> manifest.tmp ;\
	)

fulljar : manifest all
	(cd $(TOPDIR) ; \
	jar cvfm archive/$(PACKAGE).`date +%d%b%Y`.jar \
	manifest.tmp $(PACKAGEDIR); \
	rm -f manifest.tmp $(PACKAGE).jar; \
	ln -s archive/$(PACKAGE).`date +%d%b%Y`.jar $(PACKAGE).jar)

classjar : manifest all
	(cd $(TOPDIR) ;\
	jar cvfm archive/$(PACKAGE)-bin.`date +%d%b%Y`.jar \
	manifest.tmp $(PACKAGEDIR)/*.class; \
	rm -f manifest.tmp $(PACKAGE).jar; \
	ln -s archive/$(PACKAGE)-bin.`date +%d%b%Y`.jar $(PACKAGE).jar)

doc : all
	$(JAVACDOC) -public -author -version -d $(DOCDIR) $(PACKAGEDIR)

clean : 
	$(RM) *.class *~

