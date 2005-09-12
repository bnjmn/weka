/*
 *    M5Rules.java
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
package weka.classifiers.rules;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.trees.m5.*;

/**
 * Generates a decision list for regression problems using 
 * separate-and-conquer. In each iteration it builds an 
 * model tree using M5 and makes the "best" 
 * leaf into a rule. Reference: <p/>
 * M. Hall, G. Holmes, E. Frank (1999).  "Generating Rule Sets 
 * from Model Trees". Proceedings of the Twelfth Australian Joint 
 * Conference on Artificial Intelligence, Sydney, Australia. 
 * Springer-Verlag, pp. 1-12.<p>
 *
 * Valid options are:<p>
 * 
 * -U <br>
 * Use unsmoothed predictions. <p>
 *
 * -R <br>
 * Build regression tree/rule rather than model tree/rule
 *
 * -M num <br>
 * Minimum number of objects per leaf. <p>
 *
 * -N  <br>
 * Turns pruning off. <p>
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.4 $
 */
public class M5Rules extends M5Base {
    
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Generates a decision list for regression problems using " 
      + "separate-and-conquer. In each iteration it builds a "
      + "model tree using M5 and makes the \"best\" "
      + "leaf into a rule. Reference:\n\n"
      + "M. Hall, G. Holmes, E. Frank (1999).  \"Generating Rule Sets "
      + "from Model Trees\". Proceedings of the Twelfth Australian Joint "
      + "Conference on Artificial Intelligence, Sydney, Australia. "
      + "Springer-Verlag, pp. 1-12.";
  }

  public M5Rules() {
    super();
    setGenerateRules(true);
  }

  /**
   * Main method by which this class can be tested
   * 
   * @param args an array of options
   */
  public static void main(String[] args) {
    try {
      System.out.println(weka.classifiers.Evaluation.evaluateModel(
			 new M5Rules(), 
			 args));
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    } 
  } 
}

