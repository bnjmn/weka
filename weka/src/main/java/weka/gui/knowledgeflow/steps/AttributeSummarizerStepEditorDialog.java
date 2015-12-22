package weka.gui.knowledgeflow.steps;

import weka.knowledgeflow.steps.AttributeSummarizer;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class AttributeSummarizerStepEditorDialog extends ModelPerformanceChartStepEditorDialog {

  @Override
  protected void getCurrentSettings() {
    m_currentRendererName =
      ((AttributeSummarizer) getStepToEdit()).getOffscreenRendererName();
    m_currentRendererOptions =
      ((AttributeSummarizer) getStepToEdit()).getOffscreenAdditionalOpts();
  }

  @Override
  public void okPressed() {
    ((AttributeSummarizer) getStepToEdit())
      .setOffscreenRendererName(
        m_offscreenSelector.getSelectedItem().toString());
    ((AttributeSummarizer) getStepToEdit())
      .setOffscreenAdditionalOpts(m_rendererOptions.getText());
  }
}
