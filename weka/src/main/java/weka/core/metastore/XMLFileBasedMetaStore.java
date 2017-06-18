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
 *    XMLFileBasedKFMetaStore
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.metastore;

import weka.core.ResourceUtils;
import weka.core.xml.XMLBasicSerialization;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A simple default implementation of MetaStore that uses Weka's XML
 * serialization mechanism to persist entries as XML files in
 * ${WEKA_HOME}/wekaMetaStore
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class XMLFileBasedMetaStore implements MetaStore {

  /** The default location for the XML files */
  public static final String DEFAULT_STORE_LOCATION =
    ResourceUtils.getWekaHome().toString() + File.separator + "wekaMetaStore";

  /** The current home of the store */
  protected File m_storeHome = new File(DEFAULT_STORE_LOCATION);

  /** True if the store home has been successfully established */
  protected boolean m_storeDirOK;

  /**
   * Lookup for entries in the stores - just holds entry names and File paths
   */
  protected Map<String, Map<String, File>> m_stores =
    new LinkedHashMap<String, Map<String, File>>();

  /**
   * Establish the home directory for the store
   * 
   * @throws IOException if a problem occurs
   */
  protected synchronized void establishStoreHome() throws IOException {
    if (m_storeDirOK) {
      return;
    }

    if (!m_storeHome.exists()) {
      if (!m_storeHome.mkdir()) {
        throw new IOException("Unable to create the metastore directory: "
          + m_storeHome.toString());
      }
    }

    if (!m_storeHome.isDirectory()) {
      throw new IOException("The metastore (" + m_storeHome
        + ") seems to exist, but it isn't a directory!");
    }

    m_storeDirOK = true;

    lockStore();
    // now scan the contents
    File[] contents = m_storeHome.listFiles();
    for (File f : contents) {
      if (f.isDirectory()) {
        Map<String, File> store = new LinkedHashMap<String, File>();
        m_stores.put(f.getName(), store);

        File[] storeEntries = f.listFiles();
        for (File se : storeEntries) {
          store.put(se.getName(), se);
        }
      }
    }
    unlockStore();
  }

  @Override
  public Set<String> listMetaStores() throws IOException {
    return m_stores.keySet();
  }

  @Override
  public Set<String> listMetaStoreEntries(String storeName) throws IOException {
    establishStoreHome();

    Set<String> results = new HashSet<String>();
    Map<String, File> store = m_stores.get(storeName);
    if (store != null) {
      results.addAll(store.keySet());
    }

    return results;
  }

  @Override
  public synchronized Set<String> listMetaStoreEntries(String storeName,
    String prefix) throws IOException {
    establishStoreHome();
    Set<String> matches = new HashSet<String>();
    Map<String, File> store = m_stores.get(storeName);

    if (store != null) {
      for (Map.Entry<String, File> e : store.entrySet()) {
        if (e.getKey().startsWith(prefix)) {
          matches.add(e.getKey());
        }
      }
    }

    return matches;
  }

  @Override
  public Object getEntry(String storeName, String name, Class<?> clazz)
    throws IOException {
    establishStoreHome();

    Map<String, File> store = m_stores.get(storeName);

    if (store != null) {
      if (store.containsKey(name)) {
        File toLoad = store.get(name);

        try {
          lockStore();
          XMLBasicSerialization deserializer = getSerializer();
          Object loaded = deserializer.read(toLoad);

          if (loaded.getClass().equals(clazz)) {
            throw new IOException(
              "Deserialized entry (" + loaded.getClass().getName()
                + ") was not " + "the expected class: " + clazz.getName());
          }

          return loaded;
        } catch (Exception ex) {
          throw new IOException(ex);
        } finally {
          unlockStore();
        }
      }
    }

    return null;
  }

  @Override
  public void createStore(String storeName) throws IOException {
    File store = new File(m_storeHome, storeName);
    if (store.exists()) {
      throw new IOException("Meta store '" + storeName + "' already exists!");
    }

    lockStore();
    try {
      if (!store.mkdir()) {
        throw new IOException(
          "Unable to create meta store '" + storeName + "'");
      }
    } finally {
      unlockStore();
    }
  }

  @Override
  public synchronized void storeEntry(String storeName, String name,
    Object toStore) throws IOException {
    establishStoreHome();
    Map<String, File> store = m_stores.get(storeName);
    if (store == null) {
      createStore(storeName);
      store = new LinkedHashMap<String, File>();
      m_stores.put(storeName, store);
    }

    File loc =
      new File(m_storeHome.toString() + File.separator + storeName, name);
    store.put(name, loc);
    try {
      lockStore();
      XMLBasicSerialization serializer = getSerializer();
      serializer.write(loc, toStore);
    } catch (Exception ex) {
      throw new IOException(ex);
    } finally {
      unlockStore();
    }
  }

  /**
   * Lock the store
   * 
   * @throws IOException if a problem occurs
   */
  protected void lockStore() throws IOException {
    int totalWaitTime = 0;
    while (true) {
      File lock = new File(m_storeHome, ".lock");

      if (lock.createNewFile()) {
        return;
      }
      try {
        Thread.sleep(200);
      } catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
      totalWaitTime += 200;
      if (totalWaitTime > 5000) {
        throw new IOException("Unable to lock store within 5 seconds");
      }
    }
  }

  /**
   * Unlocks the metastore
   */
  protected void unlockStore() {
    File lock = new File(m_storeHome, ".lock");
    lock.delete();
  }

  /**
   * Gets a serializer to use. Creates a subclass of
   * {@code XMLBasicSerialization} that has allowed properties set for common
   * option handlers.
   * 
   * @return a serializer instance
   */
  protected XMLBasicSerialization getSerializer() {
    try {
      XMLBasicSerialization ser = new XMLBasicSerialization() {
        public void clear() throws Exception {
          super.clear();

          m_Properties.addAllowed(weka.classifiers.Classifier.class, "debug");
          m_Properties.addAllowed(weka.classifiers.Classifier.class, "options");
          m_Properties.addAllowed(weka.associations.Associator.class,
            "options");
          m_Properties.addAllowed(weka.clusterers.Clusterer.class, "options");
          m_Properties.addAllowed(weka.filters.Filter.class, "options");
          m_Properties.addAllowed(weka.core.converters.Saver.class, "options");
          m_Properties.addAllowed(weka.core.converters.Loader.class, "options");
          m_Properties.addAllowed(weka.attributeSelection.ASSearch.class,
            "options");
          m_Properties.addAllowed(weka.attributeSelection.ASEvaluation.class,
            "options");

          m_Properties.addAllowed(weka.core.converters.DatabaseSaver.class,
            "options");
          m_Properties.addAllowed(weka.core.converters.DatabaseLoader.class,
            "options");
          m_Properties.addAllowed(
            weka.core.converters.TextDirectoryLoader.class, "options");

          // we assume that classes implementing SplitEvaluator also implement
          // OptionHandler
          m_Properties.addAllowed(weka.experiment.SplitEvaluator.class,
            "options");
          // we assume that classes implementing ResultProducer also implement
          // OptionHandler
          m_Properties.addAllowed(weka.experiment.ResultProducer.class,
            "options");

        }
      };
      ser.setSuppressWarnings(true);
      return ser;
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return null;
  }
}
