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
 *    ShufflingDataSetIterator.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.dl4j;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;

/**
 * An nd4j mini-batch iterator that shuffles the data whenever it is reset.
 *
 * @author Christopher Beckham
 * @author Eibe Frank
 *
 * @version $Revision: 11711 $
 */
public class ShufflingDataSetIterator implements DataSetIterator, Serializable {

	/** The ID used to serialize this class */
	private static final long serialVersionUID = 5571114918884888578L;

	/** The dataset to operate on */
	protected DataSet m_data = null;

	/** The mini batch size */
	protected int m_batchSize = 1;

	/** The cursor */
	protected int m_cursor = 0;

	/** The random number generator used for shuffling the data */
	protected Random m_random = null;

	/**
	 * Constructs a new shuffling iterator.
	 *
	 * @param data the dataset to operate on
	 * @param batchSize the mini batch size
	 * @param seed the seed for the random number generator
	 */
	public ShufflingDataSetIterator(DataSet data, int batchSize, int seed) {

		m_data = data;
		m_batchSize = Math.min(batchSize, data.numExamples());
		m_random = new Random(seed);
	}

	/**
	 * Whether another batch of data is still available.
	 *
	 * @return true if another batch is still available
	 */
	@Override
	public boolean hasNext() { return ( m_cursor + m_batchSize <= m_data.numExamples() ); }

	/**
	 * Returns the next mini batch of data.
	 *
	 * @return the dataset corresponding to the mini batch
	 */
	@Override
	public DataSet next() {

		// Special case: getRange() does not work as expected if there is just a single example
		if ((m_cursor == 0) && (m_batchSize == 1) && (m_data.numExamples() == 1)) {
			return m_data;
		}
		DataSet thisBatch = (DataSet) m_data.getRange(m_cursor, m_cursor + m_batchSize);
		m_cursor += m_batchSize;
		return thisBatch;
	}

	/**
	 * Returns a batch of the given size
	 *
	 * @param num the size of the batch to return
	 * @return a mini-batch of the given size
	 */
	@Override
	public DataSet next(int num) {

		// Special case: getRange() does not work as expected if there is just a single example
		if ((m_cursor == 0) && (num == 1) && (m_data.numExamples() == 1)) {
			return m_data;
		}
		DataSet thisBatch = (DataSet) m_data.getRange(m_cursor, m_cursor + num);
		m_cursor += num;
		return thisBatch;
	}

	/**
	 * Returns the total number of examples in the dataset.
	 *
	 * @return the total number of examples in the dataset.
	 */
	@Override
	public int totalExamples() {
		return m_data.numExamples();
	}

	/**
	 * Returns the number of input columns.
	 *
	 * @return the number of input columns
	 */
	@Override
	public int inputColumns() {
		return m_data.get(0).getFeatureMatrix().columns();
	}

	/**
	 * Returns the total number of labels.
	 *
	 * @return the total number of labels
	 */
	@Override
	public int totalOutcomes() {
		return m_data.get(0).getLabels().columns();
	}

	/**
	 * Resets the cursor. Also shuffles the data again.
	 */
	@Override
	public void reset() {

		m_cursor = 0;
		m_data.shuffle(m_random.nextLong());
	}

	/**
	 * Whether the iterator can be reset.
	 *
	 * @return true
	 */
	@Override
	public boolean resetSupported() {
		return true;
	}

	/**
	 * Whether the iterator can be used asynchronously.
	 *
	 * @return false
	 */
	@Override
	public boolean asyncSupported() {
		return false;
	}

	/**
	 * The size of the mini batches.
	 *
	 * @return the size of the mini batches
	 */
	@Override
	public int batch() {
		return m_batchSize;
	}

	/**
	 * The cursor given the location in the dataset.
	 *
	 * @return cursor
	 */
	@Override
	public int cursor() {
		return m_cursor;
	}

	/**
	 * The number of examples in the dataset.
	 *
	 * @return the number of examples
	 */
	@Override
	public int numExamples() {
		return m_data.numExamples();
	}

	/**
	 * Sets the preprocessor.
	 *
	 * @param preProcessor
	 */
	@Override
	public void setPreProcessor(DataSetPreProcessor preProcessor) { throw new UnsupportedOperationException(); }

	/**
	 * Gets the preprocessor.
	 *
	 * @return preProcessor
	 */
	@Override
	public DataSetPreProcessor getPreProcessor() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Gets the labels.
	 *
	 * @return the labels
	 */
	@Override
	public List<String> getLabels() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Enables removing of a mini-batch.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}