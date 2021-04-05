package org.ros.internal.jarclassloader;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarException;

/**
 * Inner class with JAR entry information. Keeps JAR file and entry object.
 */
public class JarEntryInfo {
    JarFileInfo jarFileInfo;
    JarEntry jarEntry;
    JarEntryInfo(JarFileInfo jarFileInfo, JarEntry jarEntry) {
        this.jarFileInfo = jarFileInfo;
        this.jarEntry = jarEntry;
    }
    URL getURL() { // used in findResource() and findResources()
        try {
            return new URL("jar:file:" + jarFileInfo.jarFile.getName() + "!/" + jarEntry);
        } catch (MalformedURLException e) {
            return null;
        }
    }
    String getName() { // used in createTempFile() and loadJar()
        return jarEntry.getName().replace('/', '_');
    }
    @Override
    public String toString() {
        return "JAR: " + jarFileInfo.jarFile.getName() + " ENTRY: " + jarEntry;
    }
    /**
     * Read JAR entry and returns byte array of this JAR entry. This is
     * a helper method to load JAR entry into temporary file.
     *
     * @param inf JAR entry information object
     * @return byte array for the specified JAR entry
     * @throws JarClassLoaderException
     */
    byte[] getJarBytes() throws JarException {
        DataInputStream dis = null;
        byte[] a_by = null;
        try {
            long lSize = jarEntry.getSize();
            if (lSize <= 0  ||  lSize >= Integer.MAX_VALUE) {
                throw new JarException(
                        "Invalid size " + lSize + " for entry " + jarEntry);
            }
            a_by = new byte[(int)lSize];
            InputStream is = jarFileInfo.jarFile.getInputStream(jarEntry);
            dis = new DataInputStream(is);
            dis.readFully(a_by);
        } catch (IOException e) {
            throw new JarException(e.getMessage());
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                }
            }
        }
        return a_by;
    }
} 
