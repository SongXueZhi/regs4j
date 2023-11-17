# Regs4J2.0
A continuously growing dataset of software regressions constructed by [RegMiner](https://github.com/SongXueZhi/RegMiner)
## ENV
JDK 11
Maven
Mysql

## Old Version
You can find old version doc in README_V1, it's still working.

## Easy to Run

Use ``java -jar regs4j -checkout <bugID> -v <version> ``to checkout 

for example :
```
java -jar regs4j -checkout 1 -v bic 
```

This version is undergoing, we will replace mysql part by sqlite or file system
