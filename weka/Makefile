#
# $Revision: 1.2 $
#

.PHONY: all optimized debug clean install archive doc

all : 
	(cd core; make all)
	(cd classifiers; make all)
	(cd filters; make all)
	(cd estimators; make all)
	(cd associations; make all)
	(cd clusterers; make all)
	(cd attributeSelection; make all)

optimized : 
	(cd core; make optimized)
	(cd classifiers; make optimized)
	(cd filters; make optimized)
	(cd estimators; make optimized)
	(cd associations; make optimized)
	(cd clusterers; make optimized)
	(cd attributeSelection; make optimized)

debug :
	(cd core; make debug)
	(cd classifiers; make debug)
	(cd filters; make debug)
	(cd estimators; make debug)
	(cd associations; make debug)
	(cd clusterers; make debug)
	(cd attributeSelection; make debug)

clean : 
	(cd core; make clean)
	(cd classifiers; make clean) 
	(cd filters; make clean)
	(cd estimators; make clean)
	(cd associations; make clean)
	(cd clusterers; make clean)
	(cd attributeSelection; make clean)

doc :
	(cd ..; \
	javadoc -public -author -version -d doc \
	weka.core \
	weka.classifiers \
	weka.classifiers.j48 \
	weka.classifiers.m5 \
	weka.filters \
	weka.estimators \
	weka.associations \
	weka.clusterers \
	weka.attributeSelection)

install : all
	(cd ..; \
	jar cvf $$JAWSHOME/weka.jar \
	weka/core/*.class \
	weka/classifiers/*.class \
	weka/classifiers/j48/*.class \
	weka/classifiers/m5/*.class \
	weka/filters/*.class \
	weka/estimators/*class \
	weka/associations/*.class \
	weka/clusterers/*.class \
	weka/attributeSelection/*.class ; \
	jar cvf $$JAWSHOME/weka-src.jar \
        weka/core/*.java \
        weka/classifiers/*.java \
        weka/classifiers/j48/*.java \
        weka/classifiers/m5/*.java \
        weka/filters/*.java \
        weka/estimators/*java \
        weka/associations/*.java \
	weka/clusterers/*.java \
	weka/attributeSelection/*.java)
	javadoc -public -author -version -d $$JAWSHOME/doc \
	weka.core \
	weka.classifiers \
	weka.classifiers.j48 \
	weka.classifiers.m5 \
	weka.filters \
	weka.estimators \
	weka.associations \
	weka.clusterers \
	weka.attributeSelection

archive :
	(cd ..; \
	tar czf archive/weka`date +%d%b%Y`.tar.gz  weka)





