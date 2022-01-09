package example;


import core.Migrator;
import core.MysqlManager;
import core.Reducer;
import core.SourceCodeManager;
import core.coverage.model.CoverNode;
import model.Regression;
import model.Revision;
import run.Runner;
import utils.FileUtilx;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Example {

    static SourceCodeManager sourceCodeManager = new SourceCodeManager();
    static Reducer reducer = new Reducer();
    static Migrator migrator = new Migrator();
    static Runner runner = new Runner();

    public static void main(String[] args) {
        Set<String> projectFullNameList = FileUtilx.readSetFromFile("projects.txt");

        for (String projectFullName : projectFullNameList) {
            handleSingleProject(projectFullName);
        }

    }

    static void handleSingleProject(String projectFullName) {
        File projectDir = sourceCodeManager.getProjectDir(projectFullName);
        List<Regression> regressionList = MysqlManager.selectRegressions("select bfc,buggy,bic,work,testcase from regressions where full_name='" + projectFullName + "'");

        for (Regression regression : regressionList) {

            //prepare four version
            Revision rfc = regression.getRfc();
            File rfcDir = sourceCodeManager.checkout(rfc, projectDir, projectFullName);
            rfc.setLocalCodeDir(rfcDir);

            Revision buggy = regression.getBuggy();
            File buggyDir = sourceCodeManager.checkout(buggy, projectDir, projectFullName);
            buggy.setLocalCodeDir(buggyDir);

            Revision ric = regression.getRic();
            File ricDir = sourceCodeManager.checkout(ric, projectDir, projectFullName);
            ric.setLocalCodeDir(ricDir);

            Revision work = regression.getWork();
            File workDir = sourceCodeManager.checkout(work, projectDir, projectFullName);
            work.setLocalCodeDir(workDir);


            List<Revision> needToTestMigrateRevisionList = Arrays.asList(new Revision[]{buggy, ric, work});

            //
            migrateTestAndDependency(rfc, needToTestMigrateRevisionList, regression.getTestCase());

            //testWithJacoco
            List<CoverNode> rfcCoveredMethodList = runner.runTestWithJacoco(rfcDir, regression.getTestCase());
            System.out.println("rfc coverage node size:" + rfcCoveredMethodList.size());

            List<CoverNode> buggyCoveredMethodList = runner.runTestWithJacoco(buggyDir, regression.getTestCase());
            System.out.println("buggy coverage node size:" + buggyCoveredMethodList.size());

            List<CoverNode> ricCoveredMethodList = runner.runTestWithJacoco(ricDir, regression.getTestCase());
            System.out.println("ric coverage node size:" + ricCoveredMethodList.size());

            List<CoverNode> workCoveredMethodList = runner.runTestWithJacoco(workDir, regression.getTestCase());
            System.out.println("work coverage node size:" + workCoveredMethodList.size());

            //TODO Siang Hwee
            System.out.println("Please compare coverage node similarity here");
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
