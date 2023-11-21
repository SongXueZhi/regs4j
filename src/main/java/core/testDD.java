package core;

import core.git.GitUtils;
import model.DDOutput;
import model.HunkEntity;
import model.Regression;
import model.Revision;
import org.apache.commons.io.FileUtils;

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
        test("bic", "ddmin");
    }

    public static void test(String version, String tool) throws Exception {
        String sql = "select * from regressions_all where is_clean=1 and is_dirty=0 and id not in " +
                "(select regression_id from regression_dd_result where version = '" + version + "' and tool = '" + tool + "');\n";
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
                if(version.equals("bfc")) {
                    dd = new rfcDD();
                    sourceCodeManager.createShell(regression.getId(), projectName, ric, regression.getTestCase(), regression.getErrorType());
                    sourceCodeManager.createShell(regression.getId(), projectName, work, regression.getTestCase(), regression.getErrorType());
                    sourceCodeManager.createShell(regression.getId(), projectName, rfc, regression.getTestCase(), regression.getErrorType());
                    sourceCodeManager.createShell(regression.getId(), projectName, buggy, regression.getTestCase(), regression.getErrorType());
                }else {
                    dd = new ProbDD();
                    sourceCodeManager.createShell(regression.getId(), projectName, ric, regression.getTestCase());
                    sourceCodeManager.createShell(regression.getId(), projectName, work, regression.getTestCase());
                    sourceCodeManager.createShell(regression.getId(), projectName, rfc, regression.getTestCase());
                    sourceCodeManager.createShell(regression.getId(), projectName, buggy, regression.getTestCase());
                }
                dd.bw.append("\n" + regressionId);

                List<HunkEntity> hunks = GitUtils.getHunksBetweenCommits(ricDir, ric.getCommitID(), work.getCommitID());
//            hunks.removeIf(hunkEntity -> !hunkEntity.getNewPath().contains(".java") && !hunkEntity.getOldPath().contains(".java"));
                hunks.removeIf(hunkEntity -> hunkEntity.getNewPath().contains("test") || hunkEntity.getOldPath().contains("test"));
                dd.bw.append("原hunk的数量是: " + hunks.size() + ":" + hunks);
                System.out.println("原hunk的数量是: " + hunks.size() + ":" + hunks);
                DDOutput ddoutput = null;
                if(tool.equals("ddmin")){
                    ddoutput = dd.ddmin(ric.getLocalCodeDir().toString(),hunks);
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

}
