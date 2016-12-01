package weka.knowledgeflow;

import weka.knowledgeflow.steps.Step;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * A task that can be executed by the ExecutionEnvironment's submitTask()
 * service. Step's wanting to use this to execute units of work in parallel must
 * extends it and can work with it in one of two ways:<br>
 * <br>
 * 
 * 1. By using the Future<ExecutionResult> returned by submitTask(), or<br>
 * 2. By registering an implementation of StepCallback when creating a subclass
 * of StepTask. <br>
 * 
 * Subclasses of StepTask should store their results (and potentially errors) in
 * the provided ExecutionResult member variable (obtainable by calling
 * getExecutionResult()).
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * 
 * @param <T> the type of the result stored in the returned ExecutionResult
 *          object
 */
public abstract class StepTask<T> implements Callable<ExecutionResult<T>>,
  Serializable {

  /** For serialization */
  private static final long serialVersionUID = 2995081029283027784L;

  /**
   * The result of executing the task - ready to be populated by subclass's
   * call() implementation
   */
  protected ExecutionResult<T> m_result = new ExecutionResult<T>();

  /** Optional callback to invoke after the processing is complete */
  protected transient StepTaskCallback<T> m_callback;

  /** The log to use */
  protected LogManager m_log;

  /** True if this is a high resource (cpu/memory) task */
  protected boolean m_resourceIntensive = true;

  /**
   * True if only one of these tasks can be executing at any one time in the
   * Knowledge Flow/JVM. This has priority over isResourceIntensive() and causes
   * the task to run on an executor service with one worker thread.
   */
  protected boolean m_mustRunSingleThreaded;

  /**
   * The callback notifier delegate. Performs the actual notification back to
   * the step
   */
  protected CallbackNotifierDelegate m_callbackNotifier =
    new DefaultCallbackNotifierDelegate();

  /**
   * Constructor. Use this constructor if you are going to access the Future
   * returned by ExecutionEnvironment.submitTask().
   * 
   * @param source the source step producing this task
   */
  public StepTask(Step source) {
    this(source, null, false);
  }

  /**
   * Constructor. Use this constructor if you are going to access the Future
   * returned by ExecutionEnvironment.submitTask()
   *
   * @param source the source step producing this task
   * @param resourceIntensive true if this task is cpu/memory intensive
   */
  public StepTask(Step source, boolean resourceIntensive) {
    this(source, null, resourceIntensive);
  }

  /**
   * Constructor with supplied callback. Use this constructor to be notified via
   * the supplied callback when a the task has completed processing
   * 
   * @param source the source step producing this task
   * @param callback the callback to use
   */
  public StepTask(Step source, StepTaskCallback<T> callback) {
    this(source, callback, false);
  }

  /**
   * Constructor with supplied callback. Use this constructor to be notified via
   * the supplied callback when a task has completed processing
   *
   * @param source the source step producing this task
   * @param callback the callback to use
   * @param resourceIntensive true if this task is cpu/memory intensive
   */
  public StepTask(Step source, StepTaskCallback<T> callback,
    boolean resourceIntensive) {
    m_log = new LogManager(source);
    m_callback = callback;
    m_resourceIntensive = resourceIntensive;
  }

  /**
   * Set whether this {@code StepTask} is resource intensive (cpu/memory) or
   * not. By default, a {@code StepTask} is resource intensive
   *
   * @param resourceIntensive false if this {@code StepTask} is not resource
   *          intensive
   */
  public void setResourceIntensive(boolean resourceIntensive) {
    m_resourceIntensive = resourceIntensive;
  }

  /**
   * Get whether this {@code StepTask} is resource intensive (cpu/memory) or
   * not. By default, a {@code StepTask} is resource intensive
   *
   * @return false if this {@code StepTask} is not resource intensive
   */
  public boolean isResourceIntensive() {
    return m_resourceIntensive;
  }

  /**
   * Set whether this {@code StepTask} must run single threaded - i.e. only
   * one of these tasks is executing at any one time in the JVM. The Knowledge
   * Flow uses a special executor service with a single worker thread to execute
   * these tasks. This property, if true, overrides isResourceIntensive().
   *
   * @param singleThreaded true if this task must run single threaded
   */
  public void setMustRunSingleThreaded(boolean singleThreaded) {
    m_mustRunSingleThreaded = singleThreaded;
  }

  /**
   * Get whether this {@code StepTask} must run single threaded - i.e. only
   * one of these tasks is executing at any one time in the JVM. The Knowledge
   * Flow uses a special executor service with a single worker thread to execute
   * these tasks. This property, if true, overrides isResourceIntensive().
   *
   * @return true if this task must run single threaded
   */
  public boolean getMustRunSingleThreaded() {
    return m_mustRunSingleThreaded;
  }

  /**
   * Get the callback notifier delegate to use. This method is used by the
   * Execution environment and is not normally of interest to subclasses
   *
   * @return the callback notifier delegate in use
   */
  protected final CallbackNotifierDelegate getCallbackNotifierDelegate() {
    return m_callbackNotifier;
  }

  /**
   * Set the callback notifier delegate to use. This method is used by the
   * Execution environment and is not normally of interest to subclasses
   *
   * @param delegate the delegate to use
   */
  protected final void setCallbackNotifierDelegate(
    CallbackNotifierDelegate delegate) {
    m_callbackNotifier = delegate;
  }

  /**
   * Get the LogHandler to use for logging
   *
   * @return the LogHandler
   */
  protected final LogManager getLogHandler() {
    return m_log;
  }

  /**
   * Set the logger to use. This is used by the execution environment -
   * subclasses should call getLogHandler() to do logging
   *
   * @param log the log to use
   */
  protected final void setLogHandler(LogManager log) {
    m_log = log;
  }

  /**
   * Notifies the registered callback (if any)
   * 
   * @throws Exception if a problem occurs
   */
  protected final void notifyCallback() throws Exception {
    if (m_callback != null) {
      m_callbackNotifier.notifyCallback(m_callback, this, m_result);
    }
  }

  /**
   * Get the result of execution
   * 
   * @return the result of execution
   */
  protected final ExecutionResult<T> getExecutionResult() {
    return m_result;
  }

  /**
   * Set the result of execution
   * 
   * @param execResult the result of execution
   */
  protected final void setExecutionResult(ExecutionResult<T> execResult) {
    m_result = execResult;
  }

  /**
   * Executor service calls this method to do the work
   *
   * @return the results of execution in an ExecutionResult
   */
  @Override
  public ExecutionResult<T> call() throws Exception {
    try {
      process();
    } catch (Exception ex) {
      getExecutionResult().setError(ex);
    }
    notifyCallback();

    return m_result;
  }

  /**
   * The actual work gets done here. Subclasses to override. Subclasses can use
   * getExecutionResult() to obtain an ExecutionResult object to store their
   * results in
   */
  public abstract void process() throws Exception;
}
