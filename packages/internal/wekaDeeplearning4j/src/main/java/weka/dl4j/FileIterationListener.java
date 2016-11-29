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
 *    FileIterationListener.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.dl4j;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.optimize.api.IterationListener;

/**
 * Class for listening to performance stats and writing them to a file.
 *
 * @author Christopher Beckham
 *
 * @version $Revision: 11711 $
 */
public class FileIterationListener implements IterationListener {

	/** The ID used for serializing this class */
	private static final long serialVersionUID = 1948578564961956518L;

	/** The print writer to use */
	protected transient PrintWriter m_pw = null;

	/** The number of mini batches. */
	protected int m_numMiniBatches = 0;

	/** Losses per epoch */
	protected ArrayList<Double> lossesPerEpoch = new ArrayList<Double>();

	/**
	 * Constructor for this listener.
	 *
	 * @param filename the file to write the information to
	 * @param numMiniBatches the number of mini batches
	 * @throws Exception
	 */
	public FileIterationListener(String filename, int numMiniBatches) throws Exception {
		super();
		File f = new File(filename);
		if(f.exists()) f.delete();
		System.err.println("Creating debug file at: " + filename);
		m_numMiniBatches = numMiniBatches;
		m_pw = new PrintWriter(new FileWriter(filename, false));
		m_pw.write("loss\n");
	}

	/**
	 * No-op method.
	 */
	@Override
	public void invoke() { }

	/**
	 * Always returns false.
	 *
	 * @return false
	 */
	@Override
	public boolean invoked() {
		return false;
	}

	/**
	 * Method that gets called when an iteration is done.
	 *
	 * @param model the model to operate with
	 * @param epoch the epoch number
	 */
	@Override
	public void iterationDone(Model model, int epoch) {

		lossesPerEpoch.add( model.score() );
		if(lossesPerEpoch.size() == m_numMiniBatches) {
			// calculate mean
			double mean = 0;
			for(double val : lossesPerEpoch) {
				mean += val;
			}
			mean = mean / lossesPerEpoch.size();
			m_pw.write(mean + "\n");
			m_pw.flush();
			lossesPerEpoch.clear();
		}
	}
}