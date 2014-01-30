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
 *    JRILoader.java
 *    Copyright (C) 2012-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that makes sure that key JRI classes and the native library are all
 * loaded by the root class loader.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class JRILoader {

  private final static String RSESSION_IMPL = "weka.core.RSessionImpl";
  final static String JRI_NATIVE_LOADER = "weka.core.JRINativeLoader";

  private static boolean s_isLoaded;
  private static Object s_api;

  /**
   * Inject byte code into the root class loader and make sure that the native
   * libraries are loaded (just once) and all JRI classes are loaded by the root
   * class loader
   * 
   * @return a reference to the singleton RSessionImpl object loaded and
   *         initialized in the root class loader.
   * 
   * @throws Exception if a problem occurs
   */
  public static synchronized Object load() throws Exception {
    if (s_api != null) {
      return s_api;
    }

    // check if we need to inject the native loader and dependencies
    if (!hasInjectedNativeLoader()) {
      Class<?> nativeLoader = injectJRINativeLoader();
      // System.err.println("Creating session implementation......");
      // System.err.println(nativeLoader.toString());
      // s_sessionImpl = nativeLoader.newInstance();

      s_api = Class.forName(RSESSION_IMPL).newInstance();
      /*
       * Class<?> nativeLoaderClass =
       * Class.forName("weka.core.JRINativeLoader"); Method loadMethod =
       * nativeLoaderClass.getDeclaredMethod("loadLibrary", new Class[] {
       * String.class });
       * 
       * loadMethod.invoke(null,
       * "/Library/Frameworks/R.framework/Resources/library/rJava/jri/libjri.jnilib"
       * );
       */

      // check for R_HOME
      String rHome = System.getenv("R_HOME");
      if (rHome == null || rHome.length() == 0) {
        System.err
          .println("R_HOME is undefined. Cannot proceed with R native library loading");
      } else {
        Method initMethod = nativeLoader
          .getDeclaredMethod("init", new Class[0]);
        s_api = initMethod.invoke(s_api, (Object[]) null);

        if (s_api == null) {
          throw new Exception("Unable to establish R session implementation.");
        }

        s_isLoaded = true;
      }
    } else {

      // check for R_HOME
      String rHome = System.getenv("R_HOME");
      if (rHome == null || rHome.length() == 0) {
        System.err
          .println("R_HOME is undefined. Cannot proceed with R native library loading");
      } else {

        Class<?> implClass = Class.forName(RSESSION_IMPL);

        Method singletonGrabber = implClass.getDeclaredMethod(
          "getSessionSingleton", new Class[] {});

        s_api = singletonGrabber.invoke(null, (Object[]) null);

        if (s_api == null) {
          throw new Exception("Unable to establish R session implementation.");
        }

        s_isLoaded = true;
      }
    }

    return s_api;
  }

  /**
   * Returns true if the native libraries were loaded successfully.
   * 
   * @return true if the native library was loaded successfully
   */
  public static boolean isNativeLibraryLoaded() {
    return s_isLoaded;
  }

  private static boolean hasInjectedNativeLoader() {
    try {
      final String nativeLoaderClassName = "weka.core.RSessionImpl";
      Class<?> c = Class.forName(nativeLoaderClassName);
      // If this native loader class is already defined,
      // it means that another class loader already loaded the native lib

      return true;
    } catch (ClassNotFoundException e) {
      // do loading
      return false;
    }
  }

  private static ClassLoader getRootClassLoader() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    while (cl.getParent() != null) {
      System.err.println("Getting parent classloader....");
      cl = cl.getParent();
    }
    return cl;
  }

  private static byte[] getByteCode(String resourcePath) throws IOException {
    InputStream in = RSession.class.getResourceAsStream(resourcePath);
    if (in == null) {
      throw new IOException(resourcePath + " is not found");
    }

    byte[] buf = new byte[1024];
    ByteArrayOutputStream byteCodeBuf = new ByteArrayOutputStream();
    for (int readLength; (readLength = in.read(buf)) != -1;) {
      byteCodeBuf.write(buf, 0, readLength);
    }
    in.close();

    return byteCodeBuf.toByteArray();
  }

  /**
   * Inject RSessionImpl and JRINativeLoader class to the root class loader
   * 
   * @return RSessionImp class initialized in the root class loader.
   *         Instantiating this will cause the native libraries to be loaded via
   *         JRINativeLoader (also in the root class loader)
   */
  private static Class<?> injectJRINativeLoader() throws Exception {
    final String[] PRELOAD = new String[] {
      // JIRI classes
      "org.rosuda.JRI.Mutex",
      "org.rosuda.JRI.RBool",
      "org.rosuda.JRI.RConsoleOutputStream",
      "org.rosuda.JRI.REXP",
      "org.rosuda.JRI.RFactor",
      "org.rosuda.JRI.RList",
      "org.rosuda.JRI.RMainLoopCallbacks",
      "org.rosuda.JRI.RVector",
      "org.rosuda.JRI.Rengine",

      // REngine classes
      "org.rosuda.REngine.REXP",
      "org.rosuda.REngine.REngineInputInterface",
      "org.rosuda.REngine.REngineCallbacks",
      "org.rosuda.REngine.REXPVector",
      "org.rosuda.REngine.REXPGenericVector",
      "org.rosuda.REngine.REXPExpressionVector",
      "org.rosuda.REngine.REngineException",
      "org.rosuda.REngine.REXPMismatchException",
      "org.rosuda.REngine.REXPInteger",
      "org.rosuda.REngine.REXPS4",
      "org.rosuda.REngine.REXPRaw",
      "org.rosuda.REngine.REXPReference",
      "org.rosuda.REngine.REXPLogical",
      "org.rosuda.REngine.RFactor",
      "org.rosuda.REngine.REngineOutputInterface",
      "org.rosuda.REngine.REngineStdOutput",
      "org.rosuda.REngine.REXPJavaReference",
      "org.rosuda.REngine.REXPUnknown",
      "org.rosuda.REngine.REngineUIInterface",
      "org.rosuda.REngine.REngineConsoleHistoryInterface",
      "org.rosuda.REngine.REngineEvalException",
      "org.rosuda.REngine.REXPEnvironment",
      "org.rosuda.REngine.MutableREXP",
      "org.rosuda.REngine.REXPNull",
      "org.rosuda.REngine.REXPSymbol",
      "org.rosuda.REngine.REXPString",
      "org.rosuda.REngine.REXPWrapper",
      "org.rosuda.REngine.REngine",
      "org.rosuda.REngine.REXPDouble",
      "org.rosuda.REngine.RList",
      "org.rosuda.REngine.REXPFactor",
      "org.rosuda.REngine.REXPList",
      "org.rosuda.REngine.REXPLanguage",

      // JRIEngine classes
      "org.rosuda.REngine.JRI.JRIEngine",
      "org.rosuda.REngine.JRI.JRIEngine$JRIPointer",

      // JavaGD
      "org.rosuda.javaGD.GDObject", "org.rosuda.javaGD.GDState",
      "org.rosuda.javaGD.GDContainer", "org.rosuda.javaGD.GDFill",
      "org.rosuda.javaGD.GDRect", "org.rosuda.javaGD.LocatorSync",
      "org.rosuda.javaGD.GDCanvas", "org.rosuda.javaGD.GDCanvas$Refresher",
      "org.rosuda.javaGD.GDLinePar", "org.rosuda.javaGD.GDFont",
      "org.rosuda.javaGD.JGDPanel", "org.rosuda.javaGD.JGDBufferedPanel",
      "org.rosuda.javaGD.JGDBufferedPanel$Refresher",
      "org.rosuda.javaGD.GDClip", "org.rosuda.javaGD.XGDserver",
      "org.rosuda.javaGD.XGDserver$XGDworker", "org.rosuda.javaGD.GDInterface",
      "org.rosuda.javaGD.JavaGD", "org.rosuda.javaGD.GDRaster",
      "org.rosuda.javaGD.GDLine", "org.rosuda.javaGD.GDCircle",
      "org.rosuda.javaGD.GDColor", "org.rosuda.javaGD.GDPath",
      "org.rosuda.javaGD.GDPolygon", "org.rosuda.javaGD.GDText",

      // Weka
      "weka.core.RSessionException", "weka.core.WekaJavaGD", };

    ClassLoader rootClassLoader = getRootClassLoader();

    // .bytecode classes must *not* get loaded by child class loaders. Other
    // classes
    // in PRELOAD are needed as dependencies but it is not an issue if they also
    // get loaded by child class loaders
    byte[] byteCodeLogger = getByteCode("/weka/core/RLoggerAPI.bytecode");
    byte[] byteCodeAPI = getByteCode("/weka/core/RSessionAPI.bytecode");
    byte[] byteCodeGDL = getByteCode("/weka/core/JavaGDListener.bytecode");
    byte[] byteCodeGD = getByteCode("/weka/core/JavaGDOffscreenRenderer.bytecode");
    byte[] byteCodeRSession = getByteCode("/weka/core/RSessionImpl.bytecode");
    byte[] byteCodeJRINative = getByteCode("/weka/core/JRINativeLoader.bytecode");
    byte[] byteCodeJavaGDNotifier = getByteCode("/weka/core/JavaGDNotifier.bytecode");

    List<byte[]> preloadClassByteCode = new ArrayList<byte[]>();

    for (String p : PRELOAD) {
      if (p.contains("JRI") || p.contains("javaGD") || p.contains("weka")) {
        preloadClassByteCode.add(getByteCode(String.format("/%s.class",
          p.replaceAll("\\.", "/"))));
      } else {
        // REngine classes
        // .bytecode classes must *not* get loaded by child class loaders
        preloadClassByteCode.add(getByteCode(String.format(
          "/%s.class.bytecode", p.replaceAll("\\.", "/"))));
      }
    }

    Class<?> classLoader = Class.forName("java.lang.ClassLoader");
    Method defineClass = classLoader.getDeclaredMethod("defineClass",
      new Class[] { String.class, byte[].class, int.class, int.class,
        ProtectionDomain.class });

    ProtectionDomain pd = System.class.getProtectionDomain();

    // ClassLoader.defineClass is a protected method, so we have to make it
    // accessible
    defineClass.setAccessible(true);

    try {

      // Create a new class using a ClassLoader#defineClass
      System.err.println("Injecting JRI classes into the root class loader...");
      // Define dependent classes in the root class loader
      for (int i = 0; i < PRELOAD.length; ++i) {
        // System.err.println(PRELOAD[i]);
        byte[] b = preloadClassByteCode.get(i);
        defineClass.invoke(rootClassLoader, PRELOAD[i], b, 0, b.length, pd);
      }

      // Create REngineImpl class and other directly dependent classes from byte
      // code
      defineClass.invoke(rootClassLoader, "weka.core.RLoggerAPI",
        byteCodeLogger, 0, byteCodeLogger.length, pd);
      defineClass.invoke(rootClassLoader, "weka.core.RSessionAPI", byteCodeAPI,
        0, byteCodeAPI.length, pd);
      defineClass.invoke(rootClassLoader, "weka.core.JavaGDListener",
        byteCodeGDL, 0, byteCodeGDL.length, pd);
      defineClass.invoke(rootClassLoader, "weka.core.JavaGDNotifier",
        byteCodeJavaGDNotifier, 0, byteCodeJavaGDNotifier.length, pd);
      defineClass.invoke(rootClassLoader, "weka.core.JavaGDOffscreenRenderer",
        byteCodeGD, 0, byteCodeGD.length, pd);
      defineClass.invoke(rootClassLoader, JRI_NATIVE_LOADER, byteCodeJRINative,
        0, byteCodeJRINative.length, pd);
      defineClass.invoke(rootClassLoader, RSESSION_IMPL, byteCodeRSession, 0,
        byteCodeRSession.length, pd);
    } finally {
      // Reset the accessibility to defineClass method
      defineClass.setAccessible(false);
    }
    return rootClassLoader.loadClass(RSESSION_IMPL);
  }
}
