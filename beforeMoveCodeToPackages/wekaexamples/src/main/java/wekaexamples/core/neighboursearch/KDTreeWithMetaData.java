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
 *    KDTreeWithMetaData.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.core.neighboursearch;

import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.neighboursearch.KDTree;

import java.util.Random;

/**
 * Example class for demonstrating how to use KDTree with meta-data, i.e.,
 * additional attributes that are not used in the distance calculation.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class KDTreeWithMetaData {

  /**
   * Expects a dataset as first parameter. The last attribute is used as class attribute
   * and the first attribute will be excluded from the distance calculation.
   *
   * @param args          the commandline arguments
   * @throws Exception    if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // load data
    Instances data = DataSource.read(args[0]);
    data.setClassIndex(data.numAttributes() - 1);
    System.out.println("Input data has " + data.numAttributes() + " attributes.");

    // initialize KDTree
    EuclideanDistance distfunc = new EuclideanDistance();
    distfunc.setAttributeIndices("2-last");
    KDTree kdtree = new KDTree();
    kdtree.setDistanceFunction(distfunc);
    kdtree.setInstances(data);

    // obtain neighbors for a random instance
    Random rand = data.getRandomNumberGenerator(42);
    Instance inst = data.instance(rand.nextInt(data.numInstances()));
    Instances neighbors = kdtree.kNearestNeighbours(inst, 5);
    double[] distances = kdtree.getDistances();
    System.out.println("Neighbors data has " + neighbors.numAttributes() + " attributes.");
    System.out.println("\nInstance:\n" + inst);
    System.out.println("\nNeighbors:");
    for (int i = 0; i < neighbors.numInstances(); i++)
      System.out.println((i+1) + ". distance=" + distances[i] + "\n   " + neighbors.instance(i) + "");
  }
}
