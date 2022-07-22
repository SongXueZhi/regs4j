# Regressions4J
A continuously growing dataset of software regressions constructed by [RegMiner](https://github.com/SongXueZhi/RegMiner)
## ENV
JDK 8
Maven
Mysql

## Dataset Tool

To showcase the possible use cases of the tool, we have also provided a command line interface tool that can help to retrieve and checkout the bugs. The tool can be run with 2 simple steps.

### Run and Configuration

**Step 1:**  Entry the working dir.

```
cd /issta/regminer/tool
```

**Step 2:** Run the tool by running ```CLI.sh```

```
bash CLI.sh
```

### List of Commands

To use the CLI tool, there are 7 different commands. The commands are as follows:

1. ```help``` - print out a help message containing all the available commands
2. ```projects``` - list out all the projects in the database with known regression bugs
3. ```use [project]``` - use the specified project as the basis for all the other operations
4. ```list``` - list the known regression for the specified project
5. ```checkout [idx]``` - checkout the regression at index idx in the list
6. ```similarity``` - get the similarity score for the regression that has been checked out
7. ```exit``` - exit the tool

### Example

An example of how to use the tool is as follows.

**Step 1:** The user will run the projects command to retrieve an up-to-date list of all the projects that have regression bugs in it. This will allow the user to know which project to operate on.

```
RegMiner > projects
Retrieving projects... Done
uniVocity/univocity-parsers              	apache/commons-lang                      
jhy/jsoup                                	jmrozanec/cron-utils
...
```

**Step 2:** After finding an appropriate project to operate on, the user will then specify to the tool to use that project. This will also tell the user how many regression bugs there are for this project.

```
RegMiner > use uniVocity/univocity-parsers
Using project: univocity/univocity-parsers
Retrieving regressions... 25 regressions found
```

**Step 3:** The user can choose to list the regression bugs to see which bug they are interested in. This also provide the testcase which allows the user to know the features that are affected by the regression bug.

```
RegMiner > list
1. rfc: XXXXX | ric: XXXXX | testcase: XXXXX
2. rfc: XXXXX | ric: XXXXX | testcase: XXXXX
...
```

**Step 4:** The user can now checkout one of the bug in the list. This checkout process may take a while, if the repository needs to be cloned. At the end of the checkout process, two paths will be output, where the former is the regression fixing commit directory and the latter is the regression inducing commit directory. These two directories can then be used for further testing if need be.

```
RegMiner > checkout 10
Checking out univocity/univocity-parsers regression 10
... <git messages> ...
rfc directory: /home/regminer/Documents/miner-space/transfer_cache/univocity_univocity-parsers/rfc
ric directory: /home/regminer/Documents/miner-space/transfer_cache/univocity_univocity-parsers/ric
```

**Step 5:** Lastly, the user can run the similarity command to calculate the similarity between the two commits (RFC and RIC), about **5 minutes** need. This is done by recording the code coverage of the two commits on the test case and comparing the two runs. More details of this calculation can be found in our paper.

```
RegMiner > similarity

Calculating similarity score for univocity/univocity-parsers regression bug 10...

Similarity score: 0.6949152542372882

```

### Delta debugging
Now, we support ddj and ddmin.

** Step1:** Transfer reg4j to delta debugging data format by command ``predd {username}/{project name}``, example:

RegMiner > predd uniVocity/univocity-parsers

** Step2:** Run delta debugging on project by command ``dd {username}/{project name}``, example:

RegMiner > dd uniVocity/univocity-parsers




