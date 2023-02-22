package core;

import core.git.GitUtils;
import model.DDOutput;
import model.HunkEntity;
import model.Regression;
import model.Revision;

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
        String sql = "select * from regressions_all where is_clean=1 and is_dirty=0 and id not in (select regression_id from dd_result)";
        List<Regression> regressionList = MysqlManager.selectCleanRegressions(sql);
        for (int i = 0; i < regressionList.size(); i++) {
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

            List<Revision> needToTestMigrateRevisionList = Arrays.asList(ric, work);
            migrateTestAndDependency(rfc, needToTestMigrateRevisionList, regression.getTestCase());

            sourceCodeManager.createShell(regression.getId(), projectName, ric, regression.getTestCase());
            sourceCodeManager.createShell(regression.getId(), projectName, work, regression.getTestCase());

            List<HunkEntity> hunks = GitUtils.getHunksBetweenCommits(ricDir, ric.getCommitID(), work.getCommitID());
//            hunks.removeIf(hunkEntity -> !hunkEntity.getNewPath().contains(".java") && !hunkEntity.getOldPath().contains(".java"));
            hunks.removeIf(hunkEntity -> hunkEntity.getNewPath().contains("test") || hunkEntity.getOldPath().contains("test"));
            ProbDD.bw.append("\n" + regressionId);
            System.out.println("\n" + regressionId);
            ProbDD.bw.append("原hunk的数量是: " + hunks.size() + ":" + hunks);
            System.out.println("原hunk的数量是: " + hunks.size() + ":" + hunks);

            DDOutput ddminOutput= ProbDD.ddmin(ric.getLocalCodeDir().toString(),hunks);
            MysqlManager.insertResult(Integer.parseInt(regressionId), ddminOutput, "ddmin");
            DDOutput proddOutput = ProbDD.ProbDD(ric.getLocalCodeDir().toString(),hunks);
            MysqlManager.insertResult(Integer.parseInt(regressionId), proddOutput, "prodd");
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
