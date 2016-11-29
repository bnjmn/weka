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
 *    FlowByExpression.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Attribute;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * A step that splits incoming instances (or instance streams) according to the
 * evaluation of a logical expression. The expression can test the values of one
 * or more incoming attributes. The test can involve constants or comparing one
 * attribute's values to another. Inequalities along with string operations such
 * as contains, starts-with, ends-with and regular expressions may be used as
 * operators.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(
  name = "FlowByExpression",
  category = "Flow",
  toolTipText = "Route instances according to the evaluation of a logical expression. "
    + "The expression can test the values of "
    + "one or more incoming attributes. The test can involve constants or comparing "
    + "one attribute's values to another. Inequalities along with string operations "
    + "such as contains, starts-with, ends-with and regular expressions may be used "
    + "as operators. \"True\" instances can be sent to one downstream step and "
    + "\"False\" instances sent to another.",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "FlowByExpression.png")
public class FlowByExpression extends BaseStep {

  private static final long serialVersionUID = 7006511778677802572L;

  protected boolean m_isReset;

  /** The root of the expression tree */
  protected ExpressionNode m_root;

  /** The expression tree to use in internal textual format */
  protected String m_expressionString = "";

  /**
   * Name of the step to receive instances that evaluate to true via the
   * expression
   */
  protected String m_customNameOfTrueStep = "";

  /**
   * Name of the step to receive instances that evaluate to false via the
   * expression
   */
  protected String m_customNameOfFalseStep = "";

  /** Incoming structure */
  protected Instances m_incomingStructure;

  /** Keep track of how many incoming batches we've seen */
  protected AtomicInteger m_batchCount;

  /** True if the "true" step is valid (i.e. exists in the flow) */
  protected boolean m_validTrueStep;

  /** True if the "false" step is valid (i.e. exists in the flow) */
  protected boolean m_validFalseStep;

  /** Re-usable data object for streaming */
  protected Data m_streamingData;

  /**
   * Set the expression (in internal format)
   *
   * @param expressionString the expression to use (in internal format)
   */
  public void setExpressionString(String expressionString) {
    m_expressionString = expressionString;
  }

  /**
   * Get the current expression (in internal format)
   *
   * @return the current expression (in internal format)
   */
  public String getExpressionString() {
    return m_expressionString;
  }

  /**
   * Set the name of the connected step to send "true" instances to
   *
   * @param trueStep the name of the step to send "true" instances to
   */
  public void setTrueStepName(String trueStep) {
    m_customNameOfTrueStep = trueStep;
  }

  /**
   * Get the name of the connected step to send "true" instances to
   *
   * @return the name of the step to send "true" instances to
   */
  public String getTrueStepName() {
    return m_customNameOfTrueStep;
  }

  /**
   * Set the name of the connected step to send "false" instances to
   *
   * @param falseStep the name of the step to send "false" instances to
   */
  public void setFalseStepName(String falseStep) {
    m_customNameOfFalseStep = falseStep;
  }

  /**
   * Get the name of the connected step to send "false" instances to
   *
   * @return the name of the step to send "false" instances to
   */
  public String getFalseStepName() {
    return m_customNameOfFalseStep;
  }

  /**
   * Get a list of the names of connected downstream steps
   *
   * @return a list of the names of connected downstream steps
   */
  public List<String> getDownstreamStepNames() {
    List<String> result = new ArrayList<String>();

    for (List<StepManager> o : getStepManager().getOutgoingConnections()
      .values()) {
      for (StepManager m : o) {
        result.add(m.getName());
      }
    }

    return result;
  }

  /**
   * Initialize the step.
   *
   * @throws WekaException if a problem occurs during initialization
   */
  @Override
  public void stepInit() throws WekaException {
    m_isReset = true;
    m_streamingData = null;

    // see if the specified downstream steps are connected
    m_validTrueStep =
      getStepManager().getOutgoingConnectedStepWithName(
        environmentSubstitute(m_customNameOfTrueStep)) != null;
    m_validFalseStep =
      getStepManager().getOutgoingConnectedStepWithName(
        environmentSubstitute(m_customNameOfFalseStep)) != null;

    m_incomingStructure = null;

    if (m_expressionString == null || m_expressionString.length() == 0) {
      throw new WekaException("No expression defined!");
    }
  }

