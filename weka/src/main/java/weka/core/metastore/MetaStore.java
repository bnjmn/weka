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
 *    KFMetaStore
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.metastore;

import java.io.IOException;
import java.util.Set;

/**
 * Interface for metastore implementations. The metastore is a simple storage
 * place that can be used, as an example, for storing named configuration
 * settings (e.g. general application settings, reusable database connections
 * etc.).
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public interface MetaStore {

  /**
   * Get a list of all named meta stores
   * 
   * @return a list of meta stores
   * @throws IOException if a problem occurs
   */
  Set<String> listMetaStores() throws IOException;

  /**
   * Get a list of all entries in a named store
   * 
   * @param storeName the name of the store to get entries for
   * @return a list of all entries in the named store
   * @throws IOException if a problem occurs
   */
  Set<String> listMetaStoreEntries(String storeName) throws IOException;

  /**
   * Get a list of all named entries starting with the given prefix
   * 
   * @param storeName the name of the store to get entries for
   * @param prefix the prefix with which to search for entries
   * @return a list of entries
   * @throws IOException if a problem occurs
   */
  Set<String> listMetaStoreEntries(String storeName, String prefix)
    throws IOException;

  /**
   * Create a named store
   * 
   * @param storeName the name of the store to create
   * @throws IOException if a problem occurs
   */
  void createStore(String storeName) throws IOException;

  /**
   * Get a named entry from the store
   * 
   * @param storeName the name of the store to use
   * @param name the full name of the entry to retrieve
   * @param clazz the expected class of the entry when deserialized
   * @return the deserialized entry or null if the name does not exist in the
   *         store
   * @throws IOException if the deserialized entry does not match the expected
   *           class
   */
  Object getEntry(String storeName, String name, Class<?> clazz)
    throws IOException;

  /**
   * Store a named entry
   * 
   * @param storeName the name of the store to use
   * @param name the full name of the entry to store
   * @param toStore a beans compliant object to store
   * @throws IOException if a problem occurs
   */
  void storeEntry(String storeName, String name, Object toStore)
    throws IOException;
}
