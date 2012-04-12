/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * Copyright 2008 Pentaho Corporation
 */

package weka.classifiers.rules;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.Classifier;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests DTNB. Run from the command line with:<p>
 * java weka.classifiers.rules.DTNBTest
 *
 * @author Mark Hall (mhall{[at}]pentaho{[dot]}org
 * @version $Revision: 1.1.2.2 $
 */
public class DTNBTest extends AbstractClassifierTest {

  public DTNBTest(String name) { super(name);  }

  /** Creates a default DecisionTable */
  public Classifier getClassifier() {
    return new DTNB();
  }

  public static Test suite() {
    return new TestSuite(DTNBTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
