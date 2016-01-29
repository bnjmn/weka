package weka.gui.knowledgeflow;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.Beans;
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

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import weka.core.Utils;
import weka.core.WekaException;
import weka.gui.GenericObjectEditor;
import weka.gui.GenericPropertiesCreator;
import weka.gui.HierarchyPropertyParser;
import weka.core.PluginManager;
import weka.gui.knowledgeflow.VisibleLayout.LayoutOperation;
import weka.knowledgeflow.steps.KFStep;
import weka.knowledgeflow.steps.Step;
import weka.knowledgeflow.steps.WekaAlgorithmWrapper;

public class StepTree extends JTree {

  /** Property file that lists built-in steps */
  protected static final String STEP_LIST_PROPS =
    "weka/knowledgeflow/steps/steps.props";

  /**
   * For serialization
   */
  private static final long serialVersionUID = 3646119269455293741L;

  protected MainKFPerspective m_mainPerspective;

  /** Lookup for searching text of global info/tip texts */
  protected Map<String, DefaultMutableTreeNode> m_nodeTextIndex =
    new HashMap<String, DefaultMutableTreeNode>();

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
        if (((e.getModifiers()
          & InputEvent.BUTTON1_MASK) != InputEvent.BUTTON1_MASK)
          || e.isAltDown()) {
          m_mainPerspective.setFlowLayoutOperation(LayoutOperation.NONE);
          m_mainPerspective.setPalleteSelectedStep(null);
          m_mainPerspective
            .setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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
                  m_mainPerspective.setCursor(
                    Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                  if (m_mainPerspective.getDebug()) {
                    System.err.println("Instantiated " + visual.getStepName());
                  }
                  m_mainPerspective
                    .setPalleteSelectedStep(visual.getStepManager());
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
      populateTree(stepClasses, jtreeRoot, GOEProps);
    }
  }

  protected void populateTree(Set<String> stepClasses,
    DefaultMutableTreeNode jtreeRoot, Properties GOEProps) throws Exception {
    for (String stepClass : stepClasses) {
      try {
        Step toAdd =
          (Step) Beans.instantiate(getClass().getClassLoader(), stepClass);
        // check for ignore
        if (toAdd.getClass().getAnnotation(StepTreeIgnore.class) != null
          || toAdd.getClass()
            .getAnnotation(weka.gui.beans.KFIgnore.class) != null) {
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

  protected void processPackage(HierarchyPropertyParser hpp,
    DefaultMutableTreeNode parentFolder, WekaAlgorithmWrapper wrapper)
      throws Exception {

    String[] primaryPackages = hpp.childrenValues();
    for (String primaryPackage : primaryPackages) {
      hpp.goToChild(primaryPackage);
      if (hpp.isLeafReached()) {
        String algName = hpp.fullValue();
        Object wrappedA =
          Beans.instantiate(this.getClass().getClassLoader(), algName);

        if (wrappedA.getClass().getAnnotation(StepTreeIgnore.class) == null
          && wrappedA.getClass()
            .getAnnotation(weka.gui.beans.KFIgnore.class) == null) {
          WekaAlgorithmWrapper wrapperCopy = (WekaAlgorithmWrapper) Beans
            .instantiate(this.getClass().getClassLoader(),
              wrapper.getClass().getCanonicalName());
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

  protected DefaultMutableTreeNode
    getCategoryFolder(DefaultMutableTreeNode jtreeRoot, String category) {
    DefaultMutableTreeNode targetFolder = null;

    @SuppressWarnings("unchecked")
    Enumeration<Object> children = jtreeRoot.children();
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

  protected String getStepCategory(Step toAdd) {
    String category = "Plugin";

    Annotation a = toAdd.getClass().getAnnotation(KFStep.class);
    if (a != null) {
      category = ((KFStep) a).category();
    }

    return category;
  }

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
