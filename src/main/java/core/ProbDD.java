package core;

import core.git.GitUtils;
import example.Revert;
import model.DDOutput;
import model.HunkEntity;
import model.Regression;
import model.Revision;
import org.apache.commons.io.FileUtils;
import run.Executor;
import utils.FileUtilx;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.Math.*;
import static utils.FileUtilx.readListFromFile;
import static utils.FileUtilx.readSetFromFile;

public class ProbDD extends DD {

    static Reducer reducer = new Reducer();
    static Migrator migrator = new Migrator();
    static SourceCodeManager sourceCodeManager = new SourceCodeManager();
    static String projectName = (String) readSetFromFile("projects.txt").toArray()[0];

    static Map<String,Map<String,Integer>> cc = new HashMap<>();
    Map<String,Map<String,Integer>> loop = new HashMap<>();

    public ProbDD() throws IOException {
    }

//    public static void main(String [] args) throws Exception {
//
//        File projectDir = sourceCodeManager.getProjectDir(projectName);
//        String sql = "select regression_uuid,bfc,buggy,bic,work," +
//                "testcase,project_full_name from regression\n" +
//                "where project_full_name ='" + projectName +
////                "regression.project_full_name,results.error_type from regression\n" +
////                "inner join results\n" +
////                "on regression.bfc = results.rfc_id\n" +
////                "where results.error_type is not null and regression.project_full_name ='" + projectName +
//                "'";
//        List<Regression> regressionList = MysqlManager.getRegressionsWithoutError(sql);
//
//        List<String> uuid = readListFromFile("uuid.txt");
//        regressionList.removeIf(regression -> !uuid.contains(regression.getId()));
//
//        for (int i = 0; i < regressionList.size(); i++) {
//            Regression regressionTest = regressionList.get(i);
//            String regressionId =  regressionTest.getId();
//            String path = regressionId +  new SimpleDateFormat("_yyyyMMddHHmmss").format(new Date());
//            FileOutputStream puts = new FileOutputStream(path,true);
//            PrintStream out = new PrintStream(puts);
//            System.setOut(out);
//            bw.append("\n" + regressionId);
//            System.out.println("\n" + regressionId);
//
//            Revision rfc = regressionTest.getRfc();
//            File rfcDir = sourceCodeManager.checkout(regressionId, rfc, projectDir, projectName);
//            rfc.setLocalCodeDir(rfcDir);
//
//            Revision ric = regressionTest.getRic();
//            File ricDir = sourceCodeManager.checkout(regressionId, ric, projectDir, projectName);
//            ric.setLocalCodeDir(ricDir);
//
//            Revision work = regressionTest.getWork();
//            File workDir = sourceCodeManager.checkout(regressionId, work, projectDir, projectName);
//            work.setLocalCodeDir(workDir);
//
//            Revision buggy = regressionTest.getBuggy();
//            File buggyDir = sourceCodeManager.checkout(regressionId, buggy, projectDir, projectName);
//            buggy.setLocalCodeDir(buggyDir);
//
//            List<Revision> needToTestMigrateRevisionList = Arrays.asList(ric, work, buggy);
//            migrateTestAndDependency(rfc, needToTestMigrateRevisionList, regressionTest.getTestCase());
//
////            //2.create symbolicLink for good and bad
////            sourceCodeManager.symbolicLink(regression.getId(),projectFullName, ric, work);
//
//            //3.create sh(build.sh&test.sh)
//            sourceCodeManager.createShell(regressionTest.getId(), projectName, ric, regressionTest.getTestCase());
//            sourceCodeManager.createShell(regressionTest.getId(), projectName, work, regressionTest.getTestCase());
//            sourceCodeManager.createShell(regressionTest.getId(), projectName, rfc, regressionTest.getTestCase());
//            sourceCodeManager.createShell(regressionTest.getId(), projectName, buggy, regressionTest.getTestCase());
//
//            List<HunkEntity> hunks = GitUtils.getHunksBetweenCommits(ricDir, ric.getCommitID(), work.getCommitID());
//            hunks.removeIf(hunkEntity -> hunkEntity.getNewPath().contains("test"));
//            hunks.removeIf(hunkEntity -> hunkEntity.getOldPath().contains("test"));
//            List<String> relatedFile =  getRelatedFile(hunks);
//            System.out.println("原hunk的数量是: " + hunks.size() + ":" + hunks);
//            bw.append("\n原hunk的数量是: " + hunks.size());
//            bw.append("\n" + hunks + "\n");
//
//            long start = System.currentTimeMillis();
//            long end = System.currentTimeMillis();
//            System.out.println(String.format("Total Time：%d ms", end - start));
////            DDOutput ccHunks1 = ddmin(ric.getLocalCodeDir().toString(),hunks);
////            DDOutput ccHunks2 = ProbDD(ric.getLocalCodeDir().toString(),hunks);
//            //cc.put(regressionId,new HashMap<>(){{put("ddmin", ccHunks1.size());}});
//            //MysqlManager.insertCC("bic", regressionId, "ProbDD", ccHunks);
//        }
//        bw.close();
//
//    }

