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
 *    AbstractDataSetIterator.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.dl4j.iterators;

import java.io.Serializable;
import java.util.Enumeration;

import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;

/**
 * An abstract iterator wrapper.
 *
 * @author Christopher Beckham
 * @author Eibe Frank
 *
 * @version $Revision: 11711 $
 */
public abstract class AbstractDataSetIterator implements OptionHandler, Serializable {

	/** The ID used for serialization */
	private static final long serialVersionUID = 7440584973810993954L;

	/** The batch size for the mini batches */
	protected int m_batchSize = 1;

	/**
	 * Get the number of predictor attributes for this iterator.
	 *
	 * @param data the dataset to compute the number of attributes from
	 * @return the number of attributes
	 * @throws Exception if the number of attributes cannot be computed successfully
	 */
	public abstract int getNumAttributes(Instances data) throws Exception;

	/**
	 * Returns the actual iterator.
	 *
	 * @param data the dataset to use
	 * @param seed the seed for the random number generator
	 * @return the iterator
	 * @throws Exception if the constructor cannot be constructed successfully
	 */
	public  DataSetIterator getIterator(Instances data, int seed) throws Exception {

		return getIterator(data, seed, getTrainBatchSize());
	}

	/**
	 * Returns the actual iterator.
	 *
	 * @param data the dataset to use
	 * @param seed the seed for the random number generator
	 * @param batchSize the batch size to use
	 * @return the iterator
	 * @throws Exception if the constructor cannot be constructed successfully
	 */
	public abstract DataSetIterator getIterator(Instances data, int seed, int batchSize) throws Exception;

	/**
	 * Setting the training batch size
	 * @param trainBatchSize the batch size
	 */
	public void setTrainBatchSize(int trainBatchSize) {
		m_batchSize = trainBatchSize;
	}

	/**
	 * Getting the training batch size
	 * @return the batch size
	 */
	public int getTrainBatchSize() {
		return m_batchSize;
	}

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	@Override
	public Enumeration<Option> listOptions() {

		return Option.listOptionsForClass(this.getClass()).elements();
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	@Override
	public String[] getOptions() {

		return Option.getOptions(this, this.getClass());
	}

	/**
	 * Parses a given list of options.
	 *
	 * @param options the list of options as an array of strings
	 * @exception Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {

		Option.setOptions(options, this, this.getClass());
	}
}