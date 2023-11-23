package core;

import core.coverage.model.CoverNode;
import core.git.GitUtils;
import model.DDOutput;
import model.HunkEntity;
import model.Regression;
import model.Revision;
import org.apache.commons.io.FileUtils;
import run.Runner;
import utils.FileUtilx;

import java.io.*;
import java.util.*;

/**
 * @author lsn
 * @date 2023/2/15 3:18 PM
 */
public class testDD {

    static Reducer reducer = new Reducer();
    static Migrator migrator = new Migrator();
    static SourceCodeManager sourceCodeManager = new SourceCodeManager();

    public static void main(String [] args) throws Exception {
        test("bfc", "ddmin");
    }

    public static void test(String version, String tool) throws Exception {
        String sql = "select * from regressions_all where is_clean=1 and is_dirty=0 and id not in " +
                "(select regression_id from regression_dd_result where version = '" + version + "' and tool = '" + tool + "');\n";
//        String sql = "select * from regressions_all where id = 2";
        List<Regression> regressionList = MysqlManager.selectCleanRegressions(sql);
        for (int i = 0; i < regressionList.size(); i++) {
            try{

                Regression regression = regressionList.get(i);
                String projectName = regression.getProjectFullName();
                File projectDir = sourceCodeManager.getProjectDir(regression.getProjectFullName());
                String regressionId = regression.getId();
                System.out.println("\n" + regression.getId());

                Revision rfc = regression.getRfc();
                File rfcDir = sourceCodeManager.checkout(regressionId, rfc, projectDir, projectName);
                rfc.setLocalCodeDir(rfcDir);

                Revision ric = regression.getRic();
                File ricDir = sourceCodeManager.checkout(regressionId, ric, projectDir, projectName);
                ric.setLocalCodeDir(ricDir);

                Revision work = regression.getWork();
                File workDir = sourceCodeManager.checkout(regressionId, work, projectDir, projectName);
                work.setLocalCodeDir(workDir);

                Revision buggy = new Revision(regression.getRfc()+"~1","buggy");
                regression.setBuggy(buggy);
                File buggyDir = sourceCodeManager.checkout(regressionId, buggy, projectDir, projectName);
                buggy.setLocalCodeDir(buggyDir);

                List<Revision> needToTestMigrateRevisionList = Arrays.asList(ric, work, buggy);
                migrateTestAndDependency(rfc, needToTestMigrateRevisionList, regression.getTestCase());

                DD dd;
                List<HunkEntity> hunks;
                if(version.equals("bfc")) {
                    dd = new rfcDD();
                    String buggyPath = buggyDir.toString();
                    String tmpPath = buggyPath.replace("_buggy","_tmp");
                    FileUtilx.copyDirToTarget(buggyPath,tmpPath);
                    Runner buggyRunner = new Runner(tmpPath, regression.getTestCase());
                    List<String> buggyErrorMessages = buggyRunner.getErrorMessages();
                    regression.setErrorType(buggyErrorMessages.get(0));
                    sourceCodeManager.createShell(regression.getId(), projectName, ric, regression.getTestCase(), regression.getErrorType());
                    sourceCodeManager.createShell(regression.getId(), projectName, work, regression.getTestCase(), regression.getErrorType());
                    sourceCodeManager.createShell(regression.getId(), projectName, rfc, regression.getTestCase(), regression.getErrorType());
                    sourceCodeManager.createShell(regression.getId(), projectName, buggy, regression.getTestCase(), regression.getErrorType());
                    hunks = GitUtils.getHunksBetweenCommits(buggyDir, rfc.getCommitID(), buggy.getCommitID());
                }else {
                    dd = new ProbDD();
                    sourceCodeManager.createShell(regression.getId(), projectName, ric, regression.getTestCase());
                    sourceCodeManager.createShell(regression.getId(), projectName, work, regression.getTestCase());
                    sourceCodeManager.createShell(regression.getId(), projectName, rfc, regression.getTestCase());
                    sourceCodeManager.createShell(regression.getId(), projectName, buggy, regression.getTestCase());
                    hunks = GitUtils.getHunksBetweenCommits(ricDir, ric.getCommitID(), work.getCommitID());
                }
                dd.bw.append("\n" + regressionId);

//            hunks.removeIf(hunkEntity -> !hunkEntity.getNewPath().contains(".java") && !hunkEntity.getOldPath().contains(".java"));
                hunks.removeIf(hunkEntity -> hunkEntity.getNewPath().contains("test") || hunkEntity.getOldPath().contains("test"));
                dd.bw.append("原hunk的数量是: " + hunks.size() + ":" + hunks);
                System.out.println("原hunk的数量是: " + hunks.size() + ":" + hunks);
                DDOutput ddoutput = null;
                if(tool.equals("ddmin")){
                    ddoutput = dd.ddmin(buggy.getLocalCodeDir().toString(),hunks);
                }else if(tool.equals("probdd")) {
                    ddoutput = dd.ProbDD(ric.getLocalCodeDir().toString(), hunks);
                }
                MysqlManager.insertAllResult(Integer.parseInt(regressionId), ddoutput, tool, version);

                FileUtils.deleteDirectory(rfcDir);
                FileUtils.deleteDirectory(ricDir);
                FileUtils.deleteDirectory(workDir);
                FileUtils.deleteDirectory(buggyDir);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    static void migrateTestAndDependency(Revision rfc, List<Revision> needToTestMigrateRevisionList, String testCase) {
        migrator.equipRfcWithChangeInfo(rfc);
        reducer.reduceTestCases(rfc, testCase);
        needToTestMigrateRevisionList.forEach(revision -> {
            migrator.migrateTestFromTo_0(rfc, revision);
        });
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

}
