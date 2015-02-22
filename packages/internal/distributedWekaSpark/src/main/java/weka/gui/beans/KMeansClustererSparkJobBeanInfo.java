package weka.gui.beans;

import java.beans.BeanDescriptor;
import java.beans.EventSetDescriptor;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class KMeansClustererSparkJobBeanInfo extends AbstractSparkJobBeanInfo {

  /**
   * Get the event set descriptors pertinent to data sources
   *
   * @return an <code>EventSetDescriptor[]</code> value
   */
  @Override
  public EventSetDescriptor[] getEventSetDescriptors() {

    try {
      List<EventSetDescriptor> descriptors =
        new ArrayList<EventSetDescriptor>();
      for (EventSetDescriptor es : super.getEventSetDescriptors()) {
        descriptors.add(es);
      }

      descriptors.add(new EventSetDescriptor(KMeansClustererSparkJob.class,
        "text", TextListener.class, "acceptText"));
      descriptors.add(new EventSetDescriptor(KMeansClustererSparkJob.class,
        "batchClusterer", BatchClustererListener.class, "acceptClusterer"));

      return descriptors.toArray(new EventSetDescriptor[descriptors.size()]);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return null;
  }

  /**
   * Get the bean descriptor for this bean
   *
   * @return a <code>BeanDescriptor</code> value
   */
  @Override
  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(weka.gui.beans.KMeansClustererSparkJob.class,
      SparkJobCustomizer.class);
  }
}
