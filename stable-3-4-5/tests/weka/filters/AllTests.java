package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;         
import weka.filters.unsupervised.attribute.*;
import weka.filters.unsupervised.instance.*;
import weka.filters.supervised.attribute.*;
import weka.filters.supervised.instance.*;

/**
 * Test class for all tests in this directory. Run from the command line 
 * with:<p>
 * java weka.filters.AllTests
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.9 $
 */
public class AllTests extends TestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(AddTest.suite());
    suite.addTest(AllFilterTest.suite());
    suite.addTest(AddExpressionTest.suite());
    suite.addTest(RemoveTest.suite());
    suite.addTest(AttributeSelectionTest.suite());
    suite.addTest(RemoveTypeTest.suite());
    suite.addTest(CopyTest.suite());
    suite.addTest(weka.filters.unsupervised.attribute.DiscretizeTest.suite());
    suite.addTest(weka.filters.supervised.attribute.DiscretizeTest.suite());
    suite.addTest(FirstOrderTest.suite());
    suite.addTest(RemoveTest.suite());
    suite.addTest(MakeIndicatorTest.suite());
    suite.addTest(MergeTwoValuesTest.suite());
    suite.addTest(weka.filters.unsupervised.attribute.NominalToBinaryTest.suite());
    suite.addTest(weka.filters.supervised.attribute.NominalToBinaryTest.suite());
    suite.addTest(NonSparseToSparseTest.suite());
    suite.addTest(NormalizeTest.suite());
    suite.addTest(NullFilterTest.suite());
    suite.addTest(NumericToBinaryTest.suite());
    suite.addTest(NumericTransformTest.suite());
    suite.addTest(ObfuscateTest.suite());
    suite.addTest(RandomizeTest.suite());
    suite.addTest(RemoveWithValuesTest.suite());
    suite.addTest(ReplaceMissingValuesTest.suite());
    suite.addTest(weka.filters.unsupervised.instance.ResampleTest.suite());
    suite.addTest(weka.filters.supervised.instance.ResampleTest.suite());
    suite.addTest(SparseToNonSparseTest.suite());
    suite.addTest(StratifiedRemoveFoldsTest.suite());
    suite.addTest(RemoveFoldsTest.suite());
    suite.addTest(RemoveRangeTest.suite());
    suite.addTest(SpreadSubsampleTest.suite());
    suite.addTest(StringToNominalTest.suite());
    suite.addTest(StringToWordVectorTest.suite());
    suite.addTest(SwapValuesTest.suite());
    suite.addTest(TimeSeriesDeltaTest.suite());
    suite.addTest(TimeSeriesTranslateTest.suite());
    return suite;
  }

  public static void main(String []args) {
    junit.textui.TestRunner.run(suite());
  }
}
