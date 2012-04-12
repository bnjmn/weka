/*
 *    M5P.java
 *    Copyright (C) 2001 Mark Hall
 *
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
package weka.classifiers.trees.m5;

import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * M5P. Implements routines for generating M5 model trees.
 *
 * Valid options are:<p>
 * 
 * -U <br>
 * Use unsmoothed predictions. <p>
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.1 $
 */

public class M5P extends M5Base 
  implements Drawable {

  /**
   * Creates a new <code>M5P</code> instance.
   */
  public M5P() {
    super();
    setGenerateRules(false);
  }

  /**
   * Return a dot style String describing the tree.
   *
   * @return a <code>String</code> value
   * @exception Exception if an error occurs
   */
  public String graph() throws Exception {
    StringBuffer text = new StringBuffer();
    
    text.append("digraph M5Tree {\n");
    Rule temp = (Rule)m_ruleSet.elementAt(0);
    temp.m_topOfTree.graph(text);
    text.append("}\n");
    return text.toString();
  }

  /**
   * Set whether to save instance data at each node in the
   * tree for visualization purposes
   *
   * @param save a <code>boolean</code> value
   */
  public void setSaveInstances(boolean save) {
    m_saveInstances = save;
  }

  /**
   * Get whether instance data is being save.
   *
   * @return a <code>boolean</code> value
   */
  public boolean getSaveInstances() {
    return m_saveInstances;
  }

  /**
   * Main method by which this class can be tested
   * 
   * @param args an array of options
   */
  public static void main(String[] args) {
    try {
      System.out.println(weka.classifiers.Evaluation.evaluateModel(
			 new M5P(), 
			 args));
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    } 
  } 
}
