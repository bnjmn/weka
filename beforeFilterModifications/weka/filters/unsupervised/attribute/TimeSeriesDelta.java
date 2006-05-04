/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    TimeSeriesDelta.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.filters.unsupervised.attribute;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.UnsupportedAttributeTypeException;
import weka.core.Utils;
import weka.filters.Filter;

/** 
 <!-- globalinfo-start -->
 * An instance filter that assumes instances form time-series data and replaces attribute values in the current instance with the difference between the current value and the equivalent attribute attribute value of some previous (or future) instance. For instances where the time-shifted value is unknown either the instance may be dropped, or missing values used.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -R &lt;index1,index2-index4,...&gt;
 *  Specify list of columns to translate in time. First and
 *  last are valid indexes. (default none)</pre>
 * 
 * <pre> -V
 *  Invert matching sense (i.e. calculate for all non-specified columns)</pre>
 * 
 * <pre> -I &lt;num&gt;
 *  The number of instances forward to translate values
 *  between. A negative number indicates taking values from
 *  a past instance. (default -1)</pre>
 * 
 * <pre> -M
 *  For instances at the beginning or end of the dataset where
 *  the translated values are not known, use missing values
 *  (default is to remove those instances).</pre>
 * 
 <!-- options-end -->
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class TimeSeriesDelta 
  extends TimeSeriesTranslate {
  
  /** for serialization */
  static final long serialVersionUID = 3101490081896634942L;

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "An instance filter that assumes instances form time-series data and "
      + "replaces attribute values in the current instance with the difference "
      + "between the current value and the equivalent attribute attribute value "
      + "of some previous (or future) instance. For instances where the time-shifted "
      + "value is unknown either the instance may be dropped, or missing values "
      + "used.";
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws UnsupportedAttributeTypeException if selected
   * attributes are not numeric.  
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);
    // Create the output buffer
    Instances outputFormat = new Instances(instanceInfo, 0); 
    for(int i = 0; i < instanceInfo.numAttributes(); i++) {
      if (m_SelectedCols.isInRange(i)) {
	if (outputFormat.attribute(i).isNumeric()) {
	  outputFormat.renameAttribute(i, outputFormat.attribute(i).name()
				       + " d"
				       + (m_InstanceRange < 0 ? '-' : '+')
				       + Math.abs(m_InstanceRange));
	} else {
	  throw new UnsupportedAttributeTypeException("Time delta attributes must be numeric!");
	}
      }
    }
    setOutputFormat(outputFormat);
    return true;
  }
  

  /**
   * Creates a new instance the same as one instance (the "destination")
   * but with some attribute values copied from another instance
   * (the "source")
   *
   * @param source the source instance
   * @param dest the destination instance
   * @return the new merged instance
   */
  protected Instance mergeInstances(Instance source, Instance dest) {

    Instances outputFormat = outputFormatPeek();
    double[] vals = new double[outputFormat.numAttributes()];
    for(int i = 0; i < vals.length; i++) {
      if (m_SelectedCols.isInRange(i)) {
	if ((source != null)
	    && !source.isMissing(i)
	    && !dest.isMissing(i)) {
	  vals[i] = dest.value(i) - source.value(i);
	}
      } else {
	vals[i] = dest.value(i);
      }
    }
    Instance inst = null;
    if (dest instanceof SparseInstance) {
      inst = new SparseInstance(dest.weight(), vals);
    } else {
      inst = new Instance(dest.weight(), vals);
    }
    inst.setDataset(dest.dataset());
    return inst;
  }
  
  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new TimeSeriesDelta(), argv); 
      } else {
	Filter.filterFile(new TimeSeriesDelta(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}
