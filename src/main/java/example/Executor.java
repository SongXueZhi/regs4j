package example;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
    
    public String exec(String cmd) {
    	try {
    		return this.exec(cmd, 0);
    	} catch (TimeoutException e) {
    		e.printStackTrace(); // should not timeout
    		return null;
    	}
    }

    /**
     * Run command line and get results,you can combine the multi-command by ";"
     * for example: mvn test -Dtest="testcase",or git reset;mvn compile
     *
     * @param cmd command line
     * @return return result by exec command
     */
    public String exec(String cmd, int timeout) throws TimeoutException{
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
            if (timeout > 0) {
            	boolean completed = process.waitFor(timeout, TimeUnit.MINUTES);
            	if (!completed)
            		throw new TimeoutException();
            }
            inputStr = new InputStreamReader(process.getInputStream());
            bufferReader = new BufferedReader(inputStr);
            String line;
            while ((line = bufferReader.readLine()) != null) {
                builder.append("\n").append(line);
            }
            int a = process.waitFor();
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

    // 请注意以最小的单元运行任务
    public boolean execBuildWithResult(String cmd) {
        Process process = null;
        InputStreamReader inputStr = null;
        BufferedReader bufferReader = null;
        try {
            if (OS.contains(OS_WINDOWS)) {
                pb.command("cmd.exe", "/c", cmd);
            } else {
                pb.command("bash", "-c", cmd);
            }
            process = pb.start();
            inputStr = new InputStreamReader(process.getInputStream(), "gbk");
            bufferReader = new BufferedReader(inputStr);
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = bufferReader.readLine()) != null) {
                line = line.toLowerCase();
                sb.append(line + "\n");
                // FileUtils.writeStringToFile(new File("build_log.txt"), line, true);
                if (line.contains("success")) {
                    return true;
                }
            }
        } catch (IOException ex) {
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
        return false;
    }


    // 请注意以最小的单元运行任务
    public TestResult.STATUS execTestWithResult(String cmd) {
        Process process = null;
        Timer t = null;
        InputStreamReader inputStr = null;
        BufferedReader bufferReader = null;
        try {
            if (OS.contains(OS_WINDOWS)) {
                pb.command("cmd.exe", "/c", cmd);
            } else {
                pb.command("bash", "-c", cmd);
            }
            process = pb.start();
            t = new Timer();
            Process finalProcess = process;
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    finalProcess.destroy();
                }
            }, 60000);
            inputStr = new InputStreamReader(process.getInputStream());
            bufferReader = new BufferedReader(inputStr);
            String line;
            boolean testCE = false;
            while ((line = bufferReader.readLine()) != null) {
                line = line.toLowerCase();
                if (line.contains("build success")) {
                    return TestResult.STATUS.PASS;
                } else if (line.contains("compilation error") || line.contains("compilation failure")) {
                    testCE = true;
                } else if (line.contains("no test")) {
                    return TestResult.STATUS.NOTEST;
                }
            }
            if (testCE) {
                return TestResult.STATUS.CE;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {

                if (t != null) {
                    t.cancel();
                }
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
        return TestResult.STATUS.FAIL;
    }

    public List<String> runCommand(String cmd) {
        Process process = null;
        InputStreamReader inputStr = null;
        BufferedReader bufferReader = null;
        List<String> result = new ArrayList<String>();
        try {
            if (OS.contains(OS_WINDOWS)) {
                pb.command("cmd.exe", "/c", cmd);
            } else {
                pb.command("bash", "-c", cmd);
            }
            process = pb.start();
            inputStr = new InputStreamReader(process.getInputStream());
            bufferReader = new BufferedReader(inputStr);
            String line;
            while ((line = bufferReader.readLine()) != null) {
                result.add(line);
            }
            int a = process.waitFor();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            try{
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
        return result;
    }

}

