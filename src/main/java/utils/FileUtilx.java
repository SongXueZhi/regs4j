package utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.WriterOutputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileUtilx {

    public static String getDirectoryFromPath(String path) {
        return path.contains("/") ? path.substring(0, path.lastIndexOf("/")) : "";
    }

    public static String readContentFromFile(String path) {
        File file = new File(path);
        String result = null;
        try {
            InputStream is = new FileInputStream(file);
            if (file.exists() && file.isFile()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuffer sb2 = new StringBuffer();
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb2.append(line + "\n");
                }
                br.close();
                result = sb2.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Set<String> readSetFromFile(String path) {
        Set<String> result = new HashSet<>();
        File file = new File(path);
        try {
            InputStream is = new FileInputStream(file);
            if (file.exists() && file.isFile()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line = null;
                while ((line = br.readLine()) != null) {
                    result.add(line);
                }
                br.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static List<String> readListFromFile(String path) {
        List<String> result = new ArrayList<>();
        File file = new File(path);
        try {
            InputStream is = new FileInputStream(file);
            if (file.exists() && file.isFile()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line = null;
                while ((line = br.readLine()) != null) {
                    result.add(line);
                }
                br.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void writListToFile(String path,List<String> line) {
        File file = new File(path);
        try {
            FileOutputStream fos = new FileOutputStream(path,false);
            if (file.exists() && file.isFile()) {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));
                for(String s:line) {
                    bw.write(s);
                    bw.newLine();
                    bw.flush();
                }
                bw.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Boolean moveFileToTarget(String fileFullNameCurrent, String fileFullNameTarget) {
        boolean ismove = false;
        File oldName = new File(fileFullNameCurrent);
        if (!oldName.exists() || oldName.isDirectory()) {
            return false;
        }

        File newName = new File(fileFullNameTarget);
        if (newName.exists() || newName.isDirectory()) {
            return false;
        }

        String parentFile = newName.getParent();
        File parentDir = new File(parentFile);
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        ismove = oldName.renameTo(newName);
        return ismove;
    }

    public static void copyFileToTarget(String fileFullNameCurrent, String fileFullNameTarget){
        try{
            File oldName = new File(fileFullNameCurrent);
            if (!oldName.exists() || oldName.isDirectory()) {
                return;
            }
            File newName = new File(fileFullNameTarget);
            if (newName.exists() || newName.isDirectory()) {
                return;
            }

            File parentDir = new File(newName.getParent());
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            FileUtils.copyDirectory(oldName, newName);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

}
