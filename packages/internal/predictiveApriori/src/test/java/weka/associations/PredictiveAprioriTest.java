/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.associations;

import java.io.Serializable;

import weka.associations.AbstractAssociatorTest;
import weka.associations.Associator;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests PredictiveApriori. Run from the command line with:<p/>
 * java weka.associations.PredictiveAprioriTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class PredictiveAprioriTest 
  extends AbstractAssociatorTest {
  
  public static class PredictiveAprioriT extends PredictiveApriori 
  implements Serializable {
      
  private static final long serialVersionUID = 7236786154698908772L;

  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }
}

  public PredictiveAprioriTest(String name) { 
    super(name);  
  }

  /** Creates a default PredictiveApriori */
  public Associator getAssociator() {
    return new PredictiveAprioriT();
  }

  public static Test suite() {
    return new TestSuite(PredictiveAprioriTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
