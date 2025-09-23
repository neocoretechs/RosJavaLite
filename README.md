ROSJavaLite
=======

Java implementation of ROS Core services (fork) without XML-RPC, instead, Java Serialization for smaller footprint devices.
*Currently upgraded to JDK25 utilizing virtual threads and other platform improvements.*

This project is a fork of the original Apache 2.0 Licensed project with the major changes being the removal 
of 'Google Collections' dependency and their custom assertion framework in favor of standard Java collections
and 'assert' keyword. Also added additional logic in onNodeReplacement to attempt to more robustly reconnect when a 
new node replaces a previous one. None of the tools of ROS are within scope of this, but Core, that being Master and 
Parameter server and supporting messages and their plumbing. The intent was to reduce the codebase rather than address
any other issues. Also removed are the test harnesses and other tooling regarding generation of Catkin and Gradle-based
packages to produce executable JARs which are ROS 'packages'. Ant is used in each of the subprojects to do builds and typically a build.xml file
can be found in each target project. 
Eclipse was the development environment used.  The original project was broken down into several subprojects for 
easier handling: 
ROSJava - this. in NeoCoreTechs *RosJavaLite* repository
ROSCore - brings up core server, rosrun shortcut, 
ROSMsgs - generated messages from bootstrap GenerateInterfaces, in the *rosjava_messages* repository.
ROSMsgsGeom - geometry as above, 
ROSBase - org.ros interface bindings pre-generated Java classes

More significant is the removal of XML-RPC server and associated overhead. 
The dynamic proxy invocation using the generated interfaces was removed in favor of using former GernateInterfaces, now GenerateClasses to generate 
concrete serializable classes vastly outperforms interfaces and dynamic proxy invocations. 
This version will not interop with XML-RPC, yet. 
A simple intermediary to translate serialzable to XML-RPC would need constructed.
The mechanism for remote calls is a simple serialized class of 'package, method name, and parameters'. On the remote side the server classes are
reflected and the methods matched to the incoming message and invoked with the parameters supplied in the message. The result is returned
as lists of objects, as in the original version, but the only requirement is that the objects in request and result all serialize.
To this end the Standard ROS message files can be generated as concrete serializable classes with the GenerateClasses class mentioned earlier.

Addresses are now expressed in InetSocketAddress rather than URI since the primary servers are simple socket servers and can use Java serialization instead
of the heavyweight XML-RPC protocols with their associated schemes.

The MasterServer and ParameterServer are now organized as distinct servers on 2 different ports, although the reflected methods available
to remotely call both are encapsulated in the MasterServer class. The default ports for the master and parameter server are port and port+1
starting at 8090., so initially the default master is at 127.0.0.1:8090 and default parameter server is at 127.0.0.1:8091

The original incarnation was simply too unnecessarily massive to get good performance from SBC's like the Odroid and RPi. Bringing up the associated full web
server and XML RPC endpoints was a performance buzzkill.
In conjunction with the *RoboCore* and *Relatrix* repositories, along with the *ROSCAR* LLM Java model runner, a Relatrix/ROSJavaLite robot bus combines all the NeoCoreTechs robotics projects into a true general purpose
Robot Operating System usable for field robotics. Numerous robots of various sizes and functions, with similar but different architectures, using small clusters of SBC's, currently operate using these components.