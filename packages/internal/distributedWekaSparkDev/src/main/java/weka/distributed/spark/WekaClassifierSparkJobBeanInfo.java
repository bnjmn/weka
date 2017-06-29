package weka.distributed.spark;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;

/**
 * BeanInfo class for the WekaClassifierSparkJob
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 11586 $
 */
public class WekaClassifierSparkJobBeanInfo extends SparkJobBeanInfo {

  /**
   * Get an array of PropertyDescriptors for the WekaClassifierSparkJob's
   * public properties.
   *
   * @return an array of PropertyDescriptors
   */
  @Override
  public PropertyDescriptor[] getPropertyDescriptors() {
    try {
      PropertyDescriptor p1;
      ArrayList<PropertyDescriptor> pds = super.getBaseDescriptors();

      p1 = new PropertyDescriptor("classAttribute",
        WekaClassifierSparkJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("modelFileName",
        WekaClassifierSparkJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("numIterations",
        WekaClassifierSparkJob.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("pathToPreconstructedFilter",
        WekaClassifierSparkJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("randomizeAndStratify",
        WekaClassifierSparkJob.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("numRandomlyShuffledSplits",
        WekaClassifierSparkJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("writeRandomlyShuffledSplitsToOutput",
        WekaClassifierSparkJob.class);
      pds.add(p1);

      return pds.toArray(new PropertyDescriptor[1]);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

}
