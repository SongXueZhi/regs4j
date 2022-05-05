package model;

public class Regression {
    Revision rfc;
    Revision buggy;
    Revision ric;
    Revision work;
    String testCase;

    public String getTestCase() {
        return testCase;
    }

    public void setTestCase(String testCase) {
        this.testCase = testCase;
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
    
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("rfc: " + rfc);
    	sb.append(" | ric: " + ric);
    	sb.append(" | testcase: " + testCase);
    	return sb.toString();
    }
}
