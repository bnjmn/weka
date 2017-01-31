/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    CSVToARFFHeaderMapTaskBeanInfo.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.ArrayList;

/**
 * BeanInfo class for the CSVToARFFHeaderMapTask
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CSVToARFFHeaderMapTaskBeanInfo extends SimpleBeanInfo {

  /**
   * Get an array of PropertyDescriptors for the CSVToArffHeaderMapTask's public
   * properties.
   * 
   * @return an array of PropertyDescriptors
   */
  @Override
  public PropertyDescriptor[] getPropertyDescriptors() {
    try {
      PropertyDescriptor p1;
      ArrayList<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();

      p1 =
        new PropertyDescriptor("computeQuartilesAsPartOfSummaryStats",
          CSVToARFFHeaderMapTask.class);
      // p1.setPropertyEditorClass(weka.gui.beans.FileEnvironmentField.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("compressionLevelForQuartileEstimation",
          CSVToARFFHeaderMapTask.class);
      // p1.setPropertyEditorClass(weka.gui.beans.FileEnvironmentField.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("dateAttributes", CSVToARFFHeaderMapTask.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("dateFormat", CSVToARFFHeaderMapTask.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("treatUnparsableNumericValuesAsMissing",
          CSVToARFFHeaderMapTask.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("enclosureCharacters",
          CSVToARFFHeaderMapTask.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("fieldSeparator", CSVToARFFHeaderMapTask.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 = new PropertyDescriptor("missingValue", CSVToARFFHeaderMapTask.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("nominalAttributes",
          CSVToARFFHeaderMapTask.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("nominalLabelSpecs",
          CSVToARFFHeaderMapTask.class);
      p1.setPropertyEditorClass(weka.gui.GenericArrayEditor.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("nominalDefaultLabelSpecs",
          CSVToARFFHeaderMapTask.class);
      p1.setPropertyEditorClass(weka.gui.GenericArrayEditor.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("stringAttributes", CSVToARFFHeaderMapTask.class);
      p1.setPropertyEditorClass(weka.gui.beans.EnvironmentField.class);
      pds.add(p1);

      p1 =
        new PropertyDescriptor("numDecimalPlaces", CSVToARFFHeaderMapTask.class);
      pds.add(p1);

      return pds.toArray(new PropertyDescriptor[1]);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
}
