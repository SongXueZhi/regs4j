package core;

import core.git.GitUtils;
import model.Revision;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class SourceCodeManager {

    private final static String metaProjectsDirPath = Configs.workSpace + File.separator + "meta_projects";
    private final static String cacheProjectsDirPath = Configs.workSpace + File.separator + "transfer_cache";

    public File checkout(Revision revision, File projectFile, String projectFullName) {
        //copy source code from meta project dir
        String projectDirName = projectFullName.replace("/", "_");
        File projectCacheDir = new File(cacheProjectsDirPath + File.separator, projectDirName);
        if (projectCacheDir.exists() && !projectCacheDir.isDirectory()) {
            projectCacheDir.delete();
        }
        projectCacheDir.mkdirs();

        File revisionDir = new File(projectCacheDir, revision.getName());
        try {
            FileUtils.forceDelete(revisionDir);
            FileUtils.copyDirectoryToDirectory(projectFile, projectCacheDir);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
        new File(projectCacheDir, projectDirName).renameTo(revisionDir);
        //git checkout
        if(GitUtils.checkout(revision.getCommitID(),revisionDir)){
            return revisionDir;
        }
       return null;
    }

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

}
