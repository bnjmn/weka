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

import weka.core.Instance;
import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.ProgrammaticProperty;
import weka.gui.beans.StreamThroughput;
import weka.gui.knowledgeflow.StepVisual;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
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

  /** reusable data object for streaming */
  protected Data m_incrementalData;

  /** overall flow throughput when streaming */
  protected StreamThroughput m_flowThroughput;

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
    if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_INSTANCE) > 0) {
      m_incrementalData = new Data(StepManager.CON_INSTANCE);
    } else {
      m_incrementalData = null;
      m_flowThroughput = null;
    }
  }

  /**
   * Start the data generation process.
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void start() throws WekaException {
    if (getStepManager().numOutgoingConnections() > 0) {
      weka.datagenerators.DataGenerator generator = getDataGenerator();
      if (getStepManager()
        .numOutgoingConnectionsOfType(StepManager.CON_DATASET) > 0) {
        getStepManager().processing();
        StringWriter output = new StringWriter();
        try {
          generator.setOutput(new PrintWriter(output));
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
      } else {
        // streaming case
        try {
          if (!generator.getSingleModeFlag()) {
            throw new WekaException("Generator does not support "
              + "incremental generation, so cannot be used with "
              + "outgoing 'instance' connections");
          }
        } catch (Exception ex) {
          throw new WekaException(ex);
        }
        String stm =
          getName() + "$" + hashCode() + 99 + "| overall flow throughput -|";
        m_flowThroughput =
          new StreamThroughput(stm, "Starting flow...",
            ((StepManagerImpl) getStepManager()).getLog());

        try {
          getStepManager().logBasic("Generating...");
          generator.setDatasetFormat(generator.defineDataFormat());

          for (int i = 0; i < generator.getNumExamplesAct(); i++) {
            m_flowThroughput.updateStart();
            getStepManager().throughputUpdateStart();
            if (isStopRequested()) {
              getStepManager().interrupted();
              return;
            }

            // over all examples to be produced
            Instance inst = generator.generateExample();
            m_incrementalData.setPayloadElement(StepManager.CON_INSTANCE, inst);
            getStepManager().throughputUpdateEnd();

            getStepManager().outputData(m_incrementalData);
            m_flowThroughput.updateEnd(((StepManagerImpl) getStepManager())
              .getLog());
          }

          if (isStopRequested()) {
            ((StepManagerImpl) getStepManager()).getLog().statusMessage(
              stm + "remove");
            getStepManager().interrupted();
            return;
          }
          m_flowThroughput.finished(((StepManagerImpl) getStepManager())
            .getLog());

          // signal end of input
          m_incrementalData.clearPayload();
          getStepManager().throughputFinished(m_incrementalData);
        } catch (Exception ex) {
          throw new WekaException(ex);
        }
      }
    }
  }

  /**
   * If possible, get the output structure for the named connection type as a
   * header-only set of instances. Can return null if the specified connection
   * type is not representable as Instances or cannot be determined at present.
   *
   * @param connectionName the name of the connection type to get the output
   *          structure for
   * @return the output structure as a header-only Instances object
   * @throws WekaException if a problem occurs
   */
  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {
    if (getStepManager().isStepBusy()) {
      return null;
    }

    weka.datagenerators.DataGenerator generator = getDataGenerator();
    try {
      return generator.defineDataFormat();
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Get acceptable incoming connection types. None in this case since this step
   * is a start point
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
    List<String> result = new ArrayList<String>();
    if (getStepManager().numOutgoingConnections() == 0) {
      result.add(StepManager.CON_DATASET);
      try {
        if (getDataGenerator().getSingleModeFlag()) {
          result.add(StepManager.CON_INSTANCE);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    } else if (getStepManager().numOutgoingConnectionsOfType(
      StepManager.CON_DATASET) > 0) {
      result.add(StepManager.CON_DATASET);
    } else {
      result.add(StepManager.CON_INSTANCE);
    }

    return result;
  }
}
