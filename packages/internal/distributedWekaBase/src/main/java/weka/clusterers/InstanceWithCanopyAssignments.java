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
 *    InstanceWithCanopyAssignments
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.clusterers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64;

import weka.core.Instance;

/**
 * Class that encapsulates an instance with its canopy assignments
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class InstanceWithCanopyAssignments implements Serializable {

  /** For serialization */
  private static final long serialVersionUID = 4846204264955741589L;

  public static String SEPARATOR = "@@::@@";
  public static String LONG_SEPARATOR = "@";

  /** The instance */
  protected Instance m_instance;

  /** The canopies that it is assigned to */
  protected long[] m_canopies;

  public InstanceWithCanopyAssignments(Instance inst, long[] canopies) {
    m_instance = inst;
    m_canopies = canopies;
  }

  public Instance getInstance() {
    return m_instance;
  }

  public long[] getCanopyAssignments() {
    return m_canopies;
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();

    b.append(m_instance.toString());
    b.append(SEPARATOR);
    for (int i = 0; i < m_canopies.length; i++) {

      b.append(m_canopies[i]);
      if (i != m_canopies.length - 1) {
        b.append(LONG_SEPARATOR);
      }
    }

    return b.toString();
  }

  public String toStringBase64() {
    StringBuilder b = new StringBuilder();

    b.append(m_instance.toString());

    try {
      b.append(SEPARATOR).append(encodeToBase64(m_canopies));
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return b.toString();
  }

  protected String encodeToBase64(long[] canopies) throws Exception {

    // object to bytes
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    BufferedOutputStream bos = new BufferedOutputStream(bao);
    ObjectOutputStream oo = null;
    try {
      oo = new ObjectOutputStream(bos);
      oo.writeObject(canopies);
      oo.flush();
    } finally {
      if (oo != null) {
        oo.close();
      }
    }

    byte[] bytes = bao.toByteArray();

    // bytes to base 64 string
    String string = null;

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    GZIPOutputStream gzos = new GZIPOutputStream(baos);
    bos = new BufferedOutputStream(gzos);
    try {
      bos.write(bytes);
      bos.flush();
    } finally {
      if (bos != null) {
        bos.close();
      }
    }

    string = new String(Base64.encodeBase64(baos.toByteArray()));

    return string;
  }

  public static String[] splitInstanceAndEncodedCanopies(
    String instanceAndCanopies) {
    return instanceAndCanopies.split(SEPARATOR);
  }

  public static long[] decodeCanopiesNormal(String canopies) {
    String[] parts = canopies.split(LONG_SEPARATOR);

    long[] canopiesL = new long[parts.length];

    for (int i = 0; i < parts.length; i++) {
      canopiesL[i] = Long.parseLong(parts[i].trim());
    }

    return canopiesL;
  }

  public static long[] decodeCanopiesFromBase64(String base64) throws Exception {
    byte[] bytes;
    long[] canopies = null;

    if (base64 == null) {
      bytes = new byte[] {};
    } else {
      bytes = Base64.decodeBase64(base64.getBytes());
    }

    if (bytes.length > 0) {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      GZIPInputStream gzip = new GZIPInputStream(bais);
      BufferedInputStream bi = new BufferedInputStream(gzip);

      try {
        byte[] result = new byte[] {};

        byte[] extra = new byte[1000000];
        int nrExtra = bi.read(extra);
        while (nrExtra >= 0) {
          // add it to bytes...
          //
          int newSize = result.length + nrExtra;
          byte[] tmp = new byte[newSize];
          for (int i = 0; i < result.length; i++) {
            tmp[i] = result[i];
          }
          for (int i = 0; i < nrExtra; i++) {
            tmp[result.length + i] = extra[i];
          }

          // change the result
          result = tmp;
          nrExtra = bi.read(extra);
        }
        bytes = result;
      } finally {
        gzip.close();
      }

      bais = new ByteArrayInputStream(bytes);
      ObjectInputStream ois = new ObjectInputStream(bais);
      try {
        canopies = (long[]) ois.readObject();
      } finally {
        ois.close();
      }
    }

    if (canopies == null || canopies.length == 0) {
      throw new Exception("No canopy assignments decoded!");
    }

    return canopies;
  }
}