    public DDOutput ddmin(String path, List<HunkEntity> hunkEntities) throws IOException {
        int hunkSize = hunkEntities.size();
        long start = System.currentTimeMillis();
        HashMap<HunkEntity, Integer> hunkMap = new HashMap<>();
        for (HunkEntity hunk: hunkEntities) {
            hunkMap.put(hunk, hunkEntities.indexOf(hunk));
        }
        List<String> relatedFile =  getRelatedFile(hunkEntities);
        bw.append("\n -------开始ddmin---------");
        System.out.println("\n -------开始ddmin---------");
        int time = 0;
        String tmpPath = path.substring(0, path.lastIndexOf('_') + 1) + "tmp";
//        String tmpPath = path.replace("_ric","_tmp");
        FileUtilx.copyDirToTarget(path,tmpPath);
        assert Objects.equals(codeReduceTest(tmpPath, hunkEntities), "PASS");
        int n = 2;
        boolean isTimeout = false;

        while(hunkEntities.size() >= 2 ){
            if((System.currentTimeMillis() - start) / 1000 > 7200){
                isTimeout = true;
                break;
            }
            int location = 0;
            int subset_length = hunkEntities.size() / n;
            boolean some_complement_is_failing = false;
            while (location < hunkEntities.size()){
                if((System.currentTimeMillis() - start) / 1000 > 7200){
                    isTimeout = true;
                    break;
                }
                time = time + 1;
                List<HunkEntity> complement = new ArrayList<>();
                for(int i = 0; i < hunkEntities.size();i++ ){
                    if(i < location || i >= location + subset_length) {
                        complement.add(hunkEntities.get(i));
                    }
                }
                List<Integer> index = new ArrayList<>();
                for (HunkEntity com: complement) {
                    index.add(hunkMap.get(com));
                }
                FileUtilx.copyDirToTarget(path,tmpPath);
                String result = codeReduceTest(tmpPath, complement);
                bw.append( "\n" + time + " " + result + ": revert: " + index);
                //bw.append("\n" + complement);
                System.out.println(time + " " + result + ": revert: " + index);

                if (Objects.equals(result, "PASS")){
                    hunkEntities = complement;
                    n = max(n - 1, 2);
                    some_complement_is_failing = true;
                    break;
                }
                location += subset_length;
            }
            if(!some_complement_is_failing){
                if (n == hunkEntities.size()){
                    break;
                }
                n = min(n * 2, hunkEntities.size());
            }
        }
        long end = System.currentTimeMillis();
        if(isTimeout){
            bw.append("\n运行超时");
            System.out.println("运行超时");
            return new DDOutput(hunkSize, time, (end - start) / 1000, new ArrayList<HunkEntity>());
        }
        System.out.printf("Total Time：%d ms%n", end - start);
        System.out.println("循环次数: " + time);
        System.out.println("得到hunk数量：" + hunkEntities.size() + ":" +hunkEntities);
        bw.append(String.format("\nTotal Time：%d ms", end - start));
        bw.append("\n循环次数: " + time);
        bw.append("\n得到hunk数量：" + hunkEntities.size() + ":" + hunkEntities + "\n");
        FileUtils.deleteDirectory(new File(tmpPath));
        return new DDOutput(hunkSize, time, (end - start) / 1000, hunkEntities);
    }

