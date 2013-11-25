import sys

import java.io.FileReader as FileReader
import java.lang.StringBuffer as StringBuffer
import java.lang.Boolean as Boolean

import weka.core.Instances as Instances
import weka.classifiers.trees.J48 as J48
import weka.classifiers.Evaluation as Evaluation
import weka.core.Range as Range
import weka.classifiers.evaluation.output.prediction.PlainText as PlainText

"""
An example of using Weka classifiers (i.e., J48) from within Jython.

Based on this code example:

    http://www.btbytes.com/2005/11/30/weka-j48-classifier-example-using-jython/

Commandline parameter(s):

    first parameter must be the ARFF file one wants to process with J48

Note: needs Weka 3.7.x to run (due to changes in the
      weka.classifiers.Evaluation class)

"""

# check commandline parameters
if (not (len(sys.argv) == 2)):
    print "Usage: UsingJ48Ext.py <ARFF-file>"
    sys.exit()

# load data file
print "Loading data..."
file = FileReader(sys.argv[1])
data = Instances(file)

# set the class Index - the index of the dependent variable
data.setClassIndex(data.numAttributes() - 1)

# create the model
evaluation = Evaluation(data)
output = PlainText()  # plain text output for predictions
output.setHeader(data)
buffer = StringBuffer() # buffer to use
output.setBuffer(buffer)
attRange = Range()  # no additional attributes output
outputDistribution = Boolean(False)  # we don't want distribution
j48 = J48()
j48.buildClassifier(data)  # only a trained classifier can be evaluated
evaluation.evaluateModel(j48, data, [output, attRange, outputDistribution])

# print out the built model
print "--> Generated model:\n"
print j48

print "--> Evaluation:\n"
print evaluation.toSummaryString()

print "--> Predictions:\n"
print buffer

