# vertx-net-client
Vertx TCP Client using Kotlin and Gradle

A very simple TCP client that keeps a connection open to one of my custom written server in cloud. The requirement of the client 
is to send a shared secret upon connecting and then keep waiting for message from server.  
The generated jar is meant to be be executed on Raspberry Pi. 

`./gradlew shadowJar`

will generate `./build/libs/vertx-net-client-shadow.jar`

Run as `java -DserverHost=localhost -DserverPort=8888 -DconnectMessage=hello -jar vertx-net-client-shadow.jar`
