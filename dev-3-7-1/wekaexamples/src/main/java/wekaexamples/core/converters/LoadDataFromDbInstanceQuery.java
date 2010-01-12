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
 *    LoadDataFromDbInstanceQuery.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.core.converters;

import weka.core.Instances;
import weka.experiment.InstanceQuery;

/**
 * Loads data from a JDBC database using the weka.experiment.InstanceQuery
 * class.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class LoadDataFromDbInstanceQuery {

  /**
   * Expects no parameters.
   *
   * @param args        the command-line parameters
   * @throws Exception  if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // output usage
    if (args.length != 0) {
      System.err.println("\nUsage: java LoadDataFromDbInstanceQuery\n");
      System.exit(1);
    }

    System.out.println("\nReading data...");
    InstanceQuery query = new InstanceQuery();
    query.setDatabaseURL("jdbc_url");
    query.setUsername("the_user");
    query.setPassword("the_password");
    query.setQuery("select * from whatsoever");
    Instances data = query.retrieveInstances();

    System.out.println("\nHeader of dataset:\n");
    System.out.println(new Instances(data, 0));
  }
}
