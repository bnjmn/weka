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
 *    DefaultInstancesIterator.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.dl4j.iterators;

import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.DataSet;

import weka.classifiers.functions.dl4j.Utils;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.dl4j.ShufflingDataSetIterator;


/**
 * Converts the given Instances object into a DataSet and then constructs and returns a ShufflingDataSetIterator.
 *
 * @author Christopher Beckham
 * @author Eibe Frank
 *
 * @version $Revision: 11711 $
 */
public class DefaultInstancesIterator extends AbstractDataSetIterator {

	/** The ID used to serialize this class */
	private static final long serialVersionUID = 1316260988724548474L;

	@OptionMetadata(displayName = "size of mini batch",
										description = "The mini batch size to use in the iterator (default = 1).",
										commandLineParamName = "bs", commandLineParamSynopsis = "-bs <int>",
										displayOrder = 1)
	public void setTrainBatchSize(int trainBatchSize) {
		m_batchSize = trainBatchSize;
	}
	public int getTrainBatchSize() {
		return m_batchSize;
	}

	/**
	 * Returns the number of predictor attributes for this dataset.
	 *
	 * @param data the dataset to compute the number of attributes from
	 * @return the number of attributes in the Instances object minus one
	 */
	@Override
	public int getNumAttributes(Instances data) {
		return data.numAttributes() - 1;
	}

	/**
	 * Returns the actual iterator.
	 *
	 * @param data the dataset to use
	 * @param seed the seed for the random number generator
	 * @param batchSize the batch size to use
	 * @return the DataSetIterator
	 */
	@Override
	public DataSetIterator getIterator(Instances data, int seed, int batchSize) {

		// Convert Instances to DataSet
		DataSet dataset = Utils.instancesToDataSet(data);

		return new ShufflingDataSetIterator(dataset, batchSize, seed);
	}
}