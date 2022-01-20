package core.test;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class TestManager {
    final static String REPORT_PATH = "target" + File.separator + "surefire-reports" + File.separator + "junitreports";
    final static SAXReader reader = new SAXReader();

    public List<String> getErrors(File codePath) {
        File reportDirectory = new File(codePath, REPORT_PATH);
        if (!reportDirectory.exists() || !reportDirectory.isDirectory())
            return null;
        List<String> errorMessages = new ArrayList<>();
        for (String report : reportDirectory.list()) {
            String errorMessage = getError(new File(reportDirectory, report));
            if (errorMessage != null)
                errorMessages.add(errorMessage);
        }
        return errorMessages;
    }
    
    private String getError(File codePath) {
        try {
            Document doc = reader.read(codePath);
            Element root = doc.getRootElement();
            Element testCase = root.element("testcase");
            Element error = testCase.element("error");
            if (error != null)
                return error.attributeValue("type");
            Element failure = testCase.element("failure");
            if (failure != null)
                return failure.attributeValue("type");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
