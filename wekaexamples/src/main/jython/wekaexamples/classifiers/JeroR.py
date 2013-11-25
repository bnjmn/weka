#
#    This program is free software; you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation; either version 2 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program; if not, write to the Free Software
#    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
#

#    JeroR.java
#    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand

import jarray
import sys

import weka.classifiers.AbstractClassifier as AbstractClassifier
import weka.classifiers.Evaluation as Evaluation
import weka.core.Capabilities as Capabilities
import weka.core.Capabilities.Capability as Capability
import weka.core.Instance as Instance
import weka.core.Instances as Instances
import weka.core.scripting.JythonSerializableObject as JythonSerializableObject
import weka.core.Utils as Utils

class JeroR (AbstractClassifier, JythonSerializableObject):
    """
    JeroR is a Jython implementation of the Weka classifier ZeroR
    
    'author' -- FracPete (fracpete at waikato dot ac dot nz)
    
    'version' -- $Revision$
    """

    # the documentation can be generated with HappyDoc:
    #    http://happydoc.sourceforge.net/
    # Example command:
    #     happydoc --title Weka -d ./doc ./src

    # the chosen class value
    __ClassValue = Utils.missingValue()
    
    # the class attribute
    __Class = None
    
    # the counts for each class label
    __Counts = None
    
    def listOptions(self):
        """
        Returns an enumeration describing the available options.
        
        Return:
         
            an enumeration of all the available options.
        """
         
        return AbstractClassifier.listOptions(self)

    def setOptions(self, options):
        """
        Parses a given list of options.
         
        Parameter(s):
        
            'options' -- the list of options as an array of strings
        """
        
        AbstractClassifier.setOptions(self, options)
        
        return

    def getOptions(self):
        """
        Gets the current settings of the Classifier as string array.
         
        Return:
         
            an array of strings suitable for passing to setOptions
        """
         
        return AbstractClassifier.getOptions(self)
    
    def getCapabilities(self):
        """
        returns the capabilities of this classifier
        
        Return:
        
            the capabilities of this classifier
        """

        result = AbstractClassifier.getCapabilities(self)
    
        # attributes
        result.enable(Capability.NOMINAL_ATTRIBUTES)
        result.enable(Capability.NUMERIC_ATTRIBUTES)
        result.enable(Capability.DATE_ATTRIBUTES)
        result.enable(Capability.STRING_ATTRIBUTES)
        result.enable(Capability.RELATIONAL_ATTRIBUTES)
        result.enable(Capability.MISSING_VALUES)
    
        # class
        result.enable(Capability.NOMINAL_CLASS)
        result.enable(Capability.NUMERIC_CLASS)
        result.enable(Capability.DATE_CLASS)
        result.enable(Capability.MISSING_CLASS_VALUES)
    
        # instances
        result.setMinimumNumberInstances(0)
        
        return result
    
    def buildClassifier(self, instances):
        """
        builds the ZeroR classifier with the given data
        
        Parameter(s):
        
            'instances' -- the data to build the classifier from
        """
        
        self.getCapabilities().testWithFail(instances)
    
        # remove instances with missing class
        instances = Instances(instances)
        instances.deleteWithMissingClass()
        
        sumOfWeights      = 0
        self.__Class      = instances.classAttribute()
        self.__ClassValue = 0
        self.__Counts     = None
        
        if (instances.classAttribute().isNumeric()):
            self.__Counts = None
        elif (instances.classAttribute().isNominal()):
            self.__Counts = jarray.zeros(instances.numClasses(), 'd')
            for i in range(len(self.__Counts)):
                self.__Counts[i] = 1
            sumOfWeights = instances.numClasses()

        enu = instances.enumerateInstances()
        while (enu.hasMoreElements()):
            instance = enu.nextElement()
            if (not instance.classIsMissing()):
                if (instances.classAttribute().isNominal()):
                    self.__Counts[int(instance.classValue())] += instance.weight()
                else:
                    self.__ClassValue += instance.weight() * instance.classValue()
                sumOfWeights += instance.weight()
            
        if (instances.classAttribute().isNumeric()):
            if (Utils.gr(sumOfWeights, 0)):
                self.__ClassValue /= sumOfWeights
        else:
            self.__ClassValue = Utils.maxIndex(self.__Counts)
            Utils.normalize(self.__Counts, sumOfWeights)
                    
        return
        
    def classifyInstance(self, instance):
        """
        returns the prediction for the given instance
        
        Parameter(s):
        
            'instance' -- the instance to predict the class value for

        Return:
        
            the prediction for the given instance
        """
        
        return self.__ClassValue
    
    def distributionForInstance(self, instance):
        """
        returns the class distribution for the given instance
        
        Parameter(s):
        
            'instance' -- the instance to calculate the class distribution for
            
        Return:
        
            the class distribution for the given instance
        """
        
        result = None
        if (self.__Counts == None):
            result    = jarray.zeros(1, 'd')
            result[0] = self.__ClassValue
        else:
            result = self.__Counts[:]
        
        return result
    
    def toString(self):
        """
        Prints a string representation of the classifier

        Return:
            
            string representation of the classifier
        """

        if (self.__Class ==  None):
            return "JeroR: No model built yet."
        if (self.__Counts == None):
            return "JeroR predicts class value: " + str(self.__ClassValue)
        else:
            return "JeroR predicts class value: " + str(self.__Class.value(int(self.__ClassValue)))

# simulating the Java "main" method
if __name__ == "__main__":
    # need to set the following jython registory value:
    #   python.security.respectJavaAccessibility=false
    #
    # Links:
    # - Python registry
    #   http://www.jython.org/docs/registry.html
    # - Accessing Java protected static members:
    #   http://www.jython.org/cgi-bin/faqw.py?req=all#3.5
    AbstractClassifier.runClassifier(JeroR(), sys.argv[1:])
