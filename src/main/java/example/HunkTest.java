package example;

import core.MysqlManager;
import core.SourceCodeManager;
import core.git.GitUtils;
import model.HunkEntity;
import model.Regression;
import model.Revision;

import utils.FileUtilx;

import java.io.File;
import java.util.List;
import java.util.Set;

import static utils.FileUtilx.readSetFromFile;

public class HunkTest {

    static SourceCodeManager sourceCodeManager = new SourceCodeManager();

    public static void main(String[] args) {
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
        Regression regressionTest = regressionList.get(0);

        Revision ric = regressionTest.getRic();
        File ricDir = sourceCodeManager.checkout(regressionTest.getId(),ric, projectDir, projectName);
        ric.setLocalCodeDir(ricDir);

        Revision work = regressionTest.getWork();
        File workDir = sourceCodeManager.checkout(regressionTest.getId(),work, projectDir, projectName);
        work.setLocalCodeDir(workDir);

        List<HunkEntity> hunks = GitUtils.getHunksBetweenCommits(ricDir, ric.getCommitID(), work.getCommitID());

        System.out.println(hunks);
    }

}
