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
 *    UserClassifier.java
 *    Copyright (C) 1999 Malcolm Ware
 *
 */

package weka.classifiers.trees;

import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.core.Capabilities;
import weka.core.Drawable;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyDialog;
import weka.gui.treevisualizer.PlaceNode1;
import weka.gui.treevisualizer.PlaceNode2;
import weka.gui.treevisualizer.TreeDisplayEvent;
import weka.gui.treevisualizer.TreeDisplayListener;
import weka.gui.treevisualizer.TreeVisualizer;
import weka.gui.visualize.VisualizePanel;
import weka.gui.visualize.VisualizePanelEvent;
import weka.gui.visualize.VisualizePanelListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serializable;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;


/**
 <!-- globalinfo-start -->
 * Interactively classify through visual means. You are Presented with a scatter graph of the data against two user selectable attributes, as well as a view of the decision tree. You can create binary splits by creating polygons around data plotted on the scatter graph, as well as by allowing another classifier to take over at points in the decision tree should you see fit.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * Malcolm Ware, Eibe Frank, Geoffrey Holmes, Mark Hall, Ian H. Witten (2001). Interactive machine learning: letting users build classifiers. Int. J. Hum.-Comput. Stud.. 55(3):281-292.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Ware2001,
 *    author = {Malcolm Ware and Eibe Frank and Geoffrey Holmes and Mark Hall and Ian H. Witten},
 *    journal = {Int. J. Hum.-Comput. Stud.},
 *    number = {3},
 *    pages = {281-292},
 *    title = {Interactive machine learning: letting users build classifiers},
 *    volume = {55},
 *    year = {2001},
 *    PS = {http://www.cs.waikato.ac.nz/~ml/publications/2000/00MW-etal-Interactive-ML.ps}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * @author Malcolm Ware (mfw4@cs.waikato.ac.nz)
 * @version $Revision: 1.26 $
 */
