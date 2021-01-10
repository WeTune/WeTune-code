# WTune

WTune is project that automatically detects and 
optimizes inefficient database access in Web-application.

## File Structure
```
/bootstrap: entry of executable
/common: common utils used in other modules.
/sqlparser: AST parser
/stmt: stmtImpl resolver
```

## Build
### Requirement
```
jdk: version 14
gradle: 6.5.0
```
### Command
```shell script
gradle assemble
```
Artifact will be found in bootstrap/build/libs/wtune.jar

## Run
```shell script
java -jar wtune.jar
```