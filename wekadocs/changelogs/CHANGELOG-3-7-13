------------------------------------------------------------------------
r11405 | mhall | 2014-12-17 12:09:36 +1300 (Wed, 17 Dec 2014) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/version.txt

Trunk is now at 3-7-13-SNAPSHOT
------------------------------------------------------------------------
r11410 | eibe | 2014-12-19 13:56:14 +1300 (Fri, 19 Dec 2014) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/clusterers/Cobweb.java
   M /trunk/weka/src/main/java/weka/core/Instances.java
   M /trunk/weka/src/main/java/weka/core/Utils.java
   M /trunk/weka/src/main/java/weka/experiment/Stats.java
   M /trunk/weka/src/test/java/weka/core/InstancesTest.java
   M /trunk/weka/src/test/java/weka/core/UtilsTest.java
   M /trunk/weka/src/test/resources/wekarefs/weka/classifiers/rules/M5RulesTest.ref
   M /trunk/weka/src/test/resources/wekarefs/weka/classifiers/trees/M5PTest.ref
   M /trunk/weka/src/test/resources/wekarefs/weka/clusterers/CobwebTest.ref

Thanks to Benjamin Weber, we now have a numerically stable implementation of variance calculation in Instances, Utils, and Stats, in addition to an extensive testing framework.
------------------------------------------------------------------------
r11418 | mhall | 2014-12-19 19:53:11 +1300 (Fri, 19 Dec 2014) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/converters/DatabaseLoader.java

Fixed an option handling bug
------------------------------------------------------------------------
r11419 | mhall | 2014-12-19 19:54:46 +1300 (Fri, 19 Dec 2014) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/json/JSONNode.java

Added a method (that presumably was missing) for adding objects into arrays
------------------------------------------------------------------------
r11420 | mhall | 2014-12-19 19:59:01 +1300 (Fri, 19 Dec 2014) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/beans/LogPanel.java

Small bug fix
------------------------------------------------------------------------
r11424 | eibe | 2014-12-20 13:08:38 +1300 (Sat, 20 Dec 2014) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/experiment/Stats.java

Standard deviation is no longer NaN when we see a single case with weight > 1.
------------------------------------------------------------------------
r11443 | eibe | 2014-12-25 14:42:58 +1300 (Thu, 25 Dec 2014) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/Instances.java

variance() and variances() no longer return NaN if there are is only instance.
------------------------------------------------------------------------
r11444 | eibe | 2014-12-25 15:03:07 +1300 (Thu, 25 Dec 2014) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/clusterers/SimpleKMeans.java

Cluster sizes are now based on the weights of the instances in each cluster. Also updated Javadoc for corresponding method.
------------------------------------------------------------------------
r11447 | mhall | 2015-01-01 20:39:22 +1300 (Thu, 01 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/PrincipalComponents.java

Was missing unary class in capabilities checking - fixed.
------------------------------------------------------------------------
r11451 | eibe | 2015-01-05 15:58:23 +1300 (Mon, 05 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/clusterers/CheckClusterer.java
   M /trunk/weka/src/main/java/weka/clusterers/EM.java
   M /trunk/weka/src/test/java/weka/clusterers/CobwebTest.java

Fixed bugs in CheckClusterer where evaluation was not performed, so an empty string was the output of evaluation... Fixed unit test for Cobweb by fixing seed < 0 so that output for incremental and non-incremtal run is the same. Fixed bug in EM with new version of variance calculation, which returns NaN when the weight of instances is <=1, not zero. Fixed another problem in EM: CV-likelihood became NaN if test set in internal CV had zero weight.
------------------------------------------------------------------------
r11453 | eibe | 2015-01-05 16:16:48 +1300 (Mon, 05 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/attributeSelection/PrincipalComponents.java

PrincipalComponents attribute evaluator can now also deal with unary class problems.
------------------------------------------------------------------------
r11454 | eibe | 2015-01-05 16:48:01 +1300 (Mon, 05 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/visualize/Plot2D.java

When an instance with a string value is selected in the Visualize panel, the string is now shown correctly in the instance information window that pops up. Previously, only the index of the string value was shown.
------------------------------------------------------------------------
r11458 | eibe | 2015-01-07 12:14:23 +1300 (Wed, 07 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/evaluation/output/prediction/PlainText.java
   M /trunk/weka/src/main/java/weka/core/Utils.java

Utils now has two new methods for padded output of strings that allow overflow. Also, the code of the corresponding two methods that do not allow overflow has been replaced to use String.format, to make it more efficient. PlainText output now uses the methods that allow overflow; also, the space available for indices has been increased from 6 to 9 characters, before overflow occurs.
------------------------------------------------------------------------
r11461 | eibe | 2015-01-12 13:53:51 +1300 (Mon, 12 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/meta/AttributeSelectedClassifier.java
   M /trunk/weka/src/main/java/weka/classifiers/meta/Bagging.java
   M /trunk/weka/src/main/java/weka/classifiers/meta/FilteredClassifier.java
   M /trunk/weka/src/main/java/weka/classifiers/meta/IterativeClassifierOptimizer.java
   M /trunk/weka/src/main/java/weka/classifiers/meta/RandomCommittee.java
   M /trunk/weka/src/main/java/weka/classifiers/meta/RandomSubSpace.java
   M /trunk/weka/src/main/java/weka/classifiers/meta/RandomizableFilteredClassifier.java

Removed code that deleted instances with missing class values in buildClassifier() because base learner may be able to do semi-supervised learning with missing class values.
------------------------------------------------------------------------
r11463 | eibe | 2015-01-12 14:50:53 +1300 (Mon, 12 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/trees/RandomForest.java

Out-of-bag error was being calculated even if the user did not want this.
------------------------------------------------------------------------
r11465 | eibe | 2015-01-12 17:32:48 +1300 (Mon, 12 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/meta/Bagging.java

Now Bagging (and RandomForest) can process datasets with more than 22 million instances. This one was very mysterious at first.
------------------------------------------------------------------------
r11468 | mhall | 2015-01-13 21:07:38 +1300 (Tue, 13 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/functions/SGD.java

Small fix to the toString() method. It now outputs the correct name of the loss function when epsilon insensitive or Huber loss is selected.
------------------------------------------------------------------------
r11470 | eibe | 2015-01-14 09:56:18 +1300 (Wed, 14 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/filters/supervised/attribute/NominalToBinary.java

Now correctly declares that it can deal with missing class values. This was not declared as a capability previously.
------------------------------------------------------------------------
r11472 | eibe | 2015-01-14 17:25:18 +1300 (Wed, 14 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/Instances.java

The attributeStats() and numDistinctValues() methods now use hashing instead of sorting. This seems to be a little faster for large datasets with numeric attributes. It should be faster, but involves a lot of object creation. numDistinctValues() is now consistent with attributeStats for nominal attributes.
------------------------------------------------------------------------
r11496 | eibe | 2015-01-19 14:46:39 +1300 (Mon, 19 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/lib/JFlex.jar
   M /trunk/weka/lib/README
   M /trunk/weka/lib/java-cup.jar
   M /trunk/weka/parsers.xml
   M /trunk/weka/src/main/java/weka/core/json/Parser.java
   M /trunk/weka/src/main/java/weka/core/json/Scanner.java
   M /trunk/weka/src/main/java/weka/core/json/Scanner.jflex
   M /trunk/weka/src/main/java/weka/core/json/sym.java
   M /trunk/weka/src/main/java/weka/core/mathematicalexpression/Parser.java
   M /trunk/weka/src/main/java/weka/core/mathematicalexpression/Scanner.java
   M /trunk/weka/src/main/java/weka/core/mathematicalexpression/Scanner.jflex
   M /trunk/weka/src/main/java/weka/core/mathematicalexpression/sym.java
   M /trunk/weka/src/main/java/weka/filters/unsupervised/instance/subsetbyexpression/Parser.java
   M /trunk/weka/src/main/java/weka/filters/unsupervised/instance/subsetbyexpression/Scanner.java
   M /trunk/weka/src/main/java/weka/filters/unsupervised/instance/subsetbyexpression/Scanner.jflex
   M /trunk/weka/src/main/java/weka/filters/unsupervised/instance/subsetbyexpression/sym.java
   A /trunk/weka/src/test/java/weka/core/AttributeExpressionTest.java
   M /trunk/weka/src/test/java/weka/core/MathematicalExpressionTest.java
   M /trunk/weka/src/test/java/weka/filters/unsupervised/instance/SubsetByExpressionTest.java

Patch from Ben with new tests for the different languages for mathematical expressions.
------------------------------------------------------------------------
r11497 | eibe | 2015-01-19 16:27:26 +1300 (Mon, 19 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/parsers.xml
   M /trunk/weka/src/main/java/weka/classifiers/CostMatrix.java
   D /trunk/weka/src/main/java/weka/core/AttributeExpression.java
   D /trunk/weka/src/main/java/weka/core/MathematicalExpression.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/common
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/common/IfElseMacro.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/common/JavaMacro.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/common/MacroDeclarationsCompositor.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/common/MathFunctions.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/common/NoMacros.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/common/NoVariables.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/common/Operators.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/common/Primitives.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/common/SimpleVariableDeclarations.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/common/VariableDeclarationsCompositor.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/core
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/core/Macro.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/core/MacroDeclarations.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/core/Node.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/core/SemanticException.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/core/SyntaxException.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/core/VariableDeclarations.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/package-info.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/parser
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/parser/Parser.cup
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/parser/Parser.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/parser/Scanner.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/parser/Scanner.jflex
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/parser/sym.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/weka
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/weka/InstancesHelper.java
   A /trunk/weka/src/main/java/weka/core/expressionlanguage/weka/StatsHelper.java
   D /trunk/weka/src/main/java/weka/core/mathematicalexpression
   M /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/AddExpression.java
   M /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/KernelFilter.java
   M /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/MathExpression.java
   M /trunk/weka/src/main/java/weka/filters/unsupervised/instance/SubsetByExpression.java
   D /trunk/weka/src/main/java/weka/filters/unsupervised/instance/subsetbyexpression
   D /trunk/weka/src/test/java/weka/core/AttributeExpressionTest.java
   D /trunk/weka/src/test/java/weka/core/MathematicalExpressionTest.java
   A /trunk/weka/src/test/java/weka/core/expressionlanguage
   A /trunk/weka/src/test/java/weka/core/expressionlanguage/ExpressionLanguageTest.java
   M /trunk/weka/src/test/java/weka/filters/unsupervised/attribute/AddExpressionTest.java
   M /trunk/weka/src/test/java/weka/filters/unsupervised/attribute/MathExpressionTest.java
   M /trunk/weka/src/test/java/weka/filters/unsupervised/instance/SubsetByExpressionTest.java
   M /trunk/weka/src/test/resources/wekarefs/weka/filters/unsupervised/attribute/AddExpressionTest.ref

This is the grand unification, designed and written by Ben Weber, of all the different arithmetic/logical expression languages that have previously been used in WEKA (e.g., in MathExpression, AddExpression, and SubsetByExpression). For more information on the new framework, check package-info.java in weka/core/expressionlanguage. The corresponding filters should now be much faster than previously, because expressions are now compiled into Java classes before they are used and then executed for every instance in a dataset. (Previously, an expression was interpreted from scratch for every single instance.)
------------------------------------------------------------------------
r11498 | mhall | 2015-01-20 09:37:29 +1300 (Tue, 20 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/evaluation/Evaluation.java

Changed so that classifier is not copied in memory if it has been successfully loaded from a file. Making copies of very large classifiers via object serialization in memory can result in array sizes that exceed the VM limit.
------------------------------------------------------------------------
r11504 | eibe | 2015-01-20 16:40:41 +1300 (Tue, 20 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/datagenerators/classifiers/regression/Expression.java

Fixed problem that caused assertion to fail when executing unit test.
------------------------------------------------------------------------
r11506 | eibe | 2015-01-21 12:30:24 +1300 (Wed, 21 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/Attribute.java
   M /trunk/weka/src/main/java/weka/core/SparseInstance.java
   M /trunk/weka/src/main/java/weka/core/StringLocator.java
   M /trunk/weka/src/main/java/weka/core/TestInstances.java
   M /trunk/weka/src/main/java/weka/core/converters/ArffLoader.java
   M /trunk/weka/src/main/java/weka/core/converters/ArffSaver.java
   M /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/RandomProjection.java
   M /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/StringToNominal.java
   M /trunk/weka/src/main/java/weka/gui/beans/ClassValuePickerCustomizer.java
   M /trunk/weka/src/test/java/weka/filters/unsupervised/attribute/NominalToStringTest.java
   M /trunk/weka/src/test/java/weka/filters/unsupervised/attribute/RandomProjectionTest.java

Eliminated dummy attribute value hack for string attributes. The toString() method is now slower for SparseInstance objects because all attributes are scanned for each instance, so that string/relational attributes can be identified and their values always output. The StringToNominal filter is now much more memory-efficient because it no longer requires a deep copy of the training data.
------------------------------------------------------------------------
r11510 | eibe | 2015-01-22 12:47:27 +1300 (Thu, 22 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/Utils.java

doubleToString() now uses built-in DecimalFormat class. This changes the output in some cases, in particular, for very large numbers. As some classifiers use getRandomNumberGenerator() from Instances, which calls toString() on a randomly chosen instance, which in turn calls doubleToString(), this can mean that a different initialization for the random generator will now be used, depending on the data in the instance.
------------------------------------------------------------------------
r11511 | eibe | 2015-01-22 14:09:46 +1300 (Thu, 22 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/Utils.java

DecimalFormat is now a ThreadLocal variable.
------------------------------------------------------------------------
r11512 | eibe | 2015-01-22 16:01:25 +1300 (Thu, 22 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/MergeInfrequentNominalValues.java

Added option to use short IDs for merged attribute values. This is important when merging many values.
------------------------------------------------------------------------
r11516 | eibe | 2015-01-27 15:44:39 +1300 (Tue, 27 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/PrincipalComponents.java

Now almost twice as fast because only upper triangular covariance matrix needs to be explicitly computed. (Thanks to Chris Beckham for this observation!) Code has also been simplified significantly.
------------------------------------------------------------------------
r11518 | mhall | 2015-01-27 20:31:38 +1300 (Tue, 27 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/attributeSelection/PrincipalComponents.java

Now almost twice as fast because only upper triangular covariance matrix needs to be explicitly computed. (Thanks to Chris Beckham for this observation) Code has also been simplified significantly.
------------------------------------------------------------------------
r11520 | eibe | 2015-01-28 11:12:00 +1300 (Wed, 28 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/NumericToBinary.java
   M /trunk/weka/src/test/java/weka/filters/unsupervised/attribute/NumericToBinaryTest.java

NumericToBinary now supports selection of attributes to binarize (thanks to Chris Beckham).
------------------------------------------------------------------------
r11526 | eibe | 2015-01-29 09:28:45 +1300 (Thu, 29 Jan 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/Utils.java

Floating-point numbers are now output with . as the decimal separator regardless of locale.
------------------------------------------------------------------------
r11535 | eibe | 2015-02-03 11:50:12 +1300 (Tue, 03 Feb 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/StringToWordVector.java

Now uses Utils.forName() in setOptions() so that abbreviated class names can be used for stemmers, tokenizers, and stopword handlers.
------------------------------------------------------------------------
r11536 | eibe | 2015-02-03 12:03:43 +1300 (Tue, 03 Feb 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/bayes/NaiveBayesMultinomialText.java
   M /trunk/weka/src/main/java/weka/classifiers/functions/SGDText.java

Now use Utils.forName() in setOptions() so that abbreviated class names can be used for stemmers, tokenizers, and stopword handlers.
------------------------------------------------------------------------
r11542 | eibe | 2015-02-04 12:55:14 +1300 (Wed, 04 Feb 2015) | 5 lines
Changed paths:
   M /trunk/weka/src/main/java/weka/experiment/PairedTTester.java
   M /trunk/weka/src/main/java/weka/experiment/ResultMatrix.java
   M /trunk/weka/src/main/java/weka/gui/experiment/ResultsPanel.java

ResultsPanel now outputs complete command-line specification of PairedTTester. 

PairedTTester now allows complete specification of ResultMatrix at the command-line and has option to turn off header output at command-line. 

ResultMatrix has small change in setOptions(), so that rows and columns are always enumerated now when printing of the corresponding names is turned off. Also, the keys for the columns are no longer shown if enumeration of columns is turned off.
------------------------------------------------------------------------
r11551 | eibe | 2015-02-06 14:05:37 +1300 (Fri, 06 Feb 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/NumericToBinary.java

Now has a getOptions() method.
------------------------------------------------------------------------
r11552 | mhall | 2015-02-09 12:13:54 +1300 (Mon, 09 Feb 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/expressionlanguage/common/Operators.java
   M /trunk/weka/src/main/java/weka/core/expressionlanguage/common/Primitives.java
   M /trunk/weka/src/main/java/weka/core/expressionlanguage/weka/InstancesHelper.java

Made a bunch of inner classes Serializable
------------------------------------------------------------------------
r11554 | mhall | 2015-02-09 13:25:31 +1300 (Mon, 09 Feb 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/converters/CSVLoader.java

Now makes sure that any single quotes around date values are removed before parsing
------------------------------------------------------------------------
r11556 | eibe | 2015-02-12 10:02:01 +1300 (Thu, 12 Feb 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/clusterers/Cobweb.java

Now has command-line option for saving instance data.
------------------------------------------------------------------------
r11566 | eibe | 2015-02-16 17:09:59 +1300 (Mon, 16 Feb 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/functions/SimpleLogistic.java
   M /trunk/weka/src/main/java/weka/classifiers/trees/LMT.java
   M /trunk/weka/src/main/java/weka/classifiers/trees/lmt/LMTNode.java
   M /trunk/weka/src/main/java/weka/classifiers/trees/lmt/LogisticBase.java

Made it possible for the user to specify the number of decimal places to use for the output of coefficients.
------------------------------------------------------------------------
r11568 | mhall | 2015-02-17 14:15:25 +1300 (Tue, 17 Feb 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/AbstractClassifier.java
   M /trunk/weka/src/main/java/weka/classifiers/functions/SimpleLogistic.java
   M /trunk/weka/src/main/java/weka/classifiers/trees/LMT.java
   M /trunk/weka/src/main/java/weka/classifiers/trees/lmt/LogisticBase.java

Moved the num-decimal-places option to AbstractClassifier
------------------------------------------------------------------------
r11570 | mhall | 2015-02-17 14:26:00 +1300 (Tue, 17 Feb 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/AbstractClassifier.java
   M /trunk/weka/src/main/java/weka/classifiers/functions/LinearRegression.java

listOptions in AbstractClassifier now uses the m_numDecimalPlaces variable when printing the default for numDecimalPlaces (so that classifiers can override the default in their constructor); LinearRegression now honours the numDecimalPlaces option
------------------------------------------------------------------------
r11597 | mhall | 2015-02-27 15:06:24 +1300 (Fri, 27 Feb 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/ClassCache.java

Now no longer throws a null pointer exception in the case of naughty jar files that don't have a manifest
------------------------------------------------------------------------
r11602 | mhall | 2015-02-27 22:21:46 +1300 (Fri, 27 Feb 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/converters/CSVLoader.java

Added an option to force an attribute to be numeric. Useful in the case where a CSV column is all missing and you want its type in the ARFF file to be numeric rather than string
------------------------------------------------------------------------
r11609 | mhall | 2015-03-02 14:41:07 +1300 (Mon, 02 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/trees/RandomForest.java

finalizeAggregation() was not setting the correct number of trees (affecting textual output only) - fixed
------------------------------------------------------------------------
r11613 | mhall | 2015-03-02 20:42:13 +1300 (Mon, 02 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/functions/supportVector/RegOptimizer.java

Made number of kernel evals a long
------------------------------------------------------------------------
r11615 | mhall | 2015-03-02 23:39:52 +1300 (Mon, 02 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/trees/REPTree.java

Added missing super.setOptions() call
------------------------------------------------------------------------
r11644 | mhall | 2015-03-11 11:10:19 +1300 (Wed, 11 Mar 2015) | 1 line
Changed paths:
   A /trunk/weka/src/main/java/weka/core/EnvironmentProperties.java
   M /trunk/weka/src/main/java/weka/core/Utils.java
   M /trunk/weka/src/main/java/weka/core/logging/Logger.java
   M /trunk/weka/src/main/java/weka/core/logging/Logging.props

System set properties will now have presedence over any property values that are loaded via Utils.readProperties(). This easily allows individual properties to be overriden, without having to edit property files,  when launching from the command line with the -D jvm flag. Changed the property key values used by core logging to be qualified with weka.core.logging.
------------------------------------------------------------------------
r11646 | mhall | 2015-03-11 11:48:55 +1300 (Wed, 11 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/logging/Logger.java

Removed some debugging output
------------------------------------------------------------------------
r11648 | mhall | 2015-03-11 16:16:50 +1300 (Wed, 11 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/PropertySheetPanel.java

Added a constructor with an argument to specify whether the about panel should appear or not
------------------------------------------------------------------------
r11650 | mhall | 2015-03-11 16:47:21 +1300 (Wed, 11 Mar 2015) | 1 line
Changed paths:
   A /trunk/weka/src/main/java/weka/gui/ProgrammaticProperty.java

Initial import of a method annotation that can be used to indicate that a property should not be exposed in the GOE
------------------------------------------------------------------------
r11652 | mhall | 2015-03-11 16:48:27 +1300 (Wed, 11 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/PropertySheetPanel.java

Now checks for the ProgrammaticProperty method annotation
------------------------------------------------------------------------
r11654 | mhall | 2015-03-12 22:10:30 +1300 (Thu, 12 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/EnvironmentProperties.java

Made the environment variables field transient
------------------------------------------------------------------------
r11657 | mhall | 2015-03-13 11:40:29 +1300 (Fri, 13 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/experiment/ResultsPanel.java

Added a button to popup the Explorer in order to analyse the current set of raw results
------------------------------------------------------------------------
r11658 | mhall | 2015-03-13 14:21:49 +1300 (Fri, 13 Mar 2015) | 1 line
Changed paths:
   A /trunk/weka/src/main/java/weka/gui/EnvironmentField.java
   A /trunk/weka/src/main/java/weka/gui/FileEnvironmentField.java

Initial import. Effectively moved these from weka/gui/beans. The versions in weka/gui/beans remain, but will be deprecated
------------------------------------------------------------------------
r11660 | mhall | 2015-03-13 17:06:47 +1300 (Fri, 13 Mar 2015) | 1 line
Changed paths:
   A /trunk/weka/src/main/java/weka/gui/PasswordProperty.java
   A /trunk/weka/src/main/java/weka/gui/PropertyDisplayName.java

Initial import
------------------------------------------------------------------------
r11662 | mhall | 2015-03-13 17:08:03 +1300 (Fri, 13 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/PropertySheetPanel.java

Now checks for the new PropertyDisplayName and PasswordProperty annotations
------------------------------------------------------------------------
r11664 | mhall | 2015-03-13 17:09:15 +1300 (Fri, 13 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/converters/DatabaseSaver.java

Now uses the new PropertyDisplayName and PasswordProperty annotations
------------------------------------------------------------------------
r11666 | mhall | 2015-03-13 17:11:28 +1300 (Fri, 13 Mar 2015) | 1 line
Changed paths:
   A /trunk/weka/src/main/java/weka/gui/PasswordField.java

Initial import
------------------------------------------------------------------------
r11668 | mhall | 2015-03-13 21:58:55 +1300 (Fri, 13 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/PropertyDisplayName.java

Fixed spelling error in javadoc
------------------------------------------------------------------------
r11670 | mhall | 2015-03-13 22:57:14 +1300 (Fri, 13 Mar 2015) | 1 line
Changed paths:
   A /trunk/weka/src/main/java/weka/gui/PropertyDisplayOrder.java

Initial import
------------------------------------------------------------------------
r11672 | mhall | 2015-03-13 22:59:18 +1300 (Fri, 13 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/converters/DatabaseSaver.java

Now uses the new PropertyDisplayOrder annotation to control the order that properties are displayed by the GOE
------------------------------------------------------------------------
r11674 | mhall | 2015-03-13 23:00:46 +1300 (Fri, 13 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/PropertySheetPanel.java

Now looks for the  new PropertyDisplayOrder annotation in order to determine the order that property editors should be displayed
------------------------------------------------------------------------
r11676 | mhall | 2015-03-13 23:03:02 +1300 (Fri, 13 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/beans/PluginManager.java

Added some additional methods that allow clients to specify that concrete implementations should be returned in the order that they were added to the PluginManager (rather than getting sorted)
------------------------------------------------------------------------
r11678 | mhall | 2015-03-14 22:02:22 +1300 (Sat, 14 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/PropertyDisplayName.java

Added a tipText method
------------------------------------------------------------------------
r11680 | mhall | 2015-03-14 22:04:24 +1300 (Sat, 14 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/converters/DatabaseSaver.java

Custom props file is now initialized to user.home. Updated usages of PropertyDisplayName
------------------------------------------------------------------------
r11682 | mhall | 2015-03-14 22:06:55 +1300 (Sat, 14 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/PropertySheetPanel.java

Fixed a bug in tip text processing. Updated for changes to the PropertyDisplayName annotation
------------------------------------------------------------------------
r11684 | mhall | 2015-03-14 22:41:08 +1300 (Sat, 14 Mar 2015) | 1 line
Changed paths:
   A /trunk/weka/src/main/java/weka/gui/FilePropertyMetadata.java

Initial import
------------------------------------------------------------------------
r11686 | mhall | 2015-03-14 22:42:08 +1300 (Sat, 14 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/converters/DatabaseSaver.java

Now uses the FilePropertyMetadata annotation
------------------------------------------------------------------------
r11688 | mhall | 2015-03-14 22:42:58 +1300 (Sat, 14 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/PropertySheetPanel.java

Now checks for the FilePropertyMetadata annotation
------------------------------------------------------------------------
r11690 | mhall | 2015-03-16 09:00:23 +1300 (Mon, 16 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/experiment/CSVResultListener.java

outputFile property now uses a FilePropertyMetadata annotation so that Mac users can finally get a save dialog (rather than open dialog) that allows them to enter a file name
------------------------------------------------------------------------
r11701 | mhall | 2015-03-18 15:26:37 +1300 (Wed, 18 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/PropertyDisplayName.java

Now includes a defalutl displayOrder element
------------------------------------------------------------------------
r11702 | mhall | 2015-03-18 15:27:18 +1300 (Wed, 18 Mar 2015) | 1 line
Changed paths:
   D /trunk/weka/src/main/java/weka/gui/PropertyDisplayOrder.java
   M /trunk/weka/src/main/java/weka/gui/PropertySheetPanel.java

Now uses ordering info from PropertyDisplayName
------------------------------------------------------------------------
r11705 | mhall | 2015-03-18 15:34:24 +1300 (Wed, 18 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/converters/DatabaseSaver.java

Now uses PropertyDisplayName annotation to indicate relative ordering of properties in the GUI
------------------------------------------------------------------------
r11707 | mhall | 2015-03-19 17:12:18 +1300 (Thu, 19 Mar 2015) | 1 line
Changed paths:
   A /trunk/weka/src/main/java/weka/core/OptionMetadata.java (from /trunk/weka/src/main/java/weka/gui/PropertyDisplayName.java:11706)
   D /trunk/weka/src/main/java/weka/gui/PropertyDisplayName.java

Renamed and moved to core
------------------------------------------------------------------------
r11708 | mhall | 2015-03-19 17:12:51 +1300 (Thu, 19 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/converters/DatabaseSaver.java
   M /trunk/weka/src/main/java/weka/gui/PropertySheetPanel.java

Now uses renamed annotation
------------------------------------------------------------------------
r11711 | mhall | 2015-03-19 17:20:34 +1300 (Thu, 19 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/OptionMetadata.java

Added a bit more javadoc
------------------------------------------------------------------------
r11713 | mhall | 2015-03-20 10:10:01 +1300 (Fri, 20 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/bayes/NaiveBayes.java

Added accessor methods to return the estimators used by naive Bayes
------------------------------------------------------------------------
r11714 | mhall | 2015-03-20 10:11:24 +1300 (Fri, 20 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/Option.java

Added static utility methods for reflection-based processing of properties/options annotated with the OptionMetadata annotation
------------------------------------------------------------------------
r11715 | mhall | 2015-03-20 10:12:23 +1300 (Fri, 20 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/filters/Filter.java

Added support for annotated options to list/get/setOptions()
------------------------------------------------------------------------
r11716 | mhall | 2015-03-20 14:47:44 +1300 (Fri, 20 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/Option.java

Fixed a small bug in hierarchy processing
------------------------------------------------------------------------
r11717 | mhall | 2015-03-20 14:48:51 +1300 (Fri, 20 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/associations/AbstractAssociator.java
   M /trunk/weka/src/main/java/weka/classifiers/AbstractClassifier.java
   M /trunk/weka/src/main/java/weka/clusterers/AbstractClusterer.java
   M /trunk/weka/src/main/java/weka/filters/Filter.java

Now supports the new anotation/reflection-based option processing
------------------------------------------------------------------------
r11718 | mhall | 2015-03-20 17:14:32 +1300 (Fri, 20 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/SelectedTag.java

SelectedTag now implements Serializable
------------------------------------------------------------------------
r11721 | mhall | 2015-03-20 20:15:23 +1300 (Fri, 20 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/Option.java

Now handles annotated options/properties that take/return SelectedTag values
------------------------------------------------------------------------
r11723 | mhall | 2015-03-20 20:30:04 +1300 (Fri, 20 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/AbstractClassifier.java

Removed a commented out line
------------------------------------------------------------------------
r11725 | eibe | 2015-03-23 12:01:21 +1300 (Mon, 23 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/AbstractClassifier.java

getOptions() now only includes "-num_decimal_places" if the value is different from the static default value in AbstractClassifier (NUM_DECIMAL_PLACES_DEFAULT), which is set to 2. This is to remove clutter in the scheme specification.
------------------------------------------------------------------------
r11726 | mhall | 2015-03-24 12:20:06 +1300 (Tue, 24 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/CheckGOE.java

Now checks for annotations in the case of tool tip text checking
------------------------------------------------------------------------
r11728 | mhall | 2015-03-24 23:28:16 +1300 (Tue, 24 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/PropertySheetPanel.java

Construction of help information now checks for the OptionMetadata annoation
------------------------------------------------------------------------
r11732 | mhall | 2015-03-25 17:28:45 +1300 (Wed, 25 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/visualize/MatrixPanel.java

Fixed an out of bounds exceptiont that could when there are missing class/colouring values.
------------------------------------------------------------------------
r11734 | fracpete | 2015-03-26 09:51:45 +1300 (Thu, 26 Mar 2015) | 2 lines
Changed paths:
   M /trunk/weka/src/main/java/weka/core/AllJavadoc.java
   M /trunk/weka/src/main/java/weka/core/GlobalInfoJavadoc.java
   M /trunk/weka/src/main/java/weka/core/Javadoc.java
   M /trunk/weka/src/main/java/weka/core/OptionHandlerJavadoc.java
   M /trunk/weka/src/main/java/weka/core/TechnicalInformationHandlerJavadoc.java

Javadoc output is now Java 8 compliant (HTML 4 does not allow empty tags like <br/> or <p/>)

------------------------------------------------------------------------
r11736 | fracpete | 2015-03-26 10:03:09 +1300 (Thu, 26 Mar 2015) | 2 lines
Changed paths:
   M /trunk/weka/src/main/java/weka/core/AllJavadoc.java
   M /trunk/weka/src/main/java/weka/core/GlobalInfoJavadoc.java
   M /trunk/weka/src/main/java/weka/core/TechnicalInformationHandlerJavadoc.java

replaced <p> with <br><br>

------------------------------------------------------------------------
r11739 | mhall | 2015-03-29 14:34:22 +1300 (Sun, 29 Mar 2015) | 1 line
Changed paths:
   A /trunk/weka/src/main/java/weka/filters/supervised/attribute/ClassConditionalProbabilities.java
   A /trunk/weka/src/test/java/weka/filters/supervised/attribute/ClassConditionalProbabilitiesTest.java

Initial import
------------------------------------------------------------------------
r11741 | mhall | 2015-03-29 14:41:04 +1300 (Sun, 29 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/bayes/NaiveBayes.java

Added some missing javadoc
------------------------------------------------------------------------
r11753 | fracpete | 2015-03-31 14:58:00 +1300 (Tue, 31 Mar 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/datagenerators/classifiers/classification/BayesNet.java

fixed option handling: -N option was not listed; -N/-S weren't parsed properly in setOptions method
------------------------------------------------------------------------
r11756 | mhall | 2015-04-02 20:47:40 +1300 (Thu, 02 Apr 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/WekaPackageManager.java
   M /trunk/weka/src/main/java/weka/gui/PackageManager.java

Package manager now shows available packages by default. Behaviour of the available packages list changed to show not only packages that are not installed but installed packages for which there is a more recent (but compatible with the base version of weka) version available. When installing a package with one or more dependencies, the latest version of dependent packages that is still compatible with the base version of weka are now selected automatically (rather than the very latest version, which might not be compatible with the base version of weka).
------------------------------------------------------------------------
r11764 | eibe | 2015-04-15 16:45:02 +1200 (Wed, 15 Apr 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/expressionlanguage/common/SimpleVariableDeclarations.java
   M /trunk/weka/src/main/java/weka/core/expressionlanguage/core/Node.java
   M /trunk/weka/src/main/java/weka/core/expressionlanguage/core/VariableDeclarations.java
   M /trunk/weka/src/main/java/weka/core/expressionlanguage/weka/InstancesHelper.java

Made some classes/interfaces Serializable.
------------------------------------------------------------------------
r11773 | mhall | 2015-04-24 09:09:58 +1200 (Fri, 24 Apr 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/Option.java

Was adding empty string valued options to array returned by getOptions() - fixed
------------------------------------------------------------------------
r11775 | mhall | 2015-04-24 16:02:01 +1200 (Fri, 24 Apr 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/Option.java

getOptions() now gets the readable string from a SelectedTag rather than the ID
------------------------------------------------------------------------
r11795 | mhall | 2015-05-05 14:16:39 +1200 (Tue, 05 May 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/clusterers/AbstractClusterer.java

setOptions() was not processing the class hierarchy when setting options - fixed.
------------------------------------------------------------------------
r11797 | mhall | 2015-05-05 14:27:19 +1200 (Tue, 05 May 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/clusterers/ClusterEvaluation.java

Now handles BatchPredictors
------------------------------------------------------------------------
r11799 | mhall | 2015-05-12 11:05:16 +1200 (Tue, 12 May 2015) | 1 line
Changed paths:
   A /trunk/weka/src/main/java/weka/gui/CloseableTabTitle.java

Initial import
------------------------------------------------------------------------
r11804 | mhall | 2015-05-13 11:28:43 +1200 (Wed, 13 May 2015) | 1 line
Changed paths:
   D /trunk/weka/src/main/java/weka/gui/beans/KFMetaStore.java
   D /trunk/weka/src/main/java/weka/gui/beans/XMLFileBasedKFMetaStore.java

Moving packages
------------------------------------------------------------------------
r11805 | mhall | 2015-05-13 11:29:41 +1200 (Wed, 13 May 2015) | 1 line
Changed paths:
   A /trunk/weka/src/main/java/weka/core/metastore
   A /trunk/weka/src/main/java/weka/core/metastore/MetaStore.java
   A /trunk/weka/src/main/java/weka/core/metastore/XMLFileBasedMetaStore.java
   M /trunk/weka/src/main/java/weka/gui/beans/BeansProperties.java

Meta store moved from knowledge flow to core.
------------------------------------------------------------------------
r11812 | mhall | 2015-05-17 20:22:22 +1200 (Sun, 17 May 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/visualize/MatrixPanel.java

Added set methods for a couple of settings
------------------------------------------------------------------------
r11815 | eibe | 2015-05-19 17:16:23 +1200 (Tue, 19 May 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/matrix/SingularValueDecomposition.java

Fixed a couple of bugs: output matrices were not correct when m < n (i.e., more columns than rows). Also, dimensions of output matrices were not correct.
------------------------------------------------------------------------
r11828 | mhall | 2015-05-22 16:46:13 +1200 (Fri, 22 May 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/converters/DatabaseLoader.java

Now uses OptionMetadata annotations
------------------------------------------------------------------------
r11831 | mhall | 2015-05-27 08:59:54 +1200 (Wed, 27 May 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/converters/CSVLoader.java

Improved the error message generated when unknown nominal values are encountered
------------------------------------------------------------------------
r11849 | mhall | 2015-06-03 14:38:21 +1200 (Wed, 03 Jun 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/PackageManager.java

Available list was not updating after a cache refresh when new packages were available - fixed
------------------------------------------------------------------------
r11852 | mhall | 2015-06-08 22:20:48 +1200 (Mon, 08 Jun 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/attributeSelection/ASEvaluation.java
   M /trunk/weka/src/main/java/weka/attributeSelection/AttributeSelection.java
   M /trunk/weka/src/main/java/weka/attributeSelection/CfsSubsetEval.java
   M /trunk/weka/src/main/java/weka/attributeSelection/WrapperSubsetEval.java

Fixed a bug affecting ranked output with GreedyStepwise introduced in the last release
------------------------------------------------------------------------
r11858 | mhall | 2015-06-11 22:27:04 +1200 (Thu, 11 Jun 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/Copyright.props

Updated copyright year
------------------------------------------------------------------------
r11861 | mhall | 2015-06-12 14:37:59 +1200 (Fri, 12 Jun 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/evaluation/output/prediction/AbstractOutput.java

Now checks for a null argument in setHeader(), which can happen on deserialization of an unused subclass of AbstractOutput by Weka's XML serialization mechanism
------------------------------------------------------------------------
r11863 | mhall | 2015-06-12 21:09:09 +1200 (Fri, 12 Jun 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/xml/XMLBasicSerialization.java

Now handles CostMatrix correctly
------------------------------------------------------------------------
r11865 | mhall | 2015-06-13 08:33:37 +1200 (Sat, 13 Jun 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/core/xml/XMLBasicSerialization.java

Removed unecessary code for LoggingLevel
------------------------------------------------------------------------
r11868 | mhall | 2015-06-17 15:10:44 +1200 (Wed, 17 Jun 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/CostMatrix.java

Updated javadoc to provide a bit more information on expressions used for per-instance weights
------------------------------------------------------------------------
r11870 | mhall | 2015-06-22 09:34:30 +1200 (Mon, 22 Jun 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/CloseableTabTitle.java

New tabs now start in non-bold status
------------------------------------------------------------------------
r11872 | eibe | 2015-06-24 11:51:16 +1200 (Wed, 24 Jun 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/filters/supervised/attribute/NominalToBinary.java
   M /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/NominalToBinary.java

NominalBinary filters (both versions) now include relevant attribute value in attribute name when a numeric attribute is created from a two-valued nominal attribute.
------------------------------------------------------------------------
r11882 | eibe | 2015-06-26 11:35:46 +1200 (Fri, 26 Jun 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/MathExpression.java

Missing values are no longer passed to Stats, so that variables such as COUNT now have the intended meaning again (number of non-missing values for the attribute).
------------------------------------------------------------------------
r11883 | eibe | 2015-06-29 17:17:08 +1200 (Mon, 29 Jun 2015) | 1 line
Changed paths:
   A /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/ReplaceWithMissingValue.java

New filter to replace values in a dataset with missing values. Currently does so by random replacement, given a user specified probability. Useful for experimenting with methods for imputing missing values and methods for semi-supervised learning (if class values are replaced). Still needs appropriate Javadoc and a test class.
------------------------------------------------------------------------
r11884 | eibe | 2015-06-30 10:12:54 +1200 (Tue, 30 Jun 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/ReplaceWithMissingValue.java

Javadoc of ReplaceWithMissingValue has been updated. Also, added copyright notice.
------------------------------------------------------------------------
r11885 | mhall | 2015-06-30 10:24:02 +1200 (Tue, 30 Jun 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/experiment/DatabaseUtils.java
   M /trunk/weka/src/main/java/weka/experiment/DatabaseUtils.props
   M /trunk/weka/src/main/java/weka/experiment/DatabaseUtils.props.hsql
   M /trunk/weka/src/main/java/weka/experiment/DatabaseUtils.props.msaccess
   M /trunk/weka/src/main/java/weka/experiment/DatabaseUtils.props.mssqlserver
   M /trunk/weka/src/main/java/weka/experiment/DatabaseUtils.props.mssqlserver2005
   M /trunk/weka/src/main/java/weka/experiment/DatabaseUtils.props.mysql
   M /trunk/weka/src/main/java/weka/experiment/DatabaseUtils.props.odbc
   M /trunk/weka/src/main/java/weka/experiment/DatabaseUtils.props.postgresql
   M /trunk/weka/src/main/java/weka/experiment/DatabaseUtils.props.sqlite3
   M /trunk/weka/src/main/java/weka/experiment/InstanceQuery.java

Now handles java.sql.Timestamp correctly. Added a new mapping: timestamp=11
------------------------------------------------------------------------
r11889 | eibe | 2015-06-30 17:33:06 +1200 (Tue, 30 Jun 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/meta/MultiClassClassifier.java

1-vs-rest mode now produces sensible class probability estimates. Also added option to perform log-loss-based decoding for exhaustive and random output codes.
------------------------------------------------------------------------
r11896 | eibe | 2015-07-01 11:03:36 +1200 (Wed, 01 Jul 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/functions/supportVector/RBFKernel.java

Fixed ^2 documentation bug in Javadoc and globalInfo() method.
------------------------------------------------------------------------
r11905 | mhall | 2015-07-01 20:33:37 +1200 (Wed, 01 Jul 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/beans/LogPanel.java

interrupted and error status messages now stay in place until exlicitly cleared
------------------------------------------------------------------------
r11907 | eibe | 2015-07-02 13:15:43 +1200 (Thu, 02 Jul 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/trees/RandomForest.java
   M /trunk/weka/src/main/java/weka/classifiers/trees/RandomTree.java

RandomTree (and, thus, RandomForest) now have random tie breaking as an option.
------------------------------------------------------------------------
r11909 | mhall | 2015-07-02 13:53:00 +1200 (Thu, 02 Jul 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/ParallelIteratedSingleClassifierEnhancer.java

buildClassifiers() run method now catches Throwable instead of Exception.
------------------------------------------------------------------------
r11912 | mhall | 2015-07-06 16:23:45 +1200 (Mon, 06 Jul 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/ResultHistoryPanel.java

Added a setBackground() method so that the color is passed through to the JList
------------------------------------------------------------------------
r11914 | mhall | 2015-07-06 22:28:19 +1200 (Mon, 06 Jul 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/ResultHistoryPanel.java

Added a setFont() method in order to set the font used by the list component
------------------------------------------------------------------------
r11916 | fracpete | 2015-07-07 10:08:49 +1200 (Tue, 07 Jul 2015) | 1 line
Changed paths:
   M /branches/waikato/weka/src/main/java/weka/gui/GUIEditors.props
   M /branches/waikato/weka/src/main/java/weka/gui/GenericObjectEditor.java
   M /trunk/weka/src/main/java/weka/gui/GUIEditors.props
   M /trunk/weka/src/main/java/weka/gui/GenericObjectEditor.java

large tool tip in class tree can be turned off now using the "ShowGlobalInfoToolTip" property in GUIEditors.props
------------------------------------------------------------------------
r11917 | mhall | 2015-07-15 15:04:00 +1200 (Wed, 15 Jul 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/FileEnvironmentField.java

Small tidy up of imports
------------------------------------------------------------------------
r11919 | mhall | 2015-07-27 09:24:34 +1200 (Mon, 27 Jul 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/filters/unsupervised/attribute/MergeManyValues.java

Made MergeManyValues extend PotentialClassIgnorer
------------------------------------------------------------------------
r11921 | mhall | 2015-07-28 22:11:26 +1200 (Tue, 28 Jul 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/beans/SubstringLabeler.java
   M /trunk/weka/src/main/java/weka/gui/beans/SubstringLabelerRules.java

Now makes the output structure in the constructor of SubstringLabelerRules
------------------------------------------------------------------------
r11923 | mhall | 2015-07-28 22:30:49 +1200 (Tue, 28 Jul 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/beans/SubstringLabelerRules.java

Made the makeOutputInstance() method public
------------------------------------------------------------------------
r11929 | mhall | 2015-08-04 10:57:39 +1200 (Tue, 04 Aug 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/beans/SubstringReplacer.java
   M /trunk/weka/src/main/java/weka/gui/beans/SubstringReplacerRules.java

No longer clobbers the incoming instance header structure
------------------------------------------------------------------------
r11932 | eibe | 2015-08-04 14:36:12 +1200 (Tue, 04 Aug 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/filters/supervised/attribute/PartitionMembership.java

Added reference to paper (technical information).
------------------------------------------------------------------------
r11933 | mhall | 2015-08-04 16:39:40 +1200 (Tue, 04 Aug 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/attributeSelection/WrapperSubsetEval.java

Made 'default' an explicit option for evaluation. ClassifierAttributeEval was initializing the Wrapper using default options returned by getOptions(), which in turn changed EVAL_DEFAULT to EVAL_ACC. This caused it to fail on numeric class datasets unless the user specificially set an evaluation metric suitable for a numeric class.
------------------------------------------------------------------------
r11935 | mhall | 2015-08-05 10:02:17 +1200 (Wed, 05 Aug 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/attributeSelection/WrapperSubsetEval.java

Can now use plugin evaluation metrics
------------------------------------------------------------------------
r11937 | mhall | 2015-08-05 10:29:16 +1200 (Wed, 05 Aug 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/attributeSelection/WrapperSubsetEval.java

Added in correlation coefficient as a standard metric
------------------------------------------------------------------------
r11942 | mhall | 2015-08-19 21:28:03 +1200 (Wed, 19 Aug 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/attributeSelection/AttributeSelection.java

Refactored the code that updates cross-validation results into a separate method so that client code that trains attribute selection schemes externally can use this class to update/compute those results
------------------------------------------------------------------------
r11944 | fracpete | 2015-08-20 16:00:52 +1200 (Thu, 20 Aug 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/GenericPropertiesCreator.props
   A /trunk/weka/src/main/java/weka/gui/experiment/AbstractSetupPanel.java
   M /trunk/weka/src/main/java/weka/gui/experiment/Experimenter.props
   M /trunk/weka/src/main/java/weka/gui/experiment/ExperimenterDefaults.java
   M /trunk/weka/src/main/java/weka/gui/experiment/SetupModePanel.java
   M /trunk/weka/src/main/java/weka/gui/experiment/SetupPanel.java
   M /trunk/weka/src/main/java/weka/gui/experiment/SimpleSetupPanel.java

setup panels are now derived from AbstractSetupPanel and available through a combobox in the SetupModePanel instead of a radio button group; the combobox gets filled using automatic class discovery
------------------------------------------------------------------------
r11946 | mhall | 2015-08-22 18:24:23 +1200 (Sat, 22 Aug 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/visualize/VisualizePanel.java

Added a setColourIndex() method that allows the client to specify whether the colour combo box should be enabled or disabled
------------------------------------------------------------------------
r11948 | mhall | 2015-08-24 11:08:23 +1200 (Mon, 24 Aug 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/explorer/ClassifierPanel.java

Re-evaluate model routine did not handle BatchPredictor - fixed.
------------------------------------------------------------------------
r11949 | mhall | 2015-08-24 11:17:10 +1200 (Mon, 24 Aug 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/explorer/ClassifierPanel.java

Accidently committed some code that was not ready yet - removed.
------------------------------------------------------------------------
r11951 | mhall | 2015-08-26 16:15:31 +1200 (Wed, 26 Aug 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/explorer/ClassifierPanel.java

In the case of separate test sets if the classifier is a BatchPredictor and the loader being used for the test set is an ArffLoader it now makes sure that string values accumulate in memory
------------------------------------------------------------------------
r11952 | mhall | 2015-08-26 16:25:34 +1200 (Wed, 26 Aug 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/explorer/ClassifierPanel.java

Argh! Accidently checked in some code that shouldn't have been checked in yet - fixed.
------------------------------------------------------------------------
r11954 | mhall | 2015-08-27 22:35:18 +1200 (Thu, 27 Aug 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/beans/SubstringReplacerRules.java

Made the toStringInternal() method public
------------------------------------------------------------------------
r11956 | mhall | 2015-08-28 16:39:19 +1200 (Fri, 28 Aug 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/beans/SubstringLabeler.java

Fixed a bug in batch mode
------------------------------------------------------------------------
r11958 | mhall | 2015-08-31 11:32:56 +1200 (Mon, 31 Aug 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/AbstractClassifier.java
   M /trunk/weka/src/main/java/weka/classifiers/evaluation/Evaluation.java
   M /trunk/weka/src/main/java/weka/classifiers/evaluation/output/prediction/AbstractOutput.java
   M /trunk/weka/src/main/java/weka/classifiers/meta/FilteredClassifier.java
   M /trunk/weka/src/main/java/weka/classifiers/meta/LogitBoost.java
   M /trunk/weka/src/main/java/weka/clusterers/ClusterEvaluation.java
   M /trunk/weka/src/main/java/weka/core/BatchPredictor.java
   M /trunk/weka/src/main/java/weka/gui/beans/ClassifierPerformanceEvaluator.java
   M /trunk/weka/src/main/java/weka/gui/explorer/ClassifierPanel.java

Added an 'impelentsEfficientBatchPrediction() method to the BatchPredictor interface; AbstractClassifier now implements BatchPredictor with a default (inefficient) implementation of distributionsForInstances() and returns false from implementsEfficientBatchPrediction(). Altered usages of BatchPredictor in evaluation routines to check for the new method.
------------------------------------------------------------------------
r11962 | mhall | 2015-09-01 08:45:06 +1200 (Tue, 01 Sep 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/AbstractClassifier.java

Added a tool tip for the batchSize property
------------------------------------------------------------------------
r11964 | mhall | 2015-09-01 11:57:20 +1200 (Tue, 01 Sep 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/beans/SubstringLabelerRules.java
   M /trunk/weka/src/main/java/weka/gui/beans/SubstringReplacerRules.java

Now implements Serializable
------------------------------------------------------------------------
r11966 | mhall | 2015-09-01 15:44:44 +1200 (Tue, 01 Sep 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/PropertySheetPanel.java

Added a method that returns true if the object being edited has a customizer
------------------------------------------------------------------------
r11968 | mhall | 2015-09-01 20:33:19 +1200 (Tue, 01 Sep 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/gui/beans/SubstringLabelerRules.java
   M /trunk/weka/src/main/java/weka/gui/beans/SubstringReplacerRules.java

Inner class is now serializable
------------------------------------------------------------------------
r11970 | mhall | 2015-09-02 09:00:40 +1200 (Wed, 02 Sep 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/AbstractClassifier.java

Removed @Override annotation from implementsMoreEfficientBatchPrediction to remain compile compatible if older versions of the BatchPredictor interface are in the classpath first
------------------------------------------------------------------------
r11973 | mhall | 2015-09-05 11:47:02 +1200 (Sat, 05 Sep 2015) | 1 line
Changed paths:
   M /trunk/weka/src/main/java/weka/classifiers/bayes/NaiveBayesMultinomialText.java

Fixed a bug in the aggregation of models - wasn't aggregating the per class word counts
------------------------------------------------------------------------
