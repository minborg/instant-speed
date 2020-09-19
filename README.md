# instant-speed

## Build the Projects

```shell script
mvn clean install
```

## Run under Java 8

Switch to using Java 8. This process is platform-dependent. Some platforms allow the "JAVA_HOME" environment variable to be changed.

Make sure we are using Java 8:
```shell script
java -version
java version "1.8.0_181"
Java(TM) SE Runtime Environment (build 1.8.0_181-b13)
Java HotSpot(TM) 64-Bit Server VM (build 25.181-b13, mixed mode)
```

Run the benchmark:
```shell script
java -Xmx1G -verbose:gc -XX:+PrintGCDetails -jar target/zgc-1.0.0-SNAPSHOT.jar
```

## Run under Java 15

Switch to using Java 15. This process is platform-dependent. Some platforms allow the "JAVA_HOME" environment variable to be changed.

Make sure we are using Java 8:
```shell script
java -version
openjdk version "15" 2020-09-15
OpenJDK Runtime Environment AdoptOpenJDK (build 15+36)
OpenJDK 64-Bit Server VM AdoptOpenJDK (build 15+36, mixed mode, sharing)
```

Run the benchmark:
```shell script
java -Xmx1G -XX:+UseZGC -Xlog:gc --add-exports java.base/jdk.internal.ref=ALL-UNNAMED -jar target/zgc-1.0.0-SNAPSHOT.jar
```

## Run under GraalVM 20.