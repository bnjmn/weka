/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests AllFilter. Run from the command line with:<p>
 * java weka.filters.AllFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision:
 */
public class AllFilterTest extends AbstractFilterTest {
  
  public AllFilterTest(String name) { super(name);  }

  /** Creates a default AllFilter */
  public Filter getFilter() {
    return new AllFilter();
  }

  public static Test suite() {
    return new TestSuite(AllFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
