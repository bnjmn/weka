package weka.classifiers;

import weka.classifiers.meta.ThresholdSelectorTest;
import weka.classifiers.rules.ZeroRTest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;         

/**
 * Test class for all tests in this directory. Run from the command line 
 * with:<p>
 * java weka.classifiers.AllTests
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.3 $
 */
public class AllTests extends TestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite();
//      suite.addTest(AdaBoostM1Test.suite());
//      suite.addTest(AdditiveRegressionTest.suite());
//      suite.addTest(AttributeSelectedClassifierTest.suite());
//      suite.addTest(BVDecomposeTest.suite());
//      suite.addTest(BaggingTest.suite());
//      suite.addTest(CVParameterSelectionTest.suite());
//      suite.addTest(ClassificationViaRegressionTest.suite());
//      suite.addTest(CostMatrixTest.suite());
//      suite.addTest(CostSensitiveClassifierTest.suite());
//      suite.addTest(DecisionStumpTest.suite());
//      suite.addTest(DecisionTableTest.suite());
//      suite.addTest(DistributionMetaClassifierTest.suite());
//      suite.addTest(EvaluationTest.suite());
//      suite.addTest(FilteredClassifierTest.suite());
//      suite.addTest(HyperPipesTest.suite());
//      suite.addTest(IB1Test.suite());
//      suite.addTest(IBkTest.suite());
//      suite.addTest(Id3Test.suite());
//      suite.addTest(KernelDensityTest.suite());
//      suite.addTest(LWRTest.suite());
//      suite.addTest(LinearRegressionTest.suite());
//      suite.addTest(LogisticTest.suite());
//      suite.addTest(LogitBoostTest.suite());
//      suite.addTest(MetaCostTest.suite());
//      suite.addTest(MultiClassClassifierTest.suite());
//      suite.addTest(MultiSchemeTest.suite());
//      suite.addTest(NaiveBayesTest.suite());
//      suite.addTest(NaiveBayesSimpleTest.suite());
//      suite.addTest(OneRTest.suite());
//      suite.addTest(PrismTest.suite());
//      suite.addTest(RegressionByDiscretizationTest.suite());
//      suite.addTest(SMOTest.suite());
//      suite.addTest(StackingTest.suite());
    suite.addTest(ThresholdSelectorTest.suite());
//      suite.addTest(UserClassifierTest.suite());
//      suite.addTest(VFITest.suite());
//      suite.addTest(VotedPerceptronTest.suite());
    suite.addTest(ZeroRTest.suite());
//      suite.addTest(weka.classifiers.evaluation.AllTests.suite());
//      suite.addTest(weka.classifiers.j48.AllTests.suite());
//      suite.addTest(weka.classifiers.kstar.AllTests.suite());
//      suite.addTest(weka.classifiers.m5.AllTests.suite());
//?      suite.addTest(UpdateableClassifierTest.suite());
//?      suite.addTest(SourcableTest.suite());
    return suite;
  }

  public static void main(String []args) {
    junit.textui.TestRunner.run(suite());
  }
}
