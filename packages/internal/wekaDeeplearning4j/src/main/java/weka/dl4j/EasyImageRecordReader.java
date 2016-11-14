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
 *    EasyImageRecordReader.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.dl4j;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.split.InputSplit;
import org.datavec.api.writable.Writable;
import org.datavec.common.RecordConverter;
import org.datavec.image.loader.ImageLoader;
import org.datavec.image.recordreader.BaseImageRecordReader;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * ImageRecordReader in DeepLearning4j assumes that your images are separated into different folders,
 * where each folder is a class, e.g. for MNIST, all the 0 images are in a folder
 * called 0/, all the 1 images in 1/, etc. At test time, you will get instances
 * where the class is not known, e.g:<br> image.png,?<br>
 * This is why we have had to make a new image record reader and make it possible
 * that all the images are in one folder. We specify the classes for those images
 * explicitly. The reader also shuffles the data.
 *
 * @author Chris Beckham
 *
 * @version $Revision: 11711 $
 */
public class EasyImageRecordReader extends BaseImageRecordReader {

    /** The ID used for serializing objects of this class. */
    protected static final long serialVersionUID = 3806752391833402426L;

    /** The list of file names */
    protected ArrayList<File> m_filenames = null;

    /** The list of class values */
    protected ArrayList<String> m_classes = null;

    /** The iterator for the class values */
    protected Iterator<String> m_classIterator = null;

    /** Random number generator for shuffling the data */
    protected Random m_random = null;

    /**
     * Constructor for the record reader.
     *
     * @param width The desired image width.
     * @param height The desired image height.
     * @param channels The desired number of channels.
     * @param filenames The list of file names.
     * @param classes The list of class values.
     * @param seed the seed for the random number generator.
     * @throws IOException
     */
    public EasyImageRecordReader(int width, int height, int channels,
                                 ArrayList<File> filenames, ArrayList<String> classes, int seed) throws IOException {
        m_filenames = filenames;
        m_classes = classes;

        m_random = new Random(seed);
        initialize(null);

        imageLoader = new ImageLoader(width, height, channels);
    }

    /**
     * Initializes the iterators. Shuffles the lists first.
     *
     * @param split this parameter is ignored.
     */
    @Override
    public void initialize(InputSplit split) {

        long s = m_random.nextLong();
        Collections.shuffle(m_filenames, new Random(s));
        Collections.shuffle(m_classes, new Random(s));
        iter = m_filenames.listIterator();
        m_classIterator = m_classes.listIterator();
    }

    /**
     * Gets the next element
     *
     * @return the next element
     */
    @Override
    public List<Writable> next() {

        if (iter != null) {
            List<Writable> ret = new ArrayList<Writable>();
            File image = iter.next();
            String classLabel = m_classIterator.next();
            currentFile = image;
            if(image.isDirectory())
                return next();
            try {
                INDArray row = imageLoader.asRowVector(image);
                ret = RecordConverter.toRecord(row);
                if(classLabel != "?")
                    ret.add(new DoubleWritable(Double.parseDouble(classLabel)));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ret;
        }
        throw new IllegalStateException("No iterator or no more elements");
    }

    /**
     * Resets the record reader.
     */
    @Override
    public void reset() {
        initialize(null);
    }

    /**
     * A method that still needs to be implemented.
     *
     * @param uri the URI
     * @param dataInputStream the data input stream
     * @return the record
     * @throws IOException
     */
    @Override
    public List<Writable> record(URI uri, DataInputStream dataInputStream)
            throws IOException {
        throw new IOException("Method record in EasyImageRecorder is not implemented.");
    }
}