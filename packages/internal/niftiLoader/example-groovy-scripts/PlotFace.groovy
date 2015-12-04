// Script that uses NIfTIDirectoryLoader to load
// an .nii file and plot it.

import java.awt.*
import javax.swing.*

import weka.core.Instances
import weka.core.converters.NIfTIFileLoader
import weka.core.WekaPackageManager

class PlotARFF extends JPanel {

  protected void paintComponent(Graphics g) {

    try {
      super.paintComponent(g)
      Graphics2D g2 = (Graphics2D)g

      // Load .nii file into a WEKA Instances object
      NIfTIFileLoader loader = new NIfTIFileLoader()
      loader.setSource(new File(WekaPackageManager.WEKA_HOME.toString() + File.separator + "packages" +
              File.separator + "niftiLoader" + File.separator + "example_data" + File.separator + "Class_1" +
              File.separator + "face.nii"));
      Instances data = loader.getDataSet()

      // We know that XDIM = YDIM for this data and that it's a 2D image
      int numValues = (int)Math.sqrt(data.numAttributes())

      int w = getWidth() / numValues
      int h = getHeight() / numValues

      // Go through instance in correct order and plot pixels
      int index = 0;
      for (int y = 0; y < numValues; y++) {
        for (int x = 0; x < numValues; x++) {
          int val = (int) (255.0 * data.instance(0).value(index++))
          Color color = new Color(val, val, val)
          g2.setColor(color)
          g2.fill(new Rectangle(x * w, y * h, w, h))
        }
      }
    } catch (Exception e) {
      e.printStackTrace()
    }
  }
}

f = new JFrame();
f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
f.setSize(500,500);
f.setLocation(200,200);
f.add(new PlotARFF());
f.setVisible(true);