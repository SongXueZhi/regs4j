package example;

import core.Migrator;
import core.MysqlManager;
import core.Reducer;
import core.SourceCodeManager;
import core.coverage.CodeCoverage;
import core.coverage.model.CoverNode;
import core.maven.JacocoMavenManager;
import core.maven.MavenManager;
import core.test.TestManager;
import model.Methodx;
import model.Regression;
import model.Revision;
import run.Executor;
import run.Runner;
import utils.CodeUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class CLI {
    private final static String helpText = "This tool is meant to provide a basic interface to our RegMiner tool.\n" +
            "It allows for users to retrieve a list of known regressions, checkout the individual\n" +
            "regressions and test the similarity between the different regressions. A list of valid\n" +
            "commands are provided below:\n\n" +
            "projects - list all the projects that has known regression bugs.\n" +
            "use [project_name] - use the specified project as the basis for all other operations.\n" +
            "list - list the known regression for the specified project (should be called after a use command).\n" +
            "checkout [idx] - checkout the regression at index idx in list.\n" +
            "similarity - get the similarity score for the regression that has been checked out.\n" +
            "exit - exit the tool.";

    static SourceCodeManager sourceCodeManager = new SourceCodeManager();
    static Reducer reducer = new Reducer();
    static Migrator migrator = new Migrator();
    static MavenManager mvnManager = new MavenManager();

    static List<Regression> regressionList = null;
    static String projectFullName = null;
    static int targetIdx = -1;

    private static final String reg_table_name = "regression";

    private static final String proj_name = "project_full_name";

    //make sure modify the JDK dir based on your own computer
    private static String JDK_CMD = "export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_351.jdk/Contents/Home;export PATH=$JAVA_HOME/bin:$PATH;";

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {//command-line mode
            switch (args[0]) {
                case "help": System.out.println(helpText); break;
                case "projects": retrieveProjects(); break;
                case "predd":
                    TransferBugsToDD.checkout(args[1]);
                    break;
                case "dd":
                    DeltaDebugging.dd(args[1]);
                    break;
                case "list": {
                    if (args.length < 2) System.out.println("requires a project name!");
                    else {
                        retrieveBugs(args[1]);
                        if (regressionList == null)
                            System.out.println("A list command can only be called after a project is specified.");
                        else
                            printBugs();
                    }
                } break;
                case "checkout": {
                    if (args.length < 3) System.out.println("Requires a project name and a reg id!");
                    else {
                        retrieveBugs(args[1]);
                        int index = -1;
                        try {
                            index = Integer.parseInt(args[2]);
                            checkout(index);
                        } catch (NullPointerException e) {
                            System.out.println("Please specify a project before checking out a bug.");
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid index: " + args[2]);
                        } catch (IndexOutOfBoundsException e) {
                            e.printStackTrace();
                            System.out.println(String.format("Invalid index %d as there are only %d regressions " +
                                            "for project %s.",
                                    index, regressionList.size(), projectFullName));
                        }
                    }
                } break;

                case "test": {
                    if (args.length == 1) {
                        System.out.println("Usage: test <testcase> (<project dir>)");
                        System.out.println("If you want to test all testcases, type a '-' as the first parameter");
                        return;
                    }
                    String testcase = (args[1].length() > 1)? args[1] : "";
                    String codeDir = (args.length > 2)? args[2] : ".";
                    testCmd(codeDir, testcase);
                } break;

                case "compile": {
                    String codeDir = (args.length > 1) ? args[1] : ".";
                    compileCmd(codeDir);
                } break;

                case "tstwithcvg": {
                    if (args.length == 1) {
                        System.out.println("Usage: tstwithcvg <testcase> (<project dir>)");
                        System.out.println("If you want to test all testcases, type a '-' as the first parameter");
                        return;
                    }
                    String testcase = (args[1].length() > 1)? args[1] : "";
                    String codeDir = (args.length > 2)? args[2] : ".";
                    testWithCoverageCmd(codeDir, testcase);
                } break;

                case "similarity": {
                    if (args.length < 3) System.out.println("requires a project name and a reg id!");
                    else {
                        retrieveBugs(args[1]);
                        int index = -1;
                        try {
                            index = Integer.parseInt(args[2]);
                            checkout(index);
                        } catch (NullPointerException e) {
                            System.out.println("Please specify a project before checking out a bug.");
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid index: " + args[2]);
                        } catch (IndexOutOfBoundsException e) {
                            e.printStackTrace();
                            System.out.println(String.format("Invalid index %d as there are only %d regressions " +
                                            "for project %s.",
                                    index, regressionList.size(), projectFullName));
                        }
                        similarity();
                    }
                } break;


                case "checkoutreg": checkoutBicWc(args[1], args[2], args[3]); break;
                case "checkoutAll": {//just checkout
                    if (args.length < 2) System.out.println("A use command requires a project name.");
                    else {
                        checkoutAll(args[1]);
                    }

                } break;
                case "generate": {
                    checkoutAllWithTestcase(args[1]);
                    int cnt = regressionList.size();
                    for (int i = 0; i < cnt; i++) {
                        Regression r = regressionList.get(i);
                        if (r.getTestCase().equals("")) {
                            System.out.println("reg " + (i + 1) + " has no testcase, skip");
                            continue;
                        }
                        RegCleaner.generateMeta(r, i + 1);
                    }
                } break;
                case "parTest": {
                    checkoutAllBicWithTestcase(args[1]);
                    int cnt = regressionList.size();
                    for (int i = 0; i < cnt; i++) {
                        Regression r = regressionList.get(i);
                        if (r.getTestCase().equals("")) {
                            System.out.println("reg " + (i + 1) + " has no testcase, skip");
                            continue;
                        }
                        if (!r.getTestCase().contains(";")) {
                            System.out.println("reg " + (i + 1) + " has only one testcase, skip");
                            continue;
                        }
                        RegCleaner.partitionTestcaseBic(r, i + 1);
                    }
                } break;
                default: System.out.println("Invalid command. Type help for the list of valid commands."); break;
            }
            return;
        }

        Scanner sc = new Scanner(System.in);
        boolean run = true;
        System.out.println("Welcome to RegMiner tool. Type help for more information on the tool.");
        try {
            do {
                System.out.print("RegMiner > ");
                String input = sc.nextLine().strip();
                if (input.length() == 0)
                    continue;
                String[] inputs = input.split("\\s+");
                switch (inputs[0]) {
                    case "use":
                        if (inputs.length < 2)
                            System.out.println("A use command requires a project name.");
                        else
                            retrieveBugs(inputs[1]);
                        break;
                    case "projects":
                        retrieveProjects();
                        break;
                    case "list":
                        if (regressionList == null)
                            System.out.println("A list command can only be called after a project is specified.");
                        else
                            printBugs();
                        break;
                    case "checkout":
                        if (inputs.length < 2)
                            System.out.println("A checkout command requires an index or key word all.");
//					else if (inputs[1].equalsIgnoreCase("all"))
//						for (int i = 1; i <= regressionList.size(); i++) {
//							checkout(i);
//						}
                        else {
                            int index = -1;
                            try {
                                index = Integer.parseInt(inputs[1]);
                                checkout(index);
                            } catch (NullPointerException e) {
                                System.out.println("Please specify a project before checking out a bug.");
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid index: " + inputs[1]);
                            } catch (IndexOutOfBoundsException e) {
                                e.printStackTrace();
                                System.out.println(String.format("Invalid index %d as there are only %d regressions " +
												"for project %s.",
                                        index, regressionList.size(), projectFullName));
                            }

                        }
                        break;
                    case "similarity":
                        similarity();
                        break;
                    case "help":
                        System.out.println(helpText);
                        break;
                    case "predd":
						TransferBugsToDD.checkout(inputs[1]);
                        break;
                    case "dd":
                        DeltaDebugging.dd(inputs[1]);
                        break;
                    case "exit":
                        run = false;
                        break;
                    default:
                        System.out.println("Invalid command. Type help for the list of valid commands.");
                }

            } while (run);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sc.close();
            System.out.println("Goodbye!");
        }
    }

    private static void retrieveProjects() {
        System.out.print("Retrieving projects...");
        List<String> projects = MysqlManager.selectProjects("select distinct " + proj_name +" from " + reg_table_name);
//        List<String> projects = MysqlManager.selectProjects("select distinct project_full_name from regressions");
        System.out.println(" Done");
        List<String> validProjects = new ArrayList<>();
        int maxLength = 0;
        for (String project : projects) {
            project = project.strip();
            if (project.length() == 0)
                continue;
            validProjects.add(project);
            maxLength = Math.max(maxLength, project.length());
        }

        String formatString = "%-" + maxLength + "s\t%-" + maxLength + "s";
        for (int i = 0; i < validProjects.size(); i += 2) {
            if (i == validProjects.size() - 1)
                System.out.println(validProjects.get(i));
            else
                System.out.println(String.format(formatString, validProjects.get(i), validProjects.get(i + 1)));
        }
    }

    private static void retrieveBugs(String projectFullName) {
        CLI.projectFullName = projectFullName;
        System.out.println("Using project: " + projectFullName);
        System.out.print("Retrieving regressions...");
//        regressionList = MysqlManager.selectRegressions("select bfc,buggy,bic,work,testcase from regressions where " +
//				"project_full_name='" + projectFullName + "'");
        regressionList = MysqlManager.selectRegressions("select bfc,buggy,bic,work,testcase from " + reg_table_name +" where " +
                proj_name +"='" + projectFullName + "'");
        targetIdx = -1;
//        for (Regression r : regressionList) {
//            System.out.println(r);
//            if (r.getTestCase().equals("")) {
//                System.out.println("this doesn't contain testcase");
//            }
//        }
        System.out.println(" " + regressionList.size() + " regressions found");
    }

    private static void retrieveBugs4AllTestCase(String projectFullName) {//get all testcases(not the first one)
        CLI.projectFullName = projectFullName;
        System.out.println("Using project: " + projectFullName);
        System.out.print("Retrieving regressions...");

        regressionList = MysqlManager.selectRegressions4AllTestcase("select bfc,buggy,bic,work,testcase from " + reg_table_name +" where " +
                proj_name +"='" + projectFullName + "'");
        targetIdx = -1;

        System.out.println(" " + regressionList.size() + " regressions found");
    }

    private static void printBugs() {
        int index = 1;
        for (Regression r : CLI.regressionList) {
            System.out.print(String.format("%3d.", index++));
            System.out.println(r);
            System.out.println(r.getRic());
            System.out.println(r.getWork());
        }
    }

    private static void checkout(int i) {
        Regression target = regressionList.get(i - 1);
        System.out.println(String.format("Checking out %s regression %d", projectFullName, i));
        targetIdx = i;
        File projectDir = sourceCodeManager.getProjectDir(projectFullName);

        if (target.getId() == null) { // if no regression id, set the parameter i as regression id!
            target.setId(String.valueOf(i));
        }

        Revision rfc = target.getRfc();
        File rfcDir = sourceCodeManager.checkout(target.getId(),rfc, projectDir, projectFullName);
        rfc.setLocalCodeDir(rfcDir);
        target.setRfc(rfc);

        Revision buggy = target.getBuggy();
        File buggyDir = sourceCodeManager.checkout(target.getId(), buggy, projectDir, projectFullName);
        buggy.setLocalCodeDir(buggyDir);
        target.setBuggy(buggy);

        Revision ric = target.getRic();
        File ricDir = sourceCodeManager.checkout(target.getId(),ric, projectDir, projectFullName);
        ric.setLocalCodeDir(ricDir);
        target.setRic(ric);

        Revision wc = target.getWork();
        File wcDir = sourceCodeManager.checkout(target.getId(), wc, projectDir, projectFullName);
        wc.setLocalCodeDir(wcDir);
        target.setWork(wc);

        List<Revision> needToTestMigrateRevisionList = Arrays.asList(ric, wc, buggy);
        migrateTestAndDependency(rfc, needToTestMigrateRevisionList, target.getTestCase());

//        System.out.println("rfc directory: " + rfcDir.toString());
        System.out.println("ric directory: " + ricDir.toString());
//        System.out.println("buggy directory: " + buggyDir.toString());
//        System.out.println("wc directory: " + wcDir.toString());
    }

    private static void checkoutBic(int i) {
        Regression target = regressionList.get(i - 1);
        System.out.println(String.format("Checking out %s regression %d", projectFullName, i));
        targetIdx = i;
        File projectDir = sourceCodeManager.getProjectDir(projectFullName);

        if (target.getId() == null) { // if no regression id, set the parameter i as regression id!
            target.setId(String.valueOf(i));
        }

        Revision rfc = target.getRfc();
        File rfcDir = sourceCodeManager.checkout(target.getId(),rfc, projectDir, projectFullName);
        rfc.setLocalCodeDir(rfcDir);
        target.setRfc(rfc);

        Revision ric = target.getRic();
        File ricDir = sourceCodeManager.checkout(target.getId(),ric, projectDir, projectFullName);
        ric.setLocalCodeDir(ricDir);
        target.setRic(ric);

        List<Revision> needToTestMigrateRevisionList = List.of(ric);
        migrateTestAndDependency(rfc, needToTestMigrateRevisionList, target.getTestCase());

        System.out.println("ric directory: " + ricDir.toString());
    }


    private static void checkoutBicWc(String _projectFullName, String bicCommit, String wcCommit) {//todo: add directory
        retrieveBugs(_projectFullName);
        if (regressionList == null || regressionList.size() == 0) {
            System.out.println("No such project found!");
            return;
        }
        for (Regression regression : regressionList) {
            if (regression.getRic().getCommitID().equals(bicCommit) && regression.getWork().getCommitID().equals(wcCommit)) {
                System.out.println("checking out ric and wc version(with testcase)");
                File projectDir = sourceCodeManager.getProjectDir(_projectFullName);

                Revision rfc = regression.getRfc();
                File rfcDir = sourceCodeManager.checkout(regression.getId(),rfc, projectDir, _projectFullName);
                rfc.setLocalCodeDir(rfcDir);
                regression.setRfc(rfc);

                Revision ric = regression.getRic();
                File ricDir = sourceCodeManager.checkout(regression.getId(),ric, projectDir, _projectFullName);
                ric.setLocalCodeDir(ricDir);
                regression.setRic(ric);

                Revision wc = regression.getWork();
                File wcDir = sourceCodeManager.checkout(regression.getId(), wc, projectDir, _projectFullName);
                wc.setLocalCodeDir(wcDir);
                regression.setWork(wc);

                List<Revision> needToTestMigrateRevisionList = Arrays.asList(ric, wc);
                migrateTestAndDependency(rfc, needToTestMigrateRevisionList, regression.getTestCase());

                System.out.println("rfc directory: " + rfcDir.toString());
                System.out.println("ric directory: " + ricDir.toString());
                System.out.println("wc directory: " + wcDir.toString());
                return;
            }
        }
        System.out.println("No such regression found!");
    }

    private static void similarity() {
        if (projectFullName == null || targetIdx == -1) {
            System.out.println("Please checkout a regression bug before calculating the similarity.");
            return;
        }
        System.out.println(String.format("Calculating similarity score for %s regression bug %d...", projectFullName,
				targetIdx));
        Regression target = regressionList.get(targetIdx - 1);

        File rfcDir = target.getRfc().getLocalCodeDir();
        File ricDir = target.getRic().getLocalCodeDir();

        Runner rfcRunner = new Runner(rfcDir, target.getTestCase());
        List<CoverNode> rfcCoveredMethodList = rfcRunner.getCoverNodes();
        if (rfcCoveredMethodList == null) {
            System.out.println("rfc failed to run successfully. Please try another regression bug.");
            return;
        }

        Runner ricRunner = new Runner(ricDir, target.getTestCase());
        List<CoverNode> ricCoveredMethodList = ricRunner.getCoverNodes();
        if (ricCoveredMethodList == null) {
            System.out.println("ric failed to run successfully. Please try another regression bug.");
            return;
        }

        try {
            String rfcSrcDir = mvnManager.getSrcDir(rfcDir);
            String ricSrcDir = mvnManager.getSrcDir(ricDir);

            List<Methodx> rfcMethods = CodeUtil.getCoveredMethods(new File(rfcDir, rfcSrcDir), rfcCoveredMethodList);
            List<Methodx> ricMethods = CodeUtil.getCoveredMethods(new File(ricDir, ricSrcDir), ricCoveredMethodList);
            double r_rscore = Example.similarityScore(rfcMethods, ricMethods);
            System.out.println("Similarity score: " + r_rscore);
        } catch (Exception e) {
            System.out.println("Not able to find the required file. Please try another regression.");
        }
    }

    private static void migrateTestAndDependency(Revision rfc, List<Revision> needToTestMigrateRevisionList, String testCase) {
        migrator.equipRfcWithChangeInfo(rfc);
        reducer.reduceTestCases(rfc, testCase);
        needToTestMigrateRevisionList.forEach(revision -> {
            migrator.migrateTestFromTo_0(rfc, revision);
        });
    }

    //checkout all bugs for a given project, even for those who has no testcase in database
    //this cannot be used for testcase partitioning
    public static void checkoutAll(String projectFullName) {
        retrieveBugs(projectFullName);
        int regCount = regressionList.size();
        if (regCount == 0) {
            System.out.println("No regression found! Please check the project name!");
            return;
        }
        for (int i = 0; i < regCount; i++) {
            checkout(i + 1);
        }
        System.out.println("Checkout " + regCount + " bugs successfully!");
    }


    //checkout all bugs(that contain non-empty testcase set) for a given project, the bug with no relevant testcase will be ignored
    public static void checkoutAllWithTestcase(String projectFullName) {
        retrieveBugs4AllTestCase(projectFullName); // get all bugs of current project

        //checkout all regressions with non-empty testcase set
        int regCount = regressionList.size();
        if (regCount == 0) {
            System.out.println("No regression found! Please check the project name!");
            return;
        }
        int cnt = 0;
        for (int i = 0; i < regCount; i++) {
            if (regressionList.get(i).getTestCase().equals("")) continue;
            checkout(i + 1);
            cnt++;
        }
        System.out.println("Checkout " + cnt + " bugs successfully!");
    }

    public static void checkoutAllBicWithTestcase(String projectFullName) {
        retrieveBugs4AllTestCase(projectFullName); // get all bugs of current project

        //checkout all regressions with non-empty testcase set
        int regCount = regressionList.size();
        if (regCount == 0) {
            System.out.println("No regression found! Please check the project name!");
            return;
        }
        int cnt = 0;
        for (int i = 0; i < regCount; i++) {
            String testcase = regressionList.get(i).getTestCase();
            if (testcase.equals("")) continue;
            if (!testcase.contains(";")) continue;
            checkoutBic(i + 1);
            cnt++;
        }
        System.out.println("Checkout " + cnt + " bugs successfully!");
    }

    public static void testCmd(String codeDir, String testcase) {
        File dir = new File(codeDir);
        if (dir.exists() && dir.isDirectory()) {
            testCmd(dir, generateTestCmd(testcase, true), true);
        } else {
            System.out.println("This path is not a directory!");
        }
    }

    public static void compileCmd(String codeDir) {
        File dir = new File(codeDir);
        if (dir.exists() && dir.isDirectory()) {
            compileCmd(dir, true);
        } else {
            System.out.println("This path is not a directory!");
        }
    }

    private static void testCmd(File dir, String testCmd, boolean isVerbose) {
        Executor executor = new Executor().setDirectory(dir);
        List<String> errMsgs = new ArrayList<>();
        String str = "";
        try {
            str = executor.exec(JDK_CMD + testCmd, 5);//in order to support jdk8

            TestManager testManager = new TestManager();
            errMsgs = testManager.getErrors(dir);
            if (errMsgs == null) {
                System.out.println("This is not an legal directory!");
                return;
            }
            if (errMsgs.size() == 0) {
                System.out.println("Test success");
                return;
            }

        } catch (TimeoutException ex) {
            errMsgs = new ArrayList<>();
            errMsgs.add(ex.getClass().getName());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            if (isVerbose) {
                System.out.println("Test result: " + str);
            }
            if (errMsgs != null){
                System.out.println("----- " + errMsgs.size() + "error messages: ");
                for (String s : errMsgs) {
                    System.out.println(s);
                }
            }
        }
    }

    private static void compileCmd(File dir, boolean isVerbose) {
        String compileCmd = "mvn compile";
        Executor executor = new Executor().setDirectory(dir);
        try {
            String str = executor.exec(JDK_CMD + compileCmd);//in order to support jdk8
            if (isVerbose) {
                System.out.println("Compile result: " + str);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void testWithCoverageCmd(String codeDir, String testcase) {
        CodeCoverage codeCoverage = new CodeCoverage();
        JacocoMavenManager jacocoMavenManager = new JacocoMavenManager();
        File dir = new File(codeDir);
        try {
            jacocoMavenManager.addJacocoFeatureToMaven(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
        compileCmd(dir, false);

        testCmd(dir, generateTestCmd(testcase, true), false);
        List<CoverNode> coverNodes = codeCoverage.readJacocoReports(dir);
        if (coverNodes == null) {
            System.out.println("Null cover nodes!");
            return;
        }
        System.out.println("CoverNodes size: " + coverNodes.size());
        File coverageFile = new File(dir.getPath() + File.separator + "coverage.txt");
        try {
            boolean a = coverageFile.createNewFile();
            if (!a) {
                System.out.println("Create file failed! You can copy the following messages!\n");
                for (CoverNode coverNode : coverNodes) {
                    System.out.println(coverNode);
                }
                return;
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(coverageFile))) {
                for (CoverNode coverNode : coverNodes) {
                    writer.write(coverNode.toString());
                    writer.newLine();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            for (CoverNode coverNode : coverNodes) {
                System.out.println(coverNode);
            }
        }
    }

    private static String generateTestCmd(String testcase, boolean failureIgnore) {
        String testCmd = "mvn test";
        if (!testcase.equals("")) {
            testCmd += " -Dtest=" + testcase;
        }
        testCmd += " -Dmaven.test.failure.ignore=" + failureIgnore;
//        System.out.println("cmd: " + testCmd);
        return testCmd;
    }
}
