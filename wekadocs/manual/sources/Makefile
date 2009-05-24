# Manages the source code.
#
# Note: expects the WEKA classes to be present in the CLASSPATH environment
#       variable.
#
# Author: FracPete (fracpete at waikato dot ac dot nz)
# Version: $Revision$

ARCHIVE = sources.tar.gz

all: clean compile archive

clean:
	@echo "Cleaning..."
	-rm -f *.class
	-rm -f *~
	-rm -f *.tar.gz

compile: clean
	@echo "Compiling..."
	javac -classpath .:$(CLASSPATH) *.java

archive: compile
	@echo "Creating archive..."
	tar -czf $(ARCHIVE) *.java ../../COPYING

