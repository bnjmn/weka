import sys

import java.io.FileReader as FileReader
import weka.core.Instances as Instances
import weka.classifiers.trees.J48 as J48

"""
A simple example of using Weka classifiers (i.e., J48) from within Jython.

Based on this code example:

    http://www.btbytes.com/2005/11/30/weka-j48-classifier-example-using-jython/

Commandline parameter(s):

    first parameter must be the ARFF file one wants to process with J48

"""

# check commandline parameters
if (not (len(sys.argv) == 2)):
    print "Usage: UsingJ48.py <ARFF-file>"
    sys.exit()

# load data file
print "Loading data..."
file = FileReader(sys.argv[1])
data = Instances(file)

# set the class Index - the index of the dependent variable
data.setClassIndex(data.numAttributes() - 1)

# create the model
print "Training J48..."
j48 = J48()
j48.buildClassifier(data)

# print out the built model
print "Generated model:\n"
print j48