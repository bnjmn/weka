The bin directory in this directory contains a Hadoop file system
helper class called winutils.exe. This enables HDFS operations to
operate correctly under Windows OS's. Note that exceptions are thrown
when the Spark shutdown hook shuts the Spark context down - this is
expected as Spark is unable to delete temporary files/directories due
to Windows file locks.
