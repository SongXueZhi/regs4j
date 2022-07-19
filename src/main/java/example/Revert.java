package example;

import core.MysqlManager;
import core.SourceCodeManager;
import core.git.GitUtils;
import model.HunkEntity;
import model.Regression;
import model.Revision;
import org.apache.commons.io.FileUtils;
import utils.FileUtilx;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static utils.FileUtilx.readSetFromFile;

public class Revert {

    static SourceCodeManager sourceCodeManager = new SourceCodeManager();

    public static void main(String[] args) {
        String projectName = (String) FileUtilx.readSetFromFile("projects.txt").toArray()[0];

        File projectDir = sourceCodeManager.getProjectDir(projectName);
        String sql = "select regression_uuid,bfc,buggy,bic,work," +
                "testcase," +
                "regression.project_full_name,results.error_type from regression\n" +
                "inner join results\n" +
                "on regression.bfc = results.rfc_id\n" +
                "where results.error_type is not null and regression.project_full_name ='" + projectName +
                "'";
        List<Regression> regressionList = MysqlManager.getRegressions(sql);

        Set<String> uuid = readSetFromFile("uuid.txt");
        regressionList.removeIf(regression -> !uuid.contains(regression.getId()));
        Regression regressionTest = regressionList.get(0);

        Revision rfc = regressionTest.getRfc();
        File rfcDir = sourceCodeManager.checkout(regressionTest.getId(), rfc, projectDir, projectName);
        rfc.setLocalCodeDir(rfcDir);

        Revision ric = regressionTest.getRic();
        File ricDir = sourceCodeManager.checkout(regressionTest.getId(),ric, projectDir, projectName);
        ric.setLocalCodeDir(ricDir);

        Revision work = regressionTest.getWork();
        File workDir = sourceCodeManager.checkout(regressionTest.getId(),work, projectDir, projectName);
        work.setLocalCodeDir(workDir);

//        List<Revision> needToTestMigrateRevisionList = Arrays.asList(ric, work);
//        migrateTestAndDependency(rfc, needToTestMigrateRevisionList, regression.getTestCase());

//            //2.create symbolicLink for good and bad
//            sourceCodeManager.symbolicLink(regression.getId(),projectFullName, ric, work);

        //3.create sh(build.sh&test.sh)
        sourceCodeManager.createShell(regressionTest.getId(), projectName, ric, regressionTest.getTestCase(),
                regressionTest.getErrorType());
        sourceCodeManager.createShell(regressionTest.getId(), projectName, work, regressionTest.getTestCase(),
                regressionTest.getErrorType());

        List<HunkEntity> hunks = GitUtils.getHunksBetweenCommits(ricDir, ric.getCommitID(), work.getCommitID());
        //System.out.println(hunks);

        revert(ric,hunks);
    }

    public static void revert(Revision ric, List<HunkEntity> hunkEntities) {
        try {
            String ricName = ric.getLocalCodeDir().getName();
            String ricPath = ric.getLocalCodeDir().getParent();
            String tmpName = ricName + "_tmp";
            String tmpPath = ricPath + File.separator + tmpName;

            File tmpFile = new File(tmpPath);
            if (tmpFile.exists()) {
                tmpFile.deleteOnExit();
            }
            FileUtils.forceMkdirParent(tmpFile);
            FileUtils.copyDirectory(ric.getLocalCodeDir(), tmpFile);

            Map<String,List<HunkEntity>> stringListMap = hunkEntities.stream().collect(Collectors.groupingBy(HunkEntity::getNewPath));

            for (Map.Entry<String,List<HunkEntity>> entry: stringListMap.entrySet()){
                System.out.println(entry.getKey() + entry.getValue());
                revertFile(tmpPath,entry.getKey(),entry.getValue());
            }
        }catch (Exception exception){
            exception.printStackTrace();
        }
    }

//    public final Edit.Type getType() {
//        if (beginA < endA) {
//            if (beginB < endB) {
//                return Edit.Type.REPLACE;
//            }
//            return Edit.Type.DELETE;
//
//        }
//        if (beginB < endB) {
//            return Edit.Type.INSERT;
//        }
//        // beginB == endB)
//        return Edit.Type.EMPTY;
//    }

    public static void revertFile(String srcPath, String filePath, List<HunkEntity> hunkEntities){
        List<String> line = FileUtilx.readListFromFile(filePath);
        List<Integer> index = new ArrayList<>(line.size());
        for(HunkEntity hunkEntity: hunkEntities){
            if(!Objects.equals(hunkEntity.getNewPath(), hunkEntity.getOldPath())){
                String fileFullOldPath = srcPath + File.separator +hunkEntity.getOldPath();
                String fileFullNewPath = srcPath + File.separator +hunkEntity.getNewPath();
                if(!FileUtilx.moveFileToTarget(fileFullOldPath,fileFullNewPath)){
                    System.out.println("文件位置变化且移动失败");
                }
            }
            HunkEntity.HunkType type = hunkEntity.getType();
            switch (type){
                case DELETE:

                    break;
                case INSERT:
                    break;
                case REPLACE:
                    break;
                case EMPTY:
                    break;
            }
        }
    }
}
