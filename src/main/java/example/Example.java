package example;


import core.Migrator;
import core.MysqlManager;
import core.Reducer;
import core.SourceCodeManager;
import core.coverage.model.CoverNode;
import core.maven.MavenManager;
import model.Regression;
import model.Revision;
import model.Methodx;
import run.Runner;
import utils.CodeUtil;
import utils.FileUtilx;
import utils.ListUtil;
import utils.StringUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import org.eclipse.jdt.core.dom.Statement;

public class Example {

    static SourceCodeManager sourceCodeManager = new SourceCodeManager();
    static Reducer reducer = new Reducer();
    static Migrator migrator = new Migrator();
    static MavenManager mvnManager = new MavenManager();

    final static double SIMILARITY_INDEX = 0.8;

    public static void main(String[] args) {
        Set<String> projectFullNameList = FileUtilx.readSetFromFile("projects.txt");
        for (String projectFullName : projectFullNameList) {
            try {
                handleSingleProject(projectFullName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    static void handleSingleProject(String projectFullName) throws Exception{
        File projectDir = sourceCodeManager.getProjectDir(projectFullName);
        List<Regression> regressionList = MysqlManager.selectRegressions("select bfc,buggy,bic,work,testcase from regressions where project_full_name='" + projectFullName + "'");
        int count = 0;
        for (Regression regression : regressionList) {
            //prepare four version
            Revision rfc = regression.getRfc();
            File rfcDir = sourceCodeManager.checkout(regression.getId(),rfc, projectDir, projectFullName);
            rfc.setLocalCodeDir(rfcDir);

            Revision buggy = regression.getBuggy();
            File buggyDir = sourceCodeManager.checkout(regression.getId(),buggy, projectDir, projectFullName);
            buggy.setLocalCodeDir(buggyDir);

            Revision ric = regression.getRic();
            File ricDir = sourceCodeManager.checkout(regression.getId(),ric, projectDir, projectFullName);
            ric.setLocalCodeDir(ricDir);

            Revision work = regression.getWork();
            File workDir = sourceCodeManager.checkout(regression.getId(),work, projectDir, projectFullName);
            work.setLocalCodeDir(workDir);
            
            count += 1;
            System.out.println(String.format("Handling %s: %d/%d", projectFullName, count, regressionList.size()));
            
            List<Revision> needToTestMigrateRevisionList = Arrays.asList(new Revision[]{buggy, ric, work});
            migrateTestAndDependency(rfc, needToTestMigrateRevisionList, regression.getTestCase());

            //testWithJacoco
            System.out.println("\tProcessing rfc");
            Runner rfcRunner = new Runner(rfcDir, regression.getTestCase());
            List<CoverNode> rfcCoveredMethodList = rfcRunner.getCoverNodes();
            
            System.out.println("\tProcessing work");
            Runner workRunner = new Runner(workDir, regression.getTestCase());
            List<CoverNode> workCoveredMethodList = workRunner.getCoverNodes();

            System.out.println("\tProcessing buggy");
            Runner buggyRunner = new Runner(buggyDir, regression.getTestCase());
            List<CoverNode> buggyCoveredMethodList = buggyRunner.getCoverNodes();
            List<String> buggyErrorMessages = buggyRunner.getErrorMessages();
            
            System.out.println("\tProcessing ric");
            Runner ricRunner = new Runner(ricDir, regression.getTestCase());
            List<CoverNode> ricCoveredMethodList = ricRunner.getCoverNodes();
            
            double r_wscore = -1.0;
            double b_rscore = -1.0;
            double r_rscore = -1.0;
            
            if (rfcCoveredMethodList != null && workCoveredMethodList != null &&
            		buggyCoveredMethodList != null && ricCoveredMethodList != null) {
                String rfcSrcDir = mvnManager.getSrcDir(rfcDir);
                String workSrcDir = mvnManager.getSrcDir(workDir);
                String buggySrcDir = mvnManager.getSrcDir(buggyDir);
                String ricSrcDir = mvnManager.getSrcDir(ricDir);

                List<Methodx> rfcMethods = CodeUtil.getCoveredMethods(new File(rfcDir, rfcSrcDir), rfcCoveredMethodList);
                List<Methodx> workMethods = CodeUtil.getCoveredMethods(new File(workDir, workSrcDir), workCoveredMethodList);
                r_wscore = similarityScore(rfcMethods, workMethods);
                
                List<Methodx> buggyMethods = CodeUtil.getCoveredMethods(new File(buggyDir, buggySrcDir), buggyCoveredMethodList);
                List<Methodx> ricMethods = CodeUtil.getCoveredMethods(new File(ricDir, ricSrcDir), ricCoveredMethodList);
                b_rscore = similarityScore(buggyMethods, ricMethods);
                
                r_rscore = similarityScore(rfcMethods, ricMethods);
            }
            String errorsMsgs = StringUtil.join(buggyErrorMessages, ",");
            String updateQuery = String.format("INSERT INTO results VALUES ('%s', '%s', '%s', '%s', '%s', %f, %f, %f, '%s')", 
                                                projectFullName, rfc.getCommitID(), buggy.getCommitID(), ric.getCommitID(),
                                                work.getCommitID(), r_rscore, r_wscore, b_rscore, errorsMsgs);
            MysqlManager.executeUpdate(updateQuery);
            System.out.println(String.format("Done! rw: %f, br: %f, rr: %f", r_wscore, b_rscore, r_rscore));
        }
        System.out.println();
    }
    

    static void migrateTestAndDependency(Revision rfc, List<Revision> needToTestMigrateRevisionList, String testCase) {
        migrator.equipRfcWithChangeInfo(rfc);
        reducer.reduceTestCases(rfc, testCase);
        needToTestMigrateRevisionList.forEach(revision -> {
            migrator.migrateTestFromTo_0(rfc, revision);
        });
    }

    public static double similarityScore(List<Methodx> rfcMethods, List<Methodx> ricMethods) {
        double common = 0.0;
        for (Methodx rfcMethod : rfcMethods) {
        	if (rfcMethod == null) {
        		continue;
        	}
            List<Methodx> candidates = new ArrayList<>();
            String rfcName = rfcMethod.getSimpleName();
            for (Methodx ricMethod : ricMethods) {
            	if (ricMethod == null) {
            		continue;
            	}
                String ricName = ricMethod.getSimpleName();
                if (StringUtil.editDistance(rfcName, ricName) > SIMILARITY_INDEX) {
                    candidates.add(ricMethod);
                }
            }
            if (candidates.size() == 0) // not able to find a suitable candidate, try all methods
                candidates = ricMethods;
            double score = findSimilarityScore(rfcMethod, candidates);
            if (score > SIMILARITY_INDEX)
                common += 1.0;
        }
        return common/rfcMethods.size();
    }

    private static double findSimilarityScore(Methodx main, List<Methodx> candidates) {
        double result = 0.0;
        for (Methodx other : candidates) {
            double score = methodSimilarity(main, other);
            result = score > result ? score : result;
        }
        return result;
    }

    private static double methodSimilarity(Methodx main, Methodx other) {
        double totalScore = 0.0;
        if (main == null || other == null) {
        	return totalScore;
        }
        List<Statement> mainStatements = ListUtil.castList(Statement.class, main.getMethodDeclaration()
                                                                                .getBody().statements());
        List<Statement> otheStatements = ListUtil.castList(Statement.class, other.getMethodDeclaration()
                                                                                 .getBody().statements());
        for(Statement a: mainStatements) {
            double score = 0.0;
            for (Statement b: otheStatements) {
                if (a.getClass() == b.getClass()) {
                    double temp = statementSimilarity(a, b);
                    score = temp > score ? temp : score;
                }
            }
            totalScore += score;
        }
        return totalScore/mainStatements.size();
    }

    private static double statementSimilarity(Statement a, Statement b) {
        String aString = StringUtil.reduceWhitespace(a.toString());
        String bString = StringUtil.reduceWhitespace(b.toString());
        double score = StringUtil.editDistance(aString, bString);
        return score;
    }

}
