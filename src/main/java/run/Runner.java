package run;

import core.coverage.CodeCoverage;
import core.coverage.model.CoverNode;
import core.maven.JacocoMavenManager;
import model.Methodx;

import java.io.File;
import java.util.List;
import java.util.Map;

public class Runner {
    CodeCoverage codeCoverage = new CodeCoverage();
    JacocoMavenManager jacocoMavenManager = new JacocoMavenManager();

    public List<CoverNode> runTestWithJacoco(File revDir, Map<String, List<String>> testClassMethodMap)  {
        List<CoverNode> coverNodeList = codeCoverage.readJacocoReports(revDir);
        if (coverNodeList == null) {
            try {
                coverNodeList = testWithJacoco(revDir,testClassMethodMap);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return coverNodeList;
    }

    private List<CoverNode> testWithJacoco(File dir, Map<String,List<String>> testClassAndMethodMap) throws Exception {
        //add Jacoco plugin
        try {
            jacocoMavenManager.addJacocoFeatureToMaven(dir);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        for (Map.Entry<String,List<String> >entry: testClassAndMethodMap.entrySet()) {
            String testCommand = "mvn test -Dtest="+entry.getKey()+"#"+entry.getValue();
            new Executor().setDirectory(dir).exec(testCommand);
        }

        // git test coverage methods
        return codeCoverage.readJacocoReports(dir);
    }
}
