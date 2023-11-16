package example;

import core.Migrator;
import core.MysqlManager;
import core.Reducer;
import core.SourceCodeManager;
import model.Regression;
import model.Revision;
import org.apache.commons.cli.*;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class NewCLI {
    private static final Options OPTIONS = new Options();
    private static final SourceCodeManager sourceCodeManager = new SourceCodeManager();
    static Reducer reducer = new Reducer();
    static Migrator migrator = new Migrator();
    private static CommandLine commandLine;
    private static String HELP_STRING = null;

    public static void main(String[] args) {
        CommandLineParser commandLineParser = new DefaultParser();
        setupOptions();
        try {
            commandLine = commandLineParser.parse(OPTIONS, args);
            processCommands();
        } catch (ParseException e) {
            System.out.println("Error parsing command: " + e.getMessage());
            printHelp();
        } catch (InvalidVersionException | MissingVersionException | InvalidBugIDException e) {
            System.out.println(e.getMessage());
            printHelp();
        }
    }

    private static void setupOptions() {
        OPTIONS.addOption("h", "help", false, "usage help");
        OPTIONS.addOption("checkout", "checkout", true, "checkout bugs by ID");
        OPTIONS.addOption("p", true, "path to work");
        OPTIONS.addOption("v", true, "version for bug");
        OPTIONS.addOption("compile", "compile", true, "compile bugID");
        OPTIONS.addOption("test", "test", true, "test bugID");
    }

    private static void processCommands() throws InvalidVersionException, MissingVersionException,
            InvalidBugIDException {
        if (commandLine.hasOption("h")) {
            printHelp();
        }
        if (commandLine.hasOption("checkout")) {
            processCheckoutCommand();
        }
    }

    private static void processCheckoutCommand() throws InvalidVersionException, MissingVersionException,
            InvalidBugIDException {
        String bugIDParam = commandLine.getOptionValue("checkout");
        int bugID = Integer.parseInt(bugIDParam);
        if (commandLine.hasOption("v")) {
            String version = commandLine.getOptionValue("v", "");
            validateVersion(version);
            retrieveBugs(bugID, version);
        } else {
            throw new MissingVersionException("No version info");
        }
    }

    private static void validateVersion(String version) throws InvalidVersionException {
        if (!Arrays.asList("bic", "wc", "bfc", "buggy").contains(version)) {
            throw new InvalidVersionException("Invalid version: " + version);
        }
    }

    private static void retrieveBugs(int bugID, String version) throws InvalidBugIDException, InvalidVersionException {
        List<Regression> regressionList = MysqlManager.selectRegressions("select id,project_full_name,bfc,buggy,bic," +
                "work,testcase from " +
                "regression where " +
                "id='" + bugID + "'");
        if (regressionList == null || regressionList.size() == 0) {
            throw new InvalidBugIDException("Invalid bug id" + bugID);
        }

        Regression regression = regressionList.get(0);
        String projectName = regression.getProjectFullName().replace("/", "_");
        File projectDir = sourceCodeManager.getProjectDir(projectName);
        Revision rfc = regression.getRfc();
        File rfcDir = sourceCodeManager.checkout(regression.getId(), rfc, projectDir, projectName);
        rfc.setLocalCodeDir(rfcDir);

        if (version.equals("bfc")) {
            System.out.println("checkout successful:" + rfcDir);
            return;
        }
        Revision targetVersion;
        switch (version) {
            case "bic":
                targetVersion = regression.getRic();
                break;
            case "work":
                targetVersion = regression.getWork();
                break;
            case "buggy":
                targetVersion = regression.getBuggy();
                break;
            default:
                throw new InvalidVersionException("Invalid version: " + version);
        }
        File targetVersionDir = sourceCodeManager.checkout(regression.getId(), targetVersion, projectDir, projectName);
        targetVersion.setLocalCodeDir(targetVersionDir);

        List<Revision> needToTestMigrateRevisionList = Arrays.asList(targetVersion);
        migrateTestAndDependency(rfc, needToTestMigrateRevisionList, regression.getTestCase());

        System.out.println("checkout successful:" + targetVersionDir);
        System.out.println("test command:" + regression.getTestCase());

    }

    static void migrateTestAndDependency(Revision rfc, List<Revision> needToTestMigrateRevisionList, String testCase) {
        migrator.equipRfcWithChangeInfo(rfc);
        reducer.reduceTestCases(rfc, testCase);
        needToTestMigrateRevisionList.forEach(revision -> {
            migrator.migrateTestFromTo_0(rfc, revision);
        });
    }

    private static void printHelp() {
        if (HELP_STRING == null) {
            HelpFormatter helpFormatter = new HelpFormatter();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PrintWriter printWriter = new PrintWriter(byteArrayOutputStream);
            helpFormatter.printHelp(printWriter, HelpFormatter.DEFAULT_WIDTH, "regs4j -help", null,
                    OPTIONS, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, null);
            printWriter.flush();
            HELP_STRING = new String(byteArrayOutputStream.toByteArray());
            printWriter.close();
        }
        System.out.println(HELP_STRING);
    }

    static class InvalidVersionException extends Exception {
        public InvalidVersionException(String message) {
            super(message);
        }
    }

    static class MissingVersionException extends Exception {
        public MissingVersionException(String message) {
            super(message);
        }
    }

    static class InvalidBugIDException extends Exception {
        public InvalidBugIDException(String message) {
            super(message);
        }
    }
}
