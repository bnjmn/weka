package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;         

/**
 * Test class for all tests in this directory. Run from the command line 
 * with:<p>
 * java weka.filters.AllTests
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.5 $
 */
public class AllTests extends TestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(AddFilterTest.suite());
    suite.addTest(AllFilterTest.suite());
    suite.addTest(AttributeExpressionFilterTest.suite());
    suite.addTest(AttributeFilterTest.suite());
    suite.addTest(AttributeSelectionFilterTest.suite());
    suite.addTest(AttributeTypeFilterTest.suite());
    suite.addTest(CopyAttributesFilterTest.suite());
    suite.addTest(DiscretizeFilterTest.suite());
    suite.addTest(UselessAttributeFilterTest.suite());
    suite.addTest(FirstOrderFilterTest.suite());
    suite.addTest(InstanceFilterTest.suite());
    suite.addTest(MakeIndicatorFilterTest.suite());
    suite.addTest(MergeTwoValuesFilterTest.suite());
    suite.addTest(NominalToBinaryFilterTest.suite());
    suite.addTest(NonSparseToSparseFilterTest.suite());
    suite.addTest(NormalizationFilterTest.suite());
    suite.addTest(NullFilterTest.suite());
    suite.addTest(NumericToBinaryFilterTest.suite());
    suite.addTest(NumericTransformFilterTest.suite());
    suite.addTest(ObfuscateFilterTest.suite());
    suite.addTest(RandomizeFilterTest.suite());
    suite.addTest(ReplaceMissingValuesFilterTest.suite());
    suite.addTest(ResampleFilterTest.suite());
    suite.addTest(SparseToNonSparseFilterTest.suite());
    suite.addTest(SplitDatasetFilterTest.suite());
    suite.addTest(SpreadSubsampleFilterTest.suite());
    suite.addTest(StringToNominalFilterTest.suite());
    suite.addTest(StringToWordVectorFilterTest.suite());
    suite.addTest(SwapAttributeValuesFilterTest.suite());
    suite.addTest(TimeSeriesDeltaFilterTest.suite());
    suite.addTest(TimeSeriesTranslateFilterTest.suite());
    return suite;
  }

  public static void main(String []args) {
    junit.textui.TestRunner.run(suite());
  }
}
