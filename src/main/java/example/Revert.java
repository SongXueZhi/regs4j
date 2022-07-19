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
        Regression regressionTest = regressionList.get(2);

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
                revertFile(tmpPath,entry.getValue()).size();
            }
        }catch (Exception exception){
            exception.printStackTrace();
        }
    }

    /**
     * 处理一个文件的revert
     * @param tmpPath 临时项目的全路径，后缀是"_ric_tmp"
     * @param hunkEntities 需要退回的hunk
     */
    public static List<String> revertFile(String tmpPath, List<HunkEntity> hunkEntities){
        HunkEntity tmpHunk = hunkEntities.get(0);
        if(!Objects.equals(tmpHunk.getNewPath(), tmpHunk.getOldPath())){
            String fileFullOldPath = tmpPath.replace("_ric_tmp","_work") + File.separator +tmpHunk.getOldPath();
            String fileFullNewPath = tmpPath + File.separator +tmpHunk.getOldPath();
            FileUtilx.copyFileToTarget(fileFullOldPath,fileFullNewPath);
        }
        List<String> line = FileUtilx.readListFromFile(tmpPath + File.separator + tmpHunk.getNewPath());
        System.out.println("old: " + line.size());
        hunkEntities.sort(new Comparator<HunkEntity>() {
            @Override
            public int compare(HunkEntity p1, HunkEntity p2) {
                return p2.getBeginA() - p1.getBeginA();
            }
        });

        for(HunkEntity hunkEntity: hunkEntities){
            HunkEntity.HunkType type = hunkEntity.getType();
            switch (type){
                case DELETE:
                    List<String> newLine = getLinesFromWorkVersion(tmpPath.replace("_ric_tmp","_work"),hunkEntity);
                    line.addAll(hunkEntity.getBeginB(),newLine);
                    break;
                case INSERT:
                    line.subList(hunkEntity.getBeginB(), hunkEntity.getEndB()).clear();
                    break;
                case REPLACE:
                    line.subList(hunkEntity.getBeginB(), hunkEntity.getEndB()).clear();
                    List<String> replaceLine = getLinesFromWorkVersion(tmpPath.replace("_ric_tmp","_work"),hunkEntity);
                    line.addAll(hunkEntity.getBeginB(),replaceLine);
                    break;
                case EMPTY:
                    break;
            }
        }
        System.out.println("revert: " + line.size());
        return line;
    }

    public static List<String> getLinesFromWorkVersion(String workPath, HunkEntity hunk){
        List<String> result = new ArrayList<>();
        List<String> line = FileUtilx.readListFromFile(workPath + File.separator + hunk.getOldPath());
        result = line.subList(hunk.getBeginA(), hunk.getEndA());
        return result;
    }
}
