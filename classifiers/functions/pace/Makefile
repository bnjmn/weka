#/usr/local/jdk1.1.3/bin/java :   java interpreter
JAVA = javac 

SOURCES = DiscreteFunction.java MixtureDistribution.java \
	ChisqMixture.java  NormalMixture.java	PaceMatrix.java \
	PaceRegression.java \
	DoubleVector.java IntVector.java \
	ExponentialFormat.java \
	FloatingPointFormat.java FlexibleDecimalFormat.java \
        Maths.java Matrix.java 
CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVA) $< 

#-------------------------------------------------------------------------------#

pace: $(CLASSFILES)

all :	
	make clean; make

clean : 
	$(RM) $(CLASSFILES) *~ core

jar:
	(cd ..;\
	jar -c pace/*.java pace/*.class pace/util/*.java pace/util/*.class > pace.`date +%d%b%Y`.jar; \
	rm pace.jar -f; \
	ln -s pace.`date +%d%b%Y`.jar pace.jar )

archive : 
	tar -c -z $(SOURCES) Makefile > versions/pace.`date +%d%b%Y`.tgz

doc :  $(SOURCES)
	javadoc -public -author -version -d doc pace pace.util


