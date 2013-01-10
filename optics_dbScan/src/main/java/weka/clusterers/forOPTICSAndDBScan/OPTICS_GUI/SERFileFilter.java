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
 *    Copyright (C) 2004
 *    & Matthias Schubert (schubert@dbs.ifi.lmu.de)
 *    & Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 *    & Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 */

package weka.clusterers.forOPTICSAndDBScan.OPTICS_GUI;

import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * <p>
 * SERFileFilter.java <br/>
 * Authors: Rainer Holzmann, Zhanna Melnikova-Albrecht <br/>
 * Date: Sep 15, 2004 <br/>
 * Time: 6:54:56 PM <br/>
 * $ Revision 1.4 $ <br/>
 * </p>
 *
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision$
 */
public class SERFileFilter
    extends FileFilter
    implements RevisionHandler {

    /**
     * Holds the extension of the FileFilter
     */
    private String extension;

    /**
     * Holds the description for this File-Type
     */
    private String description;

    // *****************************************************************************************************************
    // constructors
    // *****************************************************************************************************************

    public SERFileFilter(String extension, String description) {
        this.extension = extension;
        this.description = description;
    }

    // *****************************************************************************************************************
    // methods
    // *****************************************************************************************************************

    /**
     * Whether the given file is accepted by this filter.
     */
    public boolean accept(File f) {
        if (f != null) {
            if (f.isDirectory()) {
                return true;
            }

            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if (i > 0 && i < filename.length() - 1) {
                extension = filename.substring(i + 1).toLowerCase();
            }
            if (extension.equals("ser")) return true;
        }

        return false;
    }

    /**
     * The description of this filter.
     * @see javax.swing.filechooser.FileView#getName
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision$");
    }
}
