package weka.gui.beans;

import java.beans.EventSetDescriptor;
import java.beans.SimpleBeanInfo;

public class AbstractSparkJobBeanInfo extends SimpleBeanInfo {

  /**
   * Get the event set descriptors pertinent to data sources
   * 
   * @return an <code>EventSetDescriptor[]</code> value
   */
  @Override
  public EventSetDescriptor[] getEventSetDescriptors() {

    try {
      EventSetDescriptor[] eds = {
        new EventSetDescriptor(AbstractSparkJob.class, "success",
          SuccessListener.class, "acceptSuccess"),
        new EventSetDescriptor(AbstractSparkJob.class, "failure",
          FailureListener.class, "acceptFailure") };

      return eds;
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return null;
  }
}
