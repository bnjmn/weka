package weka.knowledgeflow;

import weka.core.WekaException;

public interface StepOutputListener {

  /**
   * Process data produced by a knowledge flow step
   * 
   * @param data the payload to process
   * @return true if processing was successful
   * @throws WekaException in the case of a catastrophic failure of the
   *           StepOutputListener
   */
  boolean dataFromStep(Data data) throws WekaException;
}
