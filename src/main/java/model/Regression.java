package model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Regression {
    Revision rfc;
    Revision buggy;
    Revision ric;
    Revision work;
    private Map<String, List<String>> testCaseMethodMp =new HashMap<>();

    public Map<String,  List<String>> getTestCaseMethodMp() {
        return testCaseMethodMp;
    }

    public void setTestCaseMethodMp(Map<String,  List<String>> testCaseMethodMp) {
        this.testCaseMethodMp = testCaseMethodMp;
    }

    public Revision getRfc() {
        return rfc;
    }

    public void setRfc(Revision rfc) {
        this.rfc = rfc;
    }

    public Revision getBuggy() {
        return buggy;
    }

    public void setBuggy(Revision buggy) {
        this.buggy = buggy;
    }

    public Revision getRic() {
        return ric;
    }

    public void setRic(Revision ric) {
        this.ric = ric;
    }

    public Revision getWork() {
        return work;
    }

    public void setWork(Revision work) {
        this.work = work;
    }
}
