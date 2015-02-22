package weka.gui.beans;

import java.beans.BeanDescriptor;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaScoringSparkJobBeanInfo extends AbstractSparkJobBeanInfo {

  /**
   * Get the bean descriptor for this bean
   *
   * @return a <code>BeanDescriptor</code> value
   */
  @Override
  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(weka.gui.beans.WekaScoringSparkJob.class,
      SparkJobCustomizer.class);
  }
}
