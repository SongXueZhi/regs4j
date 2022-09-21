package example;

import core.Migrator;
import core.MysqlManager;
import core.Reducer;
import core.SourceCodeManager;
import core.git.GitUtils;
import model.HunkEntity;
import model.Regression;
import model.Revision;
import org.junit.Assert;
import org.junit.Test;
import run.Executor;
import utils.FileUtilx;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static utils.FileUtilx.readSetFromFile;

public class RevertTest {
    static SourceCodeManager sourceCodeManager = new SourceCodeManager();

    @Test
    public  void revertTest() throws Exception{
        String projectName = (String) FileUtilx.readSetFromFile("projects.txt").toArray()[0];

        File projectDir = sourceCodeManager.getProjectDir(projectName);
        String sql = "select regression_uuid,bfc,buggy,bic,work," +
                "testcase," +
                "regression.project_full_name,results.error_type from regression\n" +
                "inner join results\n" +
                "on regression.bfc = results.rfc_id\n" +
                "where results.error_type is not null and regression.project_full_name ='" + projectName +
                "'";
        List<Regression> regressionList = MysqlManager.getRegressions(sql);

        Set<String> uuid = readSetFromFile("uuid.txt");
        regressionList.removeIf(regression -> !uuid.contains(regression.getId()));
        for (int i = 0; i < regressionList.size(); i++) {
            Regression regressionTest = regressionList.get(i);
            System.out.println(regressionTest.getId());

            Revision rfc = regressionTest.getRfc();
            File rfcDir = sourceCodeManager.checkout(regressionTest.getId(), rfc, projectDir, projectName);
            rfc.setLocalCodeDir(rfcDir);

            Revision ric = regressionTest.getRic();
            File ricDir = sourceCodeManager.checkout(regressionTest.getId(), ric, projectDir, projectName);
            ric.setLocalCodeDir(ricDir);

            Revision work = regressionTest.getWork();
            File workDir = sourceCodeManager.checkout(regressionTest.getId(), work, projectDir, projectName);
            work.setLocalCodeDir(workDir);

            List<Revision> needToTestMigrateRevisionList = Arrays.asList(ric, work);
            Revert.migrateTestAndDependency(rfc, needToTestMigrateRevisionList, regressionTest.getTestCase());

//            //2.create symbolicLink for good and bad
//            sourceCodeManager.symbolicLink(regression.getId(),projectFullName, ric, work);

            //3.create sh(build.sh&test.sh)
            sourceCodeManager.createShell(regressionTest.getId(), projectName, ric, regressionTest.getTestCase(),
                    regressionTest.getErrorType());
            sourceCodeManager.createShell(regressionTest.getId(), projectName, work, regressionTest.getTestCase(),
                    regressionTest.getErrorType());

            List<HunkEntity> hunks = GitUtils.getHunksBetweenCommits(ricDir, ric.getCommitID(), work.getCommitID());

            String path = ric.getLocalCodeDir().toString().replace("_ric","_tmp");
            FileUtilx.copyDirToTarget(ric.getLocalCodeDir().toString(),path);
            Revert.revert(path,hunks);
            Executor executor = new Executor();
            executor.setDirectory(new File(path));
            String result = executor.exec("./build.sh; ./test.sh");
            Assert.assertEquals("PASS",result);
        }
    }
}
