// From Eibe's Wekalist post:
// https://list.waikato.ac.nz/pipermail/wekalist/2016-December/041824.html

import weka.python.PythonSession as PythonSession
import weka.core.converters.ConverterUtils.DataSource as DataSource

class ExecuteCommandInPython:

    def main(self):
        instances = DataSource.read("/Users/eibe/datasets/UCI/iris.arff")
        if not PythonSession.pythonAvailable() :
            PythonSession.initSession("python", False)
        session = PythonSession.acquireSession(self)
        session.instancesToPython(instances, "py_data",False)
        outAndErr = session.executeScript("py_data = py_data.drop(py_data.columns[[4]],axis=1)", False)
        outInst = session.getDataFrameAsInstances("py_data", False)
        print outInst.toString()
        PythonSession.releaseSession(self)

ExecuteCommandInPython().main()

