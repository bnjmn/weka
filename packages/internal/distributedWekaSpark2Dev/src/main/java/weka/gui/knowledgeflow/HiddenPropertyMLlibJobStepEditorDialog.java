package weka.gui.knowledgeflow;

import weka.classifiers.mllib.MLlibClassifier;
import weka.core.Utils;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyHidableSheetPanel;
import weka.gui.PropertySheetPanel;
import weka.knowledgeflow.StepManagerImpl;
import weka.knowledgeflow.steps.MLlibClassifierEvaluationSparkJob;
import weka.knowledgeflow.steps.MLlibClassifierSparkJob;
import weka.knowledgeflow.steps.Step;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

public class HiddenPropertyMLlibJobStepEditorDialog extends StepEditorDialog {

  private static final long serialVersionUID = -6538316662337445327L;

  /** The {@code StepManager} for the step being edited */
  protected StepManagerImpl m_manager;

  /** Holds a copy of the step (for restoring after cancel) */
  protected Step m_stepOriginal;

  /**
   * Main editor for the MLlib job step - used to hide the actual classifier
   * option (as this will be edited by a separate PropertyHidableSheetPanel
   */
  protected PropertyHidableSheetPanel m_editor =
    new PropertyHidableSheetPanel(false);

  /**
   * Used to edit the MLlib classifier object and hide the sparkOptions property
   */
  protected PropertyHidableSheetPanel m_secondaryEditor =
    new PropertyHidableSheetPanel();

  /** The main holder panel */
  protected JPanel m_editorHolder = new JPanel();

  /** The panel that contains the main editor */
  protected JPanel m_primaryEditorHolder = new JPanel();

  protected JComboBox<String> m_classifierBox = new JComboBox<>();

  protected MLlibClassifier m_classifier;

  protected void getClassifierFromStep() {
    if (getStepToEdit() instanceof MLlibClassifierSparkJob) {
      m_classifier =
        ((MLlibClassifierSparkJob) getStepToEdit()).getClassifier();
    } else if (getStepToEdit() instanceof MLlibClassifierEvaluationSparkJob) {
      m_classifier =
        ((MLlibClassifierEvaluationSparkJob) getStepToEdit()).getClassifier();
    } else {
      throw new IllegalArgumentException("Step to edit must be either "
        + "MLlibClassifierSparkJob or MLlibClassifierEvaluationSparkJob");
    }

    String name = m_classifier.getClass().getCanonicalName();
    name = name.substring(name.lastIndexOf(".") + 1);
    m_classifierBox.setSelectedItem(name);
  }

  public HiddenPropertyMLlibJobStepEditorDialog() {
    super();
  }

  @Override
  public void setStepToEdit(Step step) {
    copyOriginal(step);
    createAboutPanel(step);

    addPrimaryEditorPanel(BorderLayout.NORTH);
    addSecondaryEditorPanel(BorderLayout.SOUTH);

    JScrollPane scrollPane = new JScrollPane(m_editorHolder);
    add(scrollPane, BorderLayout.CENTER);

    if (step.getDefaultSettings() != null) {
      addSettingsButton();
    }

    layoutEditor();
  }

  /**
   * Make a copy of the original step
   *
   * @param step the step to copy
   */
  protected void copyOriginal(Step step) {
    m_manager = (StepManagerImpl) step.getStepManager();
    m_stepToEdit = step;
    try {
      // copy the original config in case of cancel
      m_stepOriginal = (Step) GenericObjectEditor.makeCopy(step);
    } catch (Exception ex) {
      showErrorDialog(ex);
    }
  }

