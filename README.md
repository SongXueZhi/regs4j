# Regs4J
A continuously growing dataset of software regressions constructed by RegMiner
## ENV
JDK 8
Maven
Mysql
## Integration with Tregression
Modifications were made to Regs4J's [Release 1.2](https://github.com/SongXueZhi/regs4j/releases/tag/1.2) to integrate with [Tregression](https://github.com/llmhyy/tregression).
- `example.CLI#checkout`
	- It additionally clones the working commit, and migrates the test case to the working commit. Before this was only done to the RIC commit.
- `core.Migrator#migrateTestFromTo_0`
	- Migrated files that were new to the working/RIC commit were deleted on exit from the CLI tool, however, it is now saved. (To save migrated test files and its dependencies)
