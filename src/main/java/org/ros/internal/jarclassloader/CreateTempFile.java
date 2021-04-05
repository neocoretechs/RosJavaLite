package org.ros.internal.jarclassloader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarException;

/**
 * Using temp files (one per inner JAR/DLL) solves many issues:
 * 1. There are no ways to load JAR defined in a JarEntry directly
 *    into the JarFile object (see also #6 below).
 * 2. Cannot use memory-mapped files because they are using
 *    nio channels, which are not supported by JarFile ctor.
 * 3. JarFile object keeps opened JAR files handlers for fast access.
 * 4. Deep resource in a jar-in-jar does not have well defined URL.
 *    Making temp file with JAR solves this problem.
 * 5. Similar issues with native libraries:
 *    <code>ClassLoader.findLibrary()</code> accepts ONLY string with
 *    absolute path to the file with native library.
 * 6. Option "java.protocol.handler.pkgs" does not allow access to nested JARs(?).
 *
 * @param inf JAR entry information.
 * @return temporary file object presenting JAR entry.
 * @throws JarClassLoaderException
 */
public class CreateTempFile {
	public static final String TMP_SUB_DIRECTORY = "JarClassLoader";
	private static File dirTemp = null;
	static File createTempFile(JarEntryInfo inf) throws JarException {
		// Temp files directory:
		//   WinXP: C:/Documents and Settings/username/Local Settings/Temp/JarClassLoader
		//    Unix: /var/tmp/JarClassLoader
		if (dirTemp == null) {
			File dir = new File(System.getProperty("java.io.tmpdir"), TMP_SUB_DIRECTORY);
			if (!dir.exists()) {
				dir.mkdir();
			}
			chmod777(dir); // Unix - allow temp directory RW access to all users.
			if (!dir.exists() || !dir.isDirectory()) {
				throw new JarException(
                    "Cannot create temp directory " + dir.getAbsolutePath());
			}
			dirTemp = dir;
		}
		File fileTmp = null;
		try {
			fileTmp = File.createTempFile(inf.getName() + ".", null, dirTemp);
			fileTmp.deleteOnExit();
			chmod777(fileTmp); // Unix - allow temp file deletion by any user
			byte[] a_by = inf.getJarBytes();
			BufferedOutputStream os = new BufferedOutputStream(
                                  new FileOutputStream(fileTmp));
			os.write(a_by);
			os.close();
			return fileTmp;
    	} catch (IOException e) {
    		throw new JarException(String.format(
                "Cannot create temp file '%s' for %s due to %s", fileTmp, inf.jarEntry, e.getMessage()));
    	}
	}
	private static void chmod777(File file) {
		file.setReadable(true, false);
		file.setWritable(true, false);
		file.setExecutable(true, false); // Unix: allow content for dir, redundant for file
	}
}

