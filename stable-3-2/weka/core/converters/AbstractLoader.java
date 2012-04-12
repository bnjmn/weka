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
 *    Loader.java
 *    Copyright (C) 2000 Webmind Corp.
 *
 */

package weka.core.converters;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.Serializable;
import weka.core.Instances;
import weka.core.Instance;

/**
 * Abstract class for Loaders that contains default implementation of the
 * setSource methods: Any of these methods that are not overwritten will
 * result in throwing IOException.
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.3 $
 */
public abstract class AbstractLoader implements Loader {

  /** For state where no instances have been retrieved yet */
  protected static final int NONE = 0;
  /** For representing that instances have been retrieved in batch mode */
  protected static final int BATCH = 1;
  /** For representing that instances have been retrieved incrementally */
  protected static final int INCREMENTAL = 2;

  protected int m_Retrieval = NONE;
  protected void setRetrieval(int mode) { m_Retrieval = mode; }
  protected int getRetrieval() { return m_Retrieval; }

  /**
   * Resets the Loader object and sets the source of the data set to be 
   * the supplied File object.
   *
   * @param file the File.
   * @exception IOException always thrown.
   */
  public void setSource(File file) throws IOException {

    throw new IOException("operation not supported");
  }

  /**
   * Resets the Loader object and sets the source of the data set to be 
   * the supplied InputStream.
   *
   * @param input the source InputStream.
   * @exception IOException always thrown.
   */
  public void setSource(InputStream input) throws IOException {

    throw new IOException("operation not supported");
  }

  /**
   * Must be overridden by subclasses.
   */
  public abstract Instances getStructure() throws IOException;

  /**
   * Must be overridden by subclasses.
   */
  public abstract Instances getDataSet() throws IOException;

  /**
   * Must be overridden by subclasses.
   */
  public abstract Instance getNextInstance() throws IOException;
}





