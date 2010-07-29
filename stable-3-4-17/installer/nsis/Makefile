# Compiles the Java classes.
#
# Author:  FracPete (fracpete at waikato dot ac dot nz)
# Version: $Revision: 1.1.2.2 $

all:	GenerateSetup RunWeka

# for generating the class that creates the actual NSIS scripts
GenerateSetup:
	javac -source 1.4 -target 1.4 $@.java

# the Weka launcher class
RunWeka:
	javac -source 1.4 -target 1.4 templates/$@.java

# removes the class files
clean:
	rm -f GenerateSetup.class
	rm -f templates/RunWeka.class

