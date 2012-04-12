/*
 *    InstanceQuery.java
 *    Copyright (C) 1999 Len Trigg
 *
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

package weka.experiment;

import java.sql.*;
import java.net.InetAddress;
import java.util.*;
import java.math.*;
import java.io.*;

import weka.core.*;

/**
 * Convert the results of a database query into instances. The jdbc
 * driver and database to be used default to "jdbc.idbDriver" and
 * "jdbc:idb=experiments.prp". These may be changed by creating
 * a java properties file called .weka.experimentrc in user.home. eg:<p>
 *
 * <code><pre>
 * jdbcDriver=jdbc.idbDriver
 * jdbcURL=jdbc:idb=experiments.prp
 * </pre></code><p>
 *
 * Command line use just outputs the instances to System.out.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public class InstanceQuery extends DatabaseUtils {

  /**
   * Sets up the database drivers
   *
   * @exception Exception if an error occurs
   */
  public InstanceQuery() throws Exception {

    super();
  }

  /**
   * Makes a database query to convert a table into a set of instances
   *
   * @param query the query to convert to instances
   * @return the instances contained in the result of the query
   * @exception Exception if an error occurs
   */
  public Instances getInstances(String query) throws Exception {

    System.err.println("Executing query: " + query);
    connectToDatabase();
    if (execute(query) == false) {
      throw new Exception("Query didn't produce results");
    }
    ResultSet rs = getResultSet();
    System.err.println("Getting metadata...");
    ResultSetMetaData md = rs.getMetaData();

    // Determine structure of the instances
    int numAttributes = md.getColumnCount();
    int [] attributeTypes = new int [numAttributes];
    Hashtable [] nominalIndexes = new Hashtable [numAttributes];
    FastVector [] nominalStrings = new FastVector [numAttributes];
    for (int i = 1; i <= numAttributes; i++) {
      switch (md.getColumnType(i)) {
      case Types.CHAR:
      case Types.VARCHAR:
      case Types.LONGVARCHAR:
	//System.err.println("String --> nominal");
	attributeTypes[i - 1] = Attribute.NOMINAL;
	nominalIndexes[i - 1] = new Hashtable();
	nominalStrings[i - 1] = new FastVector();
	break;
      case Types.BIT:
	////System.err.println("boolean --> nominal");
	attributeTypes[i - 1] = Attribute.NOMINAL;
	nominalIndexes[i - 1] = new Hashtable();
	nominalIndexes[i - 1].put("false", new Double(0));
	nominalIndexes[i - 1].put("true", new Double(1));
	nominalStrings[i - 1] = new FastVector();
	nominalStrings[i - 1].addElement("false");
	nominalStrings[i - 1].addElement("true");
	break;
      case Types.NUMERIC:
      case Types.DECIMAL:
	//System.err.println("BigDecimal --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case Types.TINYINT:
	//System.err.println("byte --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case Types.SMALLINT:
	//System.err.println("short --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case Types.INTEGER:
	//System.err.println("int --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case Types.BIGINT:
	//System.err.println("long --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case Types.REAL:
	//System.err.println("float --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case Types.FLOAT:
      case Types.DOUBLE:
	//System.err.println("double --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case Types.BINARY:
      case Types.VARBINARY:
      case Types.LONGVARBINARY:
	//System.err.println("byte[] --> unsupported");
	attributeTypes[i - 1] = Attribute.STRING;
	break;
      case Types.DATE:
	//System.err.println("Date --> unsupported");
	attributeTypes[i - 1] = Attribute.STRING;
	break;
      case Types.TIME:
	//System.err.println("Time --> unsupported");
	attributeTypes[i - 1] = Attribute.STRING;
	break;
      case Types.TIMESTAMP:
	//System.err.println("Time --> unsupported");
	attributeTypes[i - 1] = Attribute.STRING;
	break;
      default:
	//System.err.println("Unknown column type");
	attributeTypes[i - 1] = Attribute.STRING;
      }
    }

    // Step through the tuples
    System.err.println("Creating instances...");
    FastVector instances = new FastVector();
    int rowCount = 0;
    while(rs.next()) {
      if (rowCount % 100 == 0) {
	System.err.print("read " + rowCount + " instances \r");
	System.err.flush();
      }
      Instance newInst = new Instance(numAttributes);
      for(int i = 1; i <= numAttributes; i++) {
	switch (md.getColumnType(i)) {
	case Types.CHAR:
	case Types.VARCHAR:
	case Types.LONGVARCHAR:
	  String str = rs.getString(i);
	  
	  if (rs.wasNull()) {
	    newInst.setValue(i - 1, Instance.missingValue());
	  } else {
	    Double index = (Double)nominalIndexes[i - 1].get(str);
	    if (index == null) {
	      index = new Double(nominalStrings[i - 1].size());
	      nominalIndexes[i - 1].put(str, index);
	      nominalStrings[i - 1].addElement(str);
	    }
	    newInst.setValue(i - 1, index.doubleValue());
	  }
	  break;
	case Types.BIT:
	  boolean boo = rs.getBoolean(i);
	  if (rs.wasNull()) {
	    newInst.setValue(i - 1, Instance.missingValue());
	  } else {
	    newInst.setValue(i - 1, (boo ? 1.0 : 0.0));
	  }
	  break;
	case Types.NUMERIC:
	case Types.DECIMAL:
	  //	  BigDecimal bd = rs.getBigDecimal(i, 4); 
	  double dd = rs.getDouble(i);
	  // Use the column precision instead of 4?
	  if (rs.wasNull()) {
	    newInst.setValue(i - 1, Instance.missingValue());
	  } else {
	    //	    newInst.setValue(i - 1, bd.doubleValue());
	    newInst.setValue(i - 1, dd);
	  }
	  break;
	case Types.TINYINT:
	  byte by = rs.getByte(i);
	  if (rs.wasNull()) {
	    newInst.setValue(i - 1, Instance.missingValue());
	  } else {
	    newInst.setValue(i - 1, (double)by);
	  }
	  break;
	case Types.SMALLINT:
	  short sh = rs.getByte(i);
	  if (rs.wasNull()) {
	    newInst.setValue(i - 1, Instance.missingValue());
	  } else {
	    newInst.setValue(i - 1, (double)sh);
	  }
	  break;
	case Types.INTEGER:
	  int in = rs.getInt(i);
	  if (rs.wasNull()) {
	    newInst.setValue(i - 1, Instance.missingValue());
	  } else {
	    newInst.setValue(i - 1, (double)in);
	  }
	  break;
	case Types.BIGINT:
	  long lo = rs.getLong(i);
	  if (rs.wasNull()) {
	    newInst.setValue(i - 1, Instance.missingValue());
	  } else {
	    newInst.setValue(i - 1, (double)lo);
	  }
	  break;
	case Types.REAL:
	  float fl = rs.getFloat(i);
	  if (rs.wasNull()) {
	    newInst.setValue(i - 1, Instance.missingValue());
	  } else {
	    newInst.setValue(i - 1, (double)fl);
	  }
	  break;
	case Types.FLOAT:
	case Types.DOUBLE:
	  double dou = rs.getDouble(i);
	  if (rs.wasNull()) {
	    newInst.setValue(i - 1, Instance.missingValue());
	  } else {
	    newInst.setValue(i - 1, (double)dou);
	  }
	  break;
	case Types.BINARY:
	case Types.VARBINARY:
	case Types.LONGVARBINARY:
	case Types.DATE:
	case Types.TIME:
	case Types.TIMESTAMP:
	default:
	  newInst.setValue(i - 1, Instance.missingValue());
	}
      }
      instances.addElement(newInst);
    }
    rs.close();
    //disconnectFromDatabase();  (perhaps other queries might be made)
    
    // Create the header and add the instances to the dataset
    System.err.println("Creating header...");
    FastVector attribInfo = new FastVector();
    for (int i = 0; i < numAttributes; i++) {
      String attribName = md.getColumnName(i + 1);
      switch (attributeTypes[i]) {
      case Attribute.NOMINAL:
	attribInfo.addElement(new Attribute(attribName, nominalStrings[i]));
	break;
      case Attribute.NUMERIC:
	attribInfo.addElement(new Attribute(attribName));
	break;
      case Attribute.STRING:
	attribInfo.addElement(new Attribute(attribName, null));
	break;
      default:
	throw new Exception("Unknown attribute type");
      }
    }
    Instances result = new Instances("QueryResult", attribInfo, 
				     instances.size());
    for (int i = 0; i < instances.size(); i++) {
      result.add((Instance)instances.elementAt(i));
    }
    return result;
  }

  /**
   * Test the class from the command line. The instance
   * query should be specified with -Q sql_query
   *
   * @param args contains options for the instance query
   */
  public static void main(String args[]) {

    try {
      String query = Utils.getOption('Q', args);
      if (query.length() == 0) {
	query = "select * from Experiment_index";
      }
      InstanceQuery iq = new InstanceQuery();
      Utils.checkForRemainingOptions(args);
      Instances aha = iq.getInstances(query);
      iq.disconnectFromDatabase();
      // The dataset may be large, so to make things easier we'll
      // output an instance at a time (rather than having to convert
      // the entire dataset to one large string)
      System.out.println(new Instances(aha, 0));
      for (int i = 0; i < aha.numInstances(); i++) {
	System.out.println(aha.instance(i));
      }
    } catch(Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
