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
 *    ClassesToClusters.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.clusterers;

import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * This class shows how to perform a "classes-to-clusters"
 * evaluation like in the Explorer using EM. The class needs as
 * first parameter an ARFF file to work on. The last attribute is
 * interpreted as the class attribute. 
 * <p/>
 * This code is based on the method "startClusterer" of the 
 * "weka.gui.explorer.ClustererPanel" class and the 
 * "evaluateClusterer" method of the "weka.clusterers.ClusterEvaluation" 
 * class.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class ClassesToClusters {
  public static void main(String[] args) throws Exception {
    // load data
    Instances data = new Instances(new BufferedReader(new FileReader(args[0])));
    data.setClassIndex(data.numAttributes() - 1);

    // generate data for clusterer (w/o class)
    Remove filter = new Remove();
    filter.setAttributeIndices("" + (data.classIndex() + 1));
    filter.setInputFormat(data);
    Instances dataClusterer = Filter.useFilter(data, filter);

    // train clusterer
    EM clusterer = new EM();
    // set further options for EM, if necessary...
    clusterer.buildClusterer(dataClusterer);

    // evaluate clusterer
    ClusterEvaluation eval = new ClusterEvaluation();
    eval.setClusterer(clusterer);
    eval.evaluateClusterer(data);

    // print results
    System.out.println(eval.clusterResultsToString());
  }
}