  /**
   * Adds the primary editor panel to the layout
   *
   * @param borderLayoutPos the position in a {@code BorderLayout} in which to
   *          add the primary editor panel
   */
  protected void addPrimaryEditorPanel(String borderLayoutPos) {
    // hide the classifier property
    m_editor.getUserHiddenProperties().add("classifier");

    String className = m_stepToEdit.getClass().getName();

    className =
      className.substring(className.lastIndexOf('.') + 1, className.length());

    m_primaryEditorHolder.setLayout(new BorderLayout());

    m_primaryEditorHolder.setBorder(BorderFactory.createTitledBorder(className
      + " options"));
    m_editor.setUseEnvironmentPropertyEditors(true);
    m_editor.setEnvironment(m_env);
    m_editor.setTarget(m_stepToEdit);
    m_editorHolder.setLayout(new BorderLayout());
    m_editorHolder.add(m_primaryEditorHolder, borderLayoutPos);

    if (m_editor.editableProperties() > 1) {
      m_primaryEditorHolder.add(m_editor, BorderLayout.CENTER);
      m_editorHolder.add(m_primaryEditorHolder, borderLayoutPos);
    }

    m_classifierBox.addItem("MLlibDecisionTree");
    m_classifierBox.addItem("MLlibLogistic");
    m_classifierBox.addItem("MLlibNaiveBayes");
    m_classifierBox.addItem("MLlibSVM");
    m_classifierBox.addItem("MLlibRandomForest");
    m_classifierBox.addItem("MLlibGradientBoostedTrees");
    m_classifierBox.addItem("MLlibLinearRegressionSGD");
    m_classifierBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String scheme = m_classifierBox.getSelectedItem().toString();
        scheme = "." + scheme;
        try {
          MLlibClassifier newScheme =
            (MLlibClassifier) Utils
              .forName(MLlibClassifier.class, scheme, null);
          m_secondaryEditor.setTarget(newScheme);
          m_classifier = newScheme;
        } catch (Exception e1) {
          e1.printStackTrace();
        }
      }
    });



    JLabel schemeLab = new JLabel("MLlib scheme ", SwingConstants.RIGHT);
    JPanel holder = new JPanel(new BorderLayout());
    holder.add(schemeLab, BorderLayout.CENTER);
    holder.add(m_classifierBox, BorderLayout.EAST);
    m_primaryEditorHolder.add(holder, BorderLayout.NORTH);

    getClassifierFromStep();
  }

  /**
   * Add the secondary editor panel
   *
   * @param borderLayoutPos the position in a {@code BorderLayout} in which to
   *          add the secondary editor panel
   */
  protected void addSecondaryEditorPanel(String borderLayoutPos) {

    // m_secondaryEditor = new PropertySheetPanel(false);
    m_secondaryEditor.setUseEnvironmentPropertyEditors(true);
    m_secondaryEditor.getUserHiddenProperties().add("sparkJobOptions");
    m_secondaryEditor.setBorder(BorderFactory
      .createTitledBorder("Classifier/regressor options"));
    m_secondaryEditor.setEnvironment(m_env);
    m_secondaryEditor.setTarget(m_classifier);
    if (m_secondaryEditor.editableProperties() > 0
      || m_secondaryEditor.hasCustomizer()) {
      JPanel p = new JPanel();
      p.setLayout(new BorderLayout());
      p.add(m_secondaryEditor, BorderLayout.NORTH);
      m_editorHolder.add(p, borderLayoutPos);
    }

  }

  /**
   * Called when the cancel button is pressed
   */
  @Override
  protected void cancelPressed() {
    // restore original state
    if (m_stepOriginal != null && m_manager != null) {
      m_manager.setManagedStep(m_stepOriginal);
    }
  }

  /**
   * Called when the OK button is pressed
   */
  @Override
  protected void okPressed() {
    if (getStepToEdit() instanceof MLlibClassifierSparkJob) {
      ((MLlibClassifierSparkJob) getStepToEdit()).setClassifier(m_classifier);
    } else {
      ((MLlibClassifierEvaluationSparkJob) getStepToEdit())
        .setClassifier(m_classifier);
    }
    if (m_editor.hasCustomizer()) {
      m_editor.closingOK();
    }
  }
}
