package utils;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.RandomUtils;

import java.util.*;

import static java.lang.Math.*;

public class DDUtil {

    public static boolean testDone(List<Double> cPro) {
        for (double prob : cPro) {
            //abs(prob-1.0)>1e-6相当于(prob-1)!=0
            //也即是返回p!=1
            //只要有p!=1，就继续运行
            if (abs(prob - 1.0) > 1e-6 && min(prob, 1) < 1.0) {
                return false;
            }
        }
        return true;
    }

    public static List<Integer> sample(List<Double> prob) {
        List<Integer> delSet = new ArrayList<>();

        List<Integer> idxlist = sortToIndex(prob);

        int k = 0;
        double tmp = 1;
        double last = -9999;
        int i = 0;
        while (i < prob.size()) {
            if (prob.get(idxlist.get(i)) == 0) {
                k = k + 1;
                i = i + 1;
                continue;
            }
            if (!(prob.get(idxlist.get(i)) < 1)) {
                break;
            }
            for (int j = k; j < i + 1; j++) {
                tmp *= (1 - prob.get(idxlist.get(j)));
            }
            tmp *= (i - k + 1);
            if (tmp < last) {
                break;
            }
            last = tmp;
            tmp = 1;
            i = i + 1;
        }
        while (i > k) {
            i = i - 1;
            delSet.add(idxlist.get(i));
        }

        return delSet;
    }

    //从小到大排序
    private static List<Integer> sortToIndex(List<Double> p) {
        List<Integer> idxlist = new ArrayList<>();
        Map<Integer, Double> pidxMap = new HashMap<>();
        for (int j = 0; j < p.size(); j++) {
            pidxMap.put(j, p.get(j));
        }
        List<Map.Entry<Integer, Double>> entrys = new ArrayList<>(pidxMap.entrySet());
        entrys.sort(new Comparator<Map.Entry>() {
            public int compare(Map.Entry o1, Map.Entry o2) {
                //return (int) ((double) o1.getValue() * 100000 - (double) o2.getValue() * 100000);
                if(((double)o1.getValue() - (double)o2.getValue())<0)
                    return -1;
                else if(((double)o1.getValue() - (double)o2.getValue())>0)
                    return 1;
                else return 0;
            }
        });
        for (Map.Entry<Integer, Double> entry : entrys) {
            idxlist.add(entry.getKey());
        }
        return idxlist;
    }

    public static List<Integer> getTestSet(List<Integer> set, List<Integer> delSet) {
        List<Integer> result = new ArrayList<>();
        for (Integer elm : set) {
            if (!delSet.contains(elm)) {
                result.add(elm);
            }
        }
        return result;
    }

    public static double computRatio(List<Integer> deleteconfig, List<Double> p) {
        double res = 0.0;
        double tmplog = 1.0;
        for(int delc: deleteconfig){
            //todo
            if((p.get(delc) != 0)){
                tmplog *= (1.0 - p.get(delc));
            }
        }
        res = 1.0 / (1.0 - tmplog);
        return res;
    }

    public static List<Double> getAvgPro(List<Double> cPro, List<Double> dPro) {
        List<Double> avgPro = new ArrayList<>();
        double cProTotal = listToSum(cPro);
        double dProTotal = listToSum(dPro);

        for (int i = 0; i < cPro.size() && i < dPro.size(); i++) {
            avgPro.add(i, cPro.get(i) / cProTotal + dPro.get(i) / dProTotal);
        }

        return avgPro;
    }

    public static double listToSum(List<Double> pro) {
        double total = 0;
        for (double p : pro) {
            total += p;
        }
        return total;
    }

    public static List<Integer> select(List<Double> prob, int selectNum) {
        List<Integer> selectSet = new ArrayList<>(selectNum);
        double total = listToSum(prob);

        while (selectSet.size() < selectNum) {
            double slice = total * Math.random();
            double sum = 0;
            for (int j = 0; j < prob.size(); j++) {
                sum += prob.get(j);
                if (sum >= slice) {
                    if (!selectSet.contains(j)) {
                        selectSet.add(j);
                    }
                    break;
                }
            }
        }

        return selectSet;

    }

