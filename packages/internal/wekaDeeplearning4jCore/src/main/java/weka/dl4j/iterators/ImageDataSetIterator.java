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

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import org.datavec.api.split.StringSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.dl4j.EasyImageRecordReader;
import weka.dl4j.ScaleImagePixelsPreProcessor;

/**
 * An iterator that loads images.
 *
 * @author Christopher Beckham
 * @author Eibe Frank
 *
 * @version $Revision: 11711 $
 */
public class ImageDataSetIterator extends AbstractDataSetIterator {

    /** The version ID used for serializing objects of this class */
    private static final long serialVersionUID = -3701309032945158130L;

    /** The desired output height */
    protected int m_height = 28;

    /** The desired output width */
    protected int m_width = 28;

    /** The desired number of channels */
    protected int m_numChannels = 1;

    /** The location of the folder containing the images */
    protected File m_imagesLocation = new File(System.getProperty("user.dir"));

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
            displayName = "directory of images",
            description = "The directory containing the images (default = user home).",
            commandLineParamName = "imagesLocation", commandLineParamSynopsis = "-imagesLocation <string>",
            displayOrder = 1)
    public File getImagesLocation() { return m_imagesLocation; }
    public void setImagesLocation(File imagesLocation) { m_imagesLocation = imagesLocation; }

    @OptionMetadata(
            displayName = "desired width",
            description = "The desired width of the images (default = 28).",
            commandLineParamName = "width", commandLineParamSynopsis = "-width <int>",
            displayOrder = 2)
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
            displayOrder = 3)
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
            displayOrder = 4)
    public int getNumChannels() {
        return m_numChannels;
    }
    public void setNumChannels(int numChannels) {
        m_numChannels = numChannels;
    }

    /**
     * Validates the input dataset
     *
     * @param data the input dataset
     * @throws Exception if validation is unsuccessful
     */
    public void validate(Instances data) throws Exception {

        if( ! getImagesLocation().isDirectory() ) {
            throw new Exception("Directory not valid: " + getImagesLocation());
        }
        if( ! ( data.attribute(0).isString() && data.classIndex() == 1) ) {
            throw new Exception("An ARFF is required with a string attribute and a class attribute");
        }
    }

    /**
     * This just returns the number of channels.
     *
     * @param data the dataset to compute the number of attributes from
     * @return the number of channels
     */
    @Override
    public int getNumAttributes(Instances data) { return getNumChannels(); }

    /**
     * Returns the image recorder.
     *
     * @param data the dataset to use
     * @param seed the seed for the random number generator
     * @return the image recorder
     * @throws Exception
     */
    protected EasyImageRecordReader getImageRecordReader(Instances data, int seed) throws Exception {

        int len = 0;
        ArrayList<File> filenames = new ArrayList<File>();
        ArrayList<String> classes = new ArrayList<String>();
        for(int x = 0; x < data.numInstances(); x++) {
            String location = data.attribute(0).value( (int) data.get(x).value(0) );
            filenames.add( new File(getImagesLocation() + File.separator + location) );
            classes.add( String.valueOf(data.get(x).classValue() ) );

            File f = new File( getImagesLocation() + File.separator + location );
            len += f.length();
        }
        EasyImageRecordReader reader = new EasyImageRecordReader(getWidth(), getHeight(), getNumChannels(), filenames,
                classes, seed);
        reader.initialize(new StringSplit("")); // The argument is ignored anyway
        return reader;
    }

    /**
     * This method returns the iterator. Scales all intensity values: it divides them by 255. Shuffles the data
     * before the iterator is created.
     *
     * @param data the dataset to use
     * @param seed the seed for the random number generator
     * @param batchSize the batch size to use
     * @return the iterator
     * @throws Exception
     */
    @Override
    public DataSetIterator getIterator(Instances data, int seed, int batchSize) throws Exception {

        batchSize = Math.min(data.numInstances(), batchSize);
        validate(data);
        data.randomize(new Random(seed));
        EasyImageRecordReader reader = getImageRecordReader(data, seed);
        DataSetIterator tmpIter = new RecordReaderDataSetIterator( reader, batchSize, -1, data.numClasses());
        tmpIter.setPreProcessor(new ScaleImagePixelsPreProcessor());
        return tmpIter;
    }
}