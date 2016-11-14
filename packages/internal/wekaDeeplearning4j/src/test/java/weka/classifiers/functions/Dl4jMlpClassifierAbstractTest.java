package weka.classifiers.functions;

import org.deeplearning4j.nn.conf.layers.Layer;
import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.Classifier;
import weka.dl4j.layers.DenseLayer;
import weka.dl4j.layers.OutputLayer;

public class Dl4jMlpClassifierAbstractTest extends AbstractClassifierTest {

	public Dl4jMlpClassifierAbstractTest(String name) {
		super(name);
	}

	@Override
	public Classifier getClassifier() {
		Dl4jMlpClassifier mlp = new Dl4jMlpClassifier();
		DenseLayer dl = new DenseLayer();
		dl.setNOut(5);
		mlp.setLayers(new Layer[] { dl, new OutputLayer() } );
		return mlp;
	}
}
