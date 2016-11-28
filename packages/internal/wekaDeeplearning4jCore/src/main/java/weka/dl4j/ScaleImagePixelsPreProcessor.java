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
 *    ScaleImagePixelsPreProcessor.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.dl4j;

import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;

/**
 * A simple preprocessor that divides all values in the dataset by 255.
 *
 * @author Christopher Beckham
 *
 * @version $Revision: 11711 $
 */
public class ScaleImagePixelsPreProcessor implements DataSetPreProcessor {

	/**
	 * Divides all intensity values by 255.
	 *
	 * @param data the dataset containing intensity values
	 */
	@Override
	public void preProcess(DataSet data) {
		data.divideBy(255);
	}
}