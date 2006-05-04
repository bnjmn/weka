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
 * StringLocator.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 */

package weka.core;


/**
 * This class locates and records the indices of String attributes, 
 * recursively in case of Relational attributes. The indices are normally
 * used for copying the Strings from one Instances object to another.
 * 
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 * @see Attribute#STRING
 * @see Attribute#RELATIONAL
 * @see weka.filters.Filter#copyStringValues(Instance, boolean, Instances, Instances)
 */
public class StringLocator
  extends AttributeLocator {
  
  /**
   * initializes the StringLocator with the given data
   * 
   * @param data	the data to work on
   */
  public StringLocator(Instances data) {
    super(data, Attribute.STRING);
  }

  /**
   * Copies string values contained in the instance copied to a new
   * dataset. The Instance must already be assigned to a dataset. This
   * dataset and the destination dataset must have the same structure.
   *
   * @param instance the Instance containing the string values to copy.
   * @param destDataset the destination set of Instances
   * @param strAtts a AttributeLocator containing the indices of any string attributes
   * in the dataset.  
   */
  public static void copyStringValues(Instance inst, Instances destDataset, 
                               AttributeLocator strAtts) {

    if (inst.dataset() == null) {
      throw new IllegalArgumentException("Instance has no dataset assigned!!");
    } else if (inst.dataset().numAttributes() != destDataset.numAttributes()) {
      throw new IllegalArgumentException("Src and Dest differ in # of attributes!!");
    } 
    copyStringValues(inst, true, inst.dataset(), strAtts,
                     destDataset, strAtts);
  }

  /**
   * Takes string values referenced by an Instance and copies them from a
   * source dataset to a destination dataset. The instance references are
   * updated to be valid for the destination dataset. The instance may have the 
   * structure (i.e. number and attribute position) of either dataset (this
   * affects where references are obtained from). Only works if the number
   * of string attributes is the same in both indices (implicitly these string
   * attributes should be semantically same but just with shifted positions).
   *
   * @param instance the instance containing references to strings in the source
   * dataset that will have references updated to be valid for the destination
   * dataset.
   * @param instSrcCompat true if the instance structure is the same as the
   * source, or false if it is the same as the destination (i.e. which of the
   * string attribute indices contains the correct locations for this instance).
   * @param srcDataset the dataset for which the current instance string
   * references are valid (after any position mapping if needed)
   * @param srcStrAtts a AttributeLocator containing the indices of string attributes
   * in the source datset.
   * @param destDataset the dataset for which the current instance string
   * references need to be inserted (after any position mapping if needed)
   * @param destStrAtts a AttributeLocator containing the indices of string attributes
   * in the destination datset.
   */
  public static void copyStringValues(Instance instance, boolean instSrcCompat,
                                  Instances srcDataset, AttributeLocator srcStrAtts,
                                  Instances destDataset, AttributeLocator destStrAtts) {
    if (srcDataset == destDataset)
      return;
    
    if (srcStrAtts.getAttributeIndices().length != destStrAtts.getAttributeIndices().length)
      throw new IllegalArgumentException("Src and Dest string indices differ in length!!");

    if (srcStrAtts.getLocatorIndices().length != destStrAtts.getLocatorIndices().length)
      throw new IllegalArgumentException("Src and Dest locator indices differ in length!!");

    for (int i = 0; i < srcStrAtts.getAttributeIndices().length; i++) {
      int instIndex = instSrcCompat ? srcStrAtts.getAttributeIndices()[i] : destStrAtts.getAttributeIndices()[i];
      Attribute src = srcDataset.attribute(srcStrAtts.getAttributeIndices()[i]);
      Attribute dest = destDataset.attribute(destStrAtts.getAttributeIndices()[i]);
      if (!instance.isMissing(instIndex)) {
        int valIndex = dest.addStringValue(src, (int)instance.value(instIndex));
        instance.setValue(instIndex, (double)valIndex);
      }
    }
    
    // recurse if necessary
    int[] indices = srcStrAtts.getLocatorIndices();
    for (int i = 0; i < indices.length; i++) {
      Instances rel = instance.relationalValue(indices[i]);
      AttributeLocator srcStrAttsNew = srcStrAtts.getLocator(indices[i]);
      Instances srcDatasetNew = srcStrAttsNew.getData();
      AttributeLocator destStrAttsNew = destStrAtts.getLocator(indices[i]);
      Instances destDatasetNew = destStrAttsNew.getData();
      for (int n = 0; n < rel.numInstances(); n++) {
        copyStringValues(rel.instance(n), instSrcCompat, srcDatasetNew, srcStrAttsNew, destDatasetNew, destStrAttsNew);
      }
    }
  }
}
