#
# $Revision: 1.38 $
#

# Java Compiler to use
# E.g: make all JAVAC=jikes
JAVAC = javac

# Location to place install jarfiles and doc
# If relative, it's w.r.t this Makefile 
# (default is parent directory of this Makefile)
# E.g: make install WEKAHOME=/home/ml/java
WEKAHOME = .

.PHONY: all optimized debug clean install archive doc

all : 
	(cd core; make all JAVAC=$(JAVAC))
	(cd classifiers; make all JAVAC=$(JAVAC))
	(cd filters; make all JAVAC=$(JAVAC))
	(cd estimators; make all JAVAC=$(JAVAC))
	(cd associations; make all JAVAC=$(JAVAC))
	(cd clusterers; make all JAVAC=$(JAVAC))
	(cd attributeSelection; make all JAVAC=$(JAVAC))
	(cd datagenerators; make all JAVAC=$(JAVAC))
	(cd experiment; make all JAVAC=$(JAVAC))
	(cd gui; make all JAVAC=$(JAVAC))

optimized : 
	(cd core; make optimized JAVAC=$(JAVAC))
	(cd classifiers; make optimized JAVAC=$(JAVAC))
	(cd filters; make optimized JAVAC=$(JAVAC))
	(cd estimators; make optimized JAVAC=$(JAVAC))
	(cd associations; make optimized JAVAC=$(JAVAC))
	(cd clusterers; make optimized JAVAC=$(JAVAC))
	(cd attributeSelection; make optimized JAVAC=$(JAVAC))
	(cd datagenerators; make optimized JAVAC=$(JAVAC))
	(cd experiment; make optimized JAVAC=$(JAVAC))
	(cd gui; make optimized JAVAC=$(JAVAC))

debug :
	(cd core; make debug JAVAC=$(JAVAC))
	(cd classifiers; make debug JAVAC=$(JAVAC))
	(cd filters; make debug JAVAC=$(JAVAC))
	(cd estimators; make debug JAVAC=$(JAVAC))
	(cd associations; make debug JAVAC=$(JAVAC))
	(cd clusterers; make debug JAVAC=$(JAVAC))
	(cd attributeSelection; make debug JAVAC=$(JAVAC))
	(cd datagenerators; make debug JAVAC=$(JAVAC))
	(cd experiment; make debug JAVAC=$(JAVAC))
	(cd gui; make debug JAVAC=$(JAVAC))

clean : 
	(cd core; make clean)
	(cd classifiers; make clean) 
	(cd filters; make clean)
	(cd estimators; make clean)
	(cd associations; make clean)
	(cd clusterers; make clean)
	(cd attributeSelection; make clean)
	(cd datagenerators; make clean)
	(cd experiment; make clean)
	(cd gui; make clean)

# Creates Javadoc documentation in directory ../doc
doc :
	(cd ..; \
	(mkdir doc 2>&1 ) >/dev/null ; \
	javadoc -J-mx100m -public -author -version -1.1 -d doc \
	weka.core \
	weka.core.converters \
	weka.classifiers \
	weka.classifiers.bayes \
	weka.classifiers.evaluation \
	weka.classifiers.functions \
	weka.classifiers.lazy \
	weka.classifiers.meta \
	weka.classifiers.misc \
	weka.classifiers.rules \
	weka.classifiers.trees \
	weka.classifiers.trees.j48 \
	weka.classifiers.trees.m5 \
	weka.classifiers.trees.lmt \
	weka.classifiers.lazy.kstar \
	weka.classifiers.functions.neural \
	weka.classifiers.trees.adtree \
	weka.classifiers.rules.part \
	weka.filters \
	weka.filters.unsupervised \
	weka.filters.unsupervised.attribute \
	weka.filters.unsupervised.instance \
	weka.filters.supervised \
	weka.filters.supervised.attribute \
	weka.filters.supervised.instance \
	weka.estimators \
	weka.associations \
	weka.clusterers \
	weka.attributeSelection \
	weka.datagenerators \
	weka.experiment \
	weka.gui \
	weka.gui.experiment \
	weka.gui.explorer \
	weka.gui.visualize \
	weka.gui.boundaryvisualizer \
	weka.gui.treevisualizer \
	weka.gui.beans \
	weka.gui.streams ; \
	touch doc/index.html; for page in `ls doc/*.html`; \
	do cat $$page | sed "s/Index<\/A>  <A HREF=\"help.html\"/Index<\/a>  <a href=\"http:\/\/www.cs.waikato.ac.nz\/ml\/weka\/index.html\">WEKA\'s home<\/a>  <A HREF=\"help.html\"/g" > $$page.temp; mv $$page.temp $$page; \
	cat $$page | sed "s/Index<\/A><\/PRE/Index<\/a>  <a href=\"http:\/\/www.cs.waikato.ac.nz\/ml\/weka\/index.html\">WEKA\'s home<\/a><\/PRE/g" > $$page.temp; mv $$page.temp $$page; done; \
	sed 's/help.html/..\/Tutorial.pdf/g' \
	< doc/packages.html > packages_temp.html; \
	mv packages_temp.html doc/packages.html)

