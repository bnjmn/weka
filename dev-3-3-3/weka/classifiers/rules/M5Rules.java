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
 * M5 Rules. Implements routines
 * for generating a decision list using M5 Model trees and the approach
 * used by the PART algorithm. <p>
 *
 * Valid options are:<p>
 * 
 * -U <br>
 * Use unsmoothed predictions. <p>
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.1 $
 */
public class M5Rules extends M5Base {

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
