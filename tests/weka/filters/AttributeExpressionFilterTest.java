/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.core.Instance;

/**
 * Tests AttributeExpressionFilter. Run from the command line with:<p>
 * java weka.filters.AttributeExpressionFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class AttributeExpressionFilterTest extends AbstractFilterTest {
  
  private static double EXPR_DELTA = 0.001;

  public AttributeExpressionFilterTest(String name) { super(name);  }

  /** Creates a default AttributeExpressionFilter */
  public Filter getFilter() {
    return new AttributeExpressionFilter();
  }

  /** Creates a specialized AttributeExpressionFilter */
  public Filter getFilter(String expr) {
    AttributeExpressionFilter af = new AttributeExpressionFilter();
    af.setExpression(expr);
    return af;
  }

  public void testAdd() {
    m_Filter = getFilter("a1+a2");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      assertEquals("Instance " + (i + 1),
                   inst.value(0) + inst.value(1), 
                   inst.value(inst.numAttributes() - 1), EXPR_DELTA);
    }
  }

  public void testSubtract() {
    m_Filter = getFilter("a1-a2");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      assertEquals("Instance " + (i + 1),
                   inst.value(0) - inst.value(1), 
                   inst.value(inst.numAttributes() - 1), EXPR_DELTA);
    }
  }

  public void testMultiply() {
    m_Filter = getFilter("a1*a2");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      assertEquals("Instance " + (i + 1),
                   inst.value(0) * inst.value(1), 
                   inst.value(inst.numAttributes() - 1), EXPR_DELTA);
    }
  }

  public void testDivide() {
    m_Filter = getFilter("a1/a2");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      if (inst.value(1) == 0) {
        assert("Instance " + (i + 1) + " should have been ?" , 
               inst.isMissing(inst.numAttributes() - 1));
      } else {
        assertEquals("Instance " + (i + 1),
                     inst.value(0) / inst.value(1), 
                     inst.value(inst.numAttributes() - 1), EXPR_DELTA);
      }
    }
  }

  public void testExponent() {
    m_Filter = getFilter("a1^a2");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      assertEquals("Instance " + (i + 1),
                   Math.pow(inst.value(0), inst.value(1)), 
                   inst.value(inst.numAttributes() - 1), EXPR_DELTA);
    }
  }

  public void testLog() {
    m_Filter = getFilter("log(a2/5)");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      if (inst.value(1) != 0) {
        assertEquals("Instance " + (i + 1),
                     Math.log(inst.value(1)/5), 
                     inst.value(inst.numAttributes() - 1), EXPR_DELTA);
      }
    }
  }

  public void testCos() {
    m_Filter = getFilter("cos(a2/5)");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      assertEquals("Instance " + (i + 1),
                   Math.cos(inst.value(1) / 5), 
                   inst.value(inst.numAttributes() - 1), EXPR_DELTA);
    }
  }

  public void testSin() {
    m_Filter = getFilter("sin(a2/5)");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      assertEquals("Instance " + (i + 1),
                   Math.sin(inst.value(1) / 5), 
                   inst.value(inst.numAttributes() - 1), EXPR_DELTA);
    }
  }

  public void testTan() {
    m_Filter = getFilter("tan(a2/5)");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      assertEquals("Instance " + (i + 1) + ": " + inst + "\n",
                   Math.tan(inst.value(1) / 5), 
                   inst.value(inst.numAttributes() - 1), EXPR_DELTA);
    }
  }

  public void testAbs() {
    m_Filter = getFilter("abs(a2-a1)");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      assertEquals("Instance " + (i + 1),
                   Math.abs(inst.value(1) - inst.value(0)), 
                   inst.value(inst.numAttributes() - 1), EXPR_DELTA);
    }
  }

  public void testExp() {
    m_Filter = getFilter("exp(a2-a1)");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      assertEquals("Instance " + (i + 1),
                   Math.exp(inst.value(1) - inst.value(0)), 
                   inst.value(inst.numAttributes() - 1), EXPR_DELTA);
    }
  }

  public void testSqrt() {
    m_Filter = getFilter("sqrt(a2+a1/5)");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      assertEquals("Instance " + (i + 1),
                   Math.sqrt(inst.value(1) + inst.value(0)/5), 
                   inst.value(inst.numAttributes() - 1), EXPR_DELTA);
    }
  }

  public void testFloor() {
    m_Filter = getFilter("floor(a2+a1/5)");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      assertEquals("Instance " + (i + 1),
                   Math.floor(inst.value(1) + inst.value(0)/5), 
                   inst.value(inst.numAttributes() - 1), EXPR_DELTA);
    }
  }

  public void testCeil() {
    m_Filter = getFilter("ceil(a2*a1/5)");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      assertEquals("Instance " + (i + 1),
                   Math.ceil(inst.value(1) * inst.value(0)/5), 
                   inst.value(inst.numAttributes() - 1), EXPR_DELTA);
    }
  }

  public void testRint() {
    m_Filter = getFilter("rint(a2*a1/5)");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      assertEquals("Instance " + (i + 1),
                   Math.rint(inst.value(1) * inst.value(0)/5), 
                   inst.value(inst.numAttributes() - 1), EXPR_DELTA);
    }
  }

  public void testBracketing() {
    m_Filter = getFilter("(a3+a4)*((a2-a1)/5)");
    Instances result = useFilter();
    for (int i = 0; i < result.numInstances(); i++) {
      Instance inst = result.instance(i);
      if (inst.isMissing(0) || inst.isMissing(1) ||
          inst.isMissing(2) || inst.isMissing(3)) {
        assert("Instance " + (i + 1) + " should have been ?" , 
               inst.isMissing(inst.numAttributes() - 1));
      } else {
        assertEquals("Instance " + (i + 1),
                     (inst.value(3) + inst.value(2)) * 
                     ((inst.value(1) - inst.value(0))/5), 
                     inst.value(inst.numAttributes() - 1), EXPR_DELTA);
      }
    }
  }

  public void testAddNamed() {
    m_Filter = getFilter("a1+a2");
    String name = "BongoBongo";
    ((AttributeExpressionFilter)m_Filter).setName(name);
    Instances result = useFilter();
    assertEquals(name, result.attribute(result.numAttributes() - 1).name());
    name = "BongoBongoSecond";
    ((AttributeExpressionFilter)m_Filter).setName(name);
    result = useFilter();
    assertEquals(name, result.attribute(result.numAttributes() - 1).name());
  }

  public static Test suite() {
    return new TestSuite(AttributeExpressionFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
