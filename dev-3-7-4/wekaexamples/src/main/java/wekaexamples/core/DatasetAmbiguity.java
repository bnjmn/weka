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
 *    DatasetAmbiguity.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.core;

import weka.core.Instance;
import weka.core.InstanceComparator;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.util.TreeSet;

/**
 * Helper class for determining ambiguity in datasets. It outputs the total
 * count of instances in the dataset first. Then, the number of unique
 * instances, taking all attributes including the class attribute into
 * account. Finally, the number of unique instances, this time excluding
 * the class attribute.
 * <p/>
 * The difference between the first two numbers tells one how many duplicates
 * are in the data. The difference between the last two numbers indicates
 * how many instances "at least" have the exact same data, but differ in the class
 * attribute. "At least", as an instance with varying class values can
 * also have duplicates.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class DatasetAmbiguity {

  /**
   * Expects a dataset as first parameter. Last attribute is assumed to be
   * the class attribute.
   *
   * @param args	the command-line attributes
   * @throws Exception	if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // load data
    Instances data = DataSource.read(args[0]);
    data.setClassIndex(data.numAttributes() - 1);

    // output total number of instances
    int total = data.numInstances();
    System.out.println("Total #instances: " + total);

    // output total number of unique instances (incl. class)
    InstanceComparator comp = new InstanceComparator(true);
    TreeSet<Instance> set = new TreeSet<Instance>(comp);
    for (int i = 0; i < data.numInstances(); i++)
      set.add(data.instance(i));
    int uniqueWithClass = set.size();
    System.out.println("Unique #instances (incl. class): " + uniqueWithClass);

    // output total number of unique instances (incl. class)
    comp = new InstanceComparator(false);
    set = new TreeSet<Instance>(comp);
    for (int i = 0; i < data.numInstances(); i++)
      set.add(data.instance(i));
    int uniqueWithoutClass = set.size();
    System.out.println("Unique #instances (excl. class): " + uniqueWithoutClass);

    // output summary
    System.out.println();
    System.out.println("# of duplicate instances (exact): " + (total - uniqueWithClass));
    System.out.println("# of instances with different class (at least): " + (uniqueWithClass - uniqueWithoutClass));
  }
}
