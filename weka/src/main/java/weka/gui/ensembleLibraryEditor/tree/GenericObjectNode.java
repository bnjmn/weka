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
 *    GenericObjectNode.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor.tree;

import weka.classifiers.Classifier;
import weka.gui.GenericObjectEditor;
import weka.gui.ensembleLibraryEditor.AddModelsPanel;

import java.awt.Component;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * This class is responsible for allowing users to choose an object that
 * was provided with a GenericObjectEditor.  Just about every one of these 
 * Objects is a Weka Classifier.  There are two important things that these
 * nodes are responsible for beyond the other parameter node types.  First,
 * they must discover all of the parameters that need to be added in the 
 * model as child nodes.  This is done through a loop of introspection that
 * was copied and adapted from the weka.gui.PropertySheetPanel class.  
 * Second, this class is also responsible for discovering all possible 
 * combinations of GenericObject parameters that are stored in its child 
 * nodes.  This is accomplished by first discovering all of the child node 
 * parameters in the getValues method and then finding all combinations of 
 * these values with the combinAllValues method.
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision: 1.1 $
 */
public class GenericObjectNode
  extends DefaultMutableTreeNode
  implements PropertyChangeListener {
  
  /** for serialization */
  private static final long serialVersionUID = 688096727663132485L;
  
  //The following 8 arrays hold the accumulated information about the
  //Classifier parameters that we discover through introspection.  This
  //is very similar to the approach within PropertySheetPanel.

  /** Holds properties of the target */
  private PropertyDescriptor m_Properties[];
  
  /** this tracks which indexes of the m_Properties */
  private Vector m_UsedPropertyIndexes;
  
  /** Holds the methods of the target */
  private MethodDescriptor m_Methods[];
  
  /** Holds property editors of the object */
  private PropertyEditor m_Editors[];
  
  /** Holds current object values for each property */
  private Object m_Values[];
  
  /** The labels for each property */
  private String m_Names[];
  
  /** The tool tip text for each property */
  private String m_TipTexts[];
  
  /** StringBuffer containing help text for the object being edited */
  private StringBuffer m_HelpText;
  
  /** the GenericObjectEditor that was supplied for this node */
  private GenericObjectEditor m_GenericObjectEditor;
  
  /** this Vector stores all of the possible combinations of parameters 
   * that it obtains from its child nodes.  These combinations are 
   * created by the recursive combineAllValues method*/
  private Vector m_WorkingSetCombinations;
  
  /** the tip text for our node editor to display */
  private String m_ToolTipText;
  
  /** a reference to the tree model is necessary to be able to add and 
   * remove nodes in the tree */
  private DefaultTreeModel m_TreeModel;
  
  /** this is a reference to the Tree object that this node is 
   * contained within. Its required for this node to be able to 
   * add/remove nodes from the JTree*/
  private JTree m_Tree;
  
  /** This is a reference to the parent panel of the JTree so that we can 
   * supply it as the required argument when supplying warning JDialog 
   * messages*/
  private final AddModelsPanel m_ParentPanel;
  
  /** 
   * The constructor initialiazes the member variables of this node, 
   * Note that the "value" of this generic object is stored as the treeNode
   * user object. 
   * 
   * @param panel	the reference to the parent panel for calls to JDialog
   * @param value the value stored at this tree node
   * @param genericObjectEditor the GenericObjectEditor for this object
   * @param toolTipText the tipText to be displayed for this object
   */
  public GenericObjectNode(AddModelsPanel panel, Object value,
      GenericObjectEditor genericObjectEditor, String toolTipText) {
    
    super(value);
    //setObject(value);
    m_ParentPanel = panel;
    this.m_GenericObjectEditor = genericObjectEditor;
    this.m_ToolTipText = toolTipText;
    
  }
  
  /** 
   * It seems kind of dumb that the reference to the tree model is passed in
   * seperately - but know that this is actually necessary.  There is a really 
   * weird chicken before the egg problem.  You cannot create a TreeModel without
   * giving it its root node.  However, the nodes in your tree can't update the 
   * structure of the tree unless they have a reference to the TreeModel.  So in 
   * the end this was the only compromise that I could get to work well
   * 
   * @param tree	the tree to use
   */
  public void setTree(JTree tree) {
    this.m_Tree = tree;
    this.m_TreeModel = (DefaultTreeModel) m_Tree.getModel();
    
  }
  
  /**
   * returns the current tree
   * 
   * @return		the current tree
   */
  public JTree getTree() {
    return m_Tree;
  }
  
  /**
   * A getter for the GenericObjectEditor for this node
   * 
   * @return		the editor
   */
  public GenericObjectEditor getEditor() {
    return m_GenericObjectEditor;
  }
  
  /**
   * getter for the tooltip text
   * 
   * @return tooltip text
   */
  public StringBuffer getHelpText() {
    return m_HelpText;
  }
  
  /**
   * getter for the tooltip text
   * 
   * @return tooltip text
   */
  public String getToolTipText() {
    return m_ToolTipText;
  }
  
  /**
   * getter for this node's object
   * 
   * @return	the node's object
   */
  public Object getObject() {
    return getUserObject();
  }
  
  /**
   * setter for this nodes object
   * 
   * @param newValue	sets the new object
   */
  public void setObject(Object newValue) {
    setUserObject(newValue);
  }
  
  /**
   * this is a simple filter for the setUserObject method.  We basically
   * don't want null values to be passed in.
   * 
   * @param o		the object to set
   */
  public void setUserObject(Object o) {
    if (o != null)
      super.setUserObject(o);
  }
  
  /**
   * getter for the parent panel
   * 
   * @return		the parent panel
   */
  public JPanel getParentPanel() {
    return m_ParentPanel;
  }
  
  /**
   * returns always null
   * 
   * @return		always null
   */
  public String toString() {
    return null;
    //return getClass().getName() + "[" + getUserObject().toString() + "]";
  }
  
  /**
   * This implements the PropertyChangeListener for this node that gets 
   * registered with its Editor.  All we really have to do is change the 
   * Object value stored internally at this node when its editor says the 
   * value changed.
   * 
   * @param evt		the event
   */
  public void propertyChange(PropertyChangeEvent evt) {
    
    Object newValue = ((GenericObjectEditor) evt.getSource()).getValue();
    
    if (!newValue.getClass().equals(getObject().getClass())) {
      
      if (m_TreeModel.getRoot() == this) {
	
	try {
	  m_ParentPanel.buildClassifierTree((Classifier) newValue
	      .getClass().newInstance());
	} catch (InstantiationException e) {
	  e.printStackTrace();
	} catch (IllegalAccessException e) {
	  e.printStackTrace();
	}
	m_ParentPanel.update(m_ParentPanel.getGraphics());
	m_ParentPanel.repaint();
	
	//System.out.println("Changed root");
	
      } else {
	setObject(newValue);
	updateTree();
	updateTree();
	m_TreeModel.nodeChanged(this);
      }
    }
  }
  
  /**
   * This method uses introspection to programatically discover all of 
   * the parameters for this generic object.  For each one of them it
   * uses the TreeModel reference to create a new subtree to represent
   * that parameter and its value ranges.  Note that all of these nodes
   * are PropertyNodes which themselves hold the logic of figuring out
   * what type of parameter it is they are representing and thus what 
   * type of subtree to build.
   * <p/> 
   * We need to be careful because this was molded from the code inside of 
   * the PropertySheetPanel class.  Which means that we are wide open
   * to copy/paste problems.  In the future, when that code changes to 
   * adapt to other changes in Weka then this could easily become broken.
   */
  public void updateTree() {
    
    int childCount = m_TreeModel.getChildCount(this);
    
    for (int i = 0; i < childCount; i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode) m_TreeModel.getChild(this, 0);
      
      m_TreeModel.removeNodeFromParent(child);
    }
    
    //removeAllChildren();
    
    Object classifier = this.getUserObject();
    
    try {
      BeanInfo bi = Introspector.getBeanInfo(classifier.getClass());
      m_Properties = bi.getPropertyDescriptors();
      m_Methods = bi.getMethodDescriptors();
    } catch (IntrospectionException ex) {
      System.err.println("PropertySheet: Couldn't introspect");
      return;
    }
    
    //		 Look for a globalInfo method that returns a string
    // describing the target
    for (int i = 0; i < m_Methods.length; i++) {
      String name = m_Methods[i].getDisplayName();
      Method meth = m_Methods[i].getMethod();
      if (name.equals("globalInfo")) {
	if (meth.getReturnType().equals(String.class)) {
	  try {
	    Object args[] = {};
	    String globalInfo = (String) (meth.invoke(getObject(),
		args));
	    String summary = globalInfo;
	    int ci = globalInfo.indexOf('.');
	    if (ci != -1) {
	      summary = globalInfo.substring(0, ci + 1);
	    }
	    final String className = getObject().getClass().getName();
	    m_HelpText = new StringBuffer("NAME\n");
	    m_HelpText.append(className).append("\n\n");
	    m_HelpText.append("SYNOPSIS\n").append(globalInfo).append("\n\n");
	    
	  } catch (Exception ex) {
	    // ignored
	  }
	}
      }
    }
    
    m_UsedPropertyIndexes = new Vector();
    
    m_Editors = new PropertyEditor[m_Properties.length];
    
    m_Values = new Object[m_Properties.length];
    m_Names = new String[m_Properties.length];
    m_TipTexts = new String[m_Properties.length];
    boolean firstTip = true;
    
    for (int i = 0; i < m_Properties.length; i++) {
      
      // Don't display hidden or expert properties.
      if (m_Properties[i].isHidden() || m_Properties[i].isExpert()) {
	continue;
      }
      
      m_Names[i] = m_Properties[i].getDisplayName();
      Class type = m_Properties[i].getPropertyType();
      Method getter = m_Properties[i].getReadMethod();
      Method setter = m_Properties[i].getWriteMethod();
      
      // Only display read/write properties.
      if (getter == null || setter == null) {
	continue;
      }
      
      try {
	Object args[] = {};
	Object value = getter.invoke(classifier, args);
	m_Values[i] = value;
	
	PropertyEditor editor = null;
	Class pec = m_Properties[i].getPropertyEditorClass();
	if (pec != null) {
	  try {
	    editor = (PropertyEditor) pec.newInstance();
	  } catch (Exception ex) {
	    // Drop through.
	  }
	}
	if (editor == null) {
	  editor = PropertyEditorManager.findEditor(type);
	}
	m_Editors[i] = editor;
	
	// If we can't edit this component, skip it.
	if (editor == null) {
	  continue;
	}
	if (editor instanceof GenericObjectEditor) {
	  ((GenericObjectEditor) editor).setClassType(type);
	}
	
	// Don't try to set null values:
	if (value == null) {
	  continue;
	}
	
	editor.setValue(value);
	
	// now look for a TipText method for this property
	String tipName = m_Names[i] + "TipText";
	for (int j = 0; j < m_Methods.length; j++) {
	  String mname = m_Methods[j].getDisplayName();
	  Method meth = m_Methods[j].getMethod();
	  if (mname.equals(tipName)) {
	    if (meth.getReturnType().equals(String.class)) {
	      try {
		String tempTip = (String) (meth.invoke(
		    classifier, args));
		int ci = tempTip.indexOf('.');
		if (ci < 0) {
		  m_TipTexts[i] = tempTip;
		} else {
		  m_TipTexts[i] = tempTip.substring(0, ci);
		}
		
		if (m_HelpText != null) {
		  if (firstTip) {
		    m_HelpText.append("OPTIONS\n");
		    firstTip = false;
		  }
		  m_HelpText.append(m_Names[i]).append(" -- ");
		  m_HelpText.append(tempTip).append("\n\n");
		  
		}
		
	      } catch (Exception ex) {
		
	      }
	      break;
	    }
	  }
	}
	
	//Here we update the usedPropertyIndexes variable so that
	//later on we will know which ones to look at.
	m_UsedPropertyIndexes.add(new Integer(i));
	
	int currentCount = m_TreeModel.getChildCount(this);
	
	//Now we make a child node and add it to the tree underneath 
	//this one
	PropertyNode newNode = new PropertyNode(m_Tree, m_ParentPanel,
	    m_Names[i], m_TipTexts[i], m_Values[i], m_Editors[i]);
	
	m_TreeModel.insertNodeInto(newNode, this, currentCount);
	
      } catch (InvocationTargetException ex) {
	System.err.println("Skipping property " + m_Names[i]
	                                                  + " ; exception on target: " + ex.getTargetException());
	ex.getTargetException().printStackTrace();
	continue;
      } catch (Exception ex) {
	System.err.println("Skipping property " + m_Names[i]
	                                                  + " ; exception: " + ex);
	ex.printStackTrace();
	continue;
      }
      
    }
    
    //Finally we tell the TreeModel to update itself so the changes
    //will be visible
    m_TreeModel.nodeStructureChanged(this);
  }
  
  /**
   * This method iterates over all of the child nodes of this 
   * GenericObjectNode and requests the verious sets of values that the
   * user has presumably specified.  Once these sets of values are 
   * 
   * @return a Vector consisting of all parameter combinations
   */
  public Vector getValues() {
    
    Vector valuesVector = new Vector();
    
    int childCount = m_TreeModel.getChildCount(this);
    
    //poll all child nodes for their values.
    for (int i = 0; i < childCount; i++) {
      
      PropertyNode currentChild = (PropertyNode) m_TreeModel.getChild(
	  this, i);
      
      Vector v = currentChild.getAllValues();
      valuesVector.add(v);
      
    }
    
    //Need to initialize the working set of paramter combinations
    m_WorkingSetCombinations = new Vector();
    
    //obtain all combinations of the paremeters
    combineAllValues(new Vector(), valuesVector);
    
    /*
     //nice for initially debugging this - and there was a WHOLE lot of
      //that going on till this crazy idea finally worked.
       for (int i = 0; i < m_WorkingSetCombinations.size(); i++) {
       
       System.out.print("Combo "+i+": ");
       
       Vector current = (Vector)m_WorkingSetCombinations.get(i);
       for (int j = 0; j < current.size(); j++) {
       
       System.out.print(current.get(j)+"\t");
       
       }
       
       System.out.print("\n");
       }
       */
    
    //Now the real work begins.  Here we need to translate all of the values
    //received from the editors back into the actual class types that the 
    //Weka classifiers will understand.  for example, String values for 
    //enumerated values need to be turned back into the SelectedTag objects 
    //that classifiers understand. 
    //This vector will hold all of the actual generic objects that are being 
    //instantiated
    Vector newGenericObjects = new Vector();
    
    for (int i = 0; i < m_WorkingSetCombinations.size(); i++) {
      
      Vector current = (Vector) m_WorkingSetCombinations.get(i);
      
      //create a new copy of this class.  We will use this copy to test whether
      //the current set of parameters is valid.
      Object o = this.getUserObject();
      Class c = o.getClass();
      Object copy = null;
      
      try {
	copy = c.newInstance();
      } catch (InstantiationException e) {
	e.printStackTrace();
      } catch (IllegalAccessException e) {
	e.printStackTrace();
      }
      
      for (int j = 0; j < current.size(); j++) {
	
	Object[] args = new Object[1];
	
	int index = ((Integer) m_UsedPropertyIndexes.get(j)).intValue();
	
	PropertyDescriptor property = (PropertyDescriptor) m_Properties[index];
	Method setter = property.getWriteMethod();
	Class[] params = setter.getParameterTypes();
	
	Object currentVal = current.get(j);
	
	//System.out.println(currentVal.getClass().toString());
	
	//we gotta turn strings back into booleans 
	if (params.length == 1
	    && params[0].toString().equals("boolean")
	    && currentVal.getClass().toString().equals(
	    "class java.lang.String")) {
	  
	  currentVal = new Boolean((String) current.get(j));
	}
	
	//we gotta turn strings back into "Tags"
	if (params.length == 1
	    && params[0].toString().equals(
	    "class weka.core.SelectedTag")
	    && currentVal.getClass().toString().equals(
	    "class java.lang.String")) {
	  
	  String tagString = (String) current.get(j);
	  
	  m_Editors[index].setAsText(tagString);
	  currentVal = m_Editors[index].getValue();
	  
	}
	
	args[0] = currentVal;
	
	/*
	 System.out.print("setterName: "+setter.getName()+
	 " editor class: "+m_Editors[index].getClass()+
	 " params: ");
	 
	 
	 for (int k = 0; k < params.length; k++) 
	 System.out.print(params[k].toString()+" ");
	 
	 System.out.println(" value class: "+args[0].getClass().toString());
	 */
	
	try {
	  
	  //we tell the setter for the current parameter to update the copy
	  //with the current parameter value
	  setter.invoke(copy, args);
	  
	} catch (InvocationTargetException ex) {
	  if (ex.getTargetException() instanceof PropertyVetoException) {
	    String message = "WARNING: Vetoed; reason is: "
	      + ex.getTargetException().getMessage();
	    System.err.println(message);
	    
	    Component jf;
	    jf = m_ParentPanel.getRootPane();
	    JOptionPane.showMessageDialog(jf, message, "error",
		JOptionPane.WARNING_MESSAGE);
	    if (jf instanceof JFrame)
	      ((JFrame) jf).dispose();
	    
	  } else {
	    System.err.println(ex.getTargetException().getClass()
		.getName()
		+ " while updating "
		+ property.getName()
		+ ": " + ex.getTargetException().getMessage());
	    Component jf;
	    jf = m_ParentPanel.getRootPane();
	    JOptionPane.showMessageDialog(jf, ex
		.getTargetException().getClass().getName()
		+ " while updating "
		+ property.getName()
		+ ":\n" + ex.getTargetException().getMessage(),
		"error", JOptionPane.WARNING_MESSAGE);
	    if (jf instanceof JFrame)
	      ((JFrame) jf).dispose();
	    
	  }
	  
	} catch (IllegalArgumentException e) {
	  e.printStackTrace();
	} catch (IllegalAccessException e) {
	  e.printStackTrace();
	}
	
      }
      
      //At this point we have set all the parameters for this GenericObject
      //with a single combination that was generated from the 
      //m_WorkingSetCombinations Vector and can add it to the collection that
      //will be returned
      newGenericObjects.add(copy);
      
    }
    
    return newGenericObjects;
  }
  
  /** This method is responsible for returning all possible values through 
   * a recursive loop.
   * 
   * When the recursion terminates that means that there are no more parameter
   * sets to branch out through so all we have to do is save the current Vector
   * in the working set of combinations.  Otherwise we iterate through all 
   * possible values left in the next set of parameter values and recursively
   * call this function.
   * 
   * @param previouslySelected stores the values chosen in this branch of the recursion 
   * @param remainingValues the sets of values left 
   */
  public void combineAllValues(Vector previouslySelected,
      Vector remainingValues) {
    
    if (remainingValues.isEmpty()) {
      m_WorkingSetCombinations.add(previouslySelected);
      return;
    }
    
    Vector currentSet = new Vector((Vector) remainingValues.get(0));
    Vector tmpRemaining = new Vector(remainingValues);
    tmpRemaining.removeElementAt(0);
    
    for (int i = 0; i < currentSet.size(); i++) {
      Vector tmpPreviouslySelected = new Vector(previouslySelected);
      tmpPreviouslySelected.add(currentSet.get(i));
      combineAllValues(tmpPreviouslySelected, tmpRemaining);
    }
    
  }
  
}