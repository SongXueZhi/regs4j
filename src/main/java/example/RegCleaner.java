package example;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import model.Regression;

public class RegCleaner {
    private static final String MVN_COMPILE = "mvn compile test-compile";
    private static final String GRADLE_COMPILE = "./gradlew compileJava compileTestJava";
    private static final String MVN_TEST = "mvn test -Dtest=";
    private static final String GRADLE_TEST = "./gradlew test --tests ";
    private static final String REPORT_DIR = "target/surefire-reports/";
    private static final String RIC_TAG = "_ric";
    private static final String BUGGY_TAG = "_buggy";
    private static final String ERR_POSTFIX = "_err.txt";

    //make sure modify the JDK dir based on your own computer
    private static final String JDK_CMD = "export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_351.jdk/Contents/Home;export PATH=$JAVA_HOME/bin:$PATH;";

    public static void generateMeta(Regression regression, int rid) throws IOException {
        BUILD_TOOL buildTool = defectBuildTool(regression.getRfc().getLocalCodeDir());
        if (buildTool != BUILD_TOOL.MVN) {//todo: add the support of gradle
            System.out.println("Do not support other build systems except mvn");
            return;
        }

        String[] testcases = regression.getTestCase().split(";");
        String parentDir = regression.getRic().getLocalCodeDir().getParent();
        int testcaseCount = testcases.length;

        for (int i = 1; i <= testcaseCount; i++) {
            String t = testcases[i - 1];
            String testCMD = JDK_CMD + MVN_TEST + t;

//            System.out.println("executing " + testCMD);
            //compile and test bic
            Executor executor =  new Executor().setDirectory(regression.getRic().getLocalCodeDir());
            TestResult.STATUS statusBic = executor.execTestWithResult(testCMD);
            //compile and test buggy
            executor =  new Executor().setDirectory(regression.getBuggy().getLocalCodeDir());
            TestResult.STATUS statusBuggy = executor.execTestWithResult(testCMD);
            System.out.println(rid + "-> bic: " + statusBic + " buggy: " + statusBuggy);

            String[] testcaseSplit = t.split("#");//0 for filename and 1 for test method name
            String reportName = "/" + REPORT_DIR + testcaseSplit[0] + ".txt";

            File ricReportFile = new File(regression.getRic().getLocalCodeDir().getPath() + reportName);
            File buggyReportFile = new File(regression.getBuggy().getLocalCodeDir().getPath() + reportName);

            //get the reduced err msg for ric and buggy
            if (ricReportFile.exists()) {//the failing testcase(after executing test) matches the one in database; this also has the report file!
                File ricReducedFile = new File(parentDir + "/" + rid + RIC_TAG + i + ERR_POSTFIX);
                ricReducedFile.createNewFile();
                String ricErrInfo = reduceErrMsg(ricReportFile, ricReducedFile, testcaseSplit[0]);
            }

            if (buggyReportFile.exists()) {
                File buggyReducedFile = new File(parentDir + "/" + rid + BUGGY_TAG + i + ERR_POSTFIX);
                buggyReducedFile.createNewFile();
                String buggyErrInfo = reduceErrMsg(buggyReportFile, buggyReducedFile, testcaseSplit[0]);
            }
        }
    }

