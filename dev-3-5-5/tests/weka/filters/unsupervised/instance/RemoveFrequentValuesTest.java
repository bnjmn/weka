/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.instance;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.AbstractFilterTest;
import weka.filters.unsupervised.attribute.AddValues;

/**
 * Tests RemoveFrequentValues. Run from the command line with:<p>
 * java weka.filters.RemoveFrequentValuesTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 */
public class RemoveFrequentValuesTest
  extends AbstractFilterTest {
  
  /** the attribute indices we're using */
  private final static int indexString = 0;
  private final static int indexNumeric = 2; 
  private final static int indexNominal = 4;

  public RemoveFrequentValuesTest(String name) { 
    super(name);  
  }

  /** Creates a RemoveFrequentValues, with "-N 3" and "-C 4" */
  public Filter getFilter() {
    RemoveFrequentValues f = new RemoveFrequentValues();
    f.setAttributeIndex(Integer.toString(indexNominal + 1));
    f.setNumValues(3);
    return f;
  }

  /** test string attribute */
  public void testString() {
    Instances icopy = new Instances(m_Instances);
    try {
      ((RemoveFrequentValues) m_Filter).setAttributeIndex(Integer.toString(indexString + 1));
      m_Filter.setInputFormat(icopy);
      fail("Should have thrown an exception selecting on a STRING attribute!");
    } catch (Exception ex) {
      // OK
    }
  }

  /** test nominal attribute */
  public void testNominal() {
    Instances   icopy;
    Instances   result;

    // setting the nominal index
    ((RemoveFrequentValues) m_Filter).setAttributeIndex(Integer.toString(indexNominal));

    // don't modify header
    icopy    = new Instances(m_Instances);
    m_Filter = getFilter();
    try {
      m_Filter.setInputFormat(icopy);
      result = useFilter();
      assertEquals( "Doesn't modify the header, i.e. removing labels", 
	  m_Instances.attribute(indexNominal).numValues(), 
	  result.attribute(indexNominal).numValues() );
    } catch (Exception ex) {
      // OK
    }

    // modify header
    icopy    = new Instances(m_Instances);
    m_Filter = getFilter();
    try {
      ((RemoveFrequentValues) m_Filter).setModifyHeader(true);
      m_Filter.setInputFormat(icopy);
      result = useFilter();
      assertEquals(   "Returns " + ((RemoveFrequentValues) m_Filter).getNumValues() 
	  + " out of the " + m_Instances.attribute(indexNominal).numValues() + " labels", 
	  ((RemoveFrequentValues) m_Filter).getNumValues(),
	  result.attribute(indexNominal).numValues() );
    } catch (Exception ex) {
      // OK
    }

    // modify header + least common
    icopy    = new Instances(m_Instances);
    m_Filter = getFilter();
    try {
      ((RemoveFrequentValues) m_Filter).setModifyHeader(true);
      ((RemoveFrequentValues) m_Filter).setUseLeastValues(true);
      m_Filter.setInputFormat(icopy);
      result = useFilter();
      assertEquals(   "Returns " + ((RemoveFrequentValues) m_Filter).getNumValues() 
	  + " out of the " + m_Instances.attribute(indexNominal).numValues() + " labels", 
	  ((RemoveFrequentValues) m_Filter).getNumValues(),
	  result.attribute(indexNominal).numValues() );
    } catch (Exception ex) {
      // OK
    }

    // modify header + least common + inverse
    icopy    = new Instances(m_Instances);
    m_Filter = getFilter();
    try {
      ((RemoveFrequentValues) m_Filter).setModifyHeader(true);
      ((RemoveFrequentValues) m_Filter).setUseLeastValues(true);
      ((RemoveFrequentValues) m_Filter).setInvertSelection(true);
      ((RemoveFrequentValues) m_Filter).setNumValues(4);
      m_Filter.setInputFormat(icopy);
      result = useFilter();
      assertEquals(   "Returns 1 out of the " + m_Instances.attribute(indexNominal).numValues() 
	  + " labels, even though we try to remove " + ((RemoveFrequentValues) m_Filter).getNumValues()  
	  + " labels, since it always returns at least 1 label",
	  1,
	  result.attribute(indexNominal).numValues() );
    } catch (Exception ex) {
      // OK
    }
  }

  /** test numeric attribute */
  public void testNumeric() {
    Instances icopy = new Instances(m_Instances);
    try {
      ((RemoveFrequentValues) m_Filter).setAttributeIndex(Integer.toString(indexNumeric + 1));
      m_Filter.setInputFormat(icopy);
      fail("Should have thrown an exception selecting on a NUMERIC attribute!");
    } catch (Exception ex) {
      // OK
    }
  }

  /**
   * tests the filter in conjunction with the FilteredClassifier
   */
  public void testFilteredClassifier() {
    try {
      Instances data = getFilteredClassifierData();

      for (int i = 0; i < data.numAttributes(); i++) {
	if (data.classIndex() == i)
	  continue;
	if (data.attribute(i).isNominal()) {
	  ((RemoveFrequentValues) m_FilteredClassifier.getFilter()).setAttributeIndex(
	      "" + (i + 1));
	  break;
	}
      }
    }
    catch (Exception e) {
      fail("Problem setting up test for FilteredClassifier: " + e.toString());
    }

    super.testFilteredClassifier();
  }

  public static Test suite() {
    return new TestSuite(RemoveFrequentValuesTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
