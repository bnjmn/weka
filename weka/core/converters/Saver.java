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
 *    Saver.java
 *    Copyright (C) 2004 Mark Hall
 *
 */

package weka.core.converters;

import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.io.Serializable;
import weka.core.Instances;
import weka.core.Instance;

/** 
 * Interface to something that can save Instances to an output destination in some
 * format.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public interface Saver extends Serializable {


  /*@ public model instance boolean model_structureDetermined
    @   initially: model_structureDetermined == false;
    @*/

  /*@ public model instance boolean model_sourceSupplied
    @   initially: model_sourceSupplied == false;
    @*/

  /**
   * Resets the Saver object and sets the destination to be 
   * the supplied File object.
   *
   * @param file the File
   * @exception IOException if an error occurs
   * support loading from a File.
   *
   * <pre><jml>
   *    public_normal_behavior
   *      requires: file != null
   *                && (* file exists *);
   *      modifiable: model_sourceSupplied, model_structureDetermined;
   *      ensures: model_sourceSupplied == true 
   *               && model_structureDetermined == false;
   *  also
   *    public_exceptional_behavior
   *      requires: file == null
   *                || (* file does not exist *);
   *    signals: (IOException);
   * </jml></pre>
   */
  void setDestination(File file) throws IOException;

  /**
   * Resets the Saver object and sets the destination to be 
   * the supplied InputStream.
   *
   * @param input the source InputStream
   * @exception IOException if this Loader doesn't
   * support loading from a File.
   */
  void setDestination(OutputStream output) throws IOException;

  
  
}





