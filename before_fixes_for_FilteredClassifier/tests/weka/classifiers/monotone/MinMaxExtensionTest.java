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
 * Copyright (C) 2004 Stijn Lievens
 */

package weka.classifiers.monotone;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.Classifier;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests MinMaxExtension. Run from the command line with:<p>
 * java weka.classifiers.monotone.MinMaxExtensionTest
 *
 * @author Stijn Lievens
 * @version $Revision: 1.1 $
 */
public class MinMaxExtensionTest
  extends AbstractClassifierTest {

  public MinMaxExtensionTest(String name) {
    super(name);
  }

  /** Creates a default MinMaxExtension */
  public Classifier getClassifier() {
    return new MinMaxExtension();
  }

  public static Test suite() {
    return new TestSuite(MinMaxExtensionTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
