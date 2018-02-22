import weka.classifiers.CostMatrix;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Loads the cost matrix "args[0]" and prints its content to the console.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class CostMatrixLoader {
  public static void main(String[] args) throws Exception {
    CostMatrix matrix = new CostMatrix(
                          new BufferedReader(new FileReader(args[0])));
    System.out.println(matrix);
  }
}
