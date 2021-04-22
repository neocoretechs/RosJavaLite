package org.ros.internal.jarclassloader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
* <code>
* <p>
* Known issues: some temporary files created by class loader are not deleted
* on application exit because JVM does not close handles to them.
* See details in {@link #shutdown()}.
* <p>
* Unfortunately, the native method java.lang.ClassLoader$NativeLibrary.unload()
* is package accessed in a package accessed inner class.
* Moreover, it's called from finalizer. This does not allow releasing
* the native library handle and delete the temporary library file.
* Option to explore: use JNI function UnregisterNatives(). See also
* native code in ...\jdk\src\share\native\java\lang\ClassLoader.class
*</code>
*/
	public class JarClassLoader extends ClassLoader {
		private static boolean DEBUG = true;
		private static boolean DEBUGLOADCLASS = false;
		private static ConcurrentHashMap<String,Class> cache = new ConcurrentHashMap<String,Class>();

	    /**
	     * Sub directory name for temporary files.
	     * <p>
	     * JarClassLoader extracts all JARs and native libraries into temporary files
	     * and makes the best attempt to clean these files on exit.
	     * <p>
	     * The sub directory is created in the directory defined in a system
	     * property "java.io.tmpdir". Verify the content of this directory
	     * periodically and empty it if required. Temporary files could accumulate
	     * there if application was killed.
	     */
	    private List<JarFileInfo> lstJarFile;
	    private Set<File> hsDeleteOnExit;
	    private Map<String, Class<?>> hmClass;
	    
	    /**
	     * Default constructor.
	     * Defines system class loader as a parent class loader.
	     */
	    public JarClassLoader() {
	        this(ClassLoader.getSystemClassLoader());
	    }

	    /**
	     * Constructor.
	     *
	     * @param parent class loader parent.
	     */
	    public JarClassLoader(ClassLoader parent) {
	        super(parent);
	        hmClass = new HashMap<String, Class<?>>();
	        lstJarFile = new ArrayList<JarFileInfo>();
	        hsDeleteOnExit = new HashSet<File>();
	        // Prepare common for all protocols 
	        String sUrlTopJar = null;
	        ProtectionDomain pdTop = getClass().getProtectionDomain();
	        CodeSource cs = pdTop.getCodeSource();
	        URL urlTopJar = cs.getLocation();
	        String protocol = urlTopJar.getProtocol();  
	        // Work with different cases:
	        JarFileInfo jarFileInfo = null;
	        if ("http".equals(protocol) || "https".equals(protocol)) {
	            // Protocol 'http' or 'https' - application launched from WebStart / JNLP or as Java applet
	            try {
	                // Convert:
	                //   urlTopJar = "http://.../MyApp.jar" --> connection sun.net.www.protocol.http.HttpURLConnection
	                // to
	                //   urlTopJar = "jar:http://.../MyApp.jar!/" --> connection java.net.JarURLConnection
	                urlTopJar = new URL("jar:" + urlTopJar + "!/");
	                JarURLConnection jarCon = (JarURLConnection)urlTopJar.openConnection();
	                JarFile jarFile = jarCon.getJarFile();
	                jarFileInfo = new JarFileInfo(jarFile, jarFile.getName(), null, pdTop, null);
	                if(DEBUG)
	                	System.out.printf("Loading from top JAR: '%s' PROTOCOL: '%s'%n", 
	                        urlTopJar, protocol);
	            } catch (Exception e) {
	                // ClassCastException, IOException
	                System.out.printf("Failure to load HTTP JAR: %s %s%n", urlTopJar, e.toString());
	                return;
	            }
	        }        
	        if ("file".equals(protocol)) {
	            // Protocol 'file' - application launched from exploded dir or JAR 
	            // Decoding required for 'space char' in URL: 
	            //    URL.getFile() returns "/C:/my%20dir/MyApp.jar" for "/C:/my dir/MyApp.jar" 
	            try {
	            	if(DEBUG)
	            		System.out.printf("%s.JarClassLoader(%s) Calling with:%s%n", this.getClass().getName(), parent, sUrlTopJar);
	                sUrlTopJar = URLDecoder.decode(urlTopJar.getFile(), "UTF-8");
	                if(DEBUG)
	                	System.out.printf("%s.JarClassLoader(%s) Decoded to :%s%n", this.getClass().getName(), parent, sUrlTopJar);
	            } catch (UnsupportedEncodingException e) {
	            	System.out.printf( "Failure to decode URL: %s %s%n", urlTopJar, e.toString());
	                return;
	            }
	            File fileJar = new File(sUrlTopJar);
	            
	            // Application is loaded from directory: 
	            if (fileJar.isDirectory()) {
	            	if(DEBUG)
	            		System.out.printf("Loading from exploded directory: %s%n", sUrlTopJar);
	                return; // JarClassLoader completed its job
	            }
	            
	            // Application is loaded from a JAR:
	            try {
	                jarFileInfo = new JarFileInfo(new JarFile(fileJar), fileJar.getName(), null, pdTop, null);
	                if(DEBUG)
	                	System.out.printf("Loading from top JAR: '%s' PROTOCOL: '%s'%n", sUrlTopJar, protocol);
	            } catch (IOException e) { 
	            	System.out.printf("Not a JAR: %s %s%n", sUrlTopJar, e.toString());
	                return;
	            }
	        }
	        
	        // FINALLY LOAD TOP JAR:
	        try {
	            if (jarFileInfo == null) {
	                throw new IOException(String.format(
	                    "Unknown protocol %s", protocol));
	            }
	            loadJar(jarFileInfo); // start recursive JAR loading
	        } catch (IOException e) {
	        	System.out.printf("Not valid URL: %s %s%n", urlTopJar, e.toString());
	            return;
	        }
	        
	        checkShading();
	        Runtime.getRuntime().addShutdownHook(new Thread() {
	            public void run() {
	                shutdown();
	            }
	        });
	    }
	    
	    /**
	     * Loads a Jar from the specified JAR file.           
	     *  Protocol 'file' - application launched from exploded dir or JAR 
	     *  Decoding required for 'space char' in URL: 
	     *  URL.getFile() returns "/C:/my%20dir/MyApp.jar" for "/C:/my dir/MyApp.jar" 
	     * @param jarFile
	     */
	    public void loadJarFromJarfile(String sUrlTopJar) {
	    	JarFileInfo jarFileInfo;
	        // Prepare common for Protocol 'file' - application launched from exploded dir or JAR 
            // Decoding required for 'space char' in URL: 
            // URL.getFile() returns "/C:/my%20dir/MyApp.jar" for "/C:/my dir/MyApp.jar"
	    	URL urlTopJar = null;
            try {
    	        urlTopJar = new URL(sUrlTopJar);
    	        if(DEBUG)
    	        	System.out.printf("%s.loadJarFromJarFile(%s) Calling with:%s%n", this.getClass().getName(), sUrlTopJar, urlTopJar);
                sUrlTopJar = URLDecoder.decode(urlTopJar.getFile(), "UTF-8");
                if(DEBUG)
                	System.out.printf("%s.loadJarFromJarFile(%s) Decoded to:%s%n", this.getClass().getName(), sUrlTopJar, urlTopJar);
            } catch (UnsupportedEncodingException | MalformedURLException e) {
            	System.out.printf( "Failure to decode URL: %s %s%n", urlTopJar, e.toString());
                return;
            }
            //Certificate[] certs = null;
	    	//CodeSource cs = new CodeSource(urlTopJar, certs);
	        //ProtectionDomain pdTop = new ProtectionDomain(cs, null,this,null);
            ProtectionDomain pdTop = getClass().getProtectionDomain();
            
            File fileJar = new File(sUrlTopJar);
	                 
            // Application is loaded from directory: 
            if (fileJar.isDirectory()) {
            	if(DEBUG)
            		System.out.printf("Loading from exploded directory: %s%n", sUrlTopJar);
                return; // JarClassLoader completed its job
            }
            
            // Application is loaded from a JAR:
            try {
                jarFileInfo = new JarFileInfo(new JarFile(fileJar), fileJar.getName(), null, pdTop, null);
                if(DEBUG)
                	System.out.printf("Loading from top JAR: '%s' PROTOCOL: '%s'%n", sUrlTopJar, "file");
            } catch (IOException e) { 
            	System.out.printf("Not a JAR: %s %s%n", sUrlTopJar, e.toString());
                return;
            }    
        // FINALLY LOAD TOP JAR:
        try {
            loadJar(jarFileInfo); // start recursive JAR loading
        } catch (IOException e) {
        	System.out.printf("Not valid URL: %s %s%n", urlTopJar, e.toString());
            return;
        }
        
        checkShading();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });
	
	    }
	    
	    /**
	     * Loads specified JAR.
	     *
	     * @param jarFileInfo
	     * @throws IOException
	     */
	    private void loadJar(JarFileInfo jarFileInfo) throws IOException {
	        lstJarFile.add(jarFileInfo);
	        try {
	            Enumeration<JarEntry> en = jarFileInfo.jarFile.entries();
	            final String EXT_JAR = ".jar";
	            while (en.hasMoreElements()) {
	                JarEntry je = en.nextElement();
	                if (je.isDirectory()) {
	                    continue;
	                }
	                String s = je.getName().toLowerCase(); // JarEntry name
	                if (s.lastIndexOf(EXT_JAR) == s.length() - EXT_JAR.length()) {
	                    JarEntryInfo inf = new JarEntryInfo(jarFileInfo, je);
	                    File fileTemp = CreateTempFile.createTempFile(inf);
	                    if(DEBUG)
	                    	System.out.printf("Loading inner JAR %s from temp file %s %n",
	                            inf.jarEntry, fileTemp);
	                    // Construct ProtectionDomain for this inner JAR:
	                    URL url = fileTemp.toURI().toURL();
	                    ProtectionDomain pdParent = jarFileInfo.pd;
	                    // 'csParent' is never null: top JAR has it, JCL creates it for child JAR:
	                    CodeSource csParent = pdParent.getCodeSource();  
	                    Certificate[] certParent = csParent.getCertificates();
	                    CodeSource csChild = (certParent == null ? new CodeSource(url, csParent.getCodeSigners())
	                                                             : new CodeSource(url, certParent));
	                    ProtectionDomain pdChild = new ProtectionDomain(csChild, 
	                            pdParent.getPermissions(), pdParent.getClassLoader(), pdParent.getPrincipals());
	                    loadJar(new JarFileInfo(
	                            new JarFile(fileTemp), inf.getName(), jarFileInfo, pdChild, fileTemp));
	                }
	            }
	        } catch (JarException e) {
	            throw new RuntimeException(
	                    "ERROR on loading inner JAR: " + e.getMessage());
	        }
	    } // loadJar()
	    
	    private JarEntryInfo findJarEntry(String sName) {
	        for (JarFileInfo jarFileInfo : lstJarFile) {
	            JarFile jarFile = jarFileInfo.jarFile;
	            JarEntry jarEntry = jarFile.getJarEntry(sName);
	            if (jarEntry != null) {
	                return new JarEntryInfo(jarFileInfo, jarEntry);
	            }
	        }
	        return null;
	    } // findJarEntry()

	    private List<JarEntryInfo> findJarEntries(String sName) {
	        List<JarEntryInfo> lst = new ArrayList<JarEntryInfo>();
	        for (JarFileInfo jarFileInfo : lstJarFile) {
	            JarFile jarFile = jarFileInfo.jarFile;
	            JarEntry jarEntry = jarFile.getJarEntry(sName);
	            if (jarEntry != null) {
	                lst.add(new JarEntryInfo(jarFileInfo, jarEntry));
	            }
	        }
	        return lst;
	    } // findJarEntries()

	    /**
	     * Finds native library entry.
	     *
	     * @param sLib Library name. For example for the library name "Native"
	     *  - Windows returns entry "Native.dll"
	     *  - Linux returns entry "libNative.so"
	     *  - Mac returns entry "libNative.jnilib" or "libNative.dylib"
	     *    (depending on Apple or Oracle JDK and/or JDK version)
	     * @return Native library entry.
	     */
	    private JarEntryInfo findJarNativeEntry(String sLib) {
	        String sName = System.mapLibraryName(sLib);
	        for (JarFileInfo jarFileInfo : lstJarFile) {
	            JarFile jarFile = jarFileInfo.jarFile;
	            Enumeration<JarEntry> en = jarFile.entries();
	            while (en.hasMoreElements()) {
	                JarEntry je = en.nextElement();
	                if (je.isDirectory()) {
	                    continue;
	                }
	                // Example: sName is "Native.dll"
	                String sEntry = je.getName(); // "Native.dll" or "abc/xyz/Native.dll"
	                // sName "Native.dll" could be found, for example
	                //   - in the path: abc/Native.dll/xyz/my.dll <-- do not load this one!
	                //   - in the partial name: abc/aNative.dll   <-- do not load this one!
	                String[] token = sEntry.split("/"); // the last token is library name
	                if (token.length > 0 && token[token.length - 1].equals(sName)) {
	                	if(DEBUG)
	                		System.out.printf("Loading native library '%s' found as '%s' in JAR %s%n",
	                            sLib, sEntry, jarFileInfo.simpleName);
	                    return new JarEntryInfo(jarFileInfo, je);
	                }
	            }
	        }
	        return null;
	    } // findJarNativeEntry()

	    /**
	     * Loads class from a JAR and searches for all jar-in-jar.
	     *
	     * @param sClassName class to load.
	     * @return Loaded class.
	     * @throws JarClassLoaderException.
	     */
	    private Class<?> findJarClass(String sClassName) throws JarException {
	        Class<?> c = hmClass.get(sClassName);
	        if (c != null) {
	            return c;
	        }
	        // Char '/' works for Win32 and Unix.
	        String sName = sClassName.replace('.', '/') + ".class";
	        JarEntryInfo inf = findJarEntry(sName);
	        String jarSimpleName = null;
	        if (inf != null) {
	            jarSimpleName = inf.jarFileInfo.simpleName;
	            definePackage(sClassName, inf);
	            byte[] a_by = inf.getJarBytes();
	            try {
	                c = defineClass(sClassName, a_by, 0, a_by.length, inf.jarFileInfo.pd);
	            } catch (ClassFormatError e) {
	                throw new JarException(e.getMessage());
	            }
	        }
	        if (c == null) {
	            throw new JarException(sClassName);
	        }
	        hmClass.put(sClassName, c);
	        if(DEBUG)
	        	System.out.printf( "Loaded %s by %s from JAR %s%n",
	                sClassName, getClass().getName(), jarSimpleName);
	        return c;
	    } // findJarClass()

	    private void checkShading() {
	    
	        Map<String, JarFileInfo> hm = new HashMap<String, JarFileInfo>();
	        for (JarFileInfo jarFileInfo : lstJarFile) {
	            JarFile jarFile = jarFileInfo.jarFile;
	            Enumeration<JarEntry> en = jarFile.entries();
	            while (en.hasMoreElements()) {
	                JarEntry je = en.nextElement();
	                if (je.isDirectory()) {
	                    continue;
	                }
	                String sEntry = je.getName(); // "Some.txt" or "abc/xyz/Some.txt"
	                if ("META-INF/MANIFEST.MF".equals(sEntry)) {
	                    continue;
	                }
	                JarFileInfo jar = hm.get(sEntry);
	                if (jar == null) {
	                    hm.put(sEntry, jarFileInfo);
	                } else {
	                	 //System.out.printf("ENTRY %s IN %s SHADES %s%n",
	                            //sEntry, jar.simpleName, jarFileInfo.simpleName);
	                }
	            }
	        }
	    } // checkShading()
	    
	    /**
	     * Called on shutdown to cleanup temporary files.
	     * <p>
	     * JVM does not close handles to native libraries files or JARs with
	     * resources loaded as getResourceAsStream(). Temp files are not deleted
	     * even if they are marked deleteOnExit(). They also fail to delete explicitly.
	     * Workaround is to preserve list with temp files in configuration file
	     * "[user.home]/.JarClassLoader" and delete them on next application run.
	     * <p>
	     * See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4171239
	     * "This occurs only on Win32, which does not allow a file to be deleted
	     * until all streams on it have been closed."
	     */
	    private void shutdown() {
	        for (JarFileInfo jarFileInfo : lstJarFile) {
	            try {
	                jarFileInfo.jarFile.close();
	            } catch (IOException e) {
	                // Ignore. In the worst case temp files will accumulate.
	            }
	            File file = jarFileInfo.fileDeleteOnExit;
	            if (file != null  &&  !file.delete()) {
	                hsDeleteOnExit.add(file);
	            }
	        }
	        // Private configuration file with failed to delete temporary files:
	        //   WinXP: C:/Documents and Settings/username/.JarClassLoader
	        //    Unix: /export/home/username/.JarClassLoader
	        //           -or-  /home/username/.JarClassLoader
	        File fileCfg = new File(System.getProperty("user.home") + File.separator + ".JarClassLoader");
	        deleteOldTemp(fileCfg);
	        persistNewTemp(fileCfg);
	    } // shutdown()

	    /**
	     * Deletes temporary files listed in the file.
	     * The method is called on shutdown().
	     *
	     * @param fileCfg file with temporary files list.
	     */
	    private void deleteOldTemp(File fileCfg) {
	        BufferedReader reader = null;
	        try {
	            int count = 0;
	            reader = new BufferedReader(new FileReader(fileCfg));
	            String sLine;
	            while ((sLine = reader.readLine()) != null) {
	                File file = new File(sLine);
	                if (!file.exists()) {
	                    continue; // already deleted; from command line?
	                }
	                if (file.delete()) {
	                    count++;
	                } else {
	                    // Cannot delete, will try next time.
	                    hsDeleteOnExit.add(file);
	                }
	            }
	            if(DEBUG)
	            	System.out.printf( "Deleted %d old temp files listed in %s%n", count, fileCfg.getAbsolutePath());
	        } catch (IOException e) {
	            // Ignore. This file may not exist.
	        } finally {
	            if (reader != null) {
	                try { reader.close(); } catch (IOException e) { }
	            }
	        }
	    } // deleteOldTemp()

	    /**
	     * Creates file with temporary files list. This list will be used to
	     * delete temporary files on the next application launch.
	     * The method is called from shutdown().
	     *
	     * @param fileCfg file with temporary files list.
	     */
	    private void persistNewTemp(File fileCfg) {
	        if (hsDeleteOnExit.size() == 0) {
	        	if(DEBUG)
	        		System.out.printf( "No temp file names to persist on exit.%n");
	            fileCfg.delete(); // do not pollute disk
	            return;
	        }
	        if(DEBUG)
	        	System.out.printf("Persisting %d temp file names into %s%n",
	                hsDeleteOnExit.size(), fileCfg.getAbsolutePath());
	        BufferedWriter writer = null;
	        try {
	            writer = new BufferedWriter(new FileWriter(fileCfg));
	            for (File file : hsDeleteOnExit) {
	                if (!file.delete()) {
	                    String f = file.getCanonicalPath();
	                    writer.write(f);
	                    writer.newLine();
	                    System.out.printf("JVM failed to release %s", f);
	                }
	            }
	        } catch (IOException e) {
	            // Ignore. In the worst case temp files will accumulate.
	        } finally {
	            if (writer != null) {
	                try { writer.close(); } catch (IOException e) { }
	            }
	        }
	    } // persistNewTemp()
	    
	    /**
	     * Checks how the application was loaded: from JAR or file system.
	     *
	     * @return true if application was started from JAR.
	     */
	    public boolean isLaunchedFromJar() {
	        return (lstJarFile.size() > 0);
	    } // isLaunchedFromJar()

	    /**
	     * Returns the name of the jar file main class, or null if
	     * no "Main-Class" manifest attributes was defined.
	     *
	     * @return Main class declared in JAR's manifest.
	     */
	    public String getManifestMainClass() {
	        Attributes attr = null;
	        if (isLaunchedFromJar()) {
	            try {
	                // The first element in array is the top level JAR
	                Manifest m = lstJarFile.get(0).jarFile.getManifest();
	                attr = m.getMainAttributes();
	            } catch (IOException e) {
	            }
	        }
	        return (attr == null ? null : attr.getValue(Attributes.Name.MAIN_CLASS));
	    } // getManifestMainClass()

	    /**
	     * Invokes main() method on class with provided parameters.
	     *
	     * @param sClass class name in form "MyClass" for default package
	     * or "com.abc.MyClass" for class in some package
	     *
	     * @param args arguments for the main() method or null.
	     *
	     * @throws Throwable wrapper for many exceptions thrown while
	     * <p>(1) main() method lookup:
	     *        ClassNotFoundException, SecurityException, NoSuchMethodException
	     * <p>(2) main() method launch:
	     *        IllegalArgumentException, IllegalAccessException (disabled)
	     * <p>(3) Actual cause of InvocationTargetException
	     *
	     * See
	     * {@link http://java.sun.com/developer/Books/javaprogramming/JAR/api/jarclassloader.html}
	     * and
	     * {@link http://java.sun.com/developer/Books/javaprogramming/JAR/api/example-1dot2/JarClassLoader.java}
	     */
	    public void invokeMain(String sClass, String[] args) throws Throwable {
	    	invokeMethod(sClass, "main", args);
	    } // invokeMain()

	    public void invokeMethod(String sClass, String sMethod, String[] args) throws Throwable {
	        Class<?> clazz = loadClass(sClass);
	        invokeMethod(clazz, sMethod, args);
	    }
	    
	    public void invokeMethod(Class clazz, String sMethod, String[] args) throws Throwable  {
	        System.out.printf("Launch: %s.%s(); Loader: %s%n", clazz.getSimpleName(), sMethod, clazz.getClassLoader());
	        Method method = clazz.getMethod(sMethod, new Class<?>[] { String[].class });

	        boolean bValidModifiers = false;
	        boolean bValidVoid = false;

	        if (method != null) {
	            method.setAccessible(true); // Disable IllegalAccessException
	            int nModifiers = method.getModifiers(); // method() must be "public static"
	            bValidModifiers = Modifier.isPublic(nModifiers) &&
	                              Modifier.isStatic(nModifiers);
	            Class<?> clazzRet = method.getReturnType(); // method() must be "void"
	            bValidVoid = (clazzRet == void.class);
	        }
	        if (method == null  ||  !bValidModifiers  ||  !bValidVoid) {
	            throw new NoSuchMethodException(
	                    "The "+sMethod+"() method in class \"" + clazz.toGenericString() + "\" not found.");
	        }

	        // Invoke method.
	        // Crazy cast "(Object)args" because param is: "Object... args"
	        try {
	            method.invoke(null, (Object)args);
	        } catch (InvocationTargetException e) {
	            throw e.getTargetException();
	        }
	    }
	    
	    public Object invokeMethodReturn(Class clazz, String sMethod, Class<?>[] args) throws Throwable  {
	        System.out.printf("Launch: %s.%s(); Loader: %s%n", clazz.getSimpleName(), sMethod, clazz.getClassLoader());
	        Method method = clazz.getMethod(sMethod, args);

	        boolean bValidModifiers = false;
	        if (method != null) {
	            method.setAccessible(true); // Disable IllegalAccessException
	            int nModifiers = method.getModifiers(); // method() must be "public static"
	            bValidModifiers = Modifier.isPublic(nModifiers) &&
	                              Modifier.isStatic(nModifiers);
	           // Class<?> clazzRet = method.getReturnType(); // method() must be "void"          
	        }
	        if (method == null  ||  !bValidModifiers) {
	            throw new NoSuchMethodException(
	                    "The "+sMethod+"() method in class \"" + clazz.toGenericString() + "\" not found.");
	        }

	        try {
	            return method.invoke(null, (Object[])args);
	        } catch (InvocationTargetException e) {
	            throw e.getTargetException();
	        }
	    }
	    /**
	     * Class loader JavaDoc encourages overriding findClass(String) in derived
	     * class rather than overriding this method. This does not work for
	     * loading classes from a JAR. Default implementation of loadClass() is
	     * able to load a class from a JAR without calling findClass().
	     */
	    /*
	    @Override
	    protected synchronized Class<?> loadClass(String sClassName, boolean bResolve)
	    throws ClassNotFoundException {
	    	System.out.printf("LOADING %s (resolve=%b)%n", sClassName, bResolve);
	        // Each thread must have THIS class loader set as a context class loader. 
	        // This is required to prevent failure finding a class or resource from  
	        // external JAR requested by a common class loaded from rt.jar.  
	        // The best example is external LnF, explained in steps:
	        // 1. Application requests 'javax.swing.JOptionPane'.
	        // 2. THIS class loader passes request to system default class loader 
	        // to load the class from rt.jar.
	        // 3. The class 'javax.swing.JOptionPane' is loaded by system default class
	        // loader.
	        // 4. The class 'javax.swing.JOptionPane' is requesting 'UIDefaults.getUI()'
	        // for component, which resides in external LnF JAR.
	        // 5. The class loader which is used to load the requested component is 
	        // current thread context class loader if it is set, otherwise the parent  
	        // thread context class loader, or the default system class loader
	        // for the top level thread. 
	        // 6. The system class loader is used to load requested component if
	        // thread context class loader is not set. The default system class loader is 
	        //   - sun.misc.Launcher$AppClassLoader - run from file system or JAR
	        //   - com.sun.jnlp.JNLPClassLoader     - run from JNLP
	        // System class loaders cannot find requested component in external 
	        // JAR and throw exception.
	        // 
	        // Setting thread context class loader for the top thread in invokeMain()
	        // method is sufficient for most cases. It fails for new threads created
	        // not from the main thread.
	        //
	        // Setting thread context class loader below must be reconsidered 
	        // for specific conditions.
	        //
	        // Essential reading:
	        //   - Thread.getContextClassLoader() JavaDoc.
	        //   - http://www.javaworld.com/javaworld/javaqa/2003-06/01-qa-0606-load.html
	        Thread.currentThread().setContextClassLoader(this);
	        
	        Class<?> c = null;
	        try {
	            // Step 0. This class is already loaded by system classloader.
	            if (getClass().getName().equals(sClassName)) {
	                return JarClassLoader.class;
	            }
	            // Step 1. Load from JAR.
	            if (isLaunchedFromJar()) {
	                try {
	                    c = findJarClass(sClassName); // Do not simplify! See "finally"!
	                    return c;
	                } catch (JarException e) {
	                    if (e.getCause() == null) {
	                    	System.out.printf("Not found %s in JAR by %s: %s%n",
	                                sClassName, getClass().getName(), e.getMessage());
	                    } else {
	                    	System.out.printf("Error loading %s in JAR by %s: %s%n",
	                                sClassName, getClass().getName(), e.getCause());
	                    }
	                    // keep looking...
	                }
	            }
	            // Step 2. Load by parent (usually system) class loader.
	            // Call findSystemClass() AFTER attempt to find in a JAR.
	            // If it called BEFORE it will load class-in-jar using
	            // SystemClassLoader and "infect" it with SystemClassLoader.
	            // The SystemClassLoader will be used to load all dependent
	            // classes. SystemClassLoader will fail to load a class from
	            // jar-in-jar and to load dll-in-jar.
	            try {
	                // No need to call findLoadedClass(sClassName) because it's called inside:
	                ClassLoader cl = getParent();
	                c = cl.loadClass(sClassName);
	                // System classloader does not define ProtectionDomain->CodeSource - null 
	                System.out.printf( "Loaded %s by %s%n", sClassName, cl.getClass().getName());
	                return c;
	            } catch (ClassNotFoundException e) {
	            }
	            // What else?
	            throw new ClassNotFoundException("Failure to load: " + sClassName);
	        } finally {
	            if (c != null  &&  bResolve) {
	                resolveClass(c);
	            }
	        }
	    } // loadClass()
	     */
	    /**
	     * loadClass will attempt to load the named class, If not found in cache
	     * or system or user, will attempt to use Hastable of name and bytecodes
	     * set up from defineClasses.  defineClass will call this on attempting
	     * to resolve a class, so we have to be ready with the bytes.
	     * @param name The name of the class to load
	     * @param resolve true to call resolveClass()
	     * @return The resolved Class Object
	     * @throws ClassNotFoundException If we can't load the class from system, or loaded, or cache
	     */
	     public synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
	     	if(DEBUG)
	     		System.out.println("DEBUG:"+this+".loadClass("+name+")");
	         Class c = null;
	         try {
	         	 c = Class.forName(name); // can it be loaded by normal means? and initialized?
	         	 return c;
	         } catch(Exception e) {
	         	if(DEBUGLOADCLASS) {
	         		System.out.println("DEBUG:"+this+".loadClass Class.forName("+name+") exception "+e);
	         		e.printStackTrace();
	         	}
	         } 
	         try {
	             c = findSystemClass(name);
	         } catch (Exception e) {
	         	if(DEBUGLOADCLASS) {
	               System.out.println("DEBUG:"+this+".loadClass findSystemClass("+name+") exception "+e);
	               e.printStackTrace();
	         	}
	         }
	         if (c == null) {
	             c = cache.get(name);
	         } else {
	         	if(DEBUG)
	         		System.out.println("DEBUG:"+this+".loadClass exit found sys class "+name+" resolve="+resolve);
	             return c;
	         }
	         if (c == null) {
	             c = findLoadedClass(name);
	         } else {
	         	if(DEBUG)
	         		System.out.println("DEBUG:"+this+".loadClass exit cache hit:"+c+" for "+name+" resolve="+resolve);
	             return c;
	         }
	         // this is our last chance, otherwise noClassDefFoundErr and we're screwed
	         if (c == null) {
	        	 try {
	                 c = findJarClass(name);
	                 if(DEBUG)
	                 	System.out.println("DEBUG:"+this+" Putting class "+name+" of class "+c+" to cache");
	                 cache.put(name, c);
	        	 } catch (JarException e) {
	                    if (e.getCause() == null) {
	                    	System.out.printf("Not found %s in JAR by %s: %s%n",
	                                name, getClass().getName(), e.getMessage());
	                    } else {
	                    	System.out.printf("Error loading %s in JAR by %s: %s%n",
	                                name, getClass().getName(), e.getCause());
	                    }
	                    //end of the line
	                    throw new ClassNotFoundException("The requested class: "+name+" can not be found on any resource path");
	                }
	         } else {
	         	if(DEBUG)
	                System.out.println("DEBUG:"+this+".loadClass exit found loaded "+name+" resolve="+resolve);
	             	return c;
	         }
	         //if (resolve)
	             resolveClass(c);
	             if(DEBUG)
	         	   System.out.println("DEBUG:"+this+".loadClass exit resolved "+name+" resolve="+resolve);
	         return c;
	     }
	    /**
	     * @see java.lang.ClassLoader#findResource(java.lang.String)
	     *
	     * @return A URL object for reading the resource, or null if the resource could not be found.
	     * Example URL: jar:file:C:\...\some.jar!/resources/InnerText.txt
	     */
	    @Override
	    protected URL findResource(String sName) {
	    	if(DEBUG)
	    		System.out.printf("findResource: %s%n", sName);
	        if (isLaunchedFromJar()) {
	            JarEntryInfo inf = findJarEntry(normalizeResourceName(sName));
	            if (inf != null) {
	                URL url = inf.getURL();
	                if(DEBUG)
	                	System.out.printf("found resource: %s%n", url);
	                return url;
	            }
	            if(DEBUG)
	            	System.out.printf("not found resource: %s%n", sName);
	            return null;
	        }
	        return super.findResource(sName);
	    } // findResource()

	    /**
	     * @see java.lang.ClassLoader#findResources(java.lang.String)
	     *
	     * @return  An enumeration of {@link java.net.URL <tt>URL</tt>} objects for
	     *          the resources
	     */
	    @Override
	    public Enumeration<URL> findResources(String sName) throws IOException {
	    	if(DEBUG)
	    		System.out.printf("getResources: %s%n", sName);
	        if (isLaunchedFromJar()) {
	            List<JarEntryInfo> lstJarEntry = findJarEntries(normalizeResourceName(sName));
	            List<URL> lstURL = new ArrayList<URL>();
	            for (JarEntryInfo inf : lstJarEntry) {
	                URL url = inf.getURL();
	                if (url != null) {
	                    lstURL.add(url);
	                }
	            }
	            return Collections.enumeration(lstURL);
	        }
	        return super.findResources(sName);
	    } // findResources()

	    /**
	     * @see java.lang.ClassLoader#findLibrary(java.lang.String)
	     *
	     * @return The absolute path of the native library.
	     */
	    @Override
	    protected String findLibrary(String sLib) {
	    	if(DEBUG)
	    		System.out.printf("findLibrary: %s%n", sLib);
	        if (isLaunchedFromJar()) {
	            JarEntryInfo inf = findJarNativeEntry(sLib);
	            if (inf != null) {
	                try {
	                    File file = CreateTempFile.createTempFile(inf);
	                    if(DEBUG)
	                    	System.out.printf( "Loading native library %s%n",inf.jarEntry);
	                    hsDeleteOnExit.add(file);
	                    return file.getAbsolutePath();
	                } catch (JarException e) {
	                    System.out.printf("Failure to load native library %s: %s%n", sLib, e.toString());
	                }
	            }
	            return null;
	        }
	        return super.findLibrary(sLib);
	    } // findLibrary()
	    
	    /**
	     * The default <code>ClassLoader.defineClass()</code> does not create package
	     * for the loaded class and leaves it null. Each package referenced by this
	     * class loader must be created only once before the
	     * <code>ClassLoader.defineClass()</code> call.
	     * The base class <code>ClassLoader</code> keeps cache with created packages
	     * for reuse.
	     *
	     * @param sClassName class to load.
	     * @throws  IllegalArgumentException
	     *          If package name duplicates an existing package either in this
	     *          class loader or one of its ancestors.
	     */
	    private void definePackage(String sClassName, JarEntryInfo inf)
	    throws IllegalArgumentException {
	        int pos = sClassName.lastIndexOf('.');
	        String sPackageName = pos > 0 ? sClassName.substring(0, pos) : "";
	        if (getPackage(sPackageName) == null) {
	            JarFileInfo jfi = inf.jarFileInfo;
	            definePackage(sPackageName,
	                jfi.getSpecificationTitle(), jfi.getSpecificationVersion(),
	                jfi.getSpecificationVendor(), jfi.getImplementationTitle(),
	                jfi.getImplementationVersion(), jfi.getImplementationVendor(),
	                jfi.getSealURL());
	        }
	    }

	    /**
	     * The system class loader could load resources defined as
	     * "com/abc/Foo.txt" or "com\abc\Foo.txt".
	     * This method converts path with '\' to default '/' JAR delimiter.
	     *
	     * @param sName resource name including path.
	     * @return normalized resource name.
	     */
	    private String normalizeResourceName(String sName) {
	        return sName.replace('\\', '/');
	    }


	}
