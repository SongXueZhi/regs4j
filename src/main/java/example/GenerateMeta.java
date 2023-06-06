package example;

import model.Regression;

import java.util.List;

public class GenerateMeta {
//    private final String REPORT_DIR = "./target/surefire-reports/";


    private final String GENERATED_META_DIR = "./generated_meta/";//divided by project name and reg id: generated_meta/pname/rid

    private String projName;

    private List<Regression> regressionList;

    public GenerateMeta() {

    }

    public void setRegressionList(List<Regression> regressionList) {
        this.regressionList = regressionList;
    }

    public void setProjName(String projName) {
        this.projName = projName;
    }

    public void generateMeta() {
        if (regressionList == null) {
            System.out.println("Null regressionList, initialize it first!");
            return;
        }
        for (Regression r : regressionList) {
            if (r.getTestCase().equals("")) continue; //skip the bug which has no testcase
            String[] testcases = r.getTestCase().split(";");
            for (String tc : testcases) {
                //todo: test and analyze
            }
        }
    }
}
