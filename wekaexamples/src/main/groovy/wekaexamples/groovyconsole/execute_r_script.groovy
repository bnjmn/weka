// From Eibe's Wekalist post:
// https://list.waikato.ac.nz/pipermail/wekalist/2016-December/041824.html

import weka.core.RSession;
import weka.core.RUtils;
import weka.core.converters.ConverterUtils.DataSource

instances = DataSource.read("/Users/eibe/datasets/UCI/iris.arff")
session = RSession.acquireSession(this)
RUtils.instancesToDataFrame(session, this, instances, "rdata")
result = session.parseAndEval(this, "rdata = rdata[1:4]")
outInst = RUtils.dataFrameToInstances(result)
println outInst.toString()
RSession.releaseSession(this)

