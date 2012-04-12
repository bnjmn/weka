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
 * LoadBIF.java
 * Copyright (C) 2006 Mamelouk
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.classifiers;

import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;

/**
 * Loads a XML BIF file and outputs the model to stdout.
 * 
 * @author  Mamelouk -- original version
 * @author  FracPete (fracpete at waikato dot ac dot nz) -- slight modifications
 * @version $Revision$
 */
public class LoadBIF {
  
  /**
   * Loads the BIF specified by the first parameter and outputs the model to 
   * stdout.
   * 
   * @param args	the commandline arguments
   * @throws Exception	if reading fails
   */
  public static void main(String[] args) throws Exception {
    BIFReader reader = new BIFReader();    
    BayesNet network = reader.processFile(args[0]);
    System.out.println(network);
  }
}