    //partition by testcase, now only partition bic
    public static void partitionTestcaseBic(Regression regression, int rid) throws IOException {
        BUILD_TOOL buildTool = defectBuildTool(regression.getRic().getLocalCodeDir());
        if (buildTool != BUILD_TOOL.MVN) {//todo: add the support of gradle
            System.out.println("Do not support other build systems except mvn");
            return;
        }

        String[] testcases = regression.getTestCase().split(";");
        int testcaseCount = testcases.length;
        if (testcaseCount <= 1) {
            System.out.println("reg" + rid +" doesn't need partition!");
            return;
        }
        String parentDir = regression.getRic().getLocalCodeDir().getParent();
        List<TestErrResult> ricTestErrResultList = new ArrayList<>();
//        List<TestErrResult> buggyTestErrResultList = new ArrayList<>();

        for (int i = 1; i <= testcaseCount; i++) {
            String t = testcases[i - 1];
            String testCMD = JDK_CMD + MVN_TEST + t;

//            System.out.println("executing " + testCMD);
            //compile and test bic
            Executor executor =  new Executor().setDirectory(regression.getRic().getLocalCodeDir());
            TestResult.STATUS statusBic = executor.execTestWithResult(testCMD);

            String[] testcaseSplit = t.split("#");//0 for filename and 1 for test method name
            String reportName = "/" + REPORT_DIR + testcaseSplit[0] + ".txt";

            File ricReportFile = new File(regression.getRic().getLocalCodeDir().getPath() + reportName);

            //get the reduced err msg for ric and buggy
            if (ricReportFile.exists()) {//the failing testcase(after executing test) matches the one in database; this also has the report file!
                File ricReducedFile = new File(parentDir + "/" + rid + RIC_TAG + i + ERR_POSTFIX);
                ricReducedFile.createNewFile();
                String ricErrInfo = reduceErrMsg(ricReportFile, ricReducedFile, testcaseSplit[0]);
                ricTestErrResultList.add(new TestErrResult(t, ricErrInfo));
            }
        }

        Map<String, List<TestErrResult>> ricTestErrResultsByErrInfo = ricTestErrResultList.stream()
                .collect(Collectors.groupingBy(TestErrResult::getErrInfo));
        System.out.println(ricTestErrResultsByErrInfo);
        System.out.println(ricTestErrResultsByErrInfo.size());
        if (ricTestErrResultsByErrInfo.size() == 1) {
            System.out.println("No partition needed for this reg " + rid);
        } else {
            ricTestErrResultsByErrInfo.forEach((key, value)->{
                System.out.println(rid + ": " + joinTestcase(value));
            });
        }
    }

    //write the reduced err msg to file and print the error info
    private static String reduceErrMsg(File originalFile, File reducedFile, String testFileName) throws IOException {
        String errInfo = "";
        try (BufferedReader ricReader = new BufferedReader(new FileReader(originalFile))) {
            try (BufferedWriter ricWriter = new BufferedWriter(new FileWriter(reducedFile))) {
                String line = ricReader.readLine();
                while (line != null) {
                    boolean hasTestFileName = line.contains(testFileName);
                    boolean hasException = line.contains("Exception") || line.contains("Error");
                    if ((hasTestFileName || hasException) &&
                            !(line.contains("Test set:") || line.contains("Tests run:") || line.contains("Time elapsed:"))) {
                        ricWriter.write(line);
                        ricWriter.newLine();
                        if (hasException) {
                            errInfo = line;
                        }
                    }
                    line = ricReader.readLine();
                }

            }
        }
        return errInfo;
    }

    private static BUILD_TOOL defectBuildTool(File codeDir) {
        String[] childs = codeDir.list();
        if (childs == null) return BUILD_TOOL.MVN;
        boolean isMvn = Arrays.asList(childs).contains("pom.xml");
        if (isMvn) {
            return BUILD_TOOL.MVN;
        } else {
            return BUILD_TOOL.GRADLEW;
        }
    }

    //convert a testcase list to a String
    private static String joinTestcase(List<TestErrResult> testErrResultList) {
        int size = testErrResultList.size();
        StringBuilder builder = new StringBuilder(testErrResultList.get(0).getTestcase());
        for (int i = 1; i < size; i++) {
            builder.append(';').append(testErrResultList.get(i).getTestcase());
        }
        return builder.toString();
    }

    enum BUILD_TOOL {
        MVN,
        GRADLEW;
    }

    static class TestErrResult {
        String testcase;
        String errInfo;

        public TestErrResult(String testcase, String errInfo) {
            this.testcase = testcase;
            this.errInfo = errInfo;
        }

        public String getErrInfo() {
            return errInfo;
        }

        public String getTestcase() {
            return testcase;
        }

        @Override
        public String toString() {
            return testcase + ": " + errInfo + '\n';
        }
    }
}
