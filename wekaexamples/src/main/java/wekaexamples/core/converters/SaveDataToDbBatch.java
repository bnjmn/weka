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
 *    SaveDataToDbBatch.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.core.converters;

import weka.core.Instances;
import weka.core.converters.DatabaseLoader;
import weka.core.converters.DatabaseSaver;

/**
 * Loads data from a JDBC database using the
 * weka.core.converters.DatabaseLoader class and saves it to another JDBC
 * database using the weka.core.converters.DatabaseSaver class. The data is
 * loaded/saved in batch mode.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SaveDataToDbBatch {

  /**
   * Expects no parameters.
   *
   * @param args        the command-line parameters
   * @throws Exception  if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // output usage
    if (args.length != 0) {
      System.err.println("\nUsage: java SaveDataToDbBatch\n");
      System.exit(1);
    }

    System.out.println("\nReading data...");
    DatabaseLoader loader = new DatabaseLoader();
    loader.setSource("jdbc_url", "the_user", "the_password");
    loader.setQuery("select * from whatsoever");
    Instances data = loader.getDataSet();

    System.out.println("\nSaving data...");
    DatabaseSaver saver = new DatabaseSaver();
    saver.setDestination("jdbc_url", "the_user", "the_password");
    // we explicitly specify the table name here:
    saver.setTableName("whatsoever2");
    saver.setRelationForTableName(false);
    // or we could just update the name of the dataset:
    // saver.setRelationForTableName(true);
    // data.setRelationName("whatsoever2");
    saver.setInstances(data);
    saver.writeBatch();
  }
}
