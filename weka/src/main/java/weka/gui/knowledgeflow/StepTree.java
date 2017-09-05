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
 *    StepTree.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.PluginManager;
import weka.core.Utils;
import weka.core.WekaException;
import weka.core.WekaPackageClassLoaderManager;
import weka.gui.GenericObjectEditor;
import weka.gui.GenericPropertiesCreator;
import weka.gui.HierarchyPropertyParser;
import weka.gui.knowledgeflow.VisibleLayout.LayoutOperation;
import weka.knowledgeflow.steps.KFStep;
import weka.knowledgeflow.steps.Step;
import weka.knowledgeflow.steps.WekaAlgorithmWrapper;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.*;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Subclass of JTree for displaying available steps.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class StepTree extends JTree {

  /** Property file that lists built-in steps */
  protected static final String STEP_LIST_PROPS =
    "weka/knowledgeflow/steps/steps.props";

  /**
   * For serialization
   */
  private static final long serialVersionUID = 3646119269455293741L;

  /** Reference to the main knowledge flow perspective */
  protected MainKFPerspective m_mainPerspective;

  /** Lookup for searching text of global info/tip texts */
  protected Map<String, DefaultMutableTreeNode> m_nodeTextIndex =
    new HashMap<String, DefaultMutableTreeNode>();

  /**
   * Constructor
   *
   * @param mainPerspective the main knowledge flow perspective
   */
  public StepTree(MainKFPerspective mainPerspective) {
    m_mainPerspective = mainPerspective;
    DefaultMutableTreeNode jtreeRoot = new DefaultMutableTreeNode("Weka");
    // populate tree

    InvisibleTreeModel model = new InvisibleTreeModel(jtreeRoot);
    model.activateFilter(true);
    this.setModel(model);

    setEnabled(true);
    setToolTipText("");
    setShowsRootHandles(true);
    setCellRenderer(new StepIconRenderer());
    DefaultTreeSelectionModel selectionModel = new DefaultTreeSelectionModel();
    selectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    setSelectionModel(selectionModel);

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (((e.getModifiers() & InputEvent.BUTTON1_MASK) != InputEvent.BUTTON1_MASK)
          || e.isAltDown()) {
          m_mainPerspective.setFlowLayoutOperation(LayoutOperation.NONE);
          m_mainPerspective.setPalleteSelectedStep(null);
          m_mainPerspective.setCursor(Cursor
            .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          StepTree.this.clearSelection();
        }

        TreePath p = StepTree.this.getSelectionPath();
        if (p != null) {
          if (p.getLastPathComponent() instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode tNode =
              (DefaultMutableTreeNode) p.getLastPathComponent();

            if (tNode.isLeaf()) {
              Object userObject = tNode.getUserObject();
              if (userObject instanceof StepTreeLeafDetails) {
                try {
                  StepVisual visual =
                    ((StepTreeLeafDetails) userObject).instantiateStep();
                  m_mainPerspective.setCursor(Cursor
                    .getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                  if (m_mainPerspective.getDebug()) {
                    System.err.println("Instantiated " + visual.getStepName());
                  }
                  m_mainPerspective.setPalleteSelectedStep(visual
                    .getStepManager());
                } catch (Exception ex) {
                  m_mainPerspective.showErrorDialog(ex);
                }
              }
            }
          }
        }

      }
    });

    try {
      populateTree(jtreeRoot);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    expandRow(0);
    setRootVisible(false);
  }

  /**
   * Populates the tree from the given root
   *
   * @param jtreeRoot the root to populate
   * @throws Exception if a problem occurs
   */
  protected void populateTree(DefaultMutableTreeNode jtreeRoot)
    throws Exception {
    Properties GOEProps = initGOEProps();

    // builtin steps don't get added to the plugin manager because,
    // due to the package loading process, they would get added after
    // any plugin steps. This would stuff up the ordering we want in
    // the design palette

    InputStream inputStream =
      getClass().getClassLoader().getResourceAsStream(STEP_LIST_PROPS);
    Properties builtinSteps = new Properties();
    builtinSteps.load(inputStream);
    inputStream.close();
    inputStream = null;
    String stepClassNames =
      builtinSteps.getProperty("weka.knowledgeflow.steps.Step");
    String[] s = stepClassNames.split(",");
    Set<String> stepImpls = new LinkedHashSet<String>();
    stepImpls.addAll(Arrays.asList(s));
    populateTree(stepImpls, jtreeRoot, GOEProps);

    // get any plugin steps here
    Set<String> stepClasses =
      PluginManager.getPluginNamesOfType("weka.knowledgeflow.steps.Step");
    if (stepClasses != null && stepClasses.size() > 0) {
      // filtering here because the LegacyFlowLoader adds all builtin
      // steps to the PluginManager. This is really only necessary if
      // a KnowledgeFlowApp is constructed a second time, as the first
      // time round StepTree gets constructed before the LegacyFlowLoader
      // class gets loaded into the classpath (and thus populates the
      // PluginManager). We can remove this filtering when LegacyFlowLoader
      // is no longer needed.
      Set<String> filteredStepClasses = new LinkedHashSet<String>();
      for (String plugin : stepClasses) {
        if (!stepClassNames.contains(plugin)) {
          filteredStepClasses.add(plugin);
        }
      }
      populateTree(filteredStepClasses, jtreeRoot, GOEProps);
    }
  }

  /**
   * Populate the tree from the given root using a set of step classes
   *
   * @param stepClasses the set of step classes to go into the tree
   * @param jtreeRoot the root of the tree
   * @param GOEProps generic object editor properties
   * @throws Exception if a problem occurs
   */
  protected void populateTree(Set<String> stepClasses,
    DefaultMutableTreeNode jtreeRoot, Properties GOEProps) throws Exception {
    for (String stepClass : stepClasses) {
      try {
        Step toAdd =
        // (Step) Beans.instantiate(getClass().getClassLoader(), stepClass);
          (Step) WekaPackageClassLoaderManager.objectForName(stepClass);
        // check for ignore
        if (toAdd.getClass().getAnnotation(StepTreeIgnore.class) != null
          || toAdd.getClass().getAnnotation(weka.gui.beans.KFIgnore.class) != null) {
          continue;
        }

        String category = getStepCategory(toAdd);
        DefaultMutableTreeNode targetFolder =
          getCategoryFolder(jtreeRoot, category);

        if (toAdd instanceof WekaAlgorithmWrapper) {
          populateForWekaWrapper(targetFolder, (WekaAlgorithmWrapper) toAdd,
            GOEProps);
        } else {
          StepTreeLeafDetails leafData = new StepTreeLeafDetails(toAdd);
          DefaultMutableTreeNode fixedLeafNode = new InvisibleNode(leafData);
          targetFolder.add(fixedLeafNode);

          String tipText =
            leafData.getToolTipText() != null ? leafData.getToolTipText() : "";

          m_nodeTextIndex.put(stepClass.toLowerCase() + " " + tipText,
            fixedLeafNode);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  /**
   * Populates a target folder in the tree for a particular class of weka
   * algorithm wrapper step
   *
   * @param targetFolder the folder to populate
   * @param wrapper the {@code WekaAlgorithmWrapper} implementation
   * @param GOEProps generic object editor properties
   * @throws Exception if a problem occurs
   */
  protected void populateForWekaWrapper(DefaultMutableTreeNode targetFolder,
    WekaAlgorithmWrapper wrapper, Properties GOEProps) throws Exception {
    Class wrappedAlgoClass = wrapper.getWrappedAlgorithmClass();
    String implList = GOEProps.getProperty(wrappedAlgoClass.getCanonicalName());
    String hppRoot = wrappedAlgoClass.getCanonicalName();
    hppRoot = hppRoot.substring(0, hppRoot.lastIndexOf('.'));

    if (implList == null) {
      throw new WekaException(
        "Unable to get a list of weka implementations for " + "class '"
          + wrappedAlgoClass.getCanonicalName() + "'");
    }

    Hashtable<String, String> roots =
      GenericObjectEditor.sortClassesByRoot(implList);
    for (Map.Entry<String, String> e : roots.entrySet()) {
      String classes = e.getValue();
      HierarchyPropertyParser hpp = new HierarchyPropertyParser();
      hpp.build(classes, ", ");

      hpp.goTo(hppRoot);
      processPackage(hpp, targetFolder, wrapper);
    }
  }

  /**
   * Processes a package from the {@code HierarchPropertyParser}
   *
   * @param hpp the property parser to use
   * @param parentFolder the folder to populate
   * @param wrapper the {@code WekaAlgorithmWrapper} implementation to use for
   *          the class of algorithm being processed
   * @throws Exception if a problem occurs
   */
  protected void processPackage(HierarchyPropertyParser hpp,
    DefaultMutableTreeNode parentFolder, WekaAlgorithmWrapper wrapper)
    throws Exception {

    String[] primaryPackages = hpp.childrenValues();
    for (String primaryPackage : primaryPackages) {
      hpp.goToChild(primaryPackage);
      if (hpp.isLeafReached()) {
        String algName = hpp.fullValue();
        Object wrappedA =
        // Beans.instantiate(this.getClass().getClassLoader(), algName);
          WekaPackageClassLoaderManager.objectForName(algName);

        if (wrappedA.getClass().getAnnotation(StepTreeIgnore.class) == null
          && wrappedA.getClass().getAnnotation(weka.gui.beans.KFIgnore.class) == null) {
          WekaAlgorithmWrapper wrapperCopy =
          /*
           * (WekaAlgorithmWrapper) Beans.instantiate(this.getClass()
           * .getClassLoader(), wrapper.getClass().getCanonicalName());
           */
          (WekaAlgorithmWrapper) wrapper.getClass().newInstance();
          wrapperCopy.setWrappedAlgorithm(wrappedA);
          StepTreeLeafDetails leafData = new StepTreeLeafDetails(wrapperCopy);
          DefaultMutableTreeNode wrapperLeafNode = new InvisibleNode(leafData);
          parentFolder.add(wrapperLeafNode);
          String tipText =
            leafData.getToolTipText() != null ? leafData.getToolTipText() : "";

          m_nodeTextIndex.put(algName.toLowerCase() + " " + tipText,
            wrapperLeafNode);
        }

        hpp.goToParent();
      } else {
        DefaultMutableTreeNode firstLevelOfMainAlgoType =
          new InvisibleNode(primaryPackage);
        parentFolder.add(firstLevelOfMainAlgoType);
        processPackage(hpp, firstLevelOfMainAlgoType, wrapper);
        hpp.goToParent();
      }
    }
  }

  /**
   * Get a folder for a particular category from the tree. Creates the category
   * folder if it doesn't already exist
   *
   * @param jtreeRoot the root of the tree
   * @param category the name of the category to get the folder for
   * @return return the folder
   */
  protected DefaultMutableTreeNode getCategoryFolder(
    DefaultMutableTreeNode jtreeRoot, String category) {
    DefaultMutableTreeNode targetFolder = null;

    @SuppressWarnings("unchecked")
    Enumeration<TreeNode> children = jtreeRoot.children();
    while (children.hasMoreElements()) {
      Object child = children.nextElement();
      if (child instanceof DefaultMutableTreeNode) {
        if (((DefaultMutableTreeNode) child).getUserObject().toString()
          .equals(category)) {
          targetFolder = (DefaultMutableTreeNode) child;
          break;
        }
      }
    }

    if (targetFolder == null) {
      targetFolder = new InvisibleNode(category);
      jtreeRoot.add(targetFolder);
    }

    return targetFolder;
  }

  /**
   * Gets the category that the supplied step belongs to. Uses the info in the
   * {@code KFStep} annotation; otherwise the default "Plugin" category is used.
   *
   * @param toAdd the step to get the category for
   * @return the category name
   */
  protected String getStepCategory(Step toAdd) {
    String category = "Plugin";

    Annotation a = toAdd.getClass().getAnnotation(KFStep.class);
    if (a != null) {
      category = ((KFStep) a).category();
    }

    return category;
  }

  /**
   * Initializes generic object editor properties
   *
   * @return a properties object
   * @throws Exception if a problem occurs
   */
  protected Properties initGOEProps() throws Exception {
    Properties GOEProps = GenericPropertiesCreator.getGlobalOutputProperties();
    if (GOEProps == null) {
      GenericPropertiesCreator creator = new GenericPropertiesCreator();
      if (creator.useDynamic()) {
        creator.execute(false);
        GOEProps = creator.getOutputProperties();
      } else {
        GOEProps = Utils.readProperties("weka/gui/GenericObjectEditor.props");
      }
    }

    return GOEProps;
  }

  /**
   * Get tool tip text for the step closest to the mouse location in the
   * {@code StepTree}.
   *
   * @param e the {@code MouseEvent} containing the pointer location
   * @return the tool tip for the closest step in the tree
   */
  @Override
  public String getToolTipText(MouseEvent e) {
    if ((getRowForLocation(e.getX(), e.getY())) == -1) {
      return null;
    }
    TreePath currPath = getPathForLocation(e.getX(), e.getY());
    if (currPath.getLastPathComponent() instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode node =
        (DefaultMutableTreeNode) currPath.getLastPathComponent();
      if (node.isLeaf()) {
        StepTreeLeafDetails leaf = (StepTreeLeafDetails) node.getUserObject();
        return leaf.getToolTipText();
      }
    }
    return null;
  }

  /**
   * Turn on or off tool tip text popups for the steps in the tree
   *
   * @param show true if tool tip text popups are to be shown
   */
  public void setShowLeafTipText(boolean show) {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
    Enumeration e = root.depthFirstEnumeration();

    while (e.hasMoreElements()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
      if (node.isLeaf()) {
        ((StepTreeLeafDetails) node.getUserObject()).setShowTipTexts(show);
      }
    }
  }

  /**
   * Get the global info/tool tip text index
   * 
   * @return the global info/tool tip text index
   */
  protected Map<String, DefaultMutableTreeNode> getNodeTextIndex() {
    return m_nodeTextIndex;
  }

  protected static class StepIconRenderer extends DefaultTreeCellRenderer {

    /** Added ID to avoid warning. */
    private static final long serialVersionUID = -4488876734500244945L;

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
      boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
        hasFocus);

      if (leaf) {
        Object userO = ((DefaultMutableTreeNode) value).getUserObject();
        if (userO instanceof StepTreeLeafDetails) {
          Icon i = ((StepTreeLeafDetails) userO).getIcon();
          if (i != null) {
            setIcon(i);
          }
        }
      }
      return this;
    }
  }
}
