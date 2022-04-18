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
    private static final CodeCoverage codeCoverage = new CodeCoverage();
    private static final JacocoMavenManager jacocoMavenManager = new JacocoMavenManager();
    private static final TestManager testManager = new TestManager();

    protected List<CoverNode> coverNodes;
    protected List<String> errorMessages;
    protected File revDir;
    protected String testCase;

    public Runner(File revDir, String testCase) {
        this.revDir = revDir;
        this.testCase = testCase;
        this.coverNodes = codeCoverage.readJacocoReports(this.revDir);
        this.errorMessages = testManager.getErrors(this.revDir);
    }

    public List<CoverNode> getCoverNodes() {
        if (this.coverNodes == null) {
            this.runWithJacoco();
        }
        return this.coverNodes;
    }

    public List<String> getErrorMessages() {
        if (this.errorMessages == null) {
            this.run();
        }
        return this.errorMessages;
    }

    private void runWithJacoco() {
        //add Jacoco plugin
        try {
            jacocoMavenManager.addJacocoFeatureToMaven(this.revDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // execute the test
        run();
        this.coverNodes = codeCoverage.readJacocoReports(this.revDir);
    }

    public void run() {
        String buildCommand = "mvn compile";
        String testCommand = "mvn test -Dtest=" + this.testCase + " " + "-Dmaven.test.failure.ignore=true";
        try {
            new Executor().setDirectory(this.revDir).exec(buildCommand);
            new Executor().setDirectory(this.revDir).exec(testCommand, 5);
            this.errorMessages = testManager.getErrors(this.revDir);
        } catch (TimeoutException ex) {
            this.errorMessages = new ArrayList<>();
            this.errorMessages.add(ex.getClass().getName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
