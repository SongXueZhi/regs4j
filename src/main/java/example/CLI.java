package example;

import core.Migrator;
import core.MysqlManager;
import core.Reducer;
import core.SourceCodeManager;
import core.coverage.model.CoverNode;
import core.maven.MavenManager;
import model.Methodx;
import model.Regression;
import model.Revision;
import run.Runner;
import utils.CodeUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

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

    public static void main(String[] args) {
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
        List<String> projects = MysqlManager.selectProjects("select distinct project_full_name from regressions");
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
        regressionList = MysqlManager.selectRegressions("select bfc,buggy,bic,work,testcase from regressions where " +
				"project_full_name='" + projectFullName + "'");
        targetIdx = -1;
        System.out.println(" " + regressionList.size() + " regressions found");
    }

    private static void printBugs() {
        int index = 1;
        for (Regression r : CLI.regressionList) {
            System.out.print(String.format("%3d.", index++));
            System.out.println(r);
        }
    }

    private static void checkout(int i) {
        Regression target = regressionList.get(i - 1);
        System.out.println(String.format("Checking out %s regression %d", projectFullName, i));
        targetIdx = i;
        File projectDir = sourceCodeManager.getProjectDir(projectFullName);

        Revision rfc = target.getRfc();
        File rfcDir = sourceCodeManager.checkout(target.getId(),rfc, projectDir, projectFullName);
        rfc.setLocalCodeDir(rfcDir);
        target.setRfc(rfc);

        Revision ric = target.getRic();
        File ricDir = sourceCodeManager.checkout(target.getId(),ric, projectDir, projectFullName);
        ric.setLocalCodeDir(ricDir);
        target.setRic(ric);

        List<Revision> needToTestMigrateRevisionList = Arrays.asList(ric);
        migrateTestAndDependency(rfc, needToTestMigrateRevisionList, target.getTestCase());

        System.out.println("rfc directory: " + rfcDir.toString());
        System.out.println("ric directory: " + ricDir.toString());
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

}
