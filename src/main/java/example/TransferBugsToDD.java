package example;

import core.Migrator;
import core.MysqlManager;
import core.Reducer;
import core.SourceCodeManager;
import model.Regression;
import model.Revision;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class TransferBugsToDD {
    // TODO SunYujie prepare the data for DDJ and DDP
    // prepare ddj input dir and build.sh test.sh

    static Reducer reducer = new Reducer();
    static Migrator migrator = new Migrator();
    static SourceCodeManager sourceCodeManager = new SourceCodeManager();


    public static void main(String[] args) throws IOException {
        chckout();

    }

    static void chckout() throws IOException {
        //select所有error不为空的，download项目并测试用例迁移
        List<Regression> regressionList = MysqlManager.getRegressions("select bfc,buggy,bic,work,testcase,regressions.project_full_name,results.error_type from regressions\n" +
                "inner join results\n" +
                "on regressions.bfc = results.rfc_id\n" +
                "where results.error_type is not null and regressions.project_full_name = \"ktuukkan/marine-api\"");

        for (Regression regression : regressionList) {
            //1. checkout bic和work，test migration
            String projectFullName = regression.getProjectFullName();
            //need download source project
            //File projectDir = sourceCodeManager.getProjectDir(projectFullName);

            //already have source project
            File projectDir = sourceCodeManager.getMetaProjectDir(projectFullName);

            //bfc作为基准进行测试用例迁移
            Revision rfc = regression.getRfc();
            File rfcDir = sourceCodeManager.checkout(rfc, projectDir, projectFullName);
            rfc.setLocalCodeDir(rfcDir);

            Revision ric = regression.getRic();
            File ricDir = sourceCodeManager.checkout(ric, projectDir, projectFullName);
            ric.setLocalCodeDir(ricDir);

            Revision work = regression.getWork();
            File workDir = sourceCodeManager.checkout(work, projectDir, projectFullName);
            work.setLocalCodeDir(workDir);

            List<Revision> needToTestMigrateRevisionList = Arrays.asList(new Revision[]{ric, work});
            migrateTestAndDependency(rfc, needToTestMigrateRevisionList, regression.getTestCase());

            //2.create symbolicLink for good and bad
//            sourceCodeManager.symbolicLink(projectFullName,ric, work);

            //3.create sh(build.sh&test.sh)
            sourceCodeManager.createShell(projectFullName,"ric",regression.getTestCase(),regression.getErrorType());
            sourceCodeManager.createShell(projectFullName,"work",regression.getTestCase(),regression.getErrorType());

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
