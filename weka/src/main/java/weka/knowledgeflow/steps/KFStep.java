package weka.knowledgeflow.steps;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KFStep {

  /**
   * The name of this step
   * 
   * @return the name of the step
   */
  String name();

  /**
   * The top-level folder in the JTree that this step should appear in
   * 
   * @return the name of the top-level folder that this step should appear in
   */
  String category();

  /**
   * Mouse-over tool tip for this step (appears when the mouse hovers over the
   * entry in the JTree)
   * 
   * @return the tool tip text for this step
   */
  String toolTipText();

  /**
   * Path (as a resource on the classpath) to the icon for this step
   * 
   * @return the path to the icon for this step
   */
  String iconPath();

  /**
   * True if this processing step is resource intensive (cpu or memory).
   * BaseExecution environment will use the limited number of worker thread
   * executor service to execute this step in this case.
   *
   * @return true if this step is CPU-intensive
   */
  boolean resourceIntensive() default false;
}
