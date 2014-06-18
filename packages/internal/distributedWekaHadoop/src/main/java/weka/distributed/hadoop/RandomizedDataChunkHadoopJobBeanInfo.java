package weka.distributed.hadoop;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;

public class RandomizedDataChunkHadoopJobBeanInfo extends HadoopJobBeanInfo {
  /**
   * Get an array of PropertyDescriptors for the ArffHeaderHadoopJob's public
   * properties.
   * 
   * @return an array of PropertyDescriptors
   */
  @Override
  public PropertyDescriptor[] getPropertyDescriptors() {
    try {
      PropertyDescriptor p1;
      ArrayList<PropertyDescriptor> pds = super.getBaseDescriptors();

      pds.remove(1); // additional weka libraries not needed for this job

      p1 =
        new PropertyDescriptor("numRandomizedDataChunks",
          RandomizedDataChunkHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("numInstancesPerRandomizedDataChunk",
          RandomizedDataChunkHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("randomSeed", RandomizedDataChunkHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("classAttribute",
          RandomizedDataChunkHadoopJob.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("cleanOutputDirectory",
          RandomizedDataChunkHadoopJob.class);
      pds.add(p1);

      return pds.toArray(new PropertyDescriptor[1]);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
}
