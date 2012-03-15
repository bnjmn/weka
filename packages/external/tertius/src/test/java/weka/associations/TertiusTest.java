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
 * Tests Tertius. Run from the command line with:<p/>
 * java weka.associations.TertiusTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class TertiusTest 
  extends AbstractAssociatorTest {
  
  public static class TertiusT extends Tertius implements Serializable {
    
    private static final long serialVersionUID = -5252868975496746622L;

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

  public TertiusTest(String name) { 
    super(name);  
  }

  /** Creates a default Tertius */
  public Associator getAssociator() {
    return new TertiusT();
  }

  public static Test suite() {
    return new TestSuite(TertiusTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
