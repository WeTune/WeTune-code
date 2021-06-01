# WeTune

WeTune is a super-optimizer that automatically optimize SQL queries.

## File Structure
```
/common: common utilities
/sqlparser: AST parser
/stmt: statements management
/symsolver: SQL equivelence solver
/superopt: generate rules & rewrite query with rules
/testbed: query performance (latency) profiler
```

## Build
### Requirement
```
jdk: version 16
gradle: 7.0.1
```