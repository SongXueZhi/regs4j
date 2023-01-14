package core;

import core.git.GitUtils;
import model.Revision;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SourceCodeManager {

    private final static String metaProjectsDirPath = Configs.workSpace + File.separator + "meta_projects";
    private final static String cacheProjectsDirPath = Configs.workSpace;

    public File checkout(String regressionID,Revision revision, File projectFile, String projectFullName) {
        //copy source code from meta project dir
        String projectDirName = projectFullName.replace("/", "_");
        File projectCacheDir = new File(cacheProjectsDirPath + File.separator, projectDirName);
        if (projectCacheDir.exists() && !projectCacheDir.isDirectory()) {
            projectCacheDir.delete();
        }
        projectCacheDir.mkdirs();

        File revisionDir = new File(projectCacheDir,regressionID+"_"+revision.getName());
        try {
            if (revisionDir.exists()) {
                FileUtils.forceDelete(revisionDir);
            }
            FileUtils.copyDirectoryToDirectory(projectFile, projectCacheDir);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
        new File(projectCacheDir, projectDirName).renameTo(revisionDir);
        //git checkout
        if (GitUtils.checkout(revision.getCommitID(), revisionDir)) {
            return revisionDir;
        }
        return null;
    }

    //download source project
    public File getProjectDir(String projectFullName) {

        String projectDirName = projectFullName.replace("/", "_");
        checkRequiredDir();
        File projectFile = new File(metaProjectsDirPath + File.separator + projectDirName);
        if (projectFile.exists()) {
            return projectFile;
        } else {
            try {
                GitUtils.clone(projectFile, "https://github.com/" + projectFullName + ".git");
                return projectFile;
            } catch (Exception exception) {
                System.out.println(exception.getMessage());
            }
        }
        return null;
    }

    private void checkRequiredDir() {
        File metaProjectsDir = new File(metaProjectsDirPath);

        if (metaProjectsDir.exists()) {
            if (!metaProjectsDir.isDirectory()) {
                metaProjectsDir.delete();
                metaProjectsDir.mkdirs();
            }
        } else {
            metaProjectsDir.mkdirs();
        }

        File cacheProjectsDir = new File(cacheProjectsDirPath);
        if (cacheProjectsDir.exists()) {
            if (!cacheProjectsDir.isDirectory()) {
                cacheProjectsDir.delete();
                cacheProjectsDir.mkdirs();
            }
        } else {
            cacheProjectsDir.mkdirs();
        }
    }

    public void symbolicLink(String regressionID,String projectFullName, Revision ric, Revision work) throws IOException {
        String projectDirName = projectFullName.replace("/", "_");
        File badSourceFile = ric.getLocalCodeDir();
        File goodSourceFile = work.getLocalCodeDir();
        String badFlag = regressionID+"_"+ric.getName()+"_b";
        String goodFlag = regressionID+"_"+work.getName()+"_g";
        File badLink = new File(cacheProjectsDirPath + File.separator + projectDirName + File.separator + badFlag);

        File goodLink = new File(cacheProjectsDirPath + File.separator + projectDirName + File.separator + goodFlag);
        Path badSource = badSourceFile.toPath();
        Path goodSource = goodSourceFile.toPath();
        Path bLink = badLink.toPath();
        Path gLink = goodLink.toPath();
        Files.createSymbolicLink(bLink, badSource);
        Files.createSymbolicLink(gLink, goodSource);
    }

    public void createShell(String regressionID, String projectFullName, Revision revision, String testcase,
                            String errorType) {
        String projectDirName = projectFullName.replace("/", "_");
        File buildFile =
                new File(cacheProjectsDirPath + File.separator + projectDirName + File.separator + regressionID+"_"+revision.getName(),
                        "build.sh");
        File testFile =
                new File(cacheProjectsDirPath + File.separator + projectDirName + File.separator +  regressionID+"_"+revision.getName(),
                        "test.sh");
        if (!buildFile.exists()) {
            try {
                buildFile.createNewFile();
                String s1 = "#!/bin/bash";
                String s2 = "mvn clean compile test-compile &> /dev/null";
                FileUtils.write(buildFile, s1 + "\n" + s2, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!testFile.exists()) {
            try {
                testFile.createNewFile();
                String s1 = "#!/bin/bash" + "\n";
                String s2 = "_OUT=$(timeout 300 mvn test -Dtest=" + testcase + " 2>&1)" + "\n";
                String s3 = "SUCCESS=$(echo ${_OUT} | grep -c 'BUILD SUCCESS')" + "\n";
                String s4 = "FAIL=$(echo ${_OUT} | grep -c '" + errorType + "')" + "\n";
                String s5 = "if\n" +
                        " [ \"${SUCCESS}\" = '1' ]; then\n" +
                        "    PASS='1'\n" +
                        "else\n" +
                        "    PASS='0'\n" +
                        "fi\n" +
                        "\n" +
                        "if [ ${PASS} = '1' -a ${FAIL} = '0' ]; then\n" +
                        "    /bin/echo -n 'PASS'\n" +
                        "elif [ ${FAIL} = '1' ]; then\n" +
                        "    /bin/echo -n 'FAIL'\n" +
                        "else\n" +
                        "    /bin/echo -n 'UNRESOLVED'\n" +
                        "fi";
                FileUtils.write(testFile, s1 + s2 + s3 + s4 + s5, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        buildFile.setExecutable(true,false);
        buildFile.setReadable(true,false);
        buildFile.setWritable(true,false);
        testFile.setExecutable(true,false);
        testFile.setReadable(true,false);
        testFile.setWritable(true,false);
    }

    public void createShell(String regressionID, String projectFullName, Revision revision, String testcase) {
        String projectDirName = projectFullName.replace("/", "_");
        File buildFile =
                new File(cacheProjectsDirPath + File.separator + projectDirName + File.separator + regressionID+"_"+revision.getName(),
                        "build.sh");
        File testFile =
                new File(cacheProjectsDirPath + File.separator + projectDirName + File.separator +  regressionID+"_"+revision.getName(),
                        "test.sh");
        if (!buildFile.exists()) {
            try {
                buildFile.createNewFile();
                String s1 = "#!/bin/bash";
                String s2 = "mvn clean compile test-compile &> /dev/null";
                FileUtils.write(buildFile, s1 + "\n" + s2, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!testFile.exists()) {
            try {
                testFile.createNewFile();
                String s1 = "#!/bin/bash" + "\n";
                String s2 = "_OUT=$(timeout 300 mvn test -Dtest=" + testcase + " 2>&1)" + "\n";
                String s3 = "SUCCESS=$(echo ${_OUT} | grep -c 'BUILD SUCCESS')" + "\n";
                String s4 = "CE=$(echo ${_OUT} | grep -c 'COMPILATION ERROR')" + "\n";
                String s5 = "if\n" +
                        " [ \"${SUCCESS}\" = '1' ]; then\n" +
                        "    PASS='1'\n" +
                        "else\n" +
                        "    PASS='0'\n" +
                        "fi\n" +
                        "\n" +
                        "if [ ${PASS} = '1' -a ${CE} = '0' ]; then\n" +
                        "    /bin/echo -n 'PASS'\n" +
                        "elif [ ${CE} = '1' ]; then\n" +
                        "    /bin/echo -n 'CE'\n" +
                        "else\n" +
                        "    /bin/echo -n 'FAIL'\n" +
                        "fi";
                FileUtils.write(testFile, s1 + s2 + s3 + s4 + s5, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        buildFile.setExecutable(true,false);
        buildFile.setReadable(true,false);
        buildFile.setWritable(true,false);
        testFile.setExecutable(true,false);
        testFile.setReadable(true,false);
        testFile.setWritable(true,false);
    }

}
