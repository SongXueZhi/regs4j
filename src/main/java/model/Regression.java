package model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Regression {
    Revision rfc;
    Revision buggy;
    Revision ric;
    Revision work;
    String testCase;
    String projectFullName;
    String errorType;


    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("rfc: " + rfc);
    	sb.append(" | ric: " + ric);
    	sb.append(" | testcase: " + testCase);
    	return sb.toString();
    }
}
