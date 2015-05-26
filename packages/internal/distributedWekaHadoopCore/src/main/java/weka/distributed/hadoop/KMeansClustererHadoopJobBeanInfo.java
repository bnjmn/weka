/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    KMeansClustererHadoopJobBeanInfo
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;

/**
 * BeanInfo class for the KMeansClustererHadoopJob
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class KMeansClustererHadoopJobBeanInfo extends HadoopJobBeanInfo {

  /**
   * Get an array of PropertyDescriptors for the WekaClassifierHadoopJob's
   * public properties.
   * 
   * @return an array of PropertyDescriptors
   */
  @Override
  public PropertyDescriptor[] getPropertyDescriptors() {
    try {
      PropertyDescriptor p1;
      ArrayList<PropertyDescriptor> pds = super.getBaseDescriptors();

      p1 = new PropertyDescriptor("numNodesInCluster",
        KMeansClustererHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("convergenceTolerance",
        KMeansClustererHadoopJob.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("initWithRandomCentroids",
        KMeansClustererHadoopJob.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("randomlyShuffleData",
        KMeansClustererHadoopJob.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("randomlyShuffleDataNumChunks",
        KMeansClustererHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("modelFileName",
        KMeansClustererHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("numClusters",
        KMeansClustererHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("numRuns",
        KMeansClustererHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("numIterations",
        KMeansClustererHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("randomSeed",
        KMeansClustererHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("kMeansParallelInitSteps",
        KMeansClustererHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("displayCentroidStdDevs",
        KMeansClustererHadoopJob.class);
      pds.add(p1);

      return pds.toArray(new PropertyDescriptor[1]);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
}
