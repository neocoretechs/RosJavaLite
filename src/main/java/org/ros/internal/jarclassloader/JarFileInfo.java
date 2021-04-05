package org.ros.internal.jarclassloader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Inner class with JAR file information.
 */
public class JarFileInfo {
    JarFile jarFile;   // this is the essence of JarFileInfo wrapper
    String simpleName; // accumulated for logging like: "topJar!childJar!kidJar"
    File fileDeleteOnExit;
    Manifest mf; // required for package creation
    ProtectionDomain pd;
    
    /**
     * @param jarFile
     *            Never null.
     * @param simpleName
     *            Used for logging. Never null.
     * @param jarFileParent
     *            Used to make simpleName for logging. Null for top level JAR.
     * @param fileDeleteOnExit
     *            Used only to delete temporary file on exit. 
     *            Could be null if not required to delete on exit (top level JAR)
     * @throws JarClassLoaderException 
     */
    JarFileInfo(JarFile jarFile, String simpleName, JarFileInfo jarFileParent, 
                ProtectionDomain pd, File fileDeleteOnExit) 
    {
        this.simpleName = (jarFileParent == null ? "" : jarFileParent.simpleName + "!") + simpleName;
        this.jarFile = jarFile;
        this.pd = pd; 
        this.fileDeleteOnExit = fileDeleteOnExit;
        try {
            this.mf = jarFile.getManifest(); // 'null' if META-INF directory is missing
        } catch (IOException e) {
            // Ignore and create blank manifest
        }
        if (this.mf == null) {
            this.mf = new Manifest();
        }
    }
    String getSpecificationTitle() {
        return mf.getMainAttributes().getValue(Name.SPECIFICATION_TITLE);
    }
    String getSpecificationVersion() {
        return mf.getMainAttributes().getValue(Name.SPECIFICATION_VERSION);
    }
    String getSpecificationVendor() {
        return mf.getMainAttributes().getValue(Name.SPECIFICATION_VENDOR);
    }
    String getImplementationTitle() {
        return mf.getMainAttributes().getValue(Name.IMPLEMENTATION_TITLE);
    }
    String getImplementationVersion() {
        return mf.getMainAttributes().getValue(Name.IMPLEMENTATION_VERSION);
    }
    String getImplementationVendor() {
        return mf.getMainAttributes().getValue(Name.IMPLEMENTATION_VENDOR);
    }
    URL getSealURL() {
        String seal = mf.getMainAttributes().getValue(Name.SEALED);
        if (seal != null) {
            try {
                return new URL(seal);
            } catch (MalformedURLException e) {
                // Ignore, will return null
            }
        }
        return null;
    }
} // inner class JarFileInfo



