/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *   BoundaryPanel.java
 *   Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.boundaryvisualizer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.util.Vector;
import java.util.Random;

import com.sun.image.codec.jpeg.*;
import java.awt.image.*;
import java.io.*;

import weka.core.*;
import weka.classifiers.Classifier;
import weka.classifiers.DistributionClassifier;
import weka.classifiers.bayes.NaiveBayesSimple;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.j48.J48;
import weka.classifiers.lazy.IBk;
import weka.classifiers.functions.Logistic;
import weka.clusterers.EM;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.Add;

/**
 * BoundaryPanel. A class to handle the plotting operations
 * associated with generating a 2D picture of a classifier's decision
 * boundaries.
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.12 $
 * @since 1.0
 * @see JPanel
 */
public class BoundaryPanel extends JPanel {

  // default colours for classes
  public static final Color [] DEFAULT_COLORS = {
    Color.red,
    Color.green,
    Color.blue,
    new Color(0, 255, 255), // cyan
    new Color(255, 0, 255), // pink
    new Color(255, 255, 0), // yellow
    new Color(255, 255, 255), //white
    new Color(0, 0, 0)};

  protected FastVector m_Colors = new FastVector();

  // training data
  private Instances m_trainingData;

  // distribution classifier to use
  private DistributionClassifier m_classifier;

  // data generator to use
  private DataGenerator m_dataGenerator;

  // index of the class attribute
  private int m_classIndex = -1;

  // attributes for visualizing on
  private int m_xAttribute;
  private int m_yAttribute;

  // min, max and ranges of these attributes
  private double m_minX;
  private double m_minY;
  private double m_maxX;
  private double m_maxY;
  private double m_rangeX;
  private double m_rangeY;

  // pixel width and height in terms of attribute values
  private double m_pixHeight;
  private double m_pixWidth;

  // used for offscreen drawing
  private Image m_osi = null;

  // width and height of the display area
  private int m_panelWidth;
  private int m_panelHeight;

  // number of samples to take from each region in the fixed dimensions
  private int m_numOfSamplesPerRegion = 2;

  // number of samples per kernel = base ^ (# non-fixed dimensions)
  private int m_numOfSamplesPerGenerator;
  private double m_samplesBase = 2.0;

  // listeners to be notified when plot is complete
  private Vector m_listeners = new Vector();

  // small inner class for rendering the bitmap on to
  private class PlotPanel extends JPanel {
    public PlotPanel() {
      this.setToolTipText("");
    }
    
    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (m_osi != null) {
	g.drawImage(m_osi,0,0,this);
      }
    }
    