    //返回testSet以及其附带的所有依赖
    public static List<Integer> getTestSetWithDependency(List<Integer> testSet, MultiValuedMap<Integer,Integer> relatedMap){
        Set<Integer> tmpSet = new HashSet<>();
        for (int test : testSet) {
            List<Integer> dependency = getDependency(relatedMap, test);
            tmpSet.addAll(dependency);
        }
        for (int tmp : tmpSet){
            if(!testSet.contains(tmp)){
                testSet.add(tmp);
            }
        }
        return testSet;
    }

    //判断testSet是否为全集，是的话加上所有的概率为1.0的元素，后执行轮盘赌选择
    public static List<Integer> realTestSet(List<Integer> retSet, List<Integer> testSet, MultiValuedMap<Integer,Integer> relatedMap, List<Double> cPro){
        DDUtil.getTestSetWithDependency(testSet, relatedMap);
        if(testSet.size() == retSet.size()){
            testSet.clear();
            List<Double> cProTmp = new ArrayList<>(cPro);
            int s = 0;//概率不为0的元素的个数
            for(int i = 0; i < cPro.size(); i++){
                if(cPro.get(i) == 1.0) {
                    testSet.add(i);
                    cProTmp.set(i, 0.0);
                }else if(cPro.get(i) != 0.0){
                    s++;
                }
            }

//            //todo 如果剩下的所有概率不为1的元素为循环依赖，永远选全集，会死循环，怎么办呀
//            //遍历所有元素，如果他带上了全集，则不能选该元素
//            for(int i = 0; i < cProTmp.size(); i++){
//                if(cProTmp.get(i) != 0){
//                    List<Integer> tmpDependency = getDependency(relatedMap, i);
//                    if(tmpDependency.size()+1 == s){ //说明该元素的依赖包括了剩下所有的元素
//                        cProTmp.set(i, 0.0); //不能选此元素
//                        s--;
//                    }
//                }
//            }
//            //没有可以选择的元素，所有的不为0的cPro设置为1，结束
//            if(s == 0){
//                for(int i = 0; i < cPro.size(); i++){
//                    if(cPro.get(i) != 0){
//                        cPro.set(i, 1.0);
//                    }
//                }
//                return retSet;
//            }
            //如果只有一个元素的概率不为1，这时不用加上轮盘赌，单独判断该元素即可
            if(testSet.size() + 1 == retSet.size()){
                return testSet;
            }

            int selectNum = RandomUtils.nextInt(1, s);
            for(int sel: select(cProTmp,selectNum)){
                if(!testSet.contains(sel)){
                    testSet.add(sel);
                }
            }

//            List<Integer> tmpTestSet = new ArrayList<>(testSet);
//            DDUtil.getTestSetWithDependency(tmpTestSet, relatedMap);
//            if(selectNum == 1 && tmpTestSet.size() == retSet.size()){
//                return testSet;
//            }
            testSet = realTestSet(retSet,testSet,relatedMap,cPro);
            return testSet;
        } else {
            return testSet;
        }
    }

    //得到某个元素所有递归的依赖
    public static List<Integer> getDependency(MultiValuedMap<Integer,Integer> relatedMap, int test) {
        Set<Integer> dependency = new HashSet<>();
        for (int dSet : relatedMap.get(test)) {
            if (!dependency.contains(dSet)) {
                dependency.add(dSet);
                MultiValuedMap<Integer,Integer> tmpRelatedMap = new ArrayListValuedHashMap<>(relatedMap);
                tmpRelatedMap.remove(test);
                dependency.addAll(getDependency(tmpRelatedMap, dSet));
            }
        }
        return new ArrayList<>(dependency);
    }

}
