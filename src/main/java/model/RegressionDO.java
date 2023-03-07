package model;

/**
 * @Author: sxz
 * @Date: 2023/02/24/15:47
 * @Description:
 */
public class RegressionDO {
    String id;
    String rfc;
    String buggy;
    String ric;
    String work;
    String testCase;
    String projectFullName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRfc() {
        return rfc;
    }

    public void setRfc(String rfc) {
        this.rfc = rfc;
    }

    public String getBuggy() {
        return buggy;
    }

    public void setBuggy(String buggy) {
        this.buggy = buggy;
    }

    public String getRic() {
        return ric;
    }

    public void setRic(String ric) {
        this.ric = ric;
    }

    public String getWork() {
        return work;
    }

    public void setWork(String work) {
        this.work = work;
    }

    public String getTestCase() {
        return testCase;
    }

    public void setTestCase(String testCase) {
        this.testCase = testCase;
    }

    public String getProjectFullName() {
        return projectFullName;
    }

    public void setProjectFullName(String projectFullName) {
        this.projectFullName = projectFullName;
    }
}
