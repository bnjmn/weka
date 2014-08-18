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
 *    AggregateableFilteredClassifierUpdateable.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Classifier;
import weka.classifiers.UpdateableBatchProcessor;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.core.Instance;
import weka.gui.beans.KFIgnore;

/**
 * An extension of AggregateableFilteredClassifier that implements
 * UpdateableClassifier, For use with Aggregateable base classifiers that are
 * also UpdateableClassifiers
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFIgnore
public class AggregateableFilteredClassifierUpdateable extends
  AggregateableFilteredClassifier implements UpdateableClassifier,
  UpdateableBatchProcessor {

  /** For serialization */
  private static final long serialVersionUID = -852585046360926783L;

  public AggregateableFilteredClassifierUpdateable() {
    m_Classifier = new NaiveBayesUpdateable();
  }

  @Override
  public void setClassifier(Classifier c) {

    if (!(c instanceof UpdateableClassifier)) {
      throw new IllegalArgumentException("Base classifier ("
        + c.getClass().getName() + ") must be updateable!");
    }

    super.setClassifier(c);
  }

  @Override
  public void updateClassifier(Instance instance) throws Exception {
    if (getPreConstructedFilter().numPendingOutput() > 0) {
      throw new Exception("Filter output queue not empty!");
    }

    if (!getPreConstructedFilter().input(instance)) {
      // throw new Exception(
      // "Can only use PreconstructedFilters that will produce an output "
      // + "Instance immediately when given an input Instance.");

      // only allow a filter to consume an instance and not buffer anything
      if (getPreConstructedFilter().numPendingOutput() > 0) {
        throw new Exception("Filter output queue not empty!");
      }

      // nothing to train on if filter does not make instance available
      return;
    }
    getPreConstructedFilter().batchFinished();
    Instance filtered = getPreConstructedFilter().output();

    ((UpdateableClassifier) getClassifier()).updateClassifier(filtered);
  }

  @Override
  public String toString() {
    if (m_filteredInstances == null) {
      return "AggregateableFilteredClassifierUpdateable: No model built yet";
    }

    StringBuilder b = new StringBuilder();

    b.append("AggregateableFilterdClassifierUpdateable using ")
      .append(getClassifierSpec()).append(" on data filtered through ")
      .append(getFilterSpec()).append("\n\n").append("Filtered Header\n")
      .append(m_filteredInstances.toString()).append(m_Classifier.toString());

    return b.toString();
  }

  @Override
  public void batchFinished() throws Exception {
    if (getClassifier() instanceof UpdateableBatchProcessor) {
      ((UpdateableBatchProcessor) getClassifier()).batchFinished();
    }
  }
}
