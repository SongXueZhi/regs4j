package experiments.evo;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErrorMessageParser {

    public String[] parseErrorMessage(String errorMessage) {
        Pattern pattern = Pattern.compile("at\\s+([\\w\\.]+)\\.([\\w]+)\\.([\\w]+)\\(.*\\.java:(\\d+)\\)");
        Matcher matcher = pattern.matcher(errorMessage);

        String[] result = null;
        while (matcher.find()) {
            String packageName = matcher.group(1);
            String className = matcher.group(2);
            String methodName = matcher.group(3);
            String lineNumber = matcher.group(4);

            result = new String[]{packageName, className, methodName, lineNumber};
        }

        return result;
    }
}