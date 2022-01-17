package run;

import core.coverage.CodeCoverage;
import core.coverage.model.CoverNode;
import core.maven.JacocoMavenManager;
import core.test.TestManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Runner {
    private static CodeCoverage codeCoverage = new CodeCoverage();
    private static JacocoMavenManager jacocoMavenManager = new JacocoMavenManager();
    private static TestManager testManager = new TestManager();

    private List<CoverNode> coverNodes;
    private List<String> errorMessages;
    private File revDir;
    private String testCase;

    public Runner(File revDir, String testCase) {
        this.revDir = revDir;
        this.testCase = testCase;
        this.coverNodes = codeCoverage.readJacocoReports(this.revDir);
        this.errorMessages = testManager.getErrors(this.revDir);
    }

    public List<CoverNode> getCoverNodes() {
        if (this.coverNodes == null) {
            this.run();
        }
        return this.coverNodes;
    }

    public List<String> getErrorMessages() {
        if (this.errorMessages == null) {
            this.run();
        }
        return this.errorMessages;
    }

    private void run() {
        //add Jacoco plugin
        try {
            jacocoMavenManager.addJacocoFeatureToMaven(this.revDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // execute the test
        String testCommand = "mvn test -Dtest="+this.testCase+" "+"-Dmaven.test.failure.ignore=true";
        try {
            new Executor().setDirectory(this.revDir).exec(testCommand);
            this.coverNodes = codeCoverage.readJacocoReports(this.revDir);
            this.errorMessages = testManager.getErrors(this.revDir);
        } catch (TimeoutException ex) {
            this.errorMessages = new ArrayList<>();
            this.errorMessages.add(ex.getClass().getName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
