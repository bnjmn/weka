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
 *    ASSearchStrategy.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import weka.attributeSelection.ASSearch;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.StepVisual;
import weka.knowledgeflow.StepManager;

/**
 * Step that wraps a Weka attribute selection search strategy. This is just an
 * "info" step - i.e. it needs to be connection (via an StepManager.CON_INFO)
 * connection to a ASEvaluator step.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 * @see ASEvaluator
 */
@KFStep(name = "ASSearchStrategy", category = "AttSelection",
  toolTipText = "Weka attribute selection search wrapper", iconPath = "")
public class ASSearchStrategy extends WekaAlgorithmWrapper {
  private static final long serialVersionUID = 5038697382280884975L;

  /**
   * Initialize the step
   */
  @Override
  public void stepInit() {
    // nothing to do - we are just an "info" step
  }

  /**
   * Get a list of incoming connections that this step accepts. This step is an
   * info only step, so no incoming connections are allowed
   * 
   * @return a list of connections that this step accepts
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    // no incoming connections allowed
    return new ArrayList<String>();
  }

  /**
   * Get a list of outgoing connections from this step. This step is an info
   * only step, so the only outgoing connection is of type "info".
   * 
   * @return a list of outgoing connections
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    return Arrays.asList(StepManager.CON_INFO);
  }

  /**
   * Get the class of the algorithm wrapped by this wrapper step (ASSearch in
   * this case).
   * 
   * @return the class of the wrapped algorithm
   */
  @Override
  public Class getWrappedAlgorithmClass() {
    return weka.attributeSelection.ASSearch.class;
  }

  /**
   * Set the actual algorithm wrapped by this instance
   * 
   * @param algo the algorithm wrapped
   */
  @Override
  public void setWrappedAlgorithm(Object algo) {
    super.setWrappedAlgorithm(algo);
    m_defaultIconPath =
      StepVisual.BASE_ICON_PATH
        + "filters.supervised.attribute.AttributeSelection.gif";
  }

  /**
   * Set the search strategy wrapped by this step (calls setWrappedAlgorithm)
   *
   * @param searchStrategy the search strategy to wrap
   */
  @ProgrammaticProperty
  public void setSearchStrategy(ASSearch searchStrategy) {
    setWrappedAlgorithm(searchStrategy);
  }

  /**
   * Get the search strategy wrapped by this step
   *
   * @return
   */
  public ASSearch getSearchStrategy() {
    return (ASSearch) getWrappedAlgorithm();
  }
}
