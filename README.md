# stdpy

[![Release](https://jitpack.io/v/Gaming32/stdpy.svg)](https://jitpack.io/#Gaming32/stdpy)
![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/Gaming32/stdpy/maven/main?label=build)
![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/Gaming32/stdpy/maven/dev?label=build-dev)

Java implementation of the places Python stdlib where it was determined to be more optimal or clean.

Read documentation at the [Javadoc](https://gaming32.github.io/stdpy/javadoc).

This project supports Java 1.8 and later.

## Adding to your project

### Gradle

Add the repository:
```groovy
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
```

Add the dependency:
```groovy
    dependencies {
            implementation 'com.github.Gaming32:stdpy:[SEE-BELOW]'
    }
```
![Release](https://jitpack.io/v/Gaming32/stdpy.svg)

### Maven

Add the repository:
```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
```

Add the dependency:
```xml
    <dependency>
        <groupId>com.github.Gaming32</groupId>
        <artifactId>stdpy</artifactId>
        <version>[SEE-BELOW]</version>
    </dependency>
```
![Release](https://jitpack.io/v/Gaming32/stdpy.svg)
