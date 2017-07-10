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
 *    ImageDatasetIterator.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.dl4j.iterators;

import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import weka.classifiers.functions.dl4j.Utils;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionMetadata;
import weka.dl4j.ShufflingDataSetIterator;

import java.io.File;
import java.util.Enumeration;

/**
 * Converts the given Instances object into a DataSet and then constructs and returns a ShufflingDataSetIterator.
 * This iterator is designed for training convolutional networks on data that is represented as standard WEKA instances.
 * It enables specification of filter width and height, and number of channels.
 *
 * @author Christopher Beckham
 * @author Eibe Frank
 *
 * @version $Revision: 11711 $
 */
public class ConvolutionalInstancesIterator extends AbstractDataSetIterator {

    /** The version ID used for serializing objects of this class */
    private static final long serialVersionUID = -3101209034945158130L;

    /** The desired output height */
    protected int m_height = 28;

    /** The desired output width */
    protected int m_width = 28;

    /** The desired number of channels */
    protected int m_numChannels = 1;

    @OptionMetadata(displayName = "size of mini batch",
            description = "The mini batch size to use in the iterator (default = 1).",
            commandLineParamName = "bs", commandLineParamSynopsis = "-bs <int>",
            displayOrder = 0)
    public void setTrainBatchSize(int trainBatchSize) {
        m_batchSize = trainBatchSize;
    }
    public int getTrainBatchSize() {
        return m_batchSize;
    }

    @OptionMetadata(
            displayName = "desired width",
            description = "The desired width of the images (default = 28).",
            commandLineParamName = "width", commandLineParamSynopsis = "-width <int>",
            displayOrder = 1)
    public int getWidth() {
        return m_width;
    }
    public void setWidth(int width) {
        m_width = width;
    }

    @OptionMetadata(
            displayName = "desired height",
            description = "The desired height of the images (default = 28).",
            commandLineParamName = "height", commandLineParamSynopsis = "-height <int>",
            displayOrder = 2)
    public int getHeight() {
        return m_height;
    }
    public void setHeight(int height) {
        m_height = height;
    }

    @OptionMetadata(
            displayName = "desired number of channels",
            description = "The desired number of channels (default = 1).",
            commandLineParamName = "numChannels", commandLineParamSynopsis = "-numChannels <int>",
            displayOrder = 3)
    public int getNumChannels() {
        return m_numChannels;
    }
    public void setNumChannels(int numChannels) {
        m_numChannels = numChannels;
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