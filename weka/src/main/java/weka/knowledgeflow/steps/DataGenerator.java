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
 *    DataGenerator.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.WekaException;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.StepVisual;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Step that wraps a Weka DataGenerator.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "DataGenerator", category = "DataGenerators",
  toolTipText = "Weka data generator wrapper", iconPath = "")
public class DataGenerator extends WekaAlgorithmWrapper {

  private static final long serialVersionUID = -7716707145987484527L;

  /**
   * Get the class of the wrapped algorithm
   *
   * @return the class of the wrapped algorithm
   */
  @Override
  public Class getWrappedAlgorithmClass() {
    return weka.datagenerators.DataGenerator.class;
  }

  /**
   * Set the algorithm to wrap
   *
   * @param algo the algorithm to wrao
   */
  @Override
  public void setWrappedAlgorithm(Object algo) {
    super.setWrappedAlgorithm(algo);
    m_defaultIconPath = StepVisual.BASE_ICON_PATH + "DefaultDataSource.gif";
  }

  /**
   * get the data generator
   *
   * @return the data generator
   */
  public weka.datagenerators.DataGenerator getDataGenerator() {
    return (weka.datagenerators.DataGenerator) getWrappedAlgorithm();
  }

  /**
   * Set the data generator
   *
   * @param dataGenerator
   */
  @ProgrammaticProperty
  public void setDataGenerator(weka.datagenerators.DataGenerator dataGenerator) {
    setWrappedAlgorithm(dataGenerator);
  }

  /**
   * Initialize the step. Nothing to do in this case.
   */
  @Override
  public void stepInit() {

  }

  /**
   * Start the data generation process.
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void start() throws WekaException {
    if (getStepManager().numOutgoingConnections() > 0) {
      getStepManager().processing();
      StringWriter output = new StringWriter();
      weka.datagenerators.DataGenerator generator = getDataGenerator();
      generator.setOutput(new PrintWriter(output));
      try {
        getStepManager().statusMessage("Generating...");
        getStepManager().logBasic("Generating data");
        weka.datagenerators.DataGenerator.makeData(generator,
          generator.getOptions());
        Instances instances =
          new Instances(new StringReader(output.toString()));

        if (!isStopRequested()) {
          Data outputData = new Data(StepManager.CON_DATASET, instances);
          getStepManager().outputData(outputData);
        }
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
      if (isStopRequested()) {
        getStepManager().interrupted();
      } else {
        getStepManager().finished();
      }
    }
  }

  /**
   * Get acceptable incoming connection types. None in this case since this
   * step is a start point
   *
   * @return null (no acceptable incoming connections)
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    return null;
  }

  /**
   * Get a list of outgoing connection types
   *
   * @return a list of outgoing connection types
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    return Arrays.asList(StepManager.CON_DATASET);
  }
}