public class UserClassifier 
  extends Classifier 
  implements Drawable, TreeDisplayListener, VisualizePanelListener,
             TechnicalInformationHandler {
  
  /** for serialization */
  static final long serialVersionUID = 6483901103562809843L;

  /** I am not sure if these are strictly adhered to in visualizepanel
   * so I am making them private to avoid confusion, (note that they will
   * be correct in this class, VLINE and HLINE aren't used).
   */
  private static final int LEAF = 0;
  private static final int RECTANGLE = 1;
  private static final int POLYGON = 2;
  private static final int POLYLINE = 3;
  private static final int VLINE = 5;
  private static final int HLINE =6;
  

  /** The tree display panel. */
  private transient TreeVisualizer m_tView = null;
  /** The instances display. */
  private transient VisualizePanel m_iView = null;
  /** Two references to the structure of the decision tree. */
  private TreeClass m_top, m_focus;
  /** The next number that can be used as a unique id for a node. */
  private int m_nextId;
  /** The tabbed window for the tree and instances view. */
  private transient JTabbedPane m_reps;
  /** The window. */
  private transient JFrame m_mainWin;
  /** The status of whether there is a decision tree ready or not. */
  private boolean m_built=false;
  /** A list of other m_classifiers. */
  private GenericObjectEditor m_classifiers;
  /** A window for selecting other classifiers. */
  private PropertyDialog m_propertyDialog;

  /** Register the property editors we need */
  static {
     GenericObjectEditor.registerEditors();
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain command line options (see setOptions)
   */
  public static void main(String [] argv) {
    runClassifier(new UserClassifier(), argv);
  }

  /**
   * @return a string that represents this objects tree.
   */
  public String toString() {
    if (!m_built) {

      return "Tree Not Built";
    }
    StringBuffer text = new StringBuffer();
    try {
      m_top.toString(0, text);
      
      m_top.objectStrings(text);

    } catch(Exception e) {
      System.out.println("error: " + e.getMessage());
    }
    
    return text.toString();
  }

  /**
   * Receives user choices from the tree view, and then deals with these 
   * choices. 
   * @param e The choice. 
   */
  public void userCommand(TreeDisplayEvent e) {
    
    if (m_propertyDialog != null) {
      m_propertyDialog.dispose();
      m_propertyDialog = null;
    }
    try {
      if (m_iView == null || m_tView == null) {
	//throw exception
      }
      if (e.getCommand() == TreeDisplayEvent.NO_COMMAND) {
	//do nothing
      }
      else if (e.getCommand() == TreeDisplayEvent.ADD_CHILDREN) {
	//highlight the particular node and reset the vis panel
	if (m_top == null) {
	  //this shouldn't happen , someone elses code would 
	  //have to have added a trigger to this listener.
	  System.out.println("Error : Received event from a TreeDisplayer"
			     + " that is unknown to the classifier.");
	}
	else {
	  m_tView.setHighlight(e.getID());
	  /*if (m_iView == null)
	    {
	    m_iView = new VisualizePanel(this);
	    m_iView.setSize(400, 300);
	    }*/
	  m_focus = m_top.getNode(e.getID());
	  m_iView.setInstances(m_focus.m_training);
	  if (m_focus.m_attrib1 >= 0) {
	    m_iView.setXIndex(m_focus.m_attrib1);
	  }
	  if (m_focus.m_attrib2 >= 0) {
	    m_iView.setYIndex(m_focus.m_attrib2);
	  }
	  m_iView.setColourIndex(m_focus.m_training.classIndex());
	  if (((Double)((FastVector)m_focus.m_ranges.elementAt(0)).
	       elementAt(0)).intValue() != LEAF) {
	    m_iView.setShapes(m_focus.m_ranges);
	  }
	  //m_iView.setSIndex(2);
	}
      }
      else if (e.getCommand() == TreeDisplayEvent.REMOVE_CHILDREN) {
	/*if (m_iView == null)
	  {
	  m_iView = new VisualizePanel(this);
	  m_iView.setSize(400, 300);
	  }*/
	m_focus = m_top.getNode(e.getID());
	m_iView.setInstances(m_focus.m_training);
	if (m_focus.m_attrib1 >= 0) {
	  m_iView.setXIndex(m_focus.m_attrib1);
	}
	if (m_focus.m_attrib2 >= 0) {
	  m_iView.setYIndex(m_focus.m_attrib2);
	}
	m_iView.setColourIndex(m_focus.m_training.classIndex());
	if (((Double)((FastVector)m_focus.m_ranges.elementAt(0)).
	     elementAt(0)).intValue() != LEAF) {
	  m_iView.setShapes(m_focus.m_ranges);
	}
	//m_iView.setSIndex(2);
	//now to remove all the stuff
	m_focus.m_set1 = null;
	m_focus.m_set2 = null;
	m_focus.setInfo(m_focus.m_attrib1, m_focus.m_attrib2, null);
	//tree_frame.getContentPane().removeAll();
	m_tView = new TreeVisualizer(this, graph(), new PlaceNode2());
	//tree_frame.getContentPane().add(m_tView);
	m_reps.setComponentAt(0, m_tView);
	//tree_frame.getContentPane().doLayout();
	m_tView.setHighlight(m_focus.m_identity);
      }
      else if (e.getCommand() == TreeDisplayEvent.CLASSIFY_CHILD) {
	/*if (m_iView == null)
	  {
	  m_iView = new VisualizePanel(this);
	  m_iView.setSize(400, 300);
	  }*/
	m_focus = m_top.getNode(e.getID());
	m_iView.setInstances(m_focus.m_training);
	if (m_focus.m_attrib1 >= 0) {
	  m_iView.setXIndex(m_focus.m_attrib1);
	}
	if (m_focus.m_attrib2 >= 0) {
	  m_iView.setYIndex(m_focus.m_attrib2);
	}
	m_iView.setColourIndex(m_focus.m_training.classIndex());
	if (((Double)((FastVector)m_focus.m_ranges.elementAt(0)).
	     elementAt(0)).intValue() != LEAF) {
	  m_iView.setShapes(m_focus.m_ranges);
	}
	
	
	m_propertyDialog = new PropertyDialog(m_classifiers, 
					      m_mainWin.getLocationOnScreen().x,
					      m_mainWin.getLocationOnScreen().y);
	
	//note property dialog may change all the time
	//but the generic editor which has the listeners does not
	//so at the construction of the editor is when I am going to add
	//the listeners.
	
	
	
	//focus.setClassifier(new IB1());
	//tree_frame.getContentPane().removeAll();
	//////m_tView = new Displayer(this, graph(), new PlaceNode2());
	//tree_frame.getContentPane().add(m_tView);
	//tree_frame.getContentPane().doLayout();
	/////////////reps.setComponentAt(0, m_tView);
	m_tView.setHighlight(m_focus.m_identity);
      }
      /*else if (e.getCommand() == e.SEND_INSTANCES) {
	TreeClass source = m_top.getNode(e.getID());
	m_iView.setExtInstances(source.m_training);
	}*/
      else if (e.getCommand() == TreeDisplayEvent.ACCEPT) {
	
	int well = JOptionPane.showConfirmDialog(m_mainWin, 
						 "Are You Sure...\n"
						 + "Click Yes To Accept The"
						 + " Tree" 
						 + "\n Click No To Return",
						 "Accept Tree", 
						 JOptionPane.YES_NO_OPTION);
	
	if (well == 0) {
	  m_mainWin.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	  m_mainWin.dispose();
	  blocker(false);  //release the thread waiting at blocker to 
	  //continue.
	}
	
      }
    } catch(Exception er) {
      System.out.println("Error : " + er);
      System.out.println("Part of user input so had to catch here");
      er.printStackTrace();
    }
  }

  /**
   * This receives shapes from the data view. 
   * It then enters these shapes into the decision tree structure. 
   * @param e Contains the shapes, and other info.
   */
  public void userDataEvent(VisualizePanelEvent e) {
    
    if (m_propertyDialog != null) {
      m_propertyDialog.dispose();
      m_propertyDialog = null;
    }
    
    try {
      if (m_focus != null) {
	

	double wdom = e.getInstances1().numInstances() 
	  + e.getInstances2().numInstances();
	if (wdom == 0) {
	  wdom = 1;
	}
	
	TreeClass tmp = m_focus;
	m_focus.m_set1 = new TreeClass(null, e.getAttribute1(), 
				       e.getAttribute2(), m_nextId, 
				       e.getInstances1().numInstances() / wdom,
 				       e.getInstances1(), m_focus);
	
	m_focus.m_set2 = new TreeClass(null, e.getAttribute1(), 
				       e.getAttribute2(), m_nextId, 
				       e.getInstances2().numInstances() / wdom,
				       e.getInstances2(), m_focus); 
	//this needs the other instance
	
	
	//tree_frame.getContentPane().removeAll();  
	m_focus.setInfo(e.getAttribute1(), e.getAttribute2(), e.getValues());
	//System.out.println(graph());
	m_tView = new TreeVisualizer(this, graph(), new PlaceNode2());
	//tree_frame.getContentPane().add(m_tView);
	//tree_frame.getContentPane().doLayout();
	m_reps.setComponentAt(0, m_tView);
	
	m_focus = m_focus.m_set2;
	m_tView.setHighlight(m_focus.m_identity);
	m_iView.setInstances(m_focus.m_training);
	if (tmp.m_attrib1 >= 0) {
	  m_iView.setXIndex(tmp.m_attrib1);
	}
	if (tmp.m_attrib2 >= 0) {
	  m_iView.setYIndex(tmp.m_attrib2);
	}
	m_iView.setColourIndex(m_focus.m_training.classIndex());
	if (((Double)((FastVector)m_focus.m_ranges.elementAt(0)).
	     elementAt(0)).intValue() != LEAF) {
	  m_iView.setShapes(m_focus.m_ranges);
	}
	//m_iView.setSIndex(2);
      }
      else {
	System.out.println("Somehow the focus is null");
      }
    } catch(Exception er) {
      System.out.println("Error : " + er);
      System.out.println("Part of user input so had to catch here");
      //er.printStackTrace();
    }
    
  }
  
  /** 
   * Constructor
   */
  public UserClassifier() {
    //do nothing here except set alot of variables to default values
    m_top = null;
    m_tView = null;
    m_iView = null;
    m_nextId = 0; 
    
  }
  
 /**
   *  Returns the type of graph this classifier
   *  represents.
   *  @return Drawable.TREE
   */   
  public int graphType() {
      return Drawable.TREE;
  }

  /**
   * @return A string formatted with a dotty representation of the decision
   * tree.
   * @throws Exception if String can't be built properly.
   */
  public String graph() throws Exception {
    //create a dotty rep of the tree from here
    StringBuffer text = new StringBuffer();
    text.append("digraph UserClassifierTree {\n" +
		"node [fontsize=10]\n" +
		"edge [fontsize=10 style=bold]\n");
    
    m_top.toDotty(text);
    return text.toString() +"}\n";
    
    
  }
  
  /**
   * A function used to stop the code that called buildclassifier
   * from continuing on before the user has finished the decision tree.
   * @param tf True to stop the thread, False to release the thread that is
   * waiting there (if one).
   */
  private synchronized void blocker(boolean tf) {
    if (tf) {
      try {
	wait();
      } catch(InterruptedException e) {
      }
    }
    else {
      notifyAll();
    }
    
    //System.out.println("out");
  }

  /**
   * This will return a string describing the classifier.
   * @return The string.
   */
  public String globalInfo() {

    return "Interactively classify through visual means."
      + " You are Presented with a scatter graph of the data against two user"
      + " selectable attributes, as well as a view of the decision tree."
      + " You can create binary splits by creating polygons around data"
      + " plotted on the scatter graph, as well as by allowing another"
      + " classifier to take over at points in the decision tree should you"
      + " see fit.\n\n"
      + "For more information see:\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    
    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Malcolm Ware and Eibe Frank and Geoffrey Holmes and Mark Hall and Ian H. Witten");
    result.setValue(Field.YEAR, "2001");
    result.setValue(Field.TITLE, "Interactive machine learning: letting users build classifiers");
    result.setValue(Field.JOURNAL, "Int. J. Hum.-Comput. Stud.");
    result.setValue(Field.VOLUME, "55");
    result.setValue(Field.NUMBER, "3");
    result.setValue(Field.PAGES, "281-292");
    result.setValue(Field.PS, "http://www.cs.waikato.ac.nz/~ml/publications/2000/00MW-etal-Interactive-ML.ps");
    
    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.STRING_ATTRIBUTES);
    result.enable(Capability.RELATIONAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);
    
    return result;
  }

  /**
   * Call this function to build a decision tree for the training
   * data provided.
   * @param i The training data.
   * @throws Exception if can't build classification properly.
   */
  public void buildClassifier(Instances i) throws Exception {
    // can classifier handle the data?
    getCapabilities().testWithFail(i);

    // remove instances with missing class
    i = new Instances(i);
    i.deleteWithMissingClass();
    
    //construct a visualizer
    //construct a tree displayer and feed both then do nothing
    //note that I will display at the bottom of each split how many 
    //fall into each catagory
    
    m_classifiers = new GenericObjectEditor(true);
    m_classifiers.setClassType(Classifier.class);
    m_classifiers.setValue(new weka.classifiers.rules.ZeroR());
    
    ((GenericObjectEditor.GOEPanel)m_classifiers.getCustomEditor())
      .addOkListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    //I want to use the focus variable but to trust it I need
	    //to kill the window if anything gets changed by either
	    //editor
	    try {
	      m_focus.m_set1 = null;
	      m_focus.m_set2 = null;
	      m_focus.setInfo(m_focus.m_attrib1, m_focus.m_attrib2, null);
	      m_focus.setClassifier((Classifier)m_classifiers.getValue());
	      m_classifiers = new GenericObjectEditor();
	      m_classifiers.setClassType(Classifier.class);
	      m_classifiers.setValue(new weka.classifiers.rules.ZeroR());
	      ((GenericObjectEditor.GOEPanel)m_classifiers.getCustomEditor())
		.addOkListener(this);
	      m_tView = new TreeVisualizer(UserClassifier.this, graph(), 
				     new PlaceNode2());
	      m_tView.setHighlight(m_focus.m_identity);
	      m_reps.setComponentAt(0, m_tView);
	      m_iView.setShapes(null);
	    } catch(Exception er) {
	      System.out.println("Error : " + er);
	      System.out.println("Part of user input so had to catch here");
	    }
	  }
	});
    
    m_built = false;
    m_mainWin = new JFrame();
    
    m_mainWin.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  int well = JOptionPane.showConfirmDialog(m_mainWin, 
						   "Are You Sure...\n"
						   + "Click Yes To Accept"
						   + " The Tree" 
						   + "\n Click No To Return",
						   "Accept Tree", 
						   JOptionPane.YES_NO_OPTION);
	  
	  if (well == 0) {
	    m_mainWin.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	    blocker(false);
	    
	  }
	  else {
	    m_mainWin.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	  }
	}
      });
    
    m_reps = new JTabbedPane();
    m_mainWin.getContentPane().add(m_reps);
    
    //make a backup of the instances so that any changes don't go past here.
    Instances te = new Instances(i, i.numInstances());
    for (int noa = 0; noa < i.numInstances(); noa++) {
      te.add(i.instance(noa));
    }
    
    te.deleteWithMissingClass(); //remove all instances with a missing class
    //from training
    
    m_top = new TreeClass(null, 0, 0, m_nextId, 1, te, null);
    m_focus = m_top;
    //System.out.println(graph());
    m_tView = new TreeVisualizer(this, graph(), new PlaceNode1());
    
    m_reps.add("Tree Visualizer", m_tView);
    //tree_frame = new JFrame();
    //tree_frame.getContentPane().add(m_tView);
    //tree_frame.setSize(800,600);
    //tree_frame.setVisible(true);
    
    m_tView.setHighlight(m_top.m_identity);
    m_iView = new VisualizePanel(this);
    //m_iView.setSize(400, 300);
    m_iView.setInstances(m_top.m_training);
    m_iView.setColourIndex(te.classIndex());
    //vis_frame = new JFrame();
    //vis_frame.getContentPane().add(m_iView);
    //vis_frame.setSize(400, 300);
    //vis_frame.setVisible(true);
    m_reps.add("Data Visualizer", m_iView);
    m_mainWin.setSize(560, 420);
    m_mainWin.setVisible(true);
    blocker(true);          //a call so that the main thread of 
    //execution has to wait for the all clear message from the user.
    
    //so that it can be garbage 
    if (m_propertyDialog != null) {
      m_propertyDialog.dispose();
      m_propertyDialog = null;
    }
    
    //collected
    m_classifiers = null;
    m_built = true;
  }

  /**
   * Call this function to get a double array filled with the probability
   * of how likely each class type is the class of the instance.
   * @param i The instance to classify.
   * @return A double array filled with the probalities of each class type.
   * @throws Exception if can't classify instance.
   */
  public double[] distributionForInstance(Instance i) throws Exception {

    if (!m_built) {
      return null;
    }
    
    double[] res = m_top.calcClassType(i);
    if (m_top.m_training.classAttribute().isNumeric()) {
      return res;
    }

    double most_likely = 0, highest = -1;
    double count = 0;
    for (int noa = 0; noa < m_top.m_training.numClasses(); noa++) {
      count += res[noa];
      if (res[noa] > highest) {
	most_likely = noa;
	highest = res[noa];
      }
    }
    
    if (count <= 0) {
      //not sure how this happened.
      return null;
    }

    for (int noa = 0; noa < m_top.m_training.numClasses(); noa++) {
      res[noa] = res[noa] / count;
    }
    //System.out.println("ret");
    
    return res;
  }
  
  /**
   * Inner class used to represent the actual decision tree structure and data.
   */
  private class TreeClass 
    implements Serializable {
    
    /** for serialization */
    static final long serialVersionUID = 595663560871347434L;
    
    /**
     * This contains the info for the coords of the shape converted 
     * to attrib coords, 
     * for polygon the first attrib is the number of points, 
     * This is not more object oriented because that would 
     * be over kill.
     */
    public FastVector m_ranges;

    /** the first attribute */
    public int m_attrib1;
    
    /** the second attribute */
    public int m_attrib2;
    
    public TreeClass m_set1;
    public TreeClass m_set2;

    /** the parent */
    public TreeClass m_parent;

    /** A string to uniquely identify this node. */
    public String m_identity;
    
    /** the weight of this node */
    public double m_weight;
    
    /** the training instances for this node */
    public Instances m_training;
    
    /** Used instead of the standard leaf if one exists. */
    public Classifier m_classObject;

    /** Used on the instances while classifying if one exists. */
    public Filter m_filter;
    
    /**
     * Constructs a TreeClass node  with all the important information.
     * @param r A FastVector containing the shapes, null if it's a leaf node.
     * @param a1 The first attribute.
     * @param a2 The second attribute.
     * @param id The unique id number for this node.
     * @param w The weight of this node.
     * @param i The instances that make it to this node from the training data.
     * @param p the parent
     * @throws Exception if can't use 'i' properly.
     */
    public TreeClass(FastVector r, int a1, int a2, int id, double w, 
		     Instances i, TreeClass p) throws Exception {
      m_set1 = null;
      m_set2 = null;
      m_ranges = r;
      m_classObject = null;
      m_filter = null;
      m_training = i;
      m_attrib1 = a1;
      m_attrib2 = a2;
      m_identity = "N" + String.valueOf(id);
      m_weight = w;
      m_parent = p;
      m_nextId++;
      if (m_ranges == null) {
	
	setLeaf();
	//this will fill the ranges array with the 
	//number of times each class type occurs for the instances.
	/*m_ranges = new FastVector(1);
	  m_ranges.addElement(new FastVector(i.numClasses() + 1));
	  FastVector tmp = (FastVector)m_ranges.elementAt(0);
	  tmp.addElement(new Double(0));
	  for (int noa = 0; noa < i.numClasses(); noa++) {
	  tmp.addElement(new Double(0));
	  }
	  for (int noa = 0; noa < i.numInstances(); noa++) {
	  tmp.setElementAt(new Double(((Double)tmp.elementAt
	  ((int)i.instance(noa).
	  classValue() + 1)).doubleValue() + 
	  i.instance(noa).weight()),
	  (int)i.instance(noa).classValue() + 1);  
	  //this gets the current class value and alters it and replaces it
	  }*/
      }     
    }
    
    /**
     * Call this to set an alternate classifier For this node.
     * @param c The alternative classifier to use.
     * @throws Exception if alternate classifier can't build classification.
     */
    public void setClassifier(Classifier c) throws Exception {
      m_classObject = c;
      m_classObject.buildClassifier(m_training);
    }
    
    /**
     * Call this to set this node with different information to what
     * it was created with.
     * @param at1 The first attribute.
     * @param at2 The second attribute.
     * @param ar The shapes at this node, null if leaf node, or 
     * alternate classifier.
     * @throws Exception if leaf node and cant't create leaf info.
     */
    public void setInfo(int at1, int at2, FastVector ar) throws Exception {
      m_classObject = null;
      m_filter = null;
      m_attrib1 = at1;
      m_attrib2 = at2;
      m_ranges = ar;
      
      //FastVector tmp;
      if (m_ranges == null) {
	setLeaf();
	/*
	//this will fill the ranges array with the number of times 
	//each class type occurs for the instances.
	  if (m_training != null) {
	    m_ranges = new FastVector(1);
	    m_ranges.addElement(new FastVector(m_training.numClasses() + 1));
	    tmp = (FastVector)m_ranges.elementAt(0);
	    tmp.addElement(new Double(0));
	    for (int noa = 0; noa < m_training.numClasses(); noa++) {
	      tmp.addElement(new Double(0));
	    }
	    for (int noa = 0; noa < m_training.numInstances(); noa++) {
	      tmp.setElementAt(new Double(((Double)tmp.elementAt
					   ((int)m_training.instance(noa).
					    classValue() + 1)).doubleValue() + 
					  m_training.instance(noa).weight()), 
			       (int)m_training.instance(noa).classValue() + 1);
	      //this gets the current class val and alters it and replaces it
	      }
	      }*/
      }
    }
    
    /**
     * This sets up the informtion about this node such as the s.d or the
     * number of each class.
     * @throws Exception if problem with training instances.
     */
    private void setLeaf() throws Exception {
      //this will fill the ranges array with the number of times 
      //each class type occurs for the instances.
      //System.out.println("ihere");
      if (m_training != null ) {
	
	if (m_training.classAttribute().isNominal()) {
	  FastVector tmp;
	  
	  //System.out.println("ehlpe");
	  m_ranges = new FastVector(1);
	  m_ranges.addElement(new FastVector(m_training.numClasses() + 1));
	  tmp = (FastVector)m_ranges.elementAt(0);
	  tmp.addElement(new Double(0));
	  for (int noa = 0; noa < m_training.numClasses(); noa++) {
	    tmp.addElement(new Double(0));
	  }
	  for (int noa = 0; noa < m_training.numInstances(); noa++) {
	    tmp.setElementAt(new Double(((Double)tmp.elementAt
					 ((int)m_training.instance(noa).
					  classValue() + 1)).doubleValue() + 
					m_training.instance(noa).weight()), 
			     (int)m_training.instance(noa).classValue() + 1);
	    //this gets the current class val and alters it and replaces it
	  }
	}
	else {
	  //then calc the standard deviation.
	  m_ranges = new FastVector(1);
	  double t1 = 0;
	  for (int noa = 0; noa < m_training.numInstances(); noa++) {
	    t1 += m_training.instance(noa).classValue();
	  }
	  
	  if (m_training.numInstances() != 0) {
	    t1 /= m_training.numInstances();
	  }
	  double t2 = 0;
	  for (int noa = 0; noa < m_training.numInstances(); noa++) {
	    t2 += Math.pow(m_training.instance(noa).classValue() - t1, 2);
	  }
	  FastVector tmp;
	  if (m_training.numInstances() != 0) {
	    t1 = Math.sqrt(t2 / m_training.numInstances());
	    m_ranges.addElement(new FastVector(2));
	    tmp = (FastVector)m_ranges.elementAt(0);
	    tmp.addElement(new Double(0));
	    tmp.addElement(new Double(t1));
	  }
	  else {
	    m_ranges.addElement(new FastVector(2));
	    tmp = (FastVector)m_ranges.elementAt(0);
	    tmp.addElement(new Double(0));
	    tmp.addElement(new Double(Double.NaN));
	  }
	}
      }
    }

    /**
     * This will recursively go through the tree and return inside the 
     * array the weightings of each of the class types
     * for this instance. Note that this function returns an otherwise 
     * unreferenced double array so there are no worry's about
     * making changes.
     *
     * @param i The instance to test
     * @return A double array containing the results.
     * @throws Exception if can't use instance i properly.
     */
    public double[] calcClassType(Instance i) throws Exception {
      //note that it will be the same calcs for both numeric and nominal
      //attrib types.
      //note the weightings for returning stuff will need to be modified 
      //to work properly but will do for now.
      double x = 0, y = 0;
      if (m_attrib1 >= 0) {
	x = i.value(m_attrib1);
      }
      if (m_attrib2 >= 0) {
	y = i.value(m_attrib2);
      }
      double[] rt;
      if (m_training.classAttribute().isNominal()) {
	rt = new double[m_training.numClasses()];
      }
      else {
	rt = new double[1];
      }

      FastVector tmp;
      if (m_classObject != null) {
	//then use the classifier.
	if (m_training.classAttribute().isNominal()) {
	  rt[(int)m_classObject.classifyInstance(i)] = 1;
	}
	else {
	  if (m_filter != null) {
	    m_filter.input(i);
	    rt[0] = m_classObject.classifyInstance(m_filter.output());
	  }
	  else {
	    rt[0] = m_classObject.classifyInstance(i);
	  }
	}
	//System.out.println("j48");
	return rt;
      }
      else if (((Double)((FastVector)m_ranges.elementAt(0)).
		elementAt(0)).intValue() == LEAF) {
	//System.out.println("leaf");
	//then this is a leaf
	//rt = new double[m_training.numClasses()];
	
	if (m_training.classAttribute().isNumeric()) {
	 
	  setLinear();
	  m_filter.input(i);
	  rt[0] = m_classObject.classifyInstance(m_filter.output());
	  return rt;
	}
	
	int totaler = 0;
	tmp = (FastVector)m_ranges.elementAt(0);
	for (int noa = 0; noa < m_training.numClasses();noa++) {
	  rt[noa] = ((Double)tmp.elementAt(noa + 1)).doubleValue();
	  totaler += rt[noa];
	}
	for (int noa = 0; noa < m_training.numClasses(); noa++) {
	  rt[noa] = rt[noa] / totaler;
	}
	return rt;
      }
      
      for (int noa = 0; noa < m_ranges.size(); noa++) {
	
	tmp = (FastVector)m_ranges.elementAt(noa);
	
	if (((Double)tmp.elementAt(0)).intValue() 
	    == VLINE && !Instance.isMissingValue(x)) {
	  
	}
	else if (((Double)tmp.elementAt(0)).intValue() 
		 == HLINE && !Instance.isMissingValue(y)) {
	  
	}
	else if (Instance.isMissingValue(x) || Instance.isMissingValue(y)) {
	  //System.out.println("miss");
	  //then go down both branches using their weights
	  rt = m_set1.calcClassType(i);
	  double[] tem = m_set2.calcClassType(i);
	  if (m_training.classAttribute().isNominal()) {
	    for (int nob = 0; nob < m_training.numClasses(); nob++) {
	      rt[nob] *= m_set1.m_weight;
	      rt[nob] += tem[nob] * m_set2.m_weight;
	    }
	  }
	  else {
	    rt[0] *= m_set1.m_weight;
	    rt[0] += tem[0] * m_set2.m_weight;
	  }
	  return rt;
	}
	else if (((Double)tmp.elementAt(0)).intValue() == RECTANGLE) {
	  //System.out.println("RECT");
	  if (x >= ((Double)tmp.elementAt(1)).doubleValue() && 
	      x <= ((Double)tmp.elementAt(3)).doubleValue() && 
	      y <= ((Double)tmp.elementAt(2)).doubleValue() && 
	      y >= ((Double)tmp.elementAt(4)).doubleValue()) {
	    //then falls inside the rectangle
	    //System.out.println("true");
	    rt = m_set1.calcClassType(i);
	    return rt;
	  }
	  
	}
	else if (((Double)tmp.elementAt(0)).intValue() == POLYGON) {
	  if (inPoly(tmp, x, y)) {
	    rt = m_set1.calcClassType(i);
	    return rt;
	  }
	}
	else if (((Double)tmp.elementAt(0)).intValue() == POLYLINE) {
	  if (inPolyline(tmp, x, y)) {
	    rt = m_set1.calcClassType(i);
	    return rt;
	  }
	}
      }
      //is outside the split
      if (m_set2 != null) {
	rt = m_set2.calcClassType(i);
      }
      return rt;
    }
    
    /**
     * This function gets called to set the node to use a linear regression
     * and attribute filter.
     * @throws Exception If can't set a default linear egression model.
     */
    private void setLinear() throws Exception {
      //then set default behaviour for node.
      //set linear regression combined with attribute filter
      
      //find the attributes used for splitting.
      boolean[] attributeList = new boolean[m_training.numAttributes()];
      for (int noa = 0; noa < m_training.numAttributes(); noa++) {
	attributeList[noa] = false;
      }
      
      TreeClass temp = this;
      attributeList[m_training.classIndex()] = true;
      while (temp != null) {
	attributeList[temp.m_attrib1] = true;
	attributeList[temp.m_attrib2] = true;
	temp = temp.m_parent;
      }
      int classind = 0;
      
      
      //find the new class index
      for (int noa = 0; noa < m_training.classIndex(); noa++) {
	if (attributeList[noa]) {
	  classind++;
	}
      }
      //count how many attribs were used
      int count = 0;
      for (int noa = 0; noa < m_training.numAttributes(); noa++) {
	if (attributeList[noa]) {
	  count++;
	}
      }
      
      //fill an int array with the numbers of those attribs
      int[] attributeList2 = new int[count];
      count = 0;
      for (int noa = 0; noa < m_training.numAttributes(); noa++) {
	if (attributeList[noa]) {
	  attributeList2[count] = noa;
	  count++;
	}
      }
      
      m_filter = new Remove();
      ((Remove)m_filter).setInvertSelection(true);
      ((Remove)m_filter).setAttributeIndicesArray(attributeList2);
      m_filter.setInputFormat(m_training);
      
      Instances temp2 = Filter.useFilter(m_training, m_filter);
      temp2.setClassIndex(classind);
      m_classObject = new LinearRegression();
      m_classObject.buildClassifier(temp2);
    }
    
    /**
     * Call to find out if an instance is in a polyline.
     * @param ob The polyline to check.
     * @param x The value of attribute1 to check.
     * @param y The value of attribute2 to check.
     * @return True if inside, false if not.
     */
    private boolean inPolyline(FastVector ob, double x, double y) {
      //this works similar to the inPoly below except that
      //the first and last lines are treated as extending infinite 
      //in one direction and 
      //then infinitly in the x dirction their is a line that will 
      //normaly be infinite but
      //can be finite in one or both directions
      
      int countx = 0;
      double vecx, vecy;
      double change;
      double x1, y1, x2, y2;
      
      for (int noa = 1; noa < ob.size() - 4; noa+= 2) {
	y1 = ((Double)ob.elementAt(noa+1)).doubleValue();
	y2 = ((Double)ob.elementAt(noa+3)).doubleValue();
	x1 = ((Double)ob.elementAt(noa)).doubleValue();
	x2 = ((Double)ob.elementAt(noa+2)).doubleValue();
	vecy = y2 - y1;
	vecx = x2 - x1;
	if (noa == 1 && noa == ob.size() - 6) {
	  //then do special test first and last edge
	  if (vecy != 0) {
	    change = (y - y1) / vecy;
	    if (vecx * change + x1 >= x) {
	      //then intersection
	      countx++;
	    }
	  }
	}
	else if (noa == 1) {
	  if ((y < y2 && vecy > 0) || (y > y2 && vecy < 0)) {
	    //now just determine intersection or not
	    change = (y - y1) / vecy;
	    if (vecx * change + x1 >= x) {
	      //then intersection on horiz
	      countx++;
	    }
	  }
	}
	else if (noa == ob.size() - 6) {
	  //then do special test on last edge
	  if ((y <= y1 && vecy < 0) || (y >= y1 && vecy > 0)) {
	    change = (y - y1) / vecy;
	    if (vecx * change + x1 >= x) {
	      countx++;
	    }
	  }
	  
	}
	else if ((y1 <= y && y < y2) || (y2 < y && y <= y1)) {
	  //then continue tests.
	  if (vecy == 0) {
	    //then lines are parallel stop tests in 
	    //ofcourse it should never make it this far
	  }
	  else {
	    change = (y - y1) / vecy;
	    if (vecx * change + x1 >= x) {
	      //then intersects on horiz
	      countx++;
	    }
	  }
	}
	
      }
      
      //now check for intersection with the infinity line
      y1 = ((Double)ob.elementAt(ob.size() - 2)).doubleValue();
      y2 = ((Double)ob.elementAt(ob.size() - 1)).doubleValue();
      
      if (y1 > y2) {
	//then normal line
	if (y1 >= y && y > y2) {
	  countx++;
	}
      }
      else {
	//then the line segment is inverted
	if (y1 >= y || y > y2) {
	  countx++;
	}
      }
      
      if ((countx % 2) == 1) {
	return true;
      }
      else {
	return false;
      }
    }
    
    /** 
     * Call this to determine if an instance is in a polygon.
     * @param ob The polygon.
     * @param x The value of attribute 1.
     * @param y The value of attribute 2.
     * @return True if in polygon, false if not.
     */
    private boolean inPoly(FastVector ob, double x, double y) {
      int count = 0;
      double vecx, vecy;
      double change;
      double x1, y1, x2, y2;
      for (int noa = 1; noa < ob.size() - 2; noa += 2) {
	y1 = ((Double)ob.elementAt(noa+1)).doubleValue();
	y2 = ((Double)ob.elementAt(noa+3)).doubleValue();
	if ((y1 <= y && y < y2) || (y2 < y && y <= y1)) {
	  //then continue tests.
	  vecy = y2 - y1;
	  if (vecy == 0) {
	    //then lines are parallel stop tests for this line
	  }
	  else {
	    x1 = ((Double)ob.elementAt(noa)).doubleValue();
	    x2 = ((Double)ob.elementAt(noa+2)).doubleValue();
	    vecx = x2 - x1;
	    change = (y - y1) / vecy;
	    if (vecx * change + x1 >= x) {
	      //then add to count as an intersected line
	      count++;
	    }
	  }
	  
	}
      }
      if ((count % 2) == 1) {
	//then lies inside polygon
	//System.out.println("in");
	return true;
      }
      else {
	//System.out.println("out");
	return false;
      }
      //System.out.println("WHAT?!?!?!?!!?!??!?!");
      //return false;
    }

    /**
     * Goes through the tree structure recursively and returns the node that
     * has the id.
     * @param id The node to find.
     * @return The node that matches the id.
     */
    public TreeClass getNode(String id) {
      //returns the treeclass object with the particular ident
      if (id.equals(m_identity)) {
	return this;
      }
      
      if (m_set1 != null) {
	TreeClass tmp = m_set1.getNode(id);
	if (tmp != null) {
	  return tmp;
	}
      }
      if (m_set2 != null) {
	TreeClass tmp = m_set2.getNode(id);
	if (tmp != null) {
	  return tmp;
	}
      }
      return null;
    }
    
    /**
     * Returns a string containing a bit of information about this node, in 
     * alternate form.
     * @param s The string buffer to fill.
     * @throws Exception if can't create label.
     */
    public void getAlternateLabel(StringBuffer s) throws Exception {
      
      //StringBuffer s = new StringBuffer();
      
      FastVector tmp = (FastVector)m_ranges.elementAt(0);
      
      if (m_classObject != null && m_training.classAttribute().isNominal()) {
	s.append("Classified by " + m_classObject.getClass().getName());
      }
      else if (((Double)tmp.elementAt(0)).intValue() == LEAF) {
	if (m_training.classAttribute().isNominal()) {
	  double high = -1000;
	  int num = 0;
	  double count = 0;
	  for (int noa = 0; noa < m_training.classAttribute().numValues();
	       noa++) {
	    if (((Double)tmp.elementAt(noa + 1)).doubleValue() > high) {
	      high = ((Double)tmp.elementAt(noa + 1)).doubleValue();
	      num  = noa + 1;
	    }
	    count += ((Double)tmp.elementAt(noa + 1)).doubleValue();
	  }
	  s.append(m_training.classAttribute().value(num-1) + "(" + count);
	  if (count > high) {
	    s.append("/" + (count - high));
	  }
	  s.append(")");
	}
	else {
	  if (m_classObject == null 
	      && ((Double)tmp.elementAt(0)).intValue() == LEAF) {
	    setLinear();
	  }
	  s.append("Standard Deviation = " 
		   + Utils.doubleToString(((Double)tmp.elementAt(1))
					  .doubleValue(), 6));
	  
	}
      }
      else {
	s.append("Split on ");
	s.append(m_training.attribute(m_attrib1).name() + " AND ");
	s.append(m_training.attribute(m_attrib2).name());
	
	
      }
      
      //return s.toString();
    }
    
    /**
     * Returns a string containing a bit of information about this node.
     * @param s The stringbuffer to fill.
     * @throws Exception if can't create label.
     */
    public void getLabel(StringBuffer s) throws Exception {
      //for now just return identity
      //StringBuffer s = new StringBuffer();
      
      FastVector tmp = (FastVector)m_ranges.elementAt(0);
      
      
      if (m_classObject != null && m_training.classAttribute().isNominal()) {
	s.append("Classified by\\n" + m_classObject.getClass().getName());
      }
      else if (((Double)tmp.elementAt(0)).intValue() == LEAF) {
	
	if (m_training.classAttribute().isNominal()) {
	  boolean first = true;
	  for (int noa = 0; noa < m_training.classAttribute().numValues(); 
	       noa++) {
	    if (((Double)tmp.elementAt(noa + 1)).doubleValue() > 0) {
	      if (first)
		{
		  s.append("[" + m_training.classAttribute().value(noa));
		  first = false;
		}
	      else
		{
		  s.append("\\n[" + m_training.classAttribute().value(noa));
		}
	      s.append(", " + ((Double)tmp.elementAt(noa + 1)).doubleValue() 
		       + "]");
	    }      
	  }
	}
	else {
	  if (m_classObject == null 
	      && ((Double)tmp.elementAt(0)).intValue() == LEAF) {
	    setLinear();
	  }
	  s.append("Standard Deviation = " 
		   + Utils.doubleToString(((Double)tmp.elementAt(1))
		   .doubleValue(), 6));
	}
      }
      else {
	s.append("Split on\\n");
	s.append(m_training.attribute(m_attrib1).name() + " AND\\n");
	s.append(m_training.attribute(m_attrib2).name());
      }
      //return s.toString();
    }

    /**
     * Converts The tree structure to a dotty string.
     * @param t The stringbuffer to fill with the dotty structure.
     * @throws Exception if can't convert structure to dotty.
     */
    public void toDotty(StringBuffer t) throws Exception {
      //this will recursively create all the dotty info for the structure
      t.append(m_identity + " [label=\"");
      getLabel(t);
      t.append("\" ");
      //System.out.println(((Double)((FastVector)ranges.elementAt(0)).
      //elementAt(0)).intValue() + " A num ");
      if (((Double)((FastVector)m_ranges.elementAt(0)).elementAt(0)).intValue()
	  == LEAF) {
	t.append("shape=box ");
      }
      else {
	t.append("shape=ellipse ");
      }
      t.append("style=filled color=gray95]\n");
      
      if (m_set1 != null) {
	t.append(m_identity + "->");
	t.append(m_set1.m_identity + " [label=\"True\"]\n");//the edge for 
	//the left
	m_set1.toDotty(t);
      }
      if (m_set2 != null) {
	t.append(m_identity + "->");
	t.append(m_set2.m_identity + " [label=\"False\"]\n"); //the edge for 
	//the 
	//right
	m_set2.toDotty(t);
      }
      
    }
    
    /**
     * This will append the class Object in the tree to the string buffer.
     * @param t The stringbuffer.
     */
    public void objectStrings(StringBuffer t) {
      
      if (m_classObject != null) {
	t.append("\n\n" + m_identity +" {\n" + m_classObject.toString()+"\n}");
      }
      if (m_set1 != null) {
	m_set1.objectStrings(t);
      }
      if (m_set2 != null) {
	m_set2.objectStrings(t);
      }
    }
    
    /**
     * Converts the tree structure to a string. for people to read.
     * @param l How deep this node is in the tree.
     * @param t The stringbuffer to fill with the string.
     * @throws Exception if can't convert th string.
     */
    public void toString(int l, StringBuffer t) throws Exception {
      
      if (((Double)((FastVector)m_ranges.elementAt(0)).elementAt(0)).intValue()
	  == LEAF) {
	t.append(": " + m_identity + " ");
	getAlternateLabel(t);
      }
      if (m_set1 != null) {
	t.append("\n");
	for (int noa = 0; noa < l; noa++) {
	  t.append("|   ");
	  
	}
	getAlternateLabel(t);
	t.append(" (In Set)");
	m_set1.toString(l+1, t);
      }
      if (m_set2 != null) {
	t.append("\n");
	for (int noa = 0; noa < l; noa++) {
	  t.append("|   ");
	}
	getAlternateLabel(t);
	t.append(" (Not in Set)");
	m_set2.toString(l+1, t);
      }
      //return t.toString();
    }
  }
}
