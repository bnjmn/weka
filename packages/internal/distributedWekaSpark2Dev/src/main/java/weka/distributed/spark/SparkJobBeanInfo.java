package weka.distributed.spark;

import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.ArrayList;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 11586 $
 */
public class SparkJobBeanInfo extends SimpleBeanInfo {

  /**
   * Get an array of PropertyDescriptors for the HadoopJob's public properties.
   *
   * @return an array of PropertyDescriptors
   */
  public ArrayList<PropertyDescriptor> getBaseDescriptors() {
    try {
      PropertyDescriptor p1;
      ArrayList<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
      
      p1 = new PropertyDescriptor("debug", SparkJob.class);
      pds.add(p1);

      return pds;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
}
