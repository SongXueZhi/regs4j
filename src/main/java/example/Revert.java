package example;

import core.Migrator;
import core.MysqlManager;
import core.Reducer;
import core.SourceCodeManager;
import core.coverage.model.CoverNode;
import core.git.GitUtils;
import model.HunkEntity;
import model.Regression;
import model.Revision;
import org.apache.commons.io.FileUtils;
import run.Executor;
import run.Runner;
import utils.FileUtilx;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static utils.FileUtilx.readSetFromFile;

public class Revert {


    static Reducer reducer = new Reducer();
    static Migrator migrator = new Migrator();
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
        for (int i = 0; i < regressionList.size(); i++) {
            Regression regressionTest = regressionList.get(i);
            System.out.println(regressionTest.getId());

            Revision rfc = regressionTest.getRfc();
            File rfcDir = sourceCodeManager.checkout(regressionTest.getId(), rfc, projectDir, projectName);
            rfc.setLocalCodeDir(rfcDir);

            Revision ric = regressionTest.getRic();
            File ricDir = sourceCodeManager.checkout(regressionTest.getId(), ric, projectDir, projectName);
            ric.setLocalCodeDir(ricDir);

            Revision work = regressionTest.getWork();
            File workDir = sourceCodeManager.checkout(regressionTest.getId(), work, projectDir, projectName);
            work.setLocalCodeDir(workDir);

            List<Revision> needToTestMigrateRevisionList = Arrays.asList(ric, work);
            migrateTestAndDependency(rfc, needToTestMigrateRevisionList, regressionTest.getTestCase());

//            //2.create symbolicLink for good and bad
//            sourceCodeManager.symbolicLink(regression.getId(),projectFullName, ric, work);

            //3.create sh(build.sh&test.sh)
            sourceCodeManager.createShell(regressionTest.getId(), projectName, ric, regressionTest.getTestCase(),
                    regressionTest.getErrorType());
            sourceCodeManager.createShell(regressionTest.getId(), projectName, work, regressionTest.getTestCase(),
                    regressionTest.getErrorType());

            List<HunkEntity> hunks = GitUtils.getHunksBetweenCommits(ricDir, ric.getCommitID(), work.getCommitID());

            String path = ric.getLocalCodeDir().toString().replace("_ric","_tmp");
            FileUtilx.copyDirToTarget(ric.getLocalCodeDir().toString(),path);
            revert(path,hunks);
            Executor executor = new Executor();
            executor.setDirectory(new File(path));
            String result = executor.exec("./build.sh; ./test.sh");
            System.out.println("Revert result: " + result);
        }
    }


    public static void revert(String path, List<HunkEntity> hunkEntities){
        try{
            Map<String,List<HunkEntity>> stringListMap = hunkEntities.stream().collect(Collectors.groupingBy(HunkEntity::getNewPath));
            for (Map.Entry<String,List<HunkEntity>> entry: stringListMap.entrySet()){
                revertFile(path,entry.getValue());
            }
        }catch (Exception exception){
            exception.printStackTrace();
        }
    }

    /**
     * 处理一个文件的revert
     * @param tmpPath 临时项目的全路径，后缀是"_tmp"
     * @param hunkEntities 需要退回的hunk
     */
    public static List<String> revertFile(String tmpPath, List<HunkEntity> hunkEntities){
        HunkEntity tmpHunk = hunkEntities.get(0);
        if(!Objects.equals(tmpHunk.getNewPath(), tmpHunk.getOldPath())){
            String fileFullOldPath = tmpPath.replace("_tmp","_work") + File.separator +tmpHunk.getOldPath();
            String fileFullNewPath = tmpPath + File.separator +tmpHunk.getOldPath();
            FileUtilx.copyFileToTarget(fileFullOldPath,fileFullNewPath);
        }
        List<String> line = FileUtilx.readListFromFile(tmpPath + File.separator + tmpHunk.getNewPath());
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
                    List<String> newLine = getLinesFromWorkVersion(tmpPath.replace("_tmp","_work"),hunkEntity);
                    line.addAll(hunkEntity.getBeginB(),newLine);
                    break;
                case INSERT:
                    line.subList(hunkEntity.getBeginB(), hunkEntity.getEndB()).clear();
                    break;
                case REPLACE:
                    line.subList(hunkEntity.getBeginB(), hunkEntity.getEndB()).clear();
                    List<String> replaceLine = getLinesFromWorkVersion(tmpPath.replace("_tmp","_work"),hunkEntity);
                    line.addAll(hunkEntity.getBeginB(),replaceLine);
                    break;
                case EMPTY:
                    break;
            }
        }

        //oldPath是一个空文件的情况
        if(!Objects.equals(tmpHunk.getOldPath(), "/dev/null")){
            FileUtilx.writeListToFile(tmpPath + File.separator +tmpHunk.getOldPath(),line);
        }
        return line;
    }

    public static List<String> getLinesFromWorkVersion(String workPath, HunkEntity hunk){
        List<String> result = new ArrayList<>();
        List<String> line = FileUtilx.readListFromFile(workPath + File.separator + hunk.getOldPath());
        result = line.subList(hunk.getBeginA(), hunk.getEndA());
        return result;
    }

    static void migrateTestAndDependency(Revision rfc, List<Revision> needToTestMigrateRevisionList, String testCase) {
        migrator.equipRfcWithChangeInfo(rfc);
        reducer.reduceTestCases(rfc, testCase);
        needToTestMigrateRevisionList.forEach(revision -> {
            migrator.migrateTestFromTo_0(rfc, revision);
        });
    }
}
