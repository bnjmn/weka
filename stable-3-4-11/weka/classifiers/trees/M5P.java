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
package weka.classifiers.trees;

import weka.classifiers.trees.m5.*;
import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * M5P. Implements routines for generating M5 model trees.<p/>
 *
 * The original algorithm M5 was invented by Quinlan: <br/>
 * 
 * Quinlan J. R. (1992). Learning with continuous classes. Proceedings of
 * the Australian Joint Conference on Artificial Intelligence. 343--348.
 * World Scientific, Singapore. <p/>
 * 
 * Yong Wang made improvements and created M5': <br/>
 * 
 * Wang, Y and Witten, I. H. (1997). Induction of model trees for
 * predicting continuous classes. Proceedings of the poster papers of the
 * European Conference on Machine Learning. University of Economics,
 * Faculty of Informatics and Statistics, Prague. <p/>
 *
 *
 * Valid options are:<p>
 * 
 * -U <br>
 * Use unsmoothed predictions. <p>
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.1.2.1 $
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
   *  Returns the type of graph this classifier
   *  represents.
   *  @return Drawable.TREE
   */   
  public int graphType() {
      return Drawable.TREE;
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
    temp.topOfTree().graph(text);
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
   * Returns an enumeration describing the available options
   * 
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    Enumeration superOpts = super.listOptions();
    
    Vector newVector = new Vector();
    while (superOpts.hasMoreElements()) {
      newVector.addElement((Option)superOpts.nextElement());
    }

    newVector.addElement(new Option("\tSave instances at the nodes in\n"
				    +"\tthe tree (for visualization purposes)\n",
				    "L", 0, "-L"));
    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p>
   *
   * Valid options are:<p>
   * 
   * -U <br>
   * Use unsmoothed predictions. <p>
   *
   * -R <br>
   * Build a regression tree rather than a model tree. <p>
   *
   * -L <br>
   * Save instance data at each node (for visualization purposes). <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    setSaveInstances(Utils.getFlag('L', options));
    super.setOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    String[] superOpts = super.getOptions();
    String [] options = new String [superOpts.length+1];
    int current = superOpts.length;
    for (int i = 0; i < current; i++) {
      options[i] = superOpts[i];
    }
    
    if (getSaveInstances()) {
      options[current++] = "-L";
    }

    while (current < options.length) {
      options[current++] = "";
    }

    return options;
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

