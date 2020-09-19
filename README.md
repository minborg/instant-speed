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

## Run under GraalVM Enterprise

Switch to using GraalVM Enterprise. This process is platform-dependent. Some platforms allow the "JAVA_HOME" environment variable to be changed.

Make sure we are using GraalVM Enterprise:
```shell script
java -version
java version "11.0.8.0.2" 2020-07-09 LTS
Java(TM) SE Runtime Environment GraalVM EE 20.2.0 (build 11.0.8.0.2+1-LTS-jvmci-20.2-b03)
Java HotSpot(TM) 64-Bit Server VM GraalVM EE 20.2.0 (build 11.0.8.0.2+1-LTS-jvmci-20.2-b03, mixed mode, sharing)
```

Run the benchmark:
```shell script
java -Xmx1G -verbose:gc -XX:+PrintGCDetails --add-exports java.base/jdk.internal.ref=ALL-UNNAMED -jar target/zgc-1.0.0-SNAPSHOT.jar
```

## Results
The following results were obtained for a Linux (3.10.0-1062.18.1.el7.x86_64) Server with 12 Intel(R) Xeon(R) CPU E5-2650 v4 @ 2.20GHz and 24 threads. Figures are in microseconds.


### Java 8 ParallelGC

### Java 15 ZGC

### GraalVM 11.0.8.0.2 ParallelGC

