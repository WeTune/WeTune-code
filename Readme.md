# WTune

WTune is project that automatically detects and 
optimizes inefficient database access in Web-application.

## File Structure
```
/assemble: aggregation root of modules
/common: common utils used in other modules.
/parser: sql statement parser
```

## Build
### Requirement
```
jdk version 14
```
### Command
```shell script
gradle assemble
```
Artifact will be found in assemble/build/libs/wtune.jar

## Run
```shell script
java -jar wtune.jar
```