#
# $Revision: 1.17 $
#

JAVA = javac

.PHONY: all optimized debug clean install archive doc

all : 
	(cd core; make all JAVA=$(JAVA))
	(cd classifiers; make all JAVA=$(JAVA))
	(cd filters; make all JAVA=$(JAVA))
	(cd estimators; make all JAVA=$(JAVA))
	(cd associations; make all JAVA=$(JAVA))
	(cd clusterers; make all JAVA=$(JAVA))
	(cd attributeSelection; make all JAVA=$(JAVA))
	(cd experiment; make all JAVA=$(JAVA))
	(cd gui; make all JAVA=$(JAVA))

optimized : 
	(cd core; make optimized JAVA=$(JAVA))
	(cd classifiers; make optimized JAVA=$(JAVA))
	(cd filters; make optimized JAVA=$(JAVA))
	(cd estimators; make optimized JAVA=$(JAVA))
	(cd associations; make optimized JAVA=$(JAVA))
	(cd clusterers; make optimized JAVA=$(JAVA))
	(cd attributeSelection; make optimized JAVA=$(JAVA))
	(cd experiment; make optimized JAVA=$(JAVA))
	(cd gui; make optimized JAVA=$(JAVA))

debug :
	(cd core; make debug JAVA=$(JAVA))
	(cd classifiers; make debug JAVA=$(JAVA))
	(cd filters; make debug JAVA=$(JAVA))
	(cd estimators; make debug JAVA=$(JAVA))
	(cd associations; make debug JAVA=$(JAVA))
	(cd clusterers; make debug JAVA=$(JAVA))
	(cd attributeSelection; make debug JAVA=$(JAVA))
	(cd experiment; make debug JAVA=$(JAVA))
	(cd gui; make debug JAVA=$(JAVA))

clean : 
	(cd core; make clean)
	(cd classifiers; make clean) 
	(cd filters; make clean)
	(cd estimators; make clean)
	(cd associations; make clean)
	(cd clusterers; make clean)
	(cd attributeSelection; make clean)
	(cd experiment; make clean)
	(cd gui; make clean)

doc :
	(cd ..; \
	javadoc -J-mx100m -public -author -version -d doc \
	weka.core \
	weka.classifiers \
	weka.classifiers.j48 \
	weka.classifiers.m5 \
	weka.filters \
	weka.estimators \
	weka.associations \
	weka.clusterers \
	weka.attributeSelection \
	weka.experiment \
	weka.gui \
	weka.gui.experiment \
	weka.gui.explorer \
	weka.gui.streams; \
	for page in `ls doc/*.html`; \
	do cat $$page | sed "s/Index<\/a><\/pre>/Index<\/a>  <a href=\"http:\/\/www.cs.waikato.ac.nz\/ml\/weka\/index.html\">WEKA\'s home<\/a><\/pre>/g" > $$page.temp; mv $$page.temp $$page; done;\
	sed 's/API_users_guide.html/..\/Tutorial.pdf/g' \
	< doc/packages.html > packages_temp.html; \
	mv packages_temp.html doc/packages.html)

# Assumes any auxiliary classfiles are in the parent directory
# One of these must be SimpleCLI.class
install : all
	(cd ..; \
	echo "Main-Class: weka.gui.GUIChooser" > manifest.tmp ;\
	jar cvfm $$JAWSHOME/weka.jar manifest.tmp \
	weka/core/*.class \
	weka/classifiers/*.class \
	weka/classifiers/j48/*.class \
	weka/classifiers/m5/*.class \
	weka/filters/*.class \
	weka/estimators/*class \
	weka/associations/*.class \
	weka/clusterers/*.class \
	weka/attributeSelection/*.class \
	weka/experiment/*.class \
	weka/experiment/*.props \
	weka/gui/*.class \
	weka/gui/*.props \
	weka/gui/*.jpg \
	weka/gui/*.gif \
	weka/gui/experiment/*.class \
	weka/gui/explorer/*.class \
	weka/gui/streams/*.class \
	; \
	jar cvf $$JAWSHOME/weka-src.jar \
	weka/core/*.java \
	weka/classifiers/*.java \
	weka/classifiers/j48/*.java \
	weka/classifiers/m5/*.java \
	weka/filters/*.java \
	weka/estimators/*java \
	weka/associations/*.java \
	weka/clusterers/*.java \
	weka/attributeSelection/*.java \
	weka/experiment/*.java \
	weka/gui/*.java \
	weka/gui/experiment/*.java \
	weka/gui/explorer/*.java \
	weka/gui/streams/*.java \
	;\
	rm manifest.tmp )
	javadoc -J-mx100m -public -author -version -d $$JAWSHOME/doc \
	weka.core \
	weka.classifiers \
	weka.classifiers.j48 \
	weka.classifiers.m5 \
	weka.filters \
	weka.estimators \
	weka.associations \
	weka.clusterers \
	weka.attributeSelection \
	weka.experiment \
	weka.gui \
	weka.gui.experiment \
	weka.gui.explorer \
	weka.gui.streams; \
	for page in `ls $$JAWSHOME/doc/*.html`; \
	do cat $$page | sed "s/Index<\/a><\/pre>/Index<\/a>  <a href=\"http:\/\/www.cs.waikato.ac.nz\/ml\/weka\/index.html\">WEKA\'s home<\/a><\/pre>/g" > $$page.temp; mv $$page.temp $$page; done;\
	sed 's/API_users_guide.html/..\/Tutorial.pdf/g' \
	< $$JAWSHOME/doc/packages.html > $$JAWSHOME/packages_temp.html; \
	mv $$JAWSHOME/packages_temp.html $$JAWSHOME/doc/packages.html

archive :
	(cd ..; \
	tar czf archive/weka`date +%d%b%Y`.tar.gz  weka)





