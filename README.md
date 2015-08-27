ROSJavaLite
=======

Java implementation of ROS Core services (fork) without XML-RPC, instead, Java Serialization for smaller footprint devices

This project is a fork of the original Apache 2.0 Licensed project with the major changes being the removal 
of 'Google Collections' dependency and their custom assertion framework in favor of standard Java collections
and 'assert' keyword. Also added additional logic in onNodeReplacement to attempt to more robustly reconnect when a 
new node replaces a previous one. None of the tools of ROS are within scope of this, but Core, that being Master and 
Parameter server and supporting messages and their plumbing. The intent was to reduce the codebase rather than address
any other issues. Also removed are the test harnesses and other tooling regarding generation of Catkin and Gradle-based
packages to produce executable JARs which are ROS 'packages'. Ant is used in each of the subprojects to do builds. 
Eclipse was the development environment used.  The original project was broken down into several subprojects for 
easier handling: 
ROSJava - this. 
ROSCore - brings up core server, rosrun shortcut, 
ROSMsgs - generated messages from bootstrap GenerateInterfaces, 
ROSMsgsGeom - geometry as above, 
ROSBase - org.ros interface bindings pre-generated Java classes

More significant is the removal of XML-RPC server and associated overhead. 
The dynamic proxy invocation using the generated interfaces was removed in favor of using former GernateInterfaces, now GenerateClasses to generate 
concrete serializable classes vs interfaces and dynamic proxy invocations. 
This version will not interop with XML-RPC, yet. 
A simple intermediary to translate serialzable to XML-RPC would need constructed.


Addresses are now expressed in InetSocketAddress rather than URI as the primary servers are simple socket servers using Java serialization instead
of the heavyweight XML-RPC protocols.

The original incarnation was simply too unnecessarily massive to get good performance from SBC's like the Odroid and RPi.
Eventually, a cohesive Relatrix/ROSJavaLite robot bus will combine all the NeoCoreTechs robotics projects.