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
 *    LoadDataFromDbLoaderIncremental.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.core.converters;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.DatabaseLoader;

/**
 * Loads data from a JDBC database using the weka.core.converters.DatabaseLoader
 * class. The data is loaded incrementally (if that is possible).
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class LoadDataFromDbLoaderIncremental {

  /**
   * Expects no parameters.
   *
   * @param args        the command-line parameters
   * @throws Exception  if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // output usage
    if (args.length != 0) {
      System.err.println("\nUsage: java LoadDataFromDbLoaderIncremental\n");
      System.exit(1);
    }

    System.out.println("\nReading data...");
    DatabaseLoader loader = new DatabaseLoader();
    loader.setSource("jdbc_url", "the_user", "the_password");
    loader.setQuery("select * from whatsoever");
    // it might be necessary to define the columns that uniquely identify
    // a single row. Just provide them as comma-separated list:
    // loader.setKeys("col1,col2,...");
    Instances structure = loader.getStructure();
    Instances data = new Instances(structure);
    Instance inst;
    int count = 0;
    while ((inst = loader.getNextInstance(structure)) != null) {
      data.add(inst);
      count++;
      if ((count % 100) == 0)
        System.out.println(count + " rows read so far.");
    }

    System.out.println("\nHeader of dataset:\n");
    System.out.println(new Instances(data, 0));
  }
}
