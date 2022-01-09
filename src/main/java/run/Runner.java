package run;

import core.coverage.CodeCoverage;
import core.coverage.model.CoverNode;
import core.maven.JacocoMavenManager;

import java.io.File;
import java.util.List;
import java.util.Map;

public class Runner {
    CodeCoverage codeCoverage = new CodeCoverage();
    JacocoMavenManager jacocoMavenManager = new JacocoMavenManager();

    public List<CoverNode> runTestWithJacoco(File revDir, String testCase)  {
        List<CoverNode> coverNodeList = codeCoverage.readJacocoReports(revDir);
        if (coverNodeList == null) {
            try {
                coverNodeList = testWithJacoco(revDir,testCase);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return coverNodeList;
    }

    private List<CoverNode> testWithJacoco(File dir, String testCase) throws Exception {
        //add Jacoco plugin
        try {
            jacocoMavenManager.addJacocoFeatureToMaven(dir);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        String testCommand = "mvn test -Dtest="+testCase+" "+"-Dmaven.test.failure.ignore=true";
        new Executor().setDirectory(dir).exec(testCommand);

        // git test coverage methods
        return codeCoverage.readJacocoReports(dir);
    }
}