# Makes source and binary jarfiles and docs into WEKAHOME
install : all
	(cd ..; \
	echo "Main-Class: weka.gui.GUIChooser" > manifest.tmp ;\
	jar cvfm $(WEKAHOME)/weka.jar manifest.tmp \
	weka/core/*.class \
	weka/core/converters/*.class \
	weka/classifiers/*.class \
	weka/classifiers/bayes/*.class \
	weka/classifiers/evaluation/*.class \
	weka/classifiers/functions/*.class \
	weka/classifiers/lazy/*.class \
	weka/classifiers/meta/*.class \
	weka/classifiers/misc/*.class \
	weka/classifiers/rules/*.class \
	weka/classifiers/trees/*.class \
	weka/classifiers/trees/j48/*.class \
	weka/classifiers/trees/m5/*.class \
	weka/classifiers/trees/lmt/*.class \
	weka/classifiers/lazy/kstar/*.class \
	weka/classifiers/functions/neural/*.class \
	weka/classifiers/trees/adtree/*.class \
	weka.classifiers/rules/part/*.class \
	weka/filters/*.class \
	weka/filters/unsupervised/*.class \
	weka/filters/unsupervised.attribute/*.class \
	weka/filters/unsupervised.instance/*.class \
	weka/filters/supervised/*.class \
	weka/filters/supervised.attribute/*.class \
	weka/filters/supervised.instance/*.class \
	weka/estimators/*class \
	weka/associations/*.class \
	weka/clusterers/*.class \
	weka/attributeSelection/*.class \
	weka/datagenerators/*.class \
	weka/experiment/*.class \
	weka/experiment/*.props \
	weka/experiment/*.policy \
	weka/gui/*.class \
	weka/gui/*.props \
	weka/gui/*.jpg \
	weka/gui/*.gif \
	weka/gui/experiment/*.class \
	weka/gui/explorer/*.class \
	weka/gui/visualize/*.class \
	weka/gui/visualize/*.props \
	weka/gui/treevisualizer/*.class \
	weka/gui/boundaryvisualizer/*.class \
	weka/gui/beans/*.class \
	weka/gui/beans/icons/*.gif \
	weka/gui/streams/*.class \
	; \
	jar cvf $(WEKAHOME)/weka-src.jar \
	weka/core/*.java \
	weka/core/converters/*.java \
	weka/classifiers/*.java \
	weka/classifiers/bayes/*.java \
	weka/classifiers/evaluation/*.java \
	weka/classifiers/functions/*.java \
	weka/classifiers/lazy/*.java \
	weka/classifiers/meta/*.java \
	weka/classifiers/misc/*.java \
	weka/classifiers/rules/*.java \
	weka/classifiers/trees/*.java \
	weka/classifiers/trees/j48/*.java \
	weka/classifiers/trees/m5/*.java \
	weka/classifiers/trees/lmt/*.java \
	weka/classifiers/lazy/kstar/*.java \
	weka/classifiers/functions/neural/*.java \
	weka/classifiers/trees/adtree/*.java \
	weka/classifiers/rules/part/*.java \
	weka/filters/*.java \
	weka/filters/unsupervised/*.java \
	weka/filters/unsupervised.attribute/*.java \
	weka/filters/unsupervised.instance/*.java \
	weka/filters/supervised/*.java \
	weka/filters/supervised.attribute/*.java \
	weka/filters/supervised.instance/*.java \
	weka/estimators/*java \
	weka/associations/*.java \
	weka/clusterers/*.java \
	weka/attributeSelection/*.java \
	weka/datagenerators/*.java \
	weka/experiment/*.java \
	weka/experiment/*.props \
	weka/experiment/*.policy \
	weka/gui/*.java \
	weka/gui/*.props \
	weka/gui/*.jpg \
	weka/gui/*.gif \
	weka/gui/experiment/*.java \
	weka/gui/explorer/*.java \
	weka/gui/visualize/*.java \
	weka/gui/visualize/*.props \
	weka/gui/treevisualizer/*.java \
	weka/gui/boundaryvisualizer/*.java \
	weka/gui/beans/*.java \
	weka/gui/beans/icons/*.gif \
	weka/gui/streams/*.java \
	;\
	rm manifest.tmp ;\
	jar cvf remoteEngine.jar \
	weka/core/Queue*.class \
	weka/experiment/*_*.class \
	weka/experiment/RemoteEngine*.class \
	weka/experiment/Compute.class \
	weka/experiment/Task.class \
	weka/experiment/TaskStatusInfo.class \
	;\
	cp weka/experiment/remote.policy . ; \
	cp weka/experiment/DatabaseUtils.props . ; \
	jar cvf $(WEKAHOME)/remoteExperimentServer.jar \
	remoteEngine.jar \
	remote.policy \
	DatabaseUtils.props \
	;\
	rm remoteEngine.jar remote.policy DatabaseUtils.props ;\
	(mkdir $(WEKAHOME)/doc 2>&1 ) >/dev/null ; \
	javadoc -J-mx100m -public -author -version -1.1 -d $(WEKAHOME)/doc \
	weka.core \
	weka.core.converters \
	weka.classifiers \
	weka.classifiers.bayes \
	weka.classifiers.evaluation \
	weka.classifiers.functions \
	weka.classifiers.lazy \
	weka.classifiers.meta \
	weka.classifiers.misc \
	weka.classifiers.rules \
	weka.classifiers.trees \
	weka.classifiers.trees.j48 \
	weka.classifiers.trees.m5 \
	weka.classifiers.trees.lmt \
	weka.classifiers.lazy.kstar \
	weka.classifiers.functions.neural \
	weka.classifiers.trees.adtree \
	weka.classifiers.rules.part \
	weka.filters \
	weka.filters.unsupervised \
	weka.filters.unsupervised.attribute \
	weka.filters.unsupervised.instance \
	weka.filters.supervised \
	weka.filters.supervised.attribute \
	weka.filters.supervised.instance \
	weka.estimators \
	weka.associations \
	weka.clusterers \
	weka.attributeSelection \
	weka.datagenerators \
	weka.experiment \
	weka.gui \
	weka.gui.experiment \
	weka.gui.explorer \
	weka.gui.visualize \
	weka.gui.boundaryvisualizer \
	weka.gui.treevisualizer \
	weka.gui.beans \
	weka.gui.streams; \
	for page in `ls $(WEKAHOME)/doc/*.html`; \
	do cat $$page | sed "s/Index<\/A>  <A HREF=\"help.html\"/Index<\/a>  <a href=\"http:\/\/www.cs.waikato.ac.nz\/ml\/weka\/index.html\">WEKA\'s home<\/a>  <A HREF=\"help.html\"/g" > $$page.temp; mv $$page.temp $$page; \
	cat $$page | sed "s/Index<\/A><\/PRE/Index<\/a>  <a href=\"http:\/\/www.cs.waikato.ac.nz\/ml\/weka\/index.html\">WEKA\'s home<\/a><\/PRE/g" > $$page.temp; mv $$page.temp $$page; done; \
	sed 's/help.html/..\/Tutorial.pdf/g' \
	< $(WEKAHOME)/doc/packages.html > $(WEKAHOME)/packages_temp.html; \
	mv $(WEKAHOME)/packages_temp.html $(WEKAHOME)/doc/packages.html \
	)

# Creates a snapshot of the entire directory structure
archive :
	(cd ..; \
	tar czf archive/weka`date +%d%b%Y`.tar.gz  weka)