  /**
   * Get a list of incoming connection types that this step can accept. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and any existing incoming connections. E.g. a step might be able to accept
   * one (and only one) incoming batch data connection.
   *
   * @return a list of incoming connections that this step can accept given its
   *         current state
   */
  @Override
  public List<String> getIncomingConnectionTypes() {

    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays.asList(StepManager.CON_DATASET,
        StepManager.CON_TRAININGSET, StepManager.CON_TESTSET,
        StepManager.CON_INSTANCE);
    }

    if (getStepManager().numIncomingConnectionsOfType(StepManager.CON_INSTANCE) == 0) {
      return Arrays.asList(StepManager.CON_DATASET,
        StepManager.CON_TRAININGSET, StepManager.CON_TESTSET);
    }
    return null;
  }

  /**
   * Get a list of outgoing connection types that this step can produce. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and the incoming connections. E.g. depending on what incoming connection is
   * present, a step might be able to produce a trainingSet output, a testSet
   * output or neither, but not both.
   *
   * @return a list of outgoing connections that this step can produce
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if (getStepManager().numIncomingConnectionsOfType(StepManager.CON_INSTANCE) > 0) {
      result.add(StepManager.CON_INSTANCE);
    } else if (getStepManager().numIncomingConnections() > 0) {
      if (getStepManager()
        .numIncomingConnectionsOfType(StepManager.CON_DATASET) > 0) {
        result.add(StepManager.CON_DATASET);
      }

      if (getStepManager().numIncomingConnectionsOfType(
        StepManager.CON_TRAININGSET) > 0) {
        result.add(StepManager.CON_TRAININGSET);
      }

      if (getStepManager()
        .numIncomingConnectionsOfType(StepManager.CON_TESTSET) > 0) {
        result.add(StepManager.CON_TESTSET);
      }
    }

    return result;
  }

  /**
   * If possible, get the output structure for the named connection type as a
   * header-only set of instances. Can return null if the specified connection
   * type is not representable as Instances or cannot be determined at present.
   *
   * @param connectionName the name of the connection type to get the output
   *          structure for
   * @return the output structure as a header-only Instances object
   * @throws WekaException if a problem occurs
   */
  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {

    if (getStepManager().numIncomingConnections() > 0) {
      for (Map.Entry<String, List<StepManager>> e : getStepManager()
        .getIncomingConnections().entrySet()) {
        // we assume (and check for at runtime) that all incoming
        // batch connections have the same structure, so just taking
        // the first connection here is sufficient to determine the
        // output structure for any specified output connection type

        String incomingConnType = e.getKey();
        Instances incomingStruc =
          getStepManager().getIncomingStructureFromStep(e.getValue().get(0),
            incomingConnType);
        return incomingStruc;
      }
    }

    return null;
  }

  /**
   * Main processing routine
   * 
   * @param data incoming data object
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    if (m_isReset) {
      m_isReset = false;

      if (getStepManager().numIncomingConnectionsOfType(
        StepManager.CON_INSTANCE) > 0) {
        m_streamingData = new Data(StepManager.CON_INSTANCE);
        Instance inst = data.getPrimaryPayload();
        m_incomingStructure = new Instances(inst.dataset(), 0);
      } else {
        m_incomingStructure = data.getPrimaryPayload();
        m_incomingStructure = new Instances(m_incomingStructure, 0);
        m_batchCount =
          new AtomicInteger(getStepManager().numIncomingConnections());
        getStepManager().processing();
      }
      m_root = new BracketNode();
      m_root.parseFromInternal(m_expressionString);
      m_root.init(m_incomingStructure, getStepManager()
        .getExecutionEnvironment().getEnvironmentVariables());
    }

    if (m_streamingData == null) {
      // processing batches
      Instances batch = data.getPrimaryPayload();
      if (!m_incomingStructure.equalHeaders(batch)) {
        throw new WekaException("Incoming batches with different structure: "
          + m_incomingStructure.equalHeadersMsg(batch));
      }
      processBatch(data);
      if (isStopRequested()) {
        getStepManager().interrupted();
      } else if (m_batchCount.get() == 0) {
        getStepManager().finished();
      }
    } else {
      // process streaming
      processStreaming(data);

      if (isStopRequested()) {
        getStepManager().interrupted();
      }
    }
  }

  /**
   * Handles streaming "instance" connections
   * 
   * @param data incoming data object encapsulating an instance to process
   * @throws WekaException if a problem occurs
   */
  protected void processStreaming(Data data) throws WekaException {
    if (getStepManager().isStreamFinished(data)) {
      m_streamingData.clearPayload();
      getStepManager().throughputFinished(m_streamingData);
      return;
    }
    getStepManager().throughputUpdateStart();
    Instance toProcess = data.getPrimaryPayload();

    boolean result = m_root.evaluate(toProcess, true);
    m_streamingData.setPayloadElement(StepManager.CON_INSTANCE, toProcess);
    if (result) {
      if (m_validTrueStep) {
        getStepManager().outputData(StepManager.CON_INSTANCE,
          m_customNameOfTrueStep, m_streamingData);
      }
    } else {
      if (m_validFalseStep) {
        getStepManager().outputData(StepManager.CON_INSTANCE,
          m_customNameOfFalseStep, m_streamingData);
      }
    }

    getStepManager().throughputUpdateEnd();
  }

  /**
   * Processes batch data (dataset, training or test) connections.
   * 
   * @param data the data object to process
   * @throws WekaException if a problem occurs
   */
  protected void processBatch(Data data) throws WekaException {
    Instances incoming = data.getPrimaryPayload();
    Instances trueBatch = new Instances(incoming, 0);
    Instances falseBatch = new Instances(incoming, 0);

    for (int i = 0; i < incoming.numInstances(); i++) {
      if (isStopRequested()) {
        return;
      }

      Instance current = incoming.instance(i);
      boolean result = m_root.evaluate(current, true);

      if (result) {
        if (m_validTrueStep) {
          trueBatch.add(current);
        }
      } else {
        if (m_validFalseStep) {
          falseBatch.add(current);
        }
      }
    }

    Integer setNum =
      data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
    Integer maxSetNum =
      data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
    if (m_validTrueStep) {
      getStepManager().logDetailed(
        "Routing " + trueBatch.numInstances() + " instances to step "
          + m_customNameOfTrueStep);
      Data outputData = new Data(data.getConnectionName(), trueBatch);
      outputData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
      outputData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
        maxSetNum);
      getStepManager().outputData(data.getConnectionName(),
        m_customNameOfTrueStep, outputData);
    }

    if (m_validFalseStep) {
      getStepManager().logDetailed(
        "Routing " + falseBatch.numInstances() + " instances to step "
          + m_customNameOfFalseStep);
      Data outputData = new Data(data.getConnectionName(), falseBatch);
      outputData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
      outputData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
        maxSetNum);
      getStepManager().outputData(data.getConnectionName(),
        m_customNameOfFalseStep, outputData);
    }

    if (setNum == maxSetNum) {
      m_batchCount.decrementAndGet();
    }
  }

  /**
   * Return the fully qualified name of a custom editor component (JComponent)
   * to use for editing the properties of the step. This method can return null,
   * in which case the system will dynamically generate an editor using the
   * GenericObjectEditor
   *
   * @return the fully qualified name of a step editor component
   */
  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.FlowByExpressionStepEditorDialog";
  }

  /**
   * Abstract base class for parts of a boolean expression.
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static abstract class ExpressionNode implements Serializable {

    /** For serialization */
    private static final long serialVersionUID = -8427857202322768762L;

    /** boolean operator for combining with result so far */
    protected boolean m_isAnOr;

    /** is this node negated? */
    protected boolean m_isNegated;

    /** Environment variables */
    protected transient Environment m_env;

    /** Whether to show the combination operator in the textual representation */
    protected boolean m_showAndOr = true;

    /**
     * Set whether this node is to be OR'ed to the result so far
     *
     * @param isOr true if this node is to be OR'd
     */
    public void setIsOr(boolean isOr) {
      m_isAnOr = isOr;
    }

    /**
     * Get whether this node is to be OR'ed
     *
     * @return true if this node is to be OR'ed with the result so far
     */
    public boolean isOr() {
      return m_isAnOr;
    }

    /**
     * Get whether this node is negated.
     *
     * @return
     */
    public boolean isNegated() {
      return m_isNegated;
    }

    /**
     * Set whether this node is negated
     *
     * @param negated true if this node is negated
     */
    public void setNegated(boolean negated) {
      m_isNegated = negated;
    }

    /**
     * Set whether to show the combination operator in the textual description
     *
     * @param show true if the combination operator is to be shown
     */
    public void setShowAndOr(boolean show) {
      m_showAndOr = show;
    }

    /**
     * Initialize the node
     *
     * @param structure the structure of the incoming instances
     * @param env Environment variables
     */
    public void init(Instances structure, Environment env) {
      m_env = env;
    }

    /**
     * Evaluate this node and combine with the result so far
     *
     * @param inst the incoming instance to evalute with
     * @param result the result to combine with
     * @return the result after combining with this node
     */
    public abstract boolean evaluate(Instance inst, boolean result);

    /**
     * Get the internal representation of this node
     *
     * @param buff the string buffer to append to
     */
    public abstract void toStringInternal(StringBuffer buff);

    /**
     * Get the display representation of this node
     *
     * @param buff the string buffer to append to
     */
    public abstract void toStringDisplay(StringBuffer buff);

    /**
     * Parse and initialize from the internal representation
     *
     * @param expression the expression to parse in internal representation
     * @return the remaining parts of the expression after parsing and removing
     *         the part for this node
     */
    protected abstract String parseFromInternal(String expression);

    /**
     * Get a DefaultMutableTreeNode for this node
     *
     * @param parent the parent of this node (if any)
     * @return the DefaultMutableTreeNode for this node
     */
    public abstract DefaultMutableTreeNode
      toJTree(DefaultMutableTreeNode parent);
  }

  /**
   * An expression node that encloses other expression nodes in brackets
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static class BracketNode extends ExpressionNode implements
    Serializable {

    /** For serialization */
    private static final long serialVersionUID = 8732159083173001115L;

    protected List<ExpressionNode> m_children = new ArrayList<ExpressionNode>();

    @Override
    public void init(Instances structure, Environment env) {
      super.init(structure, env);

      for (ExpressionNode n : m_children) {
        n.init(structure, env);
      }
    }

    @Override
    public boolean evaluate(Instance inst, boolean result) {

      boolean thisNode = true;
      if (m_children.size() > 0) {
        for (ExpressionNode n : m_children) {
          thisNode = n.evaluate(inst, thisNode);
        }
        if (isNegated()) {
          thisNode = !thisNode;
        }
      }

      return (isOr() ? (result || thisNode) : (result && thisNode));
    }

    /**
     * Add a child to this bracket node
     *
     * @param child the ExpressionNode to add
     */
    public void addChild(ExpressionNode child) {
      m_children.add(child);

      if (m_children.size() > 0) {
        m_children.get(0).setShowAndOr(false);
      }
    }

    /**
     * Remove a child from this bracket node
     *
     * @param child the ExpressionNode to remove
     */
    public void removeChild(ExpressionNode child) {
      m_children.remove(child);

      if (m_children.size() > 0) {
        m_children.get(0).setShowAndOr(false);
      }
    }

    @Override
    public String toString() {
      // just the representation of this node (suitable for the abbreviated
      // JTree node label

      String result = "( )";
      if (isNegated()) {
        result = "!" + result;
      }

      if (m_showAndOr) {
        if (m_isAnOr) {
          result = "|| " + result;
        } else {
          result = "&& " + result;
        }
      }

      return result;
    }

    @Override
    public DefaultMutableTreeNode toJTree(DefaultMutableTreeNode parent) {

      DefaultMutableTreeNode current = new DefaultMutableTreeNode(this);
      if (parent != null) {
        parent.add(current);
      }

      for (ExpressionNode child : m_children) {
        child.toJTree(current);
      }

      return current;
    }

    private void toString(StringBuffer buff, boolean internal) {
      if (m_children.size() >= 0) {
        if (internal || m_showAndOr) {
          if (m_isAnOr) {
            buff.append("|| ");
          } else {
            buff.append("&& ");
          }
        }

        if (isNegated()) {
          buff.append("!");
        }
        buff.append("(");

        int count = 0;
        for (ExpressionNode child : m_children) {
          if (internal) {
            child.toStringInternal(buff);
          } else {
            child.toStringDisplay(buff);
          }
          count++;
          if (count != m_children.size()) {
            buff.append(" ");
          }
        }
        buff.append(")");
      }
    }

    @Override
    public void toStringDisplay(StringBuffer buff) {
      toString(buff, false);
    }

    @Override
    public void toStringInternal(StringBuffer buff) {
      toString(buff, true);
    }

    @Override
    public String parseFromInternal(String expression) {
      if (expression.startsWith("|| ")) {
        m_isAnOr = true;
      }

      if (expression.startsWith("|| ") || expression.startsWith("&& ")) {
        expression = expression.substring(3, expression.length());
      }

      if (expression.charAt(0) == '!') {
        setNegated(true);
        expression = expression.substring(1, expression.length());
      }

      if (expression.charAt(0) != '(') {
        throw new IllegalArgumentException(
          "Malformed expression! Was expecting a \"(\"");
      }

      expression = expression.substring(1, expression.length());

      while (expression.charAt(0) != ')') {
        int offset = 3;

        if (expression.charAt(offset) == '(') {
          ExpressionNode child = new BracketNode();
          expression = child.parseFromInternal(expression);
          m_children.add(child);
        } else {
          // must be an ExpressionClause
          ExpressionNode child = new ExpressionClause();
          expression = child.parseFromInternal(expression);
          m_children.add(child);
        }
      }

      if (m_children.size() > 0) {
        m_children.get(0).setShowAndOr(false);
      }

      return expression;
    }
  }

  /**
   * An expression node that represents a clause of an expression
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static class ExpressionClause extends ExpressionNode implements
    Serializable {

    /** For serialization */
    private static final long serialVersionUID = 2754006654981248325L;

    /** The operator for this expression */
    protected ExpressionType m_operator;

    /** The name of the lhs attribute */
    protected String m_lhsAttributeName;

    /** The index of the lhs attribute */
    protected int m_lhsAttIndex = -1;

    /** The rhs operand (constant value or attribute name) */
    protected String m_rhsOperand;

    /** True if the rhs operand is an attribute */
    protected boolean m_rhsIsAttribute;

    /** index of the rhs if it is an attribute */
    protected int m_rhsAttIndex = -1;

    /** The name of the lhs attribute after resolving variables */
    protected String m_resolvedLhsName;

    /** The rhs operand after resolving variables */
    protected String m_resolvedRhsOperand;

    /** the compiled regex pattern (if the operator is REGEX) */
    protected Pattern m_regexPattern;

    /** The rhs operand (if constant and is a number ) */
    protected double m_numericOperand;

    public static enum ExpressionType {
      EQUALS(" = ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          if (rhsIsAttribute) {
            if (inst.isMissing(lhsAttIndex) && inst.isMissing(rhsAttIndex)) {
              return true;
            }
            if (inst.isMissing(lhsAttIndex) || inst.isMissing(rhsAttIndex)) {
              return false;
            }
            return Utils.eq(inst.value(lhsAttIndex), inst.value(rhsAttIndex));
          }

          if (inst.isMissing(lhsAttIndex)) {
            return false;
          }
          return (Utils.eq(inst.value(lhsAttIndex), numericOperand));
        }
      },
      NOTEQUAL(" != ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          return !EQUALS.evaluate(inst, lhsAttIndex, rhsOperand,
            numericOperand, regexPattern, rhsIsAttribute, rhsAttIndex);
        }
      },
      LESSTHAN(" < ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          if (rhsIsAttribute) {
            if (inst.isMissing(lhsAttIndex) || inst.isMissing(rhsAttIndex)) {
              return false;
            }
            return (inst.value(lhsAttIndex) < inst.value(rhsAttIndex));
          }

          if (inst.isMissing(lhsAttIndex)) {
            return false;
          }
          return (inst.value(lhsAttIndex) < numericOperand);
        }
      },
      LESSTHANEQUAL(" <= ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          if (rhsIsAttribute) {
            if (inst.isMissing(lhsAttIndex) || inst.isMissing(rhsAttIndex)) {
              return false;
            }
            return (inst.value(lhsAttIndex) <= inst.value(rhsAttIndex));
          }

          if (inst.isMissing(lhsAttIndex)) {
            return false;
          }
          return (inst.value(lhsAttIndex) <= numericOperand);
        }
      },
      GREATERTHAN(" > ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          return !LESSTHANEQUAL.evaluate(inst, lhsAttIndex, rhsOperand,
            numericOperand, regexPattern, rhsIsAttribute, rhsAttIndex);
        }
      },
      GREATERTHANEQUAL(" >= ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          return !LESSTHAN.evaluate(inst, lhsAttIndex, rhsOperand,
            numericOperand, regexPattern, rhsIsAttribute, rhsAttIndex);
        }
      },
      ISMISSING(" isMissing ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          return (inst.isMissing(lhsAttIndex));
        }
      },
      CONTAINS(" contains ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          if (inst.isMissing(lhsAttIndex)) {
            return false;
          }

          String lhsString = "";
          try {
            lhsString = inst.stringValue(lhsAttIndex);
          } catch (IllegalArgumentException ex) {
            return false;
          }

          if (rhsIsAttribute) {
            if (inst.isMissing(rhsAttIndex)) {
              return false;
            }

            try {
              String rhsString = inst.stringValue(rhsAttIndex);

              return lhsString.contains(rhsString);
            } catch (IllegalArgumentException ex) {
              return false;
            }
          }

          return lhsString.contains(rhsOperand);
        }
      },
      STARTSWITH(" startsWith ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          if (inst.isMissing(lhsAttIndex)) {
            return false;
          }

          String lhsString = "";
          try {
            lhsString = inst.stringValue(lhsAttIndex);
          } catch (IllegalArgumentException ex) {
            return false;
          }

          if (rhsIsAttribute) {
            if (inst.isMissing(rhsAttIndex)) {
              return false;
            }

            try {
              String rhsString = inst.stringValue(rhsAttIndex);

              return lhsString.startsWith(rhsString);
            } catch (IllegalArgumentException ex) {
              return false;
            }
          }

          return lhsString.startsWith(rhsOperand);
        }
      },
      ENDSWITH(" endsWith ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          if (inst.isMissing(lhsAttIndex)) {
            return false;
          }

          String lhsString = "";
          try {
            lhsString = inst.stringValue(lhsAttIndex);
          } catch (IllegalArgumentException ex) {
            return false;
          }

          if (rhsIsAttribute) {
            if (inst.isMissing(rhsAttIndex)) {
              return false;
            }

            try {
              String rhsString = inst.stringValue(rhsAttIndex);

              return lhsString.endsWith(rhsString);
            } catch (IllegalArgumentException ex) {
              return false;
            }
          }

          return lhsString.endsWith(rhsOperand);
        }
      },
      REGEX(" regex ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          if (inst.isMissing(lhsAttIndex)) {
            return false;
          }

          if (regexPattern == null) {
            return false;
          }

          String lhsString = "";
          try {
            lhsString = inst.stringValue(lhsAttIndex);
          } catch (IllegalArgumentException ex) {
            return false;
          }

          return regexPattern.matcher(lhsString).matches();
        }
      };

      abstract boolean evaluate(Instance inst, int lhsAttIndex,
        String rhsOperand, double numericOperand, Pattern regexPattern,
        boolean rhsIsAttribute, int rhsAttIndex);

      private final String m_stringVal;

      ExpressionType(String name) {
        m_stringVal = name;
      }

      @Override
      public String toString() {
        return m_stringVal;
      }
    }

    public ExpressionClause() {
    }

    /**
     * Construct a new ExpressionClause
     *
     * @param operator the operator to use
     * @param lhsAttributeName the lhs attribute name
     * @param rhsOperand the rhs operand
     * @param rhsIsAttribute true if the rhs operand is an attribute
     * @param isAnOr true if the result of this expression is to be OR'ed with
     *          the result so far
     */
    public ExpressionClause(ExpressionType operator, String lhsAttributeName,
      String rhsOperand, boolean rhsIsAttribute, boolean isAnOr) {
      m_operator = operator;
      m_lhsAttributeName = lhsAttributeName;
      m_rhsOperand = rhsOperand;
      m_rhsIsAttribute = rhsIsAttribute;
      m_isAnOr = isAnOr;
    }

    /**
     * Get the lhs attribute name
     *
     * @return the lhs attribute name
     */
    public String getLHSAttName() {
      return m_lhsAttributeName;
    }

    /**
     * Set the lhs attribute name
     *
     * @param attName the lhs att naem
     */
    public void setLHSAttName(String attName) {
      m_lhsAttributeName = attName;
    }

    /**
     * Get the rhs operand
     *
     * @return the rhs operando
     */
    public String getRHSOperand() {
      return m_rhsOperand;
    }

    /**
     * Set the rhs operand
     *
     * @param opp the rhs operand to set
     */
    public void setRHSOperand(String opp) {
      m_rhsOperand = opp;
    }

    /**
     * Returns true if the RHS is an attribute rather than a constant
     *
     * @return true if the RHS is an attribute
     */
    public boolean isRHSAnAttribute() {
      return m_rhsIsAttribute;
    }

    /**
     * Set whether the RHS is an attribute rather than a constant
     *
     * @param rhs true if the RHS is an attribute rather than a constant
     */
    public void setRHSIsAnAttribute(boolean rhs) {
      m_rhsIsAttribute = rhs;
    }

    /**
     * Get the operator
     * 
     * @return the operator
     */
    public ExpressionType getOperator() {
      return m_operator;
    }

    /**
     * Set the operator
     *
     * @param opp the operator to use
     */
    public void setOperator(ExpressionType opp) {
      m_operator = opp;
    }

    @Override
    public void init(Instances structure, Environment env) {
      super.init(structure, env);

      m_resolvedLhsName = m_lhsAttributeName;
      m_resolvedRhsOperand = m_rhsOperand;
      try {
        m_resolvedLhsName = m_env.substitute(m_resolvedLhsName);
        m_resolvedRhsOperand = m_env.substitute(m_resolvedRhsOperand);
      } catch (Exception ex) {
      }

      Attribute lhs = null;
      // try as an index or "special" label first
      if (m_resolvedLhsName.toLowerCase().startsWith("/first")) {
        lhs = structure.attribute(0);
      } else if (m_resolvedLhsName.toLowerCase().startsWith("/last")) {
        lhs = structure.attribute(structure.numAttributes() - 1);
      } else {
        // try as an index
        try {
          int indx = Integer.parseInt(m_resolvedLhsName);
          indx--;
          lhs = structure.attribute(indx);
        } catch (NumberFormatException ex) {
          // ignore
        }
      }

      if (lhs == null) {
        lhs = structure.attribute(m_resolvedLhsName);
      }
      if (lhs == null) {
        throw new IllegalArgumentException("Data does not contain attribute "
          + "\"" + m_resolvedLhsName + "\"");
      }
      m_lhsAttIndex = lhs.index();

      if (m_rhsIsAttribute) {
        Attribute rhs = null;

        // try as an index or "special" label first
        if (m_resolvedRhsOperand.toLowerCase().equals("/first")) {
          rhs = structure.attribute(0);
        } else if (m_resolvedRhsOperand.toLowerCase().equals("/last")) {
          rhs = structure.attribute(structure.numAttributes() - 1);
        } else {
          // try as an index
          try {
            int indx = Integer.parseInt(m_resolvedRhsOperand);
            indx--;
            rhs = structure.attribute(indx);
          } catch (NumberFormatException ex) {
            // ignore
          }
        }

        if (rhs == null) {
          rhs = structure.attribute(m_resolvedRhsOperand);
        }
        if (rhs == null) {
          throw new IllegalArgumentException("Data does not contain attribute "
            + "\"" + m_resolvedRhsOperand + "\"");
        }
        m_rhsAttIndex = rhs.index();
      } else if (m_operator != ExpressionType.CONTAINS
        && m_operator != ExpressionType.STARTSWITH
        && m_operator != ExpressionType.ENDSWITH
        && m_operator != ExpressionType.REGEX
        && m_operator != ExpressionType.ISMISSING) {
        // make sure the operand is parseable as a number (unless missing has
        // been specified - equals only)
        if (lhs.isNominal()) {
          m_numericOperand = lhs.indexOfValue(m_resolvedRhsOperand);

          if (m_numericOperand < 0) {
            throw new IllegalArgumentException("Unknown nominal value '"
              + m_resolvedRhsOperand + "' for attribute '" + lhs.name() + "'");
          }
        } else {
          try {
            m_numericOperand = Double.parseDouble(m_resolvedRhsOperand);
          } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + m_resolvedRhsOperand
              + "\" is not parseable as a number!");
          }
        }
      }

      if (m_operator == ExpressionType.REGEX) {
        m_regexPattern = Pattern.compile(m_resolvedRhsOperand);
      }
    }

    @Override
    public boolean evaluate(Instance inst, boolean result) {

      boolean thisNode =
        m_operator.evaluate(inst, m_lhsAttIndex, m_rhsOperand,
          m_numericOperand, m_regexPattern, m_rhsIsAttribute, m_rhsAttIndex);

      if (isNegated()) {
        thisNode = !thisNode;
      }

      return (isOr() ? (result || thisNode) : (result && thisNode));
    }

    @Override
    public String toString() {
      StringBuffer buff = new StringBuffer();
      toStringDisplay(buff);

      return buff.toString();
    }

    @Override
    public void toStringDisplay(StringBuffer buff) {
      toString(buff, false);
    }

    @Override
    public void toStringInternal(StringBuffer buff) {
      toString(buff, true);
    }

    @Override
    public DefaultMutableTreeNode toJTree(DefaultMutableTreeNode parent) {
      parent.add(new DefaultMutableTreeNode(this));

      return parent;
    }

    private void toString(StringBuffer buff, boolean internal) {
      if (internal || m_showAndOr) {
        if (m_isAnOr) {
          buff.append("|| ");
        } else {
          buff.append("&& ");
        }
      }
      if (isNegated()) {
        buff.append("!");
      }

      buff.append("[");

      buff.append(m_lhsAttributeName);
      if (internal) {
        buff.append("@EC@" + m_operator.toString());
      } else {
        buff.append(" " + m_operator.toString());
      }

      if (m_operator != ExpressionType.ISMISSING) {
        // @@ indicates that the rhs is an attribute
        if (internal) {
          buff.append("@EC@" + (m_rhsIsAttribute ? "@@" : "") + m_rhsOperand);
        } else {
          buff.append(" " + (m_rhsIsAttribute ? "ATT: " : "") + m_rhsOperand);
        }
      } else {
        if (internal) {
          buff.append("@EC@");
        } else {
          buff.append(" ");
        }
      }

      buff.append("]");
    }

    @Override
    protected String parseFromInternal(String expression) {

      // first the boolean operator for this clause
      if (expression.startsWith("|| ")) {
        m_isAnOr = true;
      }

      if (expression.startsWith("|| ") || expression.startsWith("&& ")) {
        // strip the boolean operator
        expression = expression.substring(3, expression.length());
      }

      if (expression.charAt(0) == '!') {
        setNegated(true);
        expression = expression.substring(1, expression.length());
      }

      if (expression.charAt(0) != '[') {
        throw new IllegalArgumentException(
          "Was expecting a \"[\" to start this ExpressionClause!");
      }
      expression = expression.substring(1, expression.length());
      m_lhsAttributeName = expression.substring(0, expression.indexOf("@EC@"));
      expression =
        expression.substring(expression.indexOf("@EC@") + 4,
          expression.length());
      String oppName = expression.substring(0, expression.indexOf("@EC@"));
      expression =
        expression.substring(expression.indexOf("@EC@") + 4,
          expression.length());
      for (ExpressionType n : ExpressionType.values()) {
        if (n.toString().equals(oppName)) {
          m_operator = n;
          break;
        }
      }

      if (expression.startsWith("@@")) {
        // rhs is an attribute
        expression = expression.substring(2, expression.length()); // strip off
        // "@@"
        m_rhsIsAttribute = true;
      }
      m_rhsOperand = expression.substring(0, expression.indexOf(']'));

      expression =
        expression.substring(expression.indexOf(']') + 1, expression.length()); // remove
                                                                                // "]"
      if (expression.charAt(0) == ' ') {
        expression = expression.substring(1, expression.length());
      }

      return expression;
    }
  }
}
