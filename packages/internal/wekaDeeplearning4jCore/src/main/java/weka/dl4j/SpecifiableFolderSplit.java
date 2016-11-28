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
 *    SpecifiableFolderSplit.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.dl4j;

import java.net.URI;

import org.datavec.api.split.FileSplit;

/**
 * A hacky override of FileSplit that lets you specify the files directly.
 *
 * @author Christopher Beckham
 *
 * @version $Revision: 11711 $
 */
public class SpecifiableFolderSplit extends FileSplit {

    /**
     * The ID used for serialization
     */
    private static final long serialVersionUID = 462115492403223134L;

    /**
     * The constructor.
     */
    public SpecifiableFolderSplit() {
        super(null, null, true, null, false); // true
    }

    /**
     * The list of files to process.
     *
     * @param uris the list of files
     */
    public void setFiles(URI[] uris) {
        locations = uris;
    }

    /**
     * The length.
     *
     * @param len the length
     */
    public void setLength(int len) {
        length = len;
    }
}