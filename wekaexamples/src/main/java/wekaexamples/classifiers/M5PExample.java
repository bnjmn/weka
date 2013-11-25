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
 *    M5PExample.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.classifiers;

import java.util.Vector;

import weka.classifiers.Classifier;
import weka.classifiers.trees.M5P;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.experiment.InstanceQuery;

/**
 * 
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class M5PExample {
  public final static String FILENAME = "/some/where/m5pexample.save";

  public final static String URL = "jdbc_url";

  public final static String USER = "the_user";

  public final static String PASSWORD = "the_password";

  public void train() throws Exception {
    System.out.println("Training...");

    // load training data from database
    InstanceQuery query = new InstanceQuery();
    query.setDatabaseURL(URL);
    query.setUsername(USER);
    query.setPassword(PASSWORD);
    query.setQuery("select * from some_table");
    Instances data = query.retrieveInstances();
    data.setClassIndex(13);

    // train M5P
    M5P cl = new M5P();
    // further options...
    cl.buildClassifier(data);

    // save model + header
    Vector<Object> v = new Vector<Object>();
    v.add(cl);
    v.add(new Instances(data, 0));
    SerializationHelper.write(FILENAME, v);

    System.out.println("Training finished!");
  }

  public void predict() throws Exception {
    System.out.println("Predicting...");

    // load data from database that needs predicting
    InstanceQuery query = new InstanceQuery();
    query.setDatabaseURL(URL);
    query.setUsername(USER);
    query.setPassword(PASSWORD);
    query.setQuery("select * from some_table"); // retrieves the same table only
                                                // for simplicty reasons.
    Instances data = query.retrieveInstances();
    data.setClassIndex(14);

    // read model and header
    @SuppressWarnings("unchecked")
    Vector<Object> v = (Vector<Object>) SerializationHelper.read(FILENAME);
    Classifier cl = (Classifier) v.get(0);
    Instances header = (Instances) v.get(1);

    // output predictions
    System.out.println("actual -> predicted");
    for (int i = 0; i < data.numInstances(); i++) {
      Instance curr = data.instance(i);
      // create an instance for the classifier that fits the training data
      // Instances object returned here might differ slightly from the one
      // used during training the classifier, e.g., different order of
      // nominal values, different number of attributes.
      Instance inst = new DenseInstance(header.numAttributes());
      inst.setDataset(header);
      for (int n = 0; n < header.numAttributes(); n++) {
        Attribute att = data.attribute(header.attribute(n).name());
        // original attribute is also present in the current dataset
        if (att != null) {
          if (att.isNominal()) {
            // is this label also in the original data?
            // Note:
            // "numValues() > 0" is only used to avoid problems with nominal
            // attributes that have 0 labels, which can easily happen with
            // data loaded from a database
            if ((header.attribute(n).numValues() > 0) && (att.numValues() > 0)) {
              String label = curr.stringValue(att);
              int index = header.attribute(n).indexOfValue(label);
              if (index != -1) {
                inst.setValue(n, index);
              }
            }
          } else if (att.isNumeric()) {
            inst.setValue(n, curr.value(att));
          } else {
            throw new IllegalStateException("Unhandled attribute type!");
          }
        }
      }

      // predict class
      double pred = cl.classifyInstance(inst);
      System.out.println(inst.classValue() + " -> " + pred);
    }

    System.out.println("Predicting finished!");
  }

  public static void main(String[] args) throws Exception {
    M5PExample m = new M5PExample();
    m.train();
    m.predict();
  }
}