    //传入的path是ric的全路径
    public DDOutput ProbDD(String path,List<HunkEntity> hunkEntities) throws IOException {
        int hunkSize = hunkEntities.size();
        long start = System.currentTimeMillis();
        bw.append("\n -------开始ProbDD---------");
        System.out.println("\n -------开始ProbDD---------");
        int time = 0;
        String tmpPath = path.substring(0, path.lastIndexOf('_') + 1) + "tmp";
        FileUtilx.copyDirToTarget(path,tmpPath);
        assert Objects.equals(codeReduceTest(tmpPath, hunkEntities), "PASS");
        List<HunkEntity> retseq = hunkEntities;
        List<Integer> retIdx = new ArrayList<>();
        List<Double> p = new ArrayList<>();
        for(int i = 0; i < hunkEntities.size(); i++){
            retIdx.add(i);
            p.add(0.1);
        }
        boolean isTimeout = false;
        while (!testDone(p) ){
            if((System.currentTimeMillis() - start) / 1000 > 7200){
                isTimeout = true;
                break;
            }
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
//            copyRelatedFile(path,tmpPath,relatedFile);
            FileUtilx.copyDirToTarget(path,tmpPath);

            String result = codeReduceTest(tmpPath, seq2test);;
            bw.append( "\n" + time + " " + result + ": revert: " + idx2test);
            System.out.println(time + " " + result + ": revert: " + idx2test);
            if(Objects.equals(result, "PASS")){
                for(int set0 = 0; set0 < p.size(); set0++){
                    if(!idx2test.contains(set0)){
                        p.set(set0,0.0);
                    }
                }
                retseq = seq2test;
                retIdx = idx2test;
            }else {
                List<Double> pTmp = new ArrayList<>(p);
                for(int setd = 0; setd < p.size(); setd++){
                    if(delIdx.contains(setd) && (p.get(setd) != 0) && (p.get(setd) != 1)){
                        double delta = (computRatio(delIdx,pTmp) - 1) * pTmp.get(setd);
                        p.set(setd,pTmp.get(setd) + delta);
                    }
                }
            }
            bw.append("\np: " + p);
        }

        long end = System.currentTimeMillis();
        if(isTimeout){
            bw.append("\n运行超时");
            System.out.println("运行超时");
            return new DDOutput(hunkSize, time, (end - start) / 1000, new ArrayList<HunkEntity>());
        }
        System.out.printf("Total Time：%d ms%n", end - start);
        bw.append(String.format("%nTotal Time：%d ms", end - start));
        System.out.println("循环次数: " + time);
        bw.append("\n循环次数: " + time);
        System.out.println("得到hunk数量：" + retseq.size() + ":" +retseq);
        bw.append("\n得到hunk数量：" + retseq.size() + ":" + retseq + "\n");
        FileUtils.deleteDirectory(new File(tmpPath));
        return new DDOutput(hunkSize, time, (end - start) / 1000, retseq);
    }

//    public static List<HunkEntity> ProbDDplus(String path,List<HunkEntity> hunkEntities) throws IOException {
//        long start = System.currentTimeMillis();
//        Map<List<Integer>, String> record = new HashMap<>();
//        bw.append("\n -------开始ProbDDplus---------");
//        System.out.println("\n -------开始ProbDDplus---------");
//
//        String tmpPath = path.replace("_ric","_tmp");
//        FileUtilx.copyDirToTarget(path,tmpPath);
//        assert Objects.equals(codeReduceTest(tmpPath, hunkEntities), "PASS");
//        List<HunkEntity> retseq = hunkEntities;
//        List<Integer> retIdx = new ArrayList<>();
//        List<Double> cPro = new ArrayList<>();
//        List<Double> dPro = new ArrayList<>();
//        for(int i = 0; i < hunkEntities.size(); i++){
//            retIdx.add(i);
//            cPro.add(0.1);
//            dPro.add(0.1);
//        }
//
//        double dRate = 0.1;
//        int loop = 0;
//        List<Integer> delIdx = sample(cPro);
//        List<Integer> idx2test = getIdx2test(retIdx, delIdx);
//        //while (!testDone(cPro) && loop < hunkEntities.size() * Math.log(hunkEntities.size())){
//        while (!testDone(cPro) && loop < Math.pow(hunkEntities.size(), 2)){
//            loop++;
//            List<HunkEntity> seq2test = new ArrayList<>();
//            for (int idxelm: idx2test){
//                seq2test.add(hunkEntities.get(idxelm));
//            }
//            String result = null;
//            Collections.sort(idx2test);
//            if(record.containsKey(idx2test)){
//                result = record.get(idx2test);
//            }else {
//                FileUtilx.copyDirToTarget(path,tmpPath);
//                result = codeReduceTest(tmpPath, seq2test);
//                record.put(idx2test,result);
//            }
//            bw.append( "\n" + loop + " " + result + ": revert: " + idx2test);
//            bw.append("\n" + seq2test);
//            System.out.println(loop + " " + result + ": revert: " + idx2test);
//
//            if(Objects.equals(result, "PASS")){
//                //PASS: cPro=0 dPro=0
//                for(int set0 = 0; set0 < cPro.size() && set0 < dPro.size(); set0++){
//                    if(!idx2test.contains(set0)){
//                        cPro.set(set0,0.0);
//                        dPro.set(set0,0.0);
//                    }
//                }
//                retseq = seq2test;
//                retIdx = idx2test;
//                delIdx = sample(cPro);
//                idx2test = getIdx2test(retIdx,delIdx);
//            }else if(Objects.equals(result, "FAIL")){
//                //FAIL: d_pro-- c_pro++
//                List<Double> cProTmp = new ArrayList<>(cPro);
//                List<Double> dProTmp = new ArrayList<>(dPro);
//                double cRadio = computRatio(delIdx, cProTmp) - 1.0;
//                double dDelta = dRate * idx2test.size() / delIdx.size();
//                for(int setd = 0; setd < cPro.size(); setd++){
//                    if(delIdx.contains(setd) && (cPro.get(setd) != 0) && (cPro.get(setd) != 1)){
//                        double delta = cRadio * cProTmp.get(setd);
//                        cPro.set(setd,min(cProTmp.get(setd) + delta, 1.0));
//                    }
//                }
//                for (int setd = 0; setd < dPro.size(); setd++) {
//                    if (delIdx.contains(setd)) {
//                        dPro.set(setd, max(dProTmp.get(setd) - dDelta, 0.1));
//                    }
//                }
//                delIdx = sample(cPro);
//                idx2test = getIdx2test(retIdx,delIdx);
//            } else {
//                //CE: d_pro++
//                List<Double> dProTmp = new ArrayList<>(dPro);
//                double dDelta = dRate * idx2test.size() / delIdx.size();
//                for (int setd = 0; setd < dPro.size(); setd++) {
//                    if (delIdx.contains(setd)) {
//                        dPro.set(setd, dProTmp.get(setd) + dDelta);
//                    }
//                }
//                int selectSetSize = RandomUtils.nextInt(1, retIdx.size());
//                List<Double> avgPro = getAvgPro(cPro, dPro);
//                idx2test = select(avgPro, selectSetSize);
//                Collections.sort(idx2test);
//                delIdx = getIdx2test(retIdx, idx2test);
//            }
//            bw.append("\ncPro: " + cPro);
//            bw.append("\ndPro: " + dPro);
//
//            int test = 0;
//            Collections.sort(idx2test);
//            while (record.containsKey(idx2test) && record.get(idx2test).equals("CE")){
//                test++;
//                int selectSetSize = RandomUtils.nextInt(1, retIdx.size());
//                List<Double> avgPro = getAvgPro(cPro, dPro);
//                idx2test = select(avgPro, selectSetSize);
//                Collections.sort(idx2test);
//                //todo 如何确认不再尝试条件
//                if(test > 1000){
//                    idx2test = retIdx;
//                    break;
//                }
//            }
//            delIdx = getIdx2test(retIdx, idx2test);
//            if (delIdx.size() == 0) {
//                break;
//            }
//        }
//        long end = System.currentTimeMillis();
//        System.out.printf("Total Time：%d ms%n", end - start);
//        bw.append(String.format("%nTotal Time：%d ms", end - start));
//        System.out.println("循环次数: " + loop);
//        bw.append("\n循环次数: " + loop);
//        System.out.println("得到hunk数量：" + retseq.size() + ":" +retseq);
//        bw.append("\n得到hunk数量：" + retseq.size() + ":" + retseq + "\n");
//        return retseq;
//    }
//
//    public static List<HunkEntity> ProbDDplusM(String path,List<HunkEntity> hunkEntities) throws IOException {
//        long start = System.currentTimeMillis();
//        Map<List<Integer>, String> record = new HashMap<>();
//        bw.append("\n -------开始ProbDDplusM---------");
//        System.out.println("\n -------开始ProbDDplusM---------");
//
//        String tmpPath = path.replace("_ric","_tmp");
//        FileUtilx.copyDirToTarget(path,tmpPath);
//        assert Objects.equals(codeReduceTest(tmpPath, hunkEntities), "PASS");
//
//        List<Integer> CE = new ArrayList<>();
//        List<HunkEntity> retseq = hunkEntities;
//        List<Integer> retIdx = new ArrayList<>();
//        List<Double> cPro = new ArrayList<>();
//        Double[][] dPro = new Double[hunkEntities.size()][hunkEntities.size()];
//        for(int i = 0; i < hunkEntities.size(); i++){
//            retIdx.add(i);
//            cPro.add(0.1);
//            for(int j = 0; j < hunkEntities.size(); j++){
//                dPro[i][j] = 0.1;
//                if(i == j){
//                    dPro[i][j] = 0.0;
//                }
//            }
//        }
//        double cProSum = 0.0;
//        double dProSum = 0.0;
//        double lastcProSum = 0.0;
//        double lastdProSum = 0.0;
//
//        int loop = 0;
//        int stayPro = 0;
//
//        while (!testDone(cPro) && loop < Math.pow(hunkEntities.size(), 2)){
//            lastcProSum = cProSum;
//            lastdProSum = dProSum;
//            List<Integer> delIdx = sample(cPro);
//            if (delIdx.size() == 0) {
//                break;
//            }
//            List<Integer> idx2test = getIdx2test(retIdx, delIdx);
//            //使用增益公式带上一些可能的依赖
//            getProTestSet(idx2test, dPro, retIdx, cPro);
//            //testSet和CE完全一样，随机选择元素
////            if(CollectionUtils.isEqualCollection(idx2test, CE)){
////                int num = RandomUtils.nextInt(1, retIdx.size());
////                idx2test = select(cPro, num);
////            }
//            int test = 0;
//            Collections.sort(idx2test);
//            while (record.containsKey(idx2test) && record.get(idx2test).equals("CE")){
//                test++;
//                int selectSetSize = RandomUtils.nextInt(1, retIdx.size());
//                idx2test = select(cPro, selectSetSize);
//                Collections.sort(idx2test);
//                //todo 如何确认不再尝试条件
//                if(test > 1000){
//                    idx2test = retIdx;
//                    break;
//                }
//            }
//            delIdx = getIdx2test(retIdx, idx2test);
//            if (delIdx.size() == 0) {
//                break;
//            }
//            List<HunkEntity> seq2test = new ArrayList<>();
//            for (int idxelm: idx2test){
//                seq2test.add(hunkEntities.get(idxelm));
//            }
//
//            loop++;
//            String result = null;
//            Collections.sort(idx2test);
//            if(record.containsKey(idx2test)){
//                result = record.get(idx2test);
//            }else {
//                FileUtilx.copyDirToTarget(path,tmpPath);
//                result = codeReduceTest(tmpPath, seq2test);
//                record.put(idx2test,result);
//            }
//            bw.append( "\n" + loop + " " + result + ": revert: " + idx2test);
//            bw.append("\n" + seq2test);
//            System.out.println(loop + " " + result + ": revert: " + idx2test);
//            CE.clear();
//            if(Objects.equals(result, "PASS")){
//                //PASS: cPro=0 dPro=0
//                CE.add(-1);
//                for(int set0 = 0; set0 < cPro.size(); set0++){
//                    if(!idx2test.contains(set0)){
//                        cPro.set(set0,0.0);
//                        for(int i = 0; i < dPro.length; i++){
//                            dPro[i][set0] = 0.0;
//                            dPro[set0][i] = 0.0;
//                        }
//                    }
//                }
//                retseq = seq2test;
//                retIdx = idx2test;
//            }else if(Objects.equals(result, "FAIL")){
//                //FAIL: d_pro-- c_pro++
//                CE.add(-1);
//                List<Double> cProTmp = new ArrayList<>(cPro);
//                double cRadio = computRatio(delIdx, cProTmp) - 1.0;
//                for(int setd = 0; setd < cPro.size(); setd++){
//                    if(delIdx.contains(setd) && (cPro.get(setd) != 0) && (cPro.get(setd) != 1)){
//                        double delta = cRadio * cProTmp.get(setd);
//                        cPro.set(setd,min(cProTmp.get(setd) + delta, 1.0));
//                    }
//                }
//                for (int setd = 0; setd < dPro.length; setd++) {
//                    if (idx2test.contains(setd)) {
//                        for(int i = 0; i < dPro.length; i++){
//                            if(!idx2test.contains(i)) {
//                                dPro[setd][i] = 0.0;
//                            }
//                        }
//                    }
//                }
//            } else {
//                CE.addAll(idx2test);
//                //CE: d_pro++
//                double tmplog = 0.0;
//                for (int i = 0; i < idx2test.size(); i++) {
//                    for (int j = 0; j < delIdx.size(); j++) {
//                        if ((dPro[idx2test.get(i)][delIdx.get(j)] != 0)) {
////                            tmplog *= (1.0 - dPro[testSet.get(i)][delSet.get(j)]);
//                            //采用取对数的方式，将连乘转化为连加，以避免数值下溢
//                            tmplog += Math.log(1.0 - dPro[idx2test.get(i)][delIdx.get(j)]);
//                        }
//                    }
//                }
//                tmplog = Math.pow(Math.E, tmplog);
//                //放大，概率变为10^n/10
//                tmplog = Math.pow(10.0, tmplog) / 10.0;
//                for (int i = 0; i < idx2test.size(); i++) {
//                    for (int j = 0; j < delIdx.size(); j++) {
//                        if ((dPro[idx2test.get(i)][delIdx.get(j)] != 0)) {
//                            dPro[idx2test.get(i)][delIdx.get(j)] = min(dPro[idx2test.get(i)][delIdx.get(j)] / (1.0 - tmplog), 1.0);
//                        }
//                    }
//                }
//            }
//            bw.append("\ncPro: " + cPro);
//            for(int i = 0; i < dPro.length; i++){
//                bw.append("\n" + i + Arrays.deepToString(dPro[i]));
//            }
//
//            cProSum = listToSum(cPro);
//            dProSum = arrayToSum(dPro);
//            //判断结果和上一次是否相同
//            if(cProSum == lastcProSum && dProSum == lastdProSum){
//                stayPro++;
//            } else {
//                stayPro = 0;
//            }
//
//            //当dPro学习结束且cPro&dPro的值10次不改变，再传递依赖
//            if(DDUtil.testDone(dPro) && stayPro > 10){
//                for (int setd = 0; setd < cPro.size(); setd++) {
//                    if (cPro.get(setd) == 1.0) {
//                        //获取所有确定的依赖关系
//                        Set<Integer> tmpDependency = new HashSet<>();
//                        getDependency(tmpDependency, dPro, setd);
//                        List<Integer> dependency = new ArrayList<>(tmpDependency);
//                        for (int j = 0; j < dependency.size(); j++) {
//                            cPro.set(dependency.get(j), 1.0);
//                        }
//                    }
//                }
//            }
//        }
//        long end = System.currentTimeMillis();
//        System.out.printf("Total Time：%d ms%n", end - start);
//        bw.append(String.format("%nTotal Time：%d ms", end - start));
//        System.out.println("循环次数: " + loop);
//        bw.append("\n循环次数: " + loop);
//        System.out.println("得到hunk数量：" + retseq.size() + ":" +retseq);
//        bw.append("\n得到hunk数量：" + retseq.size() + ":" + retseq + "\n");
//        return retseq;
//    }

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

    public static String codeReduceTest(String path, List<HunkEntity> hunkEntities) throws IOException {
        Revert.revert(path,hunkEntities);
        Executor executor = new Executor();
        executor.setDirectory(new File(path));
        String result = executor.exec("chmod u+x build.sh; chmod u+x test.sh; ./build.sh; ./test.sh").replaceAll("\n","").trim();
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

    //按照python实现有问题，在同一次测试中更新了p，使每个元素prob不一样，要把p先记录下来为一个临时list
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

    public static void verification(String path, List<HunkEntity> failHunk) throws IOException {
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