    public String getToolTipText(MouseEvent event) {
      if (m_probabilityCache == null) {
	return null;
      }
      
      if (m_probabilityCache[event.getY()][event.getX()] == null) {
	return null;
      }
      
      String pVec = "(X: "
	+Utils.doubleToString(convertFromPanelX((double)event.getX()), 2)
	+" Y: "
	+Utils.doubleToString(convertFromPanelY((double)event.getY()), 2)+") ";
      // construct a string holding the probability vector
      for (int i = 0; i < m_trainingData.classAttribute().numValues(); i++) {
	pVec += 
	  Utils.
	  doubleToString(m_probabilityCache[event.getY()][event.getX()][i],
			 3)+" ";
      }
      return pVec;
    }
  }

  // the actual plotting area
  private PlotPanel m_plotPanel = new PlotPanel();

  // thread for running the plotting operation in
  private Thread m_plotThread = null;

  // Stop the plotting thread
  private boolean m_stopPlotting = false;

  private boolean m_stopReplotting = false;

  // A random number generator 
  private Random m_random = null;

  // cache of probabilities for fast replotting
  private double [][][] m_probabilityCache;

  // plot the training data
  private boolean m_plotTrainingData = true;

  /**
   * Creates a new <code>BoundaryPanel</code> instance.
   *
   * @param panelWidth the width in pixels of the panel
   * @param panelHeight the height in pixels of the panel
   */
  public BoundaryPanel(int panelWidth, int panelHeight) {
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
    m_panelWidth = panelWidth;
    m_panelHeight = panelHeight;
    setLayout(new BorderLayout());
    m_plotPanel.setMinimumSize(new Dimension(m_panelWidth, m_panelHeight));
    m_plotPanel.setPreferredSize(new Dimension(m_panelWidth, m_panelHeight));
    m_plotPanel.setMaximumSize(new Dimension(m_panelWidth, m_panelHeight));
    add(m_plotPanel, BorderLayout.CENTER);
    setPreferredSize(m_plotPanel.getPreferredSize());
    setMaximumSize(m_plotPanel.getMaximumSize());
    setMinimumSize(m_plotPanel.getMinimumSize());

    m_random = new Random(1);
    for (int i = 0; i < DEFAULT_COLORS.length; i++) {
      m_Colors.addElement(new Color(DEFAULT_COLORS[i].getRed(),
				    DEFAULT_COLORS[i].getGreen(),
				    DEFAULT_COLORS[i].getBlue()));
    }
    m_probabilityCache = new double[m_panelHeight][m_panelWidth][];
  }

  /**
   * Set the number of points to uniformly sample from a region (fixed
   * dimensions).
   *
   * @param num an <code>int</code> value
   */
  public void setNumSamplesPerRegion(int num) {
    m_numOfSamplesPerRegion = num;
  }

  /**
   * Get the number of points to sample from a region (fixed dimensions).
   *
   * @return an <code>int</code> value
   */
  public int getNumSamplesPerRegion() {
    return m_numOfSamplesPerRegion;
  }

  /**
   * Set the base for computing the number of samples to obtain from each
   * generator. number of samples = base ^ (# non fixed dimensions)
   *
   * @param ksb a <code>double</code> value
   */
  public void setGeneratorSamplesBase(double ksb) {
    m_samplesBase = ksb;
  }

  /**
   * Get the base used for computing the number of samples to obtain from
   * each generator
   *
   * @return a <code>double</code> value
   */
  public double getGeneratorSamplesBase() {
    return m_samplesBase;
  }

  /**
   * Set up the off screen bitmap for rendering to
   */
  protected void initialize() {
    int iwidth = m_plotPanel.getWidth();
    int iheight = m_plotPanel.getHeight();
    //    System.err.println(iwidth+" "+iheight);
    m_osi = m_plotPanel.createImage(iwidth, iheight);
    Graphics m = m_osi.getGraphics();
    m.fillRect(0,0,iwidth,iheight);
  }

  /**
   * Stop the plotting thread
   */
  public void stopPlotting() {
    m_stopPlotting = true;
  }

  private void computeMinMaxAtts() {
    m_minX = Double.MAX_VALUE;
    m_minY = Double.MAX_VALUE;
    m_maxX = Double.MIN_VALUE;
    m_maxY = Double.MIN_VALUE;

    for (int i = 0; i < m_trainingData.numInstances(); i++) {
      Instance inst = m_trainingData.instance(i);
      double x = inst.value(m_xAttribute);
      double y = inst.value(m_yAttribute);
      if (x != Instance.missingValue()) {
	if (x < m_minX) {
	  m_minX = x;
	}
	if (x > m_maxX) {
	  m_maxX = x;
	}
      }
      if (y != Instance.missingValue()) {
	if (y < m_minY) {
	  m_minY = y;
	}
	if (y > m_maxY) {
	  m_maxY = y;
	}
      }
    }
    m_rangeX = (m_maxX - m_minX);
    m_rangeY = (m_maxY - m_minY);
    m_pixWidth = m_rangeX / (double)m_panelWidth;
    m_pixHeight = m_rangeY / (double) m_panelHeight;
  }

  /**
   * Return the x attribute value that corresponds to the middle of
   * the pix'th horizontal pixel
   *
   * @param pix the horizontal pixel number
   * @return a value in attribute space
   */
  private double getRandomX(int pix) {

    double minPix =  m_minX + (pix * m_pixWidth);

    return minPix + m_random.nextDouble() * m_pixWidth;
  }

  /**
   * Return the y attribute value that corresponds to the middle of
   * the pix'th vertical pixel
   *
   * @param pix the vertical pixel number
   * @return a value in attribute space
   */
  private double getRandomY(int pix) {
    
    double minPix = m_minY + (pix * m_pixHeight);
    
    return minPix +  m_random.nextDouble() * m_pixHeight;
  }
  
  /**
   * Start the plotting thread
   *
   * @exception Exception if an error occurs
   */
  public void start() throws Exception {
    m_numOfSamplesPerGenerator = 
      (int)Math.pow(m_samplesBase, m_trainingData.numAttributes()-3);

    m_stopReplotting = true;
    if (m_trainingData == null) {
      throw new Exception("No training data set (BoundaryPanel)");
    }
    if (m_classifier == null) {
      throw new Exception("No classifier set (BoundaryPanel)");
    }
    if (m_dataGenerator == null) {
      throw new Exception("No data generator set (BoundaryPanel)");
    }
    if (m_trainingData.attribute(m_xAttribute).isNominal() || 
	m_trainingData.attribute(m_yAttribute).isNominal()) {
      throw new Exception("Visualization dimensions must be numeric "
			  +"(BoundaryPanel)");
    }
    
    computeMinMaxAtts();
    
    if (m_plotThread == null) {
      m_plotThread = new Thread() {
	  public void run() {
	    m_stopPlotting = false;
	    try {
	      initialize();
	      repaint();
	      
	      // train the classifier
	      m_probabilityCache = new double[m_panelHeight][m_panelWidth][];
	      m_classifier.buildClassifier(m_trainingData);
	      
	      // build DataGenerator
	      boolean [] attsToWeightOn = 
		new boolean[m_trainingData.numAttributes()];
	      attsToWeightOn[m_xAttribute] = true;
	      attsToWeightOn[m_yAttribute] = true;
	      
	      m_dataGenerator.setWeightingDimensions(attsToWeightOn);
	      
	      m_dataGenerator.buildGenerator(m_trainingData);
	      
	      // generate samples
	      double [] dist;
	      double [] weightingAttsValues = 
		new double [attsToWeightOn.length];
	      double [] vals = new double[m_trainingData.numAttributes()];
	      Instance predInst = new Instance(1.0, vals);
	      predInst.setDataset(m_trainingData);
	    abortPlot: for (int i = 0; i < m_panelHeight; i++) {
	      for (int j = 0; j < m_panelWidth; j++) {
		
		if (m_stopPlotting) {
		  break abortPlot;
		}
		
		double [] sumOfProbsForRegion = 
		  new double [m_trainingData.classAttribute().numValues()];
		
		for (int u = 0; u < m_numOfSamplesPerRegion; u++) {
		  
		  double [] sumOfProbsForLocation = 
		    new double [m_trainingData.classAttribute().numValues()];
		  
		  weightingAttsValues[m_xAttribute] = 
		    getRandomX(j);
		  weightingAttsValues[m_yAttribute] = 
		    getRandomY(m_panelHeight-i-1);
		  
		  m_dataGenerator.setWeightingValues(weightingAttsValues);
		  
		  double [] weights = m_dataGenerator.getWeights();
		  double sumOfWeights = Utils.sum(weights);
		  int [] indices = Utils.sort(weights);
		  
		  // Prune 1% of weight mass
		  int [] newIndices = new int[indices.length];
		  double sumSoFar = 0; 
		  double criticalMass = 0.99 * sumOfWeights;
		  int index = weights.length - 1; int counter = 0;
		  for (int z = weights.length - 1; z >= 0; z--) {
		    newIndices[index--] = indices[z];
		    sumSoFar += weights[indices[z]];
		    counter++;
		    if (sumSoFar > criticalMass) {
		      break;
		    }
		  }
		  indices = new int[counter];
		  System.arraycopy(newIndices, index + 1, indices, 0, counter);
		  
		  for (int z = 0; z < m_numOfSamplesPerGenerator; z++) {
		    
		    m_dataGenerator.setWeightingValues(weightingAttsValues);
		    double [][] values = 
		      m_dataGenerator.generateInstances(indices);

		    for (int q = 0; q < values.length; q++) {
		      if (values[q] != null) {
			System.arraycopy(values[q], 0, vals, 0, vals.length);
			vals[m_xAttribute] = weightingAttsValues[m_xAttribute];
			vals[m_yAttribute] = weightingAttsValues[m_yAttribute];
			
			// classify the instance
			dist = m_classifier.distributionForInstance(predInst);
			for (int k = 0; 
			     k < sumOfProbsForLocation.length; k++) {
			  sumOfProbsForLocation[k] += (dist[k] * weights[q]); 
			}
		      }
		    }
		  }
		  
		  for (int k = 0; k < sumOfProbsForRegion.length; k++) {
		    sumOfProbsForRegion[k] += 
		      (sumOfProbsForLocation[k] * sumOfWeights); 
		  }
		}
		
		// average
		Utils.normalize(sumOfProbsForRegion);
		// cache
		m_probabilityCache[i][j] = 
		  new double[sumOfProbsForRegion.length];
		System.arraycopy(sumOfProbsForRegion, 0, 
				 m_probabilityCache[i][j], 0, 
				 sumOfProbsForRegion.length);
		
		plotPoint(j, i, sumOfProbsForRegion);
	      }
	    }
	      if (m_plotTrainingData) {
		plotTrainingData();
	      }
	      
	    } catch (Exception ex) {
	      ex.printStackTrace();
	    } finally {
	      m_plotThread = null;
	      // notify any listeners that we are finished
	      Vector l;
	      ActionEvent e = new ActionEvent(this, 0, "");
	      synchronized(this) {
		l = (Vector)m_listeners.clone();
	      }
	      for (int i = 0; i < l.size(); i++) {
		ActionListener al = (ActionListener)l.elementAt(i);
		al.actionPerformed(e);
	      }
	    }
	  }
	};
      m_plotThread.setPriority(Thread.MIN_PRIORITY);
      m_plotThread.start();
    }
  }

  private void plotTrainingData() {
    Graphics2D osg = (Graphics2D)m_osi.getGraphics();
    Graphics g = m_plotPanel.getGraphics();
    //    AlphaComposite ac =  AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
    //    						    0.5f);
    //    osg.setComposite(ac);
    osg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			 RenderingHints.VALUE_ANTIALIAS_ON);
    double xval = 0; double yval = 0;
    for (int i = 0; i < m_trainingData.numInstances(); i++) {
      if (!m_trainingData.instance(i).isMissing(m_xAttribute) &&
	  !m_trainingData.instance(i).isMissing(m_yAttribute)) {

	xval = m_trainingData.instance(i).value(m_xAttribute);
	yval = m_trainingData.instance(i).value(m_yAttribute);
       
	int panelX = convertToPanelX(xval);
	int panelY = convertToPanelY(yval);
	Color ColorToPlotWith = 
	  ((Color)m_Colors.elementAt((int)m_trainingData.instance(i).
				     value(m_classIndex)));

	if (ColorToPlotWith.equals(Color.white)) {
	  osg.setColor(Color.black);
	} else {
	  osg.setColor(Color.white);
	}
	osg.fillOval(panelX-3, panelY-3, 7, 7);
       	osg.setColor(ColorToPlotWith);
	osg.fillOval(panelX-2, panelY-2, 5, 5);
      }
    }
    g.drawImage(m_osi,0,0,m_plotPanel);
  }

  private int convertToPanelX(double xval) {
    double temp = (xval - m_minX) / m_rangeX;
    temp = temp * (double) m_panelWidth;

    return (int)temp;
  }

  private int convertToPanelY(double yval) {
    double temp = (yval - m_minY) / m_rangeY;
    temp = temp * (double) m_panelHeight;
    temp = m_panelHeight - temp;

    return (int)temp;
  }

  private double convertFromPanelX(double pX) {
    pX /= (double) m_panelWidth;

    pX *= m_rangeX;
    return pX + m_minX;
  }

  private double convertFromPanelY(double pY) {
    pY  = m_panelHeight - pY;
    pY /= (double) m_panelHeight;
    pY *= m_rangeY;

    return pY + m_minY;
  }

  private void plotPoint(int x, int y, double [] probs) {
    // plot the point
    Graphics osg = m_osi.getGraphics();
    Graphics g = m_plotPanel.getGraphics();
    float [] colVal = new float[3];
    
    float [] tempCols = new float[3];
    for (int k = 0; k < probs.length; k++) {
      Color curr = (Color)m_Colors.elementAt(k);

      curr.getRGBColorComponents(tempCols);
      for (int z = 0 ; z < 3; z++) {
	colVal[z] += probs[k] * tempCols[z];
      }
    }
    
    osg.setColor(new Color(colVal[0], 
			   colVal[1], 
			   colVal[2]));
    osg.drawLine(x,y,x,y);
    if (x == 0) {
      g.drawImage(m_osi,0,0,m_plotPanel);
    }
  }

  /**
   * Set the training data to use
   *
   * @param trainingData the training data
   * @exception Exception if an error occurs
   */
  public void setTrainingData(Instances trainingData) throws Exception {

    m_trainingData = trainingData;
    if (m_trainingData.classIndex() < 0) {
      throw new Exception("No class attribute set (BoundaryPanel)");
    }
    m_classIndex = m_trainingData.classIndex();
  }

  /**
   * Register a listener to be notified when plotting completes
   *
   * @param newListener the listener to add
   */
  public void addActionListener(ActionListener newListener) {
    m_listeners.add(newListener);
  }

  /**
   * Remove a listener
   *
   * @param removeListener the listener to remove
   */
  public void removeActionListener(ActionListener removeListener) {
    m_listeners.removeElement(removeListener);
  }

  /**
   * Set the classifier to use.
   *
   * @param classifier the classifier to use
   */
  public void setClassifier(DistributionClassifier classifier) {
    m_classifier = classifier;
  }

  /**
   * Set the data generator to use for generating new instances
   *
   * @param dataGenerator the data generator to use
   */
  public void setDataGenerator(DataGenerator dataGenerator) {
    m_dataGenerator = dataGenerator;
  }
    
  /**
   * Set the x attribute index
   *
   * @param xatt index of the attribute to use on the x axis
   * @exception Exception if an error occurs
   */
  public void setXAttribute(int xatt) throws Exception {
    if (m_trainingData == null) {
      throw new Exception("No training data set (BoundaryPanel)");
    }
    if (xatt < 0 || 
	xatt > m_trainingData.numAttributes()) {
      throw new Exception("X attribute out of range (BoundaryPanel)");
    }
    if (m_trainingData.attribute(xatt).isNominal()) {
      throw new Exception("Visualization dimensions must be numeric "
			  +"(BoundaryPanel)");
    }
    if (m_trainingData.numDistinctValues(xatt) < 2) {
      throw new Exception("Too few distinct values for X attribute "
			  +"(BoundaryPanel)");
    }
    m_xAttribute = xatt;
  }

  /**
   * Set the y attribute index
   *
   * @param yatt index of the attribute to use on the y axis
   * @exception Exception if an error occurs
   */
  public void setYAttribute(int yatt) throws Exception {
    if (m_trainingData == null) {
      throw new Exception("No training data set (BoundaryPanel)");
    }
    if (yatt < 0 || 
	yatt > m_trainingData.numAttributes()) {
      throw new Exception("X attribute out of range (BoundaryPanel)");
    }
    if (m_trainingData.attribute(yatt).isNominal()) {
      throw new Exception("Visualization dimensions must be numeric "
			  +"(BoundaryPanel)");
    }
    if (m_trainingData.numDistinctValues(yatt) < 2) {
      throw new Exception("Too few distinct values for Y attribute "
			  +"(BoundaryPanel)");
    }
    m_yAttribute = yatt;
  }
  
  /**
   * Set a vector of Color objects for the classes
   *
   * @param colors a <code>FastVector</code> value
   */
  public void setColors(FastVector colors) {
    synchronized (m_Colors) {
      m_Colors = colors;
    }
    replot();
  }

  /**
   * Set whether to superimpose the training data
   * plot
   *
   * @param pg a <code>boolean</code> value
   */
  public void setPlotTrainingData(boolean pg) {
    m_plotTrainingData = pg;
  }

  /**
   * Returns true if training data is to be superimposed
   *
   * @return a <code>boolean</code> value
   */
  public boolean getPlotTrainingData() {
    return m_plotTrainingData;
  }

  /**
   * Get the current vector of Color objects used for the classes
   *
   * @return a <code>FastVector</code> value
   */
  public FastVector getColors() {
    return m_Colors;
  }

  /**
   * Quickly replot the display using cached probability estimates
   */
  public void replot() {
    if (m_probabilityCache[0][0] == null) {
      return;
    }
    m_stopReplotting = true;
    // wait 300 ms to give any other replot threads a chance to halt
    try {
      Thread.sleep(300);
    } catch (Exception ex) {}

    final Thread replotThread = new Thread() {
	public void run() {
	  m_stopReplotting = false;
	finishedReplot: for (int i = 0; i < m_panelHeight; i++) {
	  for (int j = 0; j < m_panelWidth; j++) {
	    if (m_probabilityCache[i][j] == null || m_stopReplotting) {
	      break finishedReplot;
	    }
	    plotPoint(j, i, m_probabilityCache[i][j]);
	  }
	}
	  if (m_plotTrainingData) {
	    plotTrainingData();
	  }
	}
      };
    
    replotThread.start();      
  }

  protected void saveImage(String fileName) {
    try {
      BufferedOutputStream out = 
	new BufferedOutputStream(new FileOutputStream(fileName));
      
      JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);

      BufferedImage bi = new BufferedImage(m_panelWidth, m_panelHeight,
					   BufferedImage.TYPE_INT_RGB);
      Graphics2D gr2 = bi.createGraphics();
      gr2.drawImage(m_osi, 0, 0, m_panelWidth, m_panelHeight, null);

      JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(bi);
      param.setQuality(1.0f, false);
      encoder.setJPEGEncodeParam(param);
      encoder.encode(bi);
      out.flush();
      out.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Main method for testing this class
   *
   * @param args a <code>String[]</code> value
   */
  public static void main (String [] args) {
    try {
      if (args.length < 7) {
	System.err.println("Usage : BoundaryPanel <dataset> "
			   +"<class col> <xAtt> <yAtt> <display width> "
			   +"<display height> <classifier "
			   +"[classifier options]>");
	System.exit(1);
      }
      final javax.swing.JFrame jf = 
	new javax.swing.JFrame("Weka classification boundary visualizer");
      jf.getContentPane().setLayout(new BorderLayout());

      System.err.println("Loading instances from : "+args[0]);
      java.io.Reader r = new java.io.BufferedReader(
			 new java.io.FileReader(args[0]));
      final Instances i = new Instances(r);
      i.setClassIndex(Integer.parseInt(args[1]));

      //      bv.setClassifier(new Logistic());
      final int xatt = Integer.parseInt(args[2]);
      final int yatt = Integer.parseInt(args[3]);
      int panelWidth = Integer.parseInt(args[4]);
      int panelHeight = Integer.parseInt(args[5]);

      final String classifierName = args[6];
      final BoundaryPanel bv = new BoundaryPanel(panelWidth,panelHeight);
      bv.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    String classifierNameNew = 
	      classifierName.substring(classifierName.lastIndexOf('.')+1, 
				       classifierName.length());
	    bv.saveImage(classifierNameNew+"_"+i.relationName()
			 +"_X"+xatt+"_Y"+yatt+".jpg");
	  }
	});

      bv.setDataGenerator(new KDDataGenerator());
      bv.setTrainingData(i);
      bv.setXAttribute(xatt);
      bv.setYAttribute(yatt);

      jf.getContentPane().add(bv, BorderLayout.CENTER);
      jf.setSize(bv.getMinimumSize());
      //      jf.setSize(200,200);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
	  public void windowClosing(java.awt.event.WindowEvent e) {
	    jf.dispose();
	    System.exit(0);
	  }
	});

      jf.pack();
      jf.setVisible(true);
      //      bv.initialize();
      bv.repaint();
      

      String [] argsR = null;
      if (args.length > 7) {
	argsR = new String [args.length-7];
	for (int j = 7; j < args.length; j++) {
	  argsR[j-7] = args[j];
	}
      }
      Classifier c = Classifier.forName(args[6], argsR);
      bv.setClassifier((DistributionClassifier)c);
      bv.start();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}

