package core;

import model.HunkEntity;
import run.Executor;

import java.io.File;
import java.util.*;

import static java.lang.Math.abs;
import static java.lang.Math.min;

public class ProbDD {


    public static List<HunkEntity> ProbDD(List<HunkEntity> hunkEntities, String path){
        int time = 0;
        assert !Objects.equals(codeRedeceTest(path, hunkEntities), "PASS");
        List<HunkEntity> retseq = hunkEntities;
        List<Integer> retIdx = new ArrayList<>();
        List<Double> p = new ArrayList<>();
        for(int i = 0; i < hunkEntities.size(); i++){
            retIdx.add(i);
            p.add(0.1);
        }
        while (!testDone(p)){
            time = time + 1;
            List<Integer> delIdx = sample(p);
            if(delIdx.size() == 0){
                break;
            }
            List<Integer> idx2test = getIdx2test(retIdx,delIdx);
            List<HunkEntity> seq2test = new ArrayList<>();
            List<HunkEntity> del2test = new ArrayList<>();
            for(int del: delIdx){
                del2test.add(hunkEntities.get(del));
            }
            for (int idxelm: idx2test){
                seq2test.add(hunkEntities.get(idxelm));
            }
            if(Objects.equals(codeRedeceTest(path,del2test), "FAIL")){
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
        return retseq;
    }

    public static String codeRedeceTest(String tmpPath, List<HunkEntity> hunkEntities){
        Executor executor = new Executor();
        //String tmpPath = ric.getLocalCodeDir().getParent() + File.separator + ric.getLocalCodeDir().getName() + "_tmp";
        executor.setDirectory(new File(tmpPath));
        String result = executor.exec("./build.sh; ./test.sh");
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
            if(p.get(delc) > 0 && p.get(delc) < 0){
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
                return (int) (((double) o1.getValue()) - (double) (o2.getValue()));
            }
        });
        for(Map.Entry<Integer,Double> entry:entrys){
            System.out.println(entry.getKey()+","+entry.getValue());
            idxlist.add(entry.getKey());
        }
        return idxlist;
    }
    public static void main(String [] args){
        List<Double> p = new ArrayList<>();
        p.add(5.0);
        p.add(3.0);
        p.add(1.0);
        p.add(2.0);
        p.add(4.0);
        p.add(2.0);

        sortToIndex(p);
    }

    public static List<Integer> sample(List<Double> p){
        List<Integer> delset = new ArrayList<>();
        List<Integer> idxlist = sortToIndex(p);
        int k = 0;
        int tmp = 1;
        int last = -9999;
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
                tmp *= (1-p.get(idxlist.get(i)));
            }
            tmp *= (i - k + 1);
            if(tmp < last){
                break;
            }
            last = tmp;
            tmp = 1;
            i = 1 + 1;
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

    public static List<HunkEntity> ddmin(List<HunkEntity> hunkEntities, String path) {

        return null;
    }

}
