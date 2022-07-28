package core;

import com.google.protobuf.Value;
import core.git.GitUtils;
import example.Revert;
import model.HunkEntity;
import model.Regression;
import model.Revision;
import run.Executor;
import utils.FileUtilx;

import java.io.File;
import java.util.*;

import static java.lang.Math.*;
import static utils.FileUtilx.readSetFromFile;

public class ProbDD {

    static Reducer reducer = new Reducer();
    static Migrator migrator = new Migrator();
    static SourceCodeManager sourceCodeManager = new SourceCodeManager();
    static String projectName = (String) readSetFromFile("projects.txt").toArray()[0];

    public static void main(String [] args){

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
            String regressionId =  regressionTest.getId();
            System.out.println(regressionId);

            Revision rfc = regressionTest.getRfc();
            File rfcDir = sourceCodeManager.checkout(regressionId, rfc, projectDir, projectName);
            rfc.setLocalCodeDir(rfcDir);

            Revision ric = regressionTest.getRic();
            File ricDir = sourceCodeManager.checkout(regressionId, ric, projectDir, projectName);
            ric.setLocalCodeDir(ricDir);

            Revision work = regressionTest.getWork();
            File workDir = sourceCodeManager.checkout(regressionId, work, projectDir, projectName);
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
            long startTime = System.currentTimeMillis();
            List<HunkEntity> failHunk = ddmin(ric.getLocalCodeDir().toString(),hunks);
            long endTime = System.currentTimeMillis();
            long usedTime = (endTime-startTime)/1000;
            System.out.println("用时: " + usedTime + "s");
            System.out.println("得到hunk数量：" + failHunk.size() + ":" +failHunk);
            verification(ric.getLocalCodeDir().toString(),failHunk );

        }
    }

    //传入的path是ric的全路径
    public static List<HunkEntity> ProbDD(String path,List<HunkEntity> hunkEntities){
        hunkEntities.removeIf(hunkEntity -> hunkEntity.getNewPath().contains("test"));
        List<String> relatedFile =  getRelatedFile(hunkEntities);
        System.out.println("原hunk的数量是: " + hunkEntities.size());
        int time = 0;
        String tmpPath = path.replace("_ric","_tmp");
        FileUtilx.copyDirToTarget(path,tmpPath);
        assert Objects.equals(codeReduceTest(tmpPath, hunkEntities), "PASS");
        List<HunkEntity> retseq = hunkEntities;
        List<Integer> retIdx = new ArrayList<>();
        List<Double> p = new ArrayList<>();
        for(int i = 0; i < hunkEntities.size(); i++){
            retIdx.add(i);
            p.add(0.1);
        }
        while (!testDone(p)){
            List<Integer> delIdx = sample(p);
            if(delIdx.size() == 0){
                break;
            }
            time = time + 1;
            List<Integer> idx2test = getIdx2test(retIdx,delIdx);
            List<HunkEntity> seq2test = new ArrayList<>();
            for (int idxelm: idx2test){
                seq2test.add(hunkEntities.get(idxelm));
            }
            FileUtilx.copyDirToTarget(path,tmpPath + time);
//            copyRelatedFile(path,tmpPath,relatedFile);
            System.out.print(time);
            if(Objects.equals(codeReduceTest(tmpPath + time,seq2test), "PASS")){
                for(int set0 = 0; set0 < p.size(); set0++){
                    if(!idx2test.contains(set0)){
                        p.set(set0,0.0);
                    }
                }
                retseq = seq2test;
                retIdx = idx2test;
            }else {
                for(int setd = 0; setd < p.size(); setd++){
                    if(delIdx.contains(setd) && (p.get(setd) != 0) && (p.get(setd) != 1)){
                        double delta = (computRatio(delIdx,p) - 1) * p.get(setd);
                        p.set(setd,p.get(setd) + delta);
                    }
                }
            }
        }
        System.out.println("循环次数: " + time);
        return retseq;
    }

    public static List<HunkEntity> ddmin(String path, List<HunkEntity> hunkEntities){
        hunkEntities.removeIf(hunkEntity -> hunkEntity.getNewPath().contains("test"));
        List<String> relatedFile =  getRelatedFile(hunkEntities);
        System.out.println("原hunk的数量是: " + hunkEntities.size());
        int time = 0;
        String tmpPath = path.replace("_ric","_tmp");
        FileUtilx.copyDirToTarget(path,tmpPath);
        assert Objects.equals(codeReduceTest(tmpPath, hunkEntities), "PASS");
        int n = 2;

        while(hunkEntities.size() >= 2){
            int start = 0;
            int subset_length = hunkEntities.size() / n;
            boolean some_complement_is_failing = false;
            while (start < hunkEntities.size()){
                time = time + 1;
                List<HunkEntity> complement = new ArrayList<>();
                for(int i = 0; i < hunkEntities.size();i++ ){
                    if(i < start || i >= start + subset_length) {
                        complement.add(hunkEntities.get(i));
                    }
                }
                FileUtilx.copyDirToTarget(path,tmpPath + time);
                System.out.print(time);
                if (Objects.equals(codeReduceTest(tmpPath + time,complement), "PASS")){
                    hunkEntities = complement;
                    n = max(n - 1, 2);
                    some_complement_is_failing = true;
                    break;
                }
                start += subset_length;
            }
            if(!some_complement_is_failing){
                if (n == hunkEntities.size()){
                    break;
                }
                n = min(n * 2, hunkEntities.size());
            }
        }
        System.out.println("循环次数: " + time);
        return hunkEntities;
    }

    public static List<HunkEntity> removeTestFile(List<HunkEntity> hunkEntities){
        hunkEntities.removeIf(hunkEntity -> hunkEntity.getNewPath().contains("test"));
        return hunkEntities;
    }

    public static List<String> getRelatedFile(List<HunkEntity> hunkEntities){
        List<String> filePath = new ArrayList<>();
        for (HunkEntity hunk: hunkEntities) {
            filePath.add(hunk.getNewPath());
        }
        return filePath;
    }

    public static void copyRelatedFile(String path, String tmpPath, List<String> relatedFile){
        for (String file: relatedFile) {
            FileUtilx.copyFileToTarget(path + File.separator + file,tmpPath + File.separator + file);
        }
    }

    public static String codeReduceTest(String path, List<HunkEntity> hunkEntities){
        Revert.revert(path,hunkEntities);
        Executor executor = new Executor();
        executor.setDirectory(new File(path));
        String result = executor.exec("./build.sh; ./test.sh").replaceAll("\n","");
        System.out.println(result + ": revert: " + hunkEntities);
        return result;
    }

    public static List<Integer> getIdx2test(List<Integer> inp1, List<Integer> inp2){
        List<Integer> result = new ArrayList<>();
        for(Integer elm: inp1){
            if(!inp2.contains(elm)){
                result.add(elm);
            }
        }
        return result;
    }

    public static double computRatio(List<Integer> deleteconfig, List<Double> p){
        double res = 0;
        double tmplog = 1;
        for(int delc: deleteconfig){
            if(p.get(delc) > 0 && p.get(delc) < 1){
                tmplog *= (1 - p.get(delc));
            }
        }
        res = 1 / (1 - tmplog);
        return res;
    }

    //返回的是从小到大的索引值List
    //相当于python中的argsort()
    private static List<Integer> sortToIndex(List<Double> p){
        List<Integer> idxlist = new ArrayList<>();
        Map<Integer,Double> pidxMap = new HashMap<>();
        for(int j = 0; j < p.size();j ++){
            pidxMap.put(j,p.get(j));
        }
        List<Map.Entry<Integer,Double>> entrys=new ArrayList<>(pidxMap.entrySet());
        entrys.sort(new Comparator<Map.Entry>() {
            public int compare(Map.Entry o1, Map.Entry o2) {
                return (int) ((double) o1.getValue()*100000 - (double) o2.getValue()*100000);
            }
        });
        for(Map.Entry<Integer,Double> entry:entrys){
            idxlist.add(entry.getKey());
        }
        return idxlist;
    }

    public static List<Integer> sample(List<Double> p){
        List<Integer> delset = new ArrayList<>();
        List<Integer> idxlist = sortToIndex(p);
        int k = 0;
        double tmp = 1;
        double last = -9999;
        int i = 0;
        while (i < p.size()){
            if (p.get(idxlist.get(i)) == 0) {
                k = k + 1;
                i = i + 1;
                continue;
            }
            if(!(p.get(idxlist.get(i))<1)){
                break;
            }
            for(int j = k; j < i+1; j++){
                tmp *= (1-p.get(idxlist.get(j)));
            }
            tmp *= (i - k + 1);
            if(tmp < last){
                break;
            }
            last = tmp;
            tmp = 1;
            i = i + 1;
        }
        while (i > k){
            i = i - 1;
            delset.add(idxlist.get(i));
        }

        return delset;
    }

    public static boolean testDone(List<Double> p){
        for( double prob :  p){
            if(abs(prob-1.0)>1e-6 && min(prob,1)<1.0){
                return false;
            }
        }
        return true;
    }

    public static void verification(String path, List<HunkEntity> failHunk){
        String tmpPath = path.replace("_ric","_tmp");
        FileUtilx.copyDirToTarget(path,tmpPath);
        System.out.println("验证正确性：" + codeReduceTest(tmpPath,failHunk));
    }

    static void migrateTestAndDependency(Revision rfc, List<Revision> needToTestMigrateRevisionList, String testCase) {
        migrator.equipRfcWithChangeInfo(rfc);
        reducer.reduceTestCases(rfc, testCase);
        needToTestMigrateRevisionList.forEach(revision -> {
            migrator.migrateTestFromTo_0(rfc, revision);
        });
    }



}
