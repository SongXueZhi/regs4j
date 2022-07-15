package example;

import core.Configs;
import core.MysqlManager;
import model.Regression;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import run.Executor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @Author: sxz
 * @Date: 2022/07/14/22:41
 * @Description:
 */
public class DeltaDebugging {
    static Executor executor = new Executor();

    public static void dd(String projectName) throws Exception {
        System.out.println("current project: "+projectName);
        String sql = "select regression_uuid,bfc,buggy,bic,work," +
                "testcase," +
                "regression.project_full_name,results.error_type from regression\n" +
                "inner join results\n" +
                "on regression.bfc = results.rfc_id\n" +
                "where results.error_type is not null and regression.project_full_name ='" + projectName +
                "'";
        List<Regression> regressionList = MysqlManager.getRegressions(sql);
        System.out.println("regression task size :"+regressionList.size());
        File workSpace = new File(Configs.workSpace);
        File projectDir = new File(workSpace, projectName.replace("/", "_"));
        System.out.println(projectDir);
        executor.setDirectory(workSpace);
        String command;
        String badName;
        String godName;
        String sql1;
        String regressionID;
        try {
            for (Regression regression : regressionList) {
                cleanCache(projectDir);
                regressionID = regression.getId();
                godName = regressionID + "_work";
                badName = regressionID + "_ric";
                System.out.println("start ddj");
                command = "./cca.py ddjava --include src/main/java " + projectName.replace("/", "_") + " "
                        + godName +" "+ badName;
                System.out.println(command);
                executor.exec(command);
                String ddjResult = getDDJResult(projectDir);
                backUP(projectDir,regressionID+"_ddj");
                System.out.println("start ddmin");
                command = "./cca.py ddplain --include src/main/java " + "--lang java " + projectName.replace("/", "_") + " "
                        + godName +" "+ badName;
                System.out.println(command);
                executor.exec(command);
                String ddMinResult = getDDMinResult(projectDir,badName,godName);
                backUP(projectDir,regressionID+"_ddmin");
                MysqlManager.insertDD(regressionID,"ric",ddMinResult,ddjResult);
                Thread.sleep(2000);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String getDDMinResult(File projectDir,String badName,String godName) throws IOException {
        String result = "";
        File[] child = projectDir.listFiles();
        for (File file : child) {
            if (file.getName().contains("__CCA__")) {
                File patchDir = new File(file,
                        "dd" + File.separator + "delta" + File.separator + projectDir.getName()+File.separator+godName+"-"+badName);
               System.out.println("ddmin patch dir:"+patchDir);
                if (patchDir.exists()){
                    File[] childs = patchDir.listFiles();
                    if (childs.length>0){
                        for (File file1 :childs){
                            if (file1.getName().startsWith("minimal")){
                                System.out.println("ddmin result file :"+file1);
                                result = FileUtils.readFileToString(file1,StandardCharsets.UTF_8);
                                System.out.println(result);
                             continue;
                            }
                        }
                    }
                }
                continue;
            }
        }
        return  result;
    }

    public static String getDDJResult(File projectDir) throws IOException {
        String result = "";
        File[] child = projectDir.listFiles();
        for (File file : child) {
            if (file.getName().contains("__CCA__")) {
                File patchDir = new File(file,
                        "dd" + File.separator + "patches" + File.separator + projectDir.getName());
                if (patchDir.exists()) {
                    File[] files = patchDir.listFiles();
                    if (files.length > 0) {
                        System.out.println("ddj result file :"+files[0]);
                        result = FileUtils.readFileToString(files[0], StandardCharsets.UTF_8);
                        System.out.println(result);
                    }
                }
                continue;
            }
        }
        return result;
    }

    public static void backUP(File projectDir,String name) throws IOException {
        File[] child = projectDir.listFiles();
        for (File file : child) {
            if (file.getName().contains("__CCA__")) {
                file.renameTo(new File(projectDir,name));
            }

        }
    }
    public static void cleanCache(File projectDir) throws IOException {
        File[] child = projectDir.listFiles();
        for (File file : child) {
            if (file.getName().contains("__CCA__")) {
                FileUtils.forceDelete(file);
                if (file.exists()){
                    FileUtils.deleteDirectory(file);
                }
            }
        }
    }
}
