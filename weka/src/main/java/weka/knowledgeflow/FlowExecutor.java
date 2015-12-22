package weka.knowledgeflow;

import weka.core.Settings;
import weka.core.WekaException;
import weka.gui.Logger;

public interface FlowExecutor {

  void setFlow(Flow flow);

  Flow getFlow();

  void setLogger(Logger logger);

  Logger getLogger();

  void setLoggingLevel(LoggingLevel level);

  public LoggingLevel getLoggingLevel();

  void setExecutionEnvironment(BaseExecutionEnvironment env);

  /**
   * Convenience method for applying settings - implementers should delegate the
   * the execution environment
   * 
   * @param settings the settings to use
   */
  void setSettings(Settings settings);

  /**
   * Convenience method for getting current settings - implementers should
   * delegate the the execution environment
   *
   * @return the settings in use
   */
  Settings getSettings();

  /**
   * Return the execution environment object for this flow executor
   * 
   * @return the execution environment
   */
  BaseExecutionEnvironment getExecutionEnvironment();

  void runSequentially() throws WekaException;

  void runParallel() throws WekaException;

  /**
   * Stop all processing
   */
  void stopProcessing();

  /**
   * Returns true if execution was stopped via the stopProcessing() method
   *
   * @return true if execution was stopped
   */
  boolean wasStopped();

  /**
   * Set a callback to notify when execution finishes
   *
   * @param callback the callback to notify
   */
  void setExecutionFinishedCallback(ExecutionFinishedCallback callback);
}
