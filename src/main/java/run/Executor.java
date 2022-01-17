package run;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author knightsong
 */
public class Executor {

    public final static String OS_WINDOWS = "windows";
    public final static String OS_MAC = "mac";
    public final static String OS_UNIX = "unix";
    protected static String OS;

    static {
        OS = System.getProperty("os.name").toLowerCase();
    }

    ProcessBuilder pb = new ProcessBuilder();

    /**
     * Set working directory to process
     *
     * @param file working directory
     */
    public Executor setDirectory(File file) {
        pb.directory(file);
        return this;
    }

    /**
     * Run command line and get results,you can combine the multi-command by ";"
     * for example: mvn test -Dtest="testcase",or git reset;mvn compile
     *
     * @param cmd command line
     * @return return result by exec command
     */
    public String exec(String cmd) throws TimeoutException{
        StringBuilder builder = new StringBuilder();
        Process process = null;
        InputStreamReader inputStr = null;
        BufferedReader bufferReader = null;
        pb.redirectErrorStream(true); //redirect error stream to standard stream
        try {
            if (OS.contains(OS_WINDOWS)) {
                pb.command("cmd.exe", "/c", cmd);
            } else {
                pb.command("bash", "-c", cmd);
            }
            process = pb.start();
            boolean completed = process.waitFor(1, TimeUnit.MINUTES);
            if (!completed) { // if process timeouts, terminate
                throw new TimeoutException();
            }
            inputStr = new InputStreamReader(process.getInputStream());
            bufferReader = new BufferedReader(inputStr);
            String line;
            while ((line = bufferReader.readLine()) != null) {
                builder.append("\n").append(line);
            }
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (process != null) {
                    process.destroy();
                }
                if (inputStr != null) {
                    IOUtils.close(inputStr);
                }
                if (bufferReader != null) {
                    IOUtils.close(bufferReader);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return builder.toString();
    }
}

