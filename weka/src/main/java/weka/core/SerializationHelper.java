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
 * SerializationHelper.java
 * Copyright (C) 2007-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import java.io.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

/**
 * A helper class for determining serialVersionUIDs and checking whether classes
 * contain one and/or need one. One can also serialize and deserialize objects
 * to and from files or streams.
 * 
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SerializationHelper implements RevisionHandler {

  /** the field name of serialVersionUID. */
  public final static String SERIAL_VERSION_UID = "serialVersionUID";

  /**
   * checks whether a class is serializable.
   * 
   * @param classname the class to check
   * @return true if the class or one of its ancestors implements the
   *         Serializable interface, otherwise false (also if the class cannot
   *         be loaded)
   */
  public static boolean isSerializable(String classname) {
    boolean result;

    try {
      // result = isSerializable(Class.forName(classname));
      result = isSerializable(WekaPackageClassLoaderManager.forName(classname));
    } catch (Exception e) {
      result = false;
    }

    return result;
  }

  /**
   * checks whether a class is serializable.
   * 
   * @param c the class to check
   * @return true if the class or one of its ancestors implements the
   *         Serializable interface, otherwise false
   */
  public static boolean isSerializable(Class<?> c) {
    return InheritanceUtils.hasInterface(Serializable.class, c);
  }

  /**
   * checks whether the given class contains a serialVersionUID.
   * 
   * @param classname the class to check
   * @return true if the class contains a serialVersionUID, otherwise false
   *         (also if the class is not implementing serializable or cannot be
   *         loaded)
   */
  public static boolean hasUID(String classname) {
    boolean result;

    try {
      // result = hasUID(Class.forName(classname));
      result = hasUID(WekaPackageClassLoaderManager.forName(classname));
    } catch (Exception e) {
      result = false;
    }

    return result;
  }

  /**
   * checks whether the given class contains a serialVersionUID.
   * 
   * @param c the class to check
   * @return true if the class contains a serialVersionUID, otherwise false
   *         (also if the class is not implementing serializable)
   */
  public static boolean hasUID(Class<?> c) {
    boolean result;

    result = false;

    if (isSerializable(c)) {
      try {
        c.getDeclaredField(SERIAL_VERSION_UID);
        result = true;
      } catch (Exception e) {
        result = false;
      }
    }

    return result;
  }

  /**
   * checks whether a class needs to declare a serialVersionUID, i.e., it
   * implements the java.io.Serializable interface but doesn't declare a
   * serialVersionUID.
   * 
   * @param classname the class to check
   * @return true if the class needs to declare one, false otherwise (also if
   *         the class cannot be loaded!)
   */
  public static boolean needsUID(String classname) {
    boolean result;

    try {
      // result = needsUID(Class.forName(classname));
      result = needsUID(WekaPackageClassLoaderManager.forName(classname));
    } catch (Exception e) {
      result = false;
    }

    return result;
  }

  /**
   * checks whether a class needs to declare a serialVersionUID, i.e., it
   * implements the java.io.Serializable interface but doesn't declare a
   * serialVersionUID.
   * 
   * @param c the class to check
   * @return true if the class needs to declare one, false otherwise
   */
  public static boolean needsUID(Class<?> c) {
    boolean result;

    if (isSerializable(c)) {
      result = !hasUID(c);
    } else {
      result = false;
    }

    return result;
  }

  /**
   * reads or creates the serialVersionUID for the given class.
   * 
   * @param classname the class to get the serialVersionUID for
   * @return the UID, 0L for non-serializable classes (or if the class cannot be
   *         loaded)
   */
  public static long getUID(String classname) {
    long result;

    try {
      // result = getUID(Class.forName(classname));
      result = getUID(WekaPackageClassLoaderManager.forName(classname));
    } catch (Exception e) {
      result = 0L;
    }

    return result;
  }

  /**
   * reads or creates the serialVersionUID for the given class.
   * 
   * @param c the class to get the serialVersionUID for
   * @return the UID, 0L for non-serializable classes
   */
  public static long getUID(Class<?> c) {
    return ObjectStreamClass.lookup(c).getSerialVersionUID();
  }

  /**
   * serializes the given object to the specified file.
   * 
   * @param filename the file to write the object to
   * @param o the object to serialize
   * @throws Exception if serialization fails
   */
  public static void write(String filename, Object o) throws Exception {
    write(new FileOutputStream(filename), o);
  }

  /**
   * serializes the given object to the specified stream.
   * 
   * @param stream the stream to write the object to
   * @param o the object to serialize
   * @throws Exception if serialization fails
   */
  public static void write(OutputStream stream, Object o) throws Exception {
    ObjectOutputStream oos;

    if (!(stream instanceof BufferedOutputStream)) {
      stream = new BufferedOutputStream(stream);
    }

    oos = new ObjectOutputStream(stream);
    oos.writeObject(o);
    oos.flush();
    oos.close();
  }

  /**
   * serializes the given objects to the specified file.
   * 
   * @param filename the file to write the object to
   * @param o the objects to serialize
   * @throws Exception if serialization fails
   */
  public static void writeAll(String filename, Object[] o) throws Exception {
    writeAll(new FileOutputStream(filename), o);
  }

  /**
   * serializes the given objects to the specified stream.
   * 
   * @param stream the stream to write the object to
   * @param o the objects to serialize
   * @throws Exception if serialization fails
   */
  public static void writeAll(OutputStream stream, Object[] o) throws Exception {
    ObjectOutputStream oos;
    int i;

    if (!(stream instanceof BufferedOutputStream)) {
      stream = new BufferedOutputStream(stream);
    }

    oos = new ObjectOutputStream(stream);
    for (i = 0; i < o.length; i++) {
      oos.writeObject(o[i]);
    }
    oos.flush();
    oos.close();
  }

  /**
   * deserializes the given file and returns the object from it.
   * 
   * @param filename the file to deserialize from
   * @return the deserialized object
   * @throws Exception if deserialization fails
   */
  public static Object read(String filename) throws Exception {
    return read(new FileInputStream(filename));
  }

  /**
   * deserializes from the given stream and returns the object from it.
   * 
   * @param stream the stream to deserialize from
   * @return the deserialized object
   * @throws Exception if deserialization fails
   */
  public static Object read(InputStream stream) throws Exception {
    ObjectInputStream ois;
    Object result;

    ois = getObjectInputStream(stream);
    result = ois.readObject();
    ois.close();

    return result;
  }

  /**
   * Checks to see if the supplied package class loader (or any of its dependent
   * package class loaders) has the given third party class.
   *
   * @param className the name of the third-party class to check for
   * @param l the third party class loader
   * @return the class loader that owns the named third-party class, or null if
   *         not found.
   */
  public static ClassLoader checkForThirdPartyClass(String className,
    WekaPackageLibIsolatingClassLoader l) {
    ClassLoader result = null;

    if (l.hasThirdPartyClass(className)) {
      return l;
    }

    for (WekaPackageLibIsolatingClassLoader dep : l
      .getPackageClassLoadersForDependencies()) {
      result = checkForThirdPartyClass(className, dep);
      if (result != null) {
        break;
      }
    }

    return result;
  }

  /**
   * Get a (Weka package classloader aware) {@code ObjectInputStream} instance
   * for reading objects from the supplied input stream
   *
   * @param stream the stream to wrap
   * @return an {@code ObjectInputStream} instance that is aware of of Weka
   *         package classloaders
   * @throws IOException if a problem occurs
   */
  public static ObjectInputStream getObjectInputStream(InputStream stream)
    throws IOException {
    if (!(stream instanceof BufferedInputStream)) {
      stream = new BufferedInputStream(stream);
    }

    return new ObjectInputStream(stream) {
      protected Set<WekaPackageLibIsolatingClassLoader> m_thirdPartyLoaders =
        new LinkedHashSet<>();

      @Override
      protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException {

        // make sure that the type descriptor for arrays gets removed from
        // what we're going to look up!
        String arrayStripped =
          desc.getName().replace("[L", "").replace("[", "").replace(";", "");
        ClassLoader cl =
          WekaPackageClassLoaderManager.getWekaPackageClassLoaderManager()
            .getLoaderForClass(arrayStripped);

        if (cl instanceof WekaPackageLibIsolatingClassLoader) {
          // might be third-party classes involved, store the classloader
          m_thirdPartyLoaders
            .add((WekaPackageLibIsolatingClassLoader) cl);
        }

        Class<?> result = null;
        try {
          result = Class.forName(desc.getName(), true, cl);
        } catch (ClassNotFoundException ex) {
          for (WekaPackageLibIsolatingClassLoader l : m_thirdPartyLoaders) {
            ClassLoader checked =
              checkForThirdPartyClass(arrayStripped, l);
            if (checked != null) {
              result = Class.forName(desc.getName(), true, checked);
            }
          }
        }

        if (result == null) {
          throw new ClassNotFoundException("Unable to find class "
            + arrayStripped);
        }

        return result;
      }
    };
  }

  /**
   * deserializes the given file and returns the objects from it.
   * 
   * @param filename the file to deserialize from
   * @return the deserialized objects
   * @throws Exception if deserialization fails
   */
  public static Object[] readAll(String filename) throws Exception {
    return readAll(new FileInputStream(filename));
  }

  /**
   * deserializes from the given stream and returns the object from it.
   * 
   * @param stream the stream to deserialize from
   * @return the deserialized object
   * @throws Exception if deserialization fails
   */
  public static Object[] readAll(InputStream stream) throws Exception {
    ObjectInputStream ois;
    Vector<Object> result;

    ois = getObjectInputStream(stream);

    result = new Vector<Object>();
    try {
      while (true) {
        result.add(ois.readObject());
      }
    } catch (IOException e) {
      // ignored
    }
    ois.close();

    return result.toArray(new Object[result.size()]);
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Outputs information about a class on the commandline, takes class name as
   * arguments.
   * 
   * @param args the classnames to check
   * @throws Exception if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("\nUsage: " + SerializationHelper.class.getName()
        + " classname [classname [classname [...]]]\n");
      System.exit(1);
    }

    // check all the classes
    System.out.println();
    for (String arg : args) {
      System.out.println(arg);
      System.out.println("- is serializable: " + isSerializable(arg));
      System.out.println("- has " + SERIAL_VERSION_UID + ": " + hasUID(arg));
      System.out
        .println("- needs " + SERIAL_VERSION_UID + ": " + needsUID(arg));
      System.out.println("- " + SERIAL_VERSION_UID
        + ": private static final long serialVersionUID = " + getUID(arg)
        + "L;");
      System.out.println();
    }
  }
}
