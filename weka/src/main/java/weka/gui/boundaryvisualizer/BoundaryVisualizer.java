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
 *   BoundaryVisualizer.java
 *   Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.boundaryvisualizer;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import java.util.Vector;

import weka.core.*;
import weka.classifiers.Classifier;
import weka.gui.visualize.ClassPanel;

/**
 * BoundaryVisualizer. Allows the visualization of classifier decision
 * boundaries in two dimensions. A supplied classifier is first
 * trained on supplied training data, then a data generator (currently
 * using kernels) is used to generate new instances at points fixed in
 * the two visualization dimensions but random in the other
 * dimensions. These instances are classified by the classifier and
 * plotted as points with colour corresponding to the probability
 * distribution predicted by the classifier. At present, 2 * 2^(#
 * non-fixed dimensions) points are generated from each kernel per
 * pixel in the display. In practice, fewer points than this are
 * actually classified because kernels are weighted (on a per-pixel
 * basis) according to the fixexd dimensions and kernels corresponding
 * to the lowest 1% of the weight mass are discarded. Predicted
 * probability distributions are weighted (acording to the fixed
 * visualization dimensions) and averaged to produce an RGB value for
 * the pixel. For more information, see<p>
 * 
 * Eibe Frank and Mark Hall (2003). Visualizing Class Probability
 * Estimators. Working Paper 02/03, Department of Computer Science,
 * University of Waikato.
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.12.2.1 $
 * @since 1.0
 * @see JPanel 
 */
public class BoundaryVisualizer extends JPanel {

  /**
   * Inner class to handle rendering the axis
   *
   * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
   * @version 1.0
   * @since 1.0
   * @see JPanel
   */
  private class AxisPanel extends JPanel {
    
    private static final int MAX_PRECISION = 10;
    private boolean m_vertical = false;
    private final int PAD = 5;
    private FontMetrics m_fontMetrics;
    private int m_fontHeight;
    
    public AxisPanel(boolean vertical) {
      m_vertical = vertical;
      this.setBackground(Color.black);
      //      Graphics g = this.getGraphics();
      String fontFamily = this.getFont().getFamily();
      Font newFont = new Font(fontFamily, Font.PLAIN, 10);
      this.setFont(newFont);
    }

    public Dimension getPreferredSize() {
      if (m_fontMetrics == null) {
	Graphics g = this.getGraphics();
	m_fontMetrics = g.getFontMetrics();
	m_fontHeight = m_fontMetrics.getHeight();
      }
      if (!m_vertical) {
	return new Dimension(this.getSize().width, PAD+2+m_fontHeight);
      }
      return new Dimension(50, this.getSize().height);
    }

    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      this.setBackground(Color.black);
      if (m_fontMetrics == null) {
	m_fontMetrics = g.getFontMetrics();
	m_fontHeight = m_fontMetrics.getHeight();
      }

