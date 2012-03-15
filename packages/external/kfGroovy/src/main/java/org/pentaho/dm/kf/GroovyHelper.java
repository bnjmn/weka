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
 *    GroovyHelper.java
 *    Copyright (C) 2009 Pentaho Corporation
 *
 */

package org.pentaho.dm.kf;

import java.util.ArrayList;

import weka.gui.beans.BatchClassifierListener;
import weka.gui.beans.BatchClustererListener;
import weka.gui.beans.ChartListener;
import weka.gui.beans.DataSourceListener;
import weka.gui.beans.GraphListener;
import weka.gui.beans.IncrementalClassifierListener;
import weka.gui.beans.InstanceListener;
import weka.gui.beans.TestSetListener;
import weka.gui.beans.TextListener;
import weka.gui.beans.ThresholdDataListener;
import weka.gui.beans.TrainingSetListener;
import weka.gui.beans.VisualizableErrorListener;

/**
 * Helper interface that allows Groovy scripts to call back
 * to the GroovyComponent to manage event notifications.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision $
 */
public interface GroovyHelper {
   
  /**
   * Notify the appropriate listener(s) for the supplied
   * event.
   * 
   * @param event the event to pass on to listeners.
   */
  void notifyListenerType(Object event);
  
  /**
   * Get the list of training set listeners
   * 
   * @return the list of training set listeners
   */
  ArrayList<TrainingSetListener> getTrainingSetListeners();
  
  /**
   * Get the list of test set listeners
   * 
   * @return the list of test set listeners
   */
  ArrayList<TestSetListener> getTestSetListeners();
  
  /**
   * Get the list of instance listeners
   * 
   * @return the list of instance listeners
   */
  ArrayList<InstanceListener> getInstanceListeners();
  
  /**
   * Get the list of text listeners
   * 
   * @return the list of text listeners
   */
  ArrayList<TextListener> getTextListeners();
  
  /**
   * Get the list of data source listeners
   * 
   * @return the list of data source listeners
   */
  ArrayList<DataSourceListener> getDataSourceListeners();
  
  /**
   * Get the list of batch classifier listeners
   * 
   * @return the list of batch classifier listeners
   */
  ArrayList<BatchClassifierListener> getBatchClassifierListeners();
  
  /**
   * Get the list of incremental classifier listeners
   * 
   * @return the list of incremental classifier listeners
   */
  ArrayList<IncrementalClassifierListener> getIncrementalClassifierListeners();
  
  /**
   * Get the list of batch clusterer listeners
   * 
   * @return the list of batch clusterer listeners
   */
  ArrayList<BatchClustererListener> getBatchClustererListeners();
  
  /**
   * Get the list of graph listeners
   * 
   * @return the list of graph listeners
   */
  ArrayList<GraphListener> getGraphListeners();
  
  /**
   * Get the list of chart listeners
   * 
   * @return the list of chart listeners
   */
  ArrayList<ChartListener> getChartListeners();
  
  /**
   * Get the list of threshold data listeners
   * 
   * @return the list of threshold data listeners
   */
  ArrayList<ThresholdDataListener> getThresholdDataListeners();
  
  /**
   * Get the list of visualizable error listeners
   * 
   * @return the list of visualizable error listeners
   */
  ArrayList<VisualizableErrorListener> getVisualizableErrorListeners();
}
