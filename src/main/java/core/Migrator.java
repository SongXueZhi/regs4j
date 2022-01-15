package core;

import core.git.GitUtils;
import model.*;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.diff.DiffEntry;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Migrator {
    public final static String NONE_PATH = "/dev/null";

    public void migrateTestFromTo_0(Revision from, Revision to) {
        File bfcDir = from.getLocalCodeDir();
        File tDir = to.getLocalCodeDir();
        for (ChangedFile changedFile: from.getChangedFiles()) {

            if (changedFile instanceof  NormalFile){
                return;
            }

            String newPath = changedFile.getNewPath();
            if (newPath.contains(NONE_PATH)) {
                continue;
            }
            File bfcFile = new File(bfcDir, newPath);
            File tFile = new File(tDir, newPath);
            if (tFile.exists()) {
                tFile.deleteOnExit();
            }
            // 直接copy过去
            try {
                FileUtils.forceMkdirParent(tFile);
                FileUtils.copyFileToDirectory(bfcFile, tFile.getParentFile());
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void equipRfcWithChangeInfo(Revision rfc) {
        // method can just use to rfc revision
        assert (rfc.getName().equals("rfc"));
        List<DiffEntry> diffEntries = GitUtils.getDiffEntriesBetweenCommits(rfc.getLocalCodeDir(), rfc.getCommitID(), rfc.getCommitID()+"~1");
        assert (diffEntries != null);
        diffEntries.forEach(diffEntry -> {
            addChangedFileToRfc(diffEntry,rfc);
        });
    }


    private void addChangedFileToRfc(DiffEntry entry, Revision rfc) {
        ChangedFile file =null;
        String path = entry.getNewPath();
        if (path.contains("test") && path.endsWith(".java")) {
            String testCode = null;
            try {
                testCode = FileUtils.readFileToString(new File(rfc.getLocalCodeDir(), path), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (testCode.contains("@Test") || testCode.contains("junit")) {
                file = new TestFile(path);
            } else {
                file = new TestRelatedFile(path);
            }
        }

//      if not end with ".java",it may be source file
        if (!path.endsWith(".java") && !path.endsWith("pom.xml") && !path.contains("gradle")) {
            file = new SourceFile(path);
        }
        if (file!=null){
            file.setOldPath(entry.getOldPath());
            rfc.getChangedFiles().add(file);
        }
    }
}