      Dimension d = this.getSize();
      Dimension d2 = m_boundaryPanel.getSize();
      g.setColor(Color.gray);
      int hf = m_fontMetrics.getAscent();
      if (!m_vertical) {
	g.drawLine(d.width, PAD, d.width-d2.width, PAD);
	// try and draw some scale values
	if (getInstances() != null) {
	  int precisionXmax = 1;
	  int precisionXmin = 1;
	  int whole = (int)Math.abs(m_maxX);
	  double decimal = Math.abs(m_maxX) - whole;
	  int nondecimal;
	  nondecimal = (whole > 0) 
	    ? (int)(Math.log(whole) / Math.log(10))
	    : 1;
	  
	  precisionXmax = (decimal > 0) 
	    ? (int)Math.abs(((Math.log(Math.abs(m_maxX)) / 
			      Math.log(10))))+2
	    : 1;
	  if (precisionXmax > MAX_PRECISION) {
	    precisionXmax = 1;
	  }
	  String maxStringX = Utils.doubleToString(m_maxX,
						   nondecimal+1+precisionXmax
						   ,precisionXmax);
	  
	  whole = (int)Math.abs(m_minX);
	  decimal = Math.abs(m_minX) - whole;
	  nondecimal = (whole > 0) 
	    ? (int)(Math.log(whole) / Math.log(10))
	    : 1;
	  precisionXmin = (decimal > 0) 
	    ? (int)Math.abs(((Math.log(Math.abs(m_minX)) / 
			      Math.log(10))))+2
	    : 1;
	  if (precisionXmin > MAX_PRECISION) {
	    precisionXmin = 1;
	  }
	  
	  String minStringX = Utils.doubleToString(m_minX,
						   nondecimal+1+precisionXmin,
						   precisionXmin);
	  g.drawString(minStringX,  d.width-d2.width, PAD+hf+2);
	  int maxWidth = m_fontMetrics.stringWidth(maxStringX);
	  g.drawString(maxStringX, d.width-maxWidth, PAD+hf+2);
	}
      } else {
	g.drawLine(d.width-PAD, 0, d.width-PAD, d2.height);
	// try and draw some scale values
	if (getInstances() != null) {
	  int precisionYmax = 1;
	  int precisionYmin = 1;
	  int whole = (int)Math.abs(m_maxY);
	  double decimal = Math.abs(m_maxY) - whole;
	  int nondecimal;
	  nondecimal = (whole > 0) 
	    ? (int)(Math.log(whole) / Math.log(10))
	    : 1;
	  
	  precisionYmax = (decimal > 0) 
	    ? (int)Math.abs(((Math.log(Math.abs(m_maxY)) / 
			      Math.log(10))))+2
	    : 1;
	  if (precisionYmax > MAX_PRECISION) {
	    precisionYmax = 1;
	  }
	  String maxStringY = Utils.doubleToString(m_maxY,
						   nondecimal+1+precisionYmax
						   ,precisionYmax);
	  
	  whole = (int)Math.abs(m_minY);
	  decimal = Math.abs(m_minY) - whole;
	  nondecimal = (whole > 0) 
	    ? (int)(Math.log(whole) / Math.log(10))
	    : 1;
	  precisionYmin = (decimal > 0) 
	    ? (int)Math.abs(((Math.log(Math.abs(m_minY)) / 
			      Math.log(10))))+2
	    : 1;
	  if (precisionYmin > MAX_PRECISION) {
	    precisionYmin = 1;
	  }
	  
	  String minStringY = Utils.doubleToString(m_minY,
						   nondecimal+1+precisionYmin,
						   precisionYmin);
	  int maxWidth = m_fontMetrics.stringWidth(minStringY);
	  g.drawString(minStringY,  d.width-PAD-maxWidth-2, d2.height);
	  maxWidth = m_fontMetrics.stringWidth(maxStringY);
	  g.drawString(maxStringY, d.width-PAD-maxWidth-2, hf);
	}
      }
    }
  }

  // the training instances
  private Instances m_trainingInstances;

  // the classifier to use
  private Classifier m_classifier;

  // plot area dimensions
  protected int m_plotAreaWidth = 512;
  protected int m_plotAreaHeight = 384;

  // the plotting panel
  protected BoundaryPanel m_boundaryPanel;

  // combo boxes for selecting the class attribute, class values (for
  // colouring pixels), and visualization attributes
  protected JComboBox m_classAttBox = new JComboBox();
  protected JComboBox m_xAttBox = new JComboBox();
  protected JComboBox m_yAttBox = new JComboBox();

  protected Dimension COMBO_SIZE = 
    new Dimension(m_plotAreaWidth / 2,
		  m_classAttBox.getPreferredSize().height);

  protected JButton m_startBut = new JButton("Start");

  protected JCheckBox m_plotTrainingData = new JCheckBox("Plot training data");

  protected JPanel m_controlPanel;

  protected ClassPanel m_classPanel = new ClassPanel();

  // separate panels for rendering axis information
  private AxisPanel m_xAxisPanel;
  private AxisPanel m_yAxisPanel;

  // min and max values for visualization dimensions
  private double m_maxX;
  private double m_maxY;
  private double m_minX;
  private double m_minY;

  private int m_xIndex;
  private int m_yIndex;

  // Kernel density estimator/generator
  private KDDataGenerator m_dataGenerator;

  // number of samples per pixel (fixed dimensions only)
  private int m_numberOfSamplesFromEachRegion;

  // base for sampling in the non-fixed dimensions
  private int m_generatorSamplesBase;

  // Set the kernel bandwidth to cover this many nearest neighbours
  private int m_kernelBandwidth;
  
  private JTextField m_regionSamplesText = 
    new JTextField(""+0);

  private JTextField m_generatorSamplesText = 
    new JTextField(""+0);

  private JTextField m_kernelBandwidthText = 
    new JTextField(""+3+"  ");


  /**
   * Creates a new <code>BoundaryVisualizer</code> instance.
   */
  public BoundaryVisualizer() {
    
    setLayout(new BorderLayout());
    m_classAttBox.setMinimumSize(COMBO_SIZE);
    m_classAttBox.setPreferredSize(COMBO_SIZE);
    m_classAttBox.setMaximumSize(COMBO_SIZE);
    m_classAttBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	m_classPanel.setCindex(m_classAttBox.getSelectedIndex());
      }
      });
				    

    m_xAttBox.setMinimumSize(COMBO_SIZE);
    m_xAttBox.setPreferredSize(COMBO_SIZE);
    m_xAttBox.setMaximumSize(COMBO_SIZE);

    m_yAttBox.setMinimumSize(COMBO_SIZE);
    m_yAttBox.setPreferredSize(COMBO_SIZE);
    m_yAttBox.setMaximumSize(COMBO_SIZE);

    m_classPanel.setMinimumSize(new 
      Dimension((int)COMBO_SIZE.getWidth()*2, 
		(int)COMBO_SIZE.getHeight()*2));
    m_classPanel.setPreferredSize(new 
      Dimension((int)COMBO_SIZE.getWidth()*2, 
		(int)COMBO_SIZE.getHeight()*2));


    m_controlPanel = new JPanel();
    m_controlPanel.setLayout(new BorderLayout());

    JPanel cHolder = new JPanel();
    cHolder.setBorder(BorderFactory.createTitledBorder("Class Attribute"));
    cHolder.add(m_classAttBox);

    JPanel vAttHolder = new JPanel();
    vAttHolder.setLayout(new GridLayout(2,1));
    vAttHolder.setBorder(BorderFactory.
			 createTitledBorder("Visualization Attributes"));
    vAttHolder.add(m_xAttBox);
    vAttHolder.add(m_yAttBox);

    JPanel colOne = new JPanel();
    colOne.setLayout(new BorderLayout());
    colOne.add(cHolder, BorderLayout.NORTH);
    colOne.add(vAttHolder, BorderLayout.CENTER);

    JPanel tempPanel = new JPanel();
    tempPanel.setBorder(BorderFactory.
			createTitledBorder("Sampling control"));
    tempPanel.setLayout(new GridLayout(3,1));

    JPanel colTwo = new JPanel();
    colTwo.setLayout(new BorderLayout());
    JPanel gsP = new JPanel(); gsP.setLayout(new BorderLayout());
    gsP.add(new JLabel(" Base for sampling (r)"), BorderLayout.CENTER);
    gsP.add(m_generatorSamplesText, BorderLayout.WEST);
    tempPanel.add(gsP);

    JPanel rsP = new JPanel(); rsP.setLayout(new BorderLayout());
    rsP.add(new JLabel(" Num. locations per pixel"), BorderLayout.CENTER);
    rsP.add(m_regionSamplesText, BorderLayout.WEST);
    tempPanel.add(rsP);

    JPanel ksP = new JPanel(); ksP.setLayout(new BorderLayout());
    ksP.add(new JLabel(" Kernel bandwidth (k)"), BorderLayout.CENTER);
    ksP.add(m_kernelBandwidthText, BorderLayout.WEST);
    tempPanel.add(ksP);
    
    colTwo.add(tempPanel, BorderLayout.NORTH);

    JPanel startPanel = new JPanel();
    startPanel.setBorder(BorderFactory.
			 createTitledBorder("Plotting"));
    startPanel.setLayout(new BorderLayout());
    startPanel.add(m_startBut, BorderLayout.CENTER);
    startPanel.add(m_plotTrainingData, BorderLayout.WEST);

    colTwo.add(startPanel, BorderLayout.SOUTH);

    m_controlPanel.add(colOne, BorderLayout.WEST);
    m_controlPanel.add(colTwo, BorderLayout.CENTER);
    JPanel classHolder = new JPanel();
    classHolder.setBorder(BorderFactory.createTitledBorder("Class color"));
    classHolder.add(m_classPanel);
    m_controlPanel.add(classHolder, BorderLayout.SOUTH);

    add(m_controlPanel, BorderLayout.NORTH);

    m_boundaryPanel = new BoundaryPanel(m_plotAreaWidth, m_plotAreaHeight);
    m_numberOfSamplesFromEachRegion = m_boundaryPanel.getNumSamplesPerRegion();
    m_regionSamplesText.setText(""+m_numberOfSamplesFromEachRegion+"  ");
    m_generatorSamplesBase = (int)m_boundaryPanel.getGeneratorSamplesBase();
    m_generatorSamplesText.setText(""+m_generatorSamplesBase+"  ");

    m_dataGenerator = new KDDataGenerator();
    m_kernelBandwidth = m_dataGenerator.getKernelBandwidth();
    m_kernelBandwidthText.setText(""+m_kernelBandwidth+"  ");
    m_boundaryPanel.setDataGenerator(m_dataGenerator);
    add(m_boundaryPanel, BorderLayout.CENTER);

    m_xAxisPanel = new AxisPanel(false);
    add(m_xAxisPanel, BorderLayout.SOUTH);
    m_yAxisPanel = new AxisPanel(true);
    add(m_yAxisPanel, BorderLayout.WEST);

    m_startBut.setEnabled(false);
    m_startBut.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  if (m_startBut.getText().equals("Start")) {
	    if (m_trainingInstances != null && m_classifier != null) {
	      try {
		int tempSamples = m_numberOfSamplesFromEachRegion;
		try {
		  tempSamples = 
		    Integer.parseInt(m_regionSamplesText.getText().trim());
		} catch (Exception ex) {
		  m_regionSamplesText.setText(""+tempSamples);
		}
		m_numberOfSamplesFromEachRegion = tempSamples;
		m_boundaryPanel.
		  setNumSamplesPerRegion(tempSamples);

		tempSamples = m_generatorSamplesBase;
		try {
		  tempSamples = 
		    Integer.parseInt(m_generatorSamplesText.getText().trim());
		} catch (Exception ex) {
		  m_generatorSamplesText.setText(""+tempSamples);
		}
		m_generatorSamplesBase = tempSamples;
		m_boundaryPanel.setGeneratorSamplesBase((double)tempSamples);

		tempSamples = m_kernelBandwidth;
		try {
		  tempSamples = 
		    Integer.parseInt(m_kernelBandwidthText.getText().trim());
		} catch (Exception ex) {
		  m_kernelBandwidthText.setText(""+tempSamples);
		}
		m_kernelBandwidth = tempSamples;
		m_dataGenerator.setKernelBandwidth(tempSamples);

		m_trainingInstances.
		  setClassIndex(m_classAttBox.getSelectedIndex());
		m_boundaryPanel.setClassifier(m_classifier);
		m_boundaryPanel.setTrainingData(m_trainingInstances);
		m_boundaryPanel.setXAttribute(m_xIndex);
		m_boundaryPanel.setYAttribute(m_yIndex);
		m_boundaryPanel.
		  setPlotTrainingData(m_plotTrainingData.isSelected());
		m_boundaryPanel.start();
		m_startBut.setText("Stop");
		setControlEnabledStatus(false);
	      } catch (Exception ex) {
		ex.printStackTrace();
	      }
	    }
	  } else {
	    m_boundaryPanel.stopPlotting();
	    m_startBut.setText("Start");
	    setControlEnabledStatus(true);
	  }
	}
      });

    m_boundaryPanel.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  m_startBut.setText("Start");
	  setControlEnabledStatus(true);
	}
      });

    m_classPanel.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {

	  try {
	    // save color vector to a file
	    FastVector colors = m_boundaryPanel.getColors();
	    FileOutputStream fos = new FileOutputStream("colors.ser");
	    ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject(colors);
	    oos.flush();
	    oos.close();
	  } catch (Exception ex) {}

	  m_boundaryPanel.replot();
	}
      });
  }

  /**
   * Set the enabled status of the controls
   *
   * @param status a <code>boolean</code> value
   */
  private void setControlEnabledStatus(boolean status) {
    m_classAttBox.setEnabled(status);
    m_xAttBox.setEnabled(status);
    m_yAttBox.setEnabled(status);
    m_regionSamplesText.setEnabled(status);
    m_generatorSamplesText.setEnabled(status);
    m_kernelBandwidthText.setEnabled(status);
    m_plotTrainingData.setEnabled(status);
  }

  /**
   * Set a classifier to use
   *
   * @param newClassifier the classifier to use
   * @exception Exception if an error occurs
   */
  public void setClassifier(Classifier newClassifier) throws Exception {

    m_classifier = newClassifier;
  }

  private void computeBounds() {
    String xName = (String)m_xAttBox.getSelectedItem();
    if (xName == null) {
      return;
    }
    xName = Utils.removeSubstring(xName, "X: ");
    xName = Utils.removeSubstring(xName, " (Num)");
    String yName = (String)m_yAttBox.getSelectedItem();
    yName = Utils.removeSubstring(yName, "Y: ");
    yName = Utils.removeSubstring(yName, " (Num)");

    m_xIndex = -1;
    m_yIndex = -1;
    for (int i = 0; i < m_trainingInstances.numAttributes(); i++) {
      if (m_trainingInstances.attribute(i).name().equals(xName)) {
	m_xIndex = i;
      } 
      if (m_trainingInstances.attribute(i).name().equals(yName)) {
	m_yIndex = i;
      }
    }

    if (m_xIndex != -1 && m_yIndex != -1) {
      // find the min and max values
      m_minX = Double.MAX_VALUE;
      m_minY = Double.MAX_VALUE;
      m_maxX = Double.MIN_VALUE;
      m_maxY = Double.MIN_VALUE;

      for (int i = 0; i < m_trainingInstances.numInstances(); i++) {
	Instance inst = m_trainingInstances.instance(i);
	if (!inst.isMissing(m_xIndex)) {
	  double value = inst.value(m_xIndex);
	  if (value < m_minX) {
	    m_minX = value;
	  }
	  if (value > m_maxX) {
	    m_maxX = value;
	  }
	}
	if (!inst.isMissing(m_yIndex)) {
	  double value = inst.value(m_yIndex);
	  if (value < m_minY) {
	    m_minY = value;
	  }
	  if (value > m_maxY) {
	    m_maxY = value;
	  }
	}
      }
    }
  }

  /**
   * Get the training instances
   *
   * @return the training instances
   */
  public Instances getInstances() {
    return m_trainingInstances;
  }

  /**
   * Set the training instances
   *
   * @param inst the instances to use
   */
  public void setInstances(Instances inst) throws Exception {
    if (inst.numAttributes() < 3) {
      throw new Exception("Not enough attributes in the data to visualize!");
    }
    m_trainingInstances = inst;
    m_classPanel.setInstances(m_trainingInstances);
    // setup combo boxes
    String [] classAttNames = new String [m_trainingInstances.numAttributes()];
    final Vector xAttNames = new Vector();
    Vector yAttNames = new Vector();

    for (int i = 0; i < m_trainingInstances.numAttributes(); i++) {
      classAttNames[i] = m_trainingInstances.attribute(i).name();
      String type = "";
      switch (m_trainingInstances.attribute(i).type()) {
	case Attribute.NOMINAL:
	  type = " (Nom)";
	  break;
	case Attribute.NUMERIC:
	  type = " (Num)";
	  break;
	case Attribute.STRING:
	  type = " (Str)";
	  break;
	case Attribute.DATE:
	  type = " (Dat)";
	  break;
	default:
	  type = " (???)";
      }
      classAttNames[i] += type;
      if (m_trainingInstances.attribute(i).isNumeric()) {
	xAttNames.addElement("X: "+classAttNames[i]);
	yAttNames.addElement("Y: "+classAttNames[i]);
      }
    }

    m_classAttBox.setModel(new DefaultComboBoxModel(classAttNames));
    m_xAttBox.setModel(new DefaultComboBoxModel(xAttNames));
    m_yAttBox.setModel(new DefaultComboBoxModel(yAttNames));
    if (xAttNames.size() > 1) {
      m_yAttBox.setSelectedIndex(1);
    }

    m_classAttBox.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  configureForClassAttribute();
	}
      });

    m_xAttBox.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent e) {
	  if (e.getStateChange() == ItemEvent.SELECTED) {
	    if (xAttNames.size() > 1) {
	      if (m_xAttBox.getSelectedIndex() == 
		  m_yAttBox.getSelectedIndex()) {
		m_xAttBox.setSelectedIndex((m_xAttBox.getSelectedIndex() + 1) %
					   xAttNames.size());
	      }
	    }
	    computeBounds();
	    repaint();
	  }
	}
      });

    m_yAttBox.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent e) {
	  if (e.getStateChange() == ItemEvent.SELECTED) {
	    if (xAttNames.size() > 1) {
	      if (m_yAttBox.getSelectedIndex() == 
		  m_xAttBox.getSelectedIndex()) {
		m_yAttBox.setSelectedIndex((m_yAttBox.getSelectedIndex() + 1) %
					   xAttNames.size());
	      }
	    }
	    computeBounds();
	    repaint();
	  }
	}
      });
    computeBounds();
    revalidate();
    repaint();
  }
  
  /**
   * Set up the class values combo boxes
   */
  private void configureForClassAttribute() {
    int classIndex = m_classAttBox.getSelectedIndex();
    if (classIndex >= 0) {
      // see if this is a nominal attribute
      if (!m_trainingInstances.attribute(classIndex).isNominal()) {
	m_startBut.setEnabled(false);
      } else {
	m_startBut.setEnabled(true);
	// set up class colours
	FastVector colors = new FastVector();
	for (int i = 0; i < 
	       m_trainingInstances.attribute(classIndex).numValues(); i++) {
	  colors.addElement(BoundaryPanel.
		    DEFAULT_COLORS[i % BoundaryPanel.DEFAULT_COLORS.length]);
	  m_classPanel.setColours(colors);	  
	  m_boundaryPanel.setColors(colors);
	}
      }
    }
  }

  /**
   * Main method for testing this class
   *
   * @param args a <code>String[]</code> value
   */
  public static void main(String [] args) {

    try {
      if (args.length < 2) {
	System.err.println("Usage : BoundaryPanel <dataset> <classifier "
			   +"[classifier options]>");
	System.exit(1);
      }
      final javax.swing.JFrame jf = 
	new javax.swing.JFrame("Weka classification boundary visualizer");
      jf.getContentPane().setLayout(new BorderLayout());
      BoundaryVisualizer bv = new BoundaryVisualizer();
      jf.getContentPane().add(bv, BorderLayout.CENTER);
      jf.setSize(bv.getMinimumSize());
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
	  public void windowClosing(java.awt.event.WindowEvent e) {
	    jf.dispose();
	    System.exit(0);
	  }
	});

      jf.pack();
      jf.setVisible(true);
      jf.setResizable(false);
      Dimension t = jf.getSize();

      System.err.println("Loading instances from : "+args[0]);
      java.io.Reader r = new java.io.BufferedReader(
			 new java.io.FileReader(args[0]));
      Instances i = new Instances(r);
      bv.setInstances(i);
      String [] argsR = null;
      if (args.length > 2) {
	argsR = new String [args.length-2];
	for (int j = 2; j < args.length; j++) {
	  argsR[j-2] = args[j];
	}
      }
      Classifier c = Classifier.forName(args[1], argsR);
      bv.setClassifier(c);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}


