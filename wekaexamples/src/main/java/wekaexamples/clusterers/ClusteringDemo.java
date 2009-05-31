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
 *    ClusteringDemo.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.clusterers;

import weka.clusterers.ClusterEvaluation;
import weka.clusterers.DensityBasedClusterer;
import weka.clusterers.EM;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * An example class that shows the use of Weka clusterers from Java.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class ClusteringDemo {
  /**
   * Run clusterers
   *
   * @param filename      the name of the ARFF file to run on
   */
  public ClusteringDemo(String filename) throws Exception {
    ClusterEvaluation eval;
    Instances               data;
    String[]                options;
    DensityBasedClusterer   cl;    

    data = new Instances(new BufferedReader(new FileReader(filename)));
    
    // normal
    System.out.println("\n--> normal");
    options    = new String[2];
    options[0] = "-t";
    options[1] = filename;
    System.out.println(
        ClusterEvaluation.evaluateClusterer(new EM(), options));
    
    // manual call
    System.out.println("\n--> manual");
    cl   = new EM();
    cl.buildClusterer(data);
    eval = new ClusterEvaluation();
    eval.setClusterer(cl);
    eval.evaluateClusterer(new Instances(data));
    System.out.println("# of clusters: " + eval.getNumClusters());

    // density based
    System.out.println("\n--> density (CV)");
    cl   = new EM();
    eval = new ClusterEvaluation();
    eval.setClusterer(cl);
    eval.crossValidateModel(
           cl, data, 10, data.getRandomNumberGenerator(1));
    System.out.println("# of clusters: " + eval.getNumClusters());
  }

  /**
   * usage:
   *   ClusteringDemo arff-file
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("usage: " + ClusteringDemo.class.getName() + " <arff-file>");
      System.exit(1);
    }

    new ClusteringDemo(args[0]);
  }
}
