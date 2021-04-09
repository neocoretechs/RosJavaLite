package org.ros;

import org.ros.internal.jarclassloader.JarClassLoader;

public class RosLauncher {
    public static void main(String[] args) {
        JarClassLoader jcl = new JarClassLoader();
        try {
            jcl.invokeMain("org.ros.RosRun", args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
