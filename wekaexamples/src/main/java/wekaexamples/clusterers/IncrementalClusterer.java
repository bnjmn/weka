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
 *    IncrementalClusterer.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.clusterers;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.clusterers.Cobweb;

import java.io.File;

/**
 * This example trains Cobweb incrementally on data obtained
 * from the ArffLoader.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class IncrementalClusterer {

  /**
   * Expects an ARFF file as first argument.
   *
   * @param args        the commandline arguments
   * @throws Exception  if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // load data
    ArffLoader loader = new ArffLoader();
    loader.setFile(new File(args[0]));
    Instances structure = loader.getStructure();

    // train Cobweb
    Cobweb cw = new Cobweb();
    cw.buildClusterer(structure);
    Instance current;
    while ((current = loader.getNextInstance(structure)) != null)
      cw.updateClusterer(current);
    cw.updateFinished();

    // output generated model
    System.out.println(cw);
  }
}
