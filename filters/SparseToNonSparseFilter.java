/*
 *    SparseToNonSparseFilter.java
 *    Copyright (C) 2000 Intelligenesis Corp.
 *
 */


package weka.filters;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.Instance;
import weka.core.Utils;


/** 
 * A filter that converts all incoming sparse instances into 
 * non-sparse format.
 *
 * @author Len Trigg (len@intelligenesis.net)
 * @version $Revision: 1.5 $ 
 */
public class SparseToNonSparseFilter extends Filter {

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "An instance filter that converts all incoming sparse instances"
      + " into non-sparse format.";
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   */
  public boolean inputFormat(Instances instanceInfo) throws Exception {

    super.inputFormat(instanceInfo);
    setOutputFormat(instanceInfo);
    return true;
  }


  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception IllegalStateException if no input structure has been defined
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    Instance inst = null;
    if (instance instanceof SparseInstance) {
      inst = new Instance(instance.weight(), instance.toDoubleArray());
      inst.setDataset(instance.dataset());
    } else {
      inst = instance;
    }
    push(inst);
    return true;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {
    
    try {
      if (Utils.getFlag('b', argv)) {
	Filter.batchFilterFile(new SparseToNonSparseFilter(), argv);
      } else {
	Filter.filterFile(new SparseToNonSparseFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








