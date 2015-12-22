package weka.knowledgeflow;

/**
 * Callback that Steps can use when executing StepTasks via
 * EnvironmentManager.submitTask().
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * 
 * @param <T> the result return type (gets encapsulated in an ExecutionResult)
 */
public interface StepTaskCallback<T> {

  public void taskFinished(ExecutionResult<T> result) throws Exception;

  public void taskFailed(StepTask<T> failedTask,
    ExecutionResult<T> failedResult) throws Exception;
}
