package org.ros.internal.system;

import java.lang.management.ManagementFactory;

/**
 * Process-related utility methods.
 *
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2021
 */
public class Process {
  
  private Process() {
    // Utility class.
  }
  
  /**
   * @return PID of node process if available, 
   * @throws {@link UnsupportedOperationException} otherwise.
   */
  public static Long getPid() {
    // Java 8 returns '1234@localhost'.
	// Java 9 ProcessHandle processHandle = ProcessHandle.current();
	//     return processHandle.pid();
    try {
      String mxName = ManagementFactory.getRuntimeMXBean().getName();
      int idx = mxName.indexOf('@');
      if (idx > 0) {
        try {
        	return Long.parseLong(mxName.split("@")[0]);
          //return Integer.parseInt(mxName.substring(0, idx));
        } catch (NumberFormatException e) {
          return 0L;
        }
      }
    } catch (NoClassDefFoundError unused) {
      // Android does not support ManagementFactory. Try to get the PID on
      // Android.
      try {
        return new Long((Integer)Class.forName("android.os.Process").getMethod("myPid").invoke(null));
      } catch (Exception unused1) {
        // Ignore this exception and fall through to the
        // UnsupportedOperationException.
      }
    }
    throw new UnsupportedOperationException();
  }
}
