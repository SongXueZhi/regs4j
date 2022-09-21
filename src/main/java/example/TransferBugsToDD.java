package example;

import core.Migrator;
import core.MysqlManager;
import core.Reducer;
import core.SourceCodeManager;
import model.Regression;
import model.Revision;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static utils.FileUtilx.readSetFromFile;

public class TransferBugsToDD {
    // prepare ddj input dir and build.sh test.sh

    static Reducer reducer = new Reducer();
    static Migrator migrator = new Migrator();
    static SourceCodeManager sourceCodeManager = new SourceCodeManager();

    static void checkout(String projectName) {

        //select所有error不为空的，download项目并测试用例迁移
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
        try {
            for (int i = 0; i < regressionList.size(); i++) {
                Regression regression = regressionList.get(i);
                //1. checkout bic和work，test migration
                String projectFullName = regression.getProjectFullName();
                //need download source project
                //File projectDir = sourceCodeManager.getProjectDir(projectFullName);

                //already have source project
                File projectDir = sourceCodeManager.getProjectDir(projectFullName);

                //bfc作为基准进行测试用例迁移
                Revision rfc = regression.getRfc();
                File rfcDir = sourceCodeManager.checkout(regression.getId(), rfc, projectDir, projectFullName);
                rfc.setLocalCodeDir(rfcDir);

                Revision ric = regression.getRic();
                File ricDir = sourceCodeManager.checkout(regression.getId(), ric, projectDir, projectFullName);
                ric.setLocalCodeDir(ricDir);

                Revision work = regression.getWork();
                File workDir = sourceCodeManager.checkout(regression.getId(), work, projectDir, projectFullName);
                work.setLocalCodeDir(workDir);

                List<Revision> needToTestMigrateRevisionList = Arrays.asList(ric, work);
                migrateTestAndDependency(rfc, needToTestMigrateRevisionList, regression.getTestCase());

//            //2.create symbolicLink for good and bad
//            sourceCodeManager.symbolicLink(regression.getId(),projectFullName, ric, work);

                //3.create sh(build.sh&test.sh)
                sourceCodeManager.createShell(regression.getId(), projectFullName, ric, regression.getTestCase(),
                        regression.getErrorType());
                sourceCodeManager.createShell(regression.getId(), projectFullName, work, regression.getTestCase(),
                        regression.getErrorType());

            }
        } catch (Exception e) {
            e.printStackTrace();
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
