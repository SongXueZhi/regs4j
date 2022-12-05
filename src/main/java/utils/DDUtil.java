package utils;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.RandomUtils;

import java.util.*;

import static java.lang.Math.*;

public class DDUtil {

    public static boolean testDone(List<Double> cPro) {
        for (double prob : cPro) {
            if (abs(prob - 1.0) > 1e-6 && min(prob, 1) < 1.0) {
                return false;
            }
        }
        return true;
    }

    public static boolean testDone(Double[][] dPro) {
        boolean allZero = true;
        for (Double[] p : dPro) {
            for (double prob: p) {
                if(prob != 0.0){
                    //没有依赖关系时，应等到cPro结束
                    allZero = false;
                }
                if (prob != 0.0 && prob != 1.0) {
                    return false;
                }
            }
        }
        return !allZero;
    }

    public static List<Integer> sample(List<Double> prob) {
        List<Integer> delSet = new ArrayList<>();

        List<Integer> idxlist = sortToIndex(prob);

        int k = 0;
        double tmp = 0.0;
        double last = -9999;
        int i = 0;
        while (i < prob.size()) {
            //概率为0不考虑在内
            if (prob.get(idxlist.get(i)) == 0) {
                k = k + 1;
                i = i + 1;
                continue;
            }
            if (!(prob.get(idxlist.get(i)) < 1)) {
                break;
            }
            for (int j = k; j < i + 1; j++) {
                //tmp *= (1 - prob.get(idxlist.get(j)));
                tmp += Math.log(1.0 - prob.get(idxlist.get(j)));
            }
            tmp = Math.pow(Math.E, tmp);
            tmp *= (i - k + 1);
            if (tmp < last) {
                break;
            }
            last = tmp;
            tmp = 0;
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
        double tmplog = 0.0;
        for(int delc: deleteconfig){
            if((p.get(delc) != 0)){
                //tmplog *= (1.0 - p.get(delc));
                tmplog += Math.log(1.0 - p.get(delc));
            }
        }
        tmplog = Math.pow(Math.E, tmplog);
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
        getTestSetWithDependency(testSet, relatedMap);
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

//            //todo 如果剩下的所有概率不为1的元素为循环依赖，永远选全集，会死循环，怎么办呀，暂时不处理
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

    //得到testSet及其所有可能的依赖
    public static List<Integer> getProDependency(List<Integer> testSet, Double[][] dPro, List<Integer> retSet){
        for(int i = 0; i < testSet.size(); i++){
            List<Double> idPro = new ArrayList<>(Arrays.asList(dPro[testSet.get(i)]));
            List<Integer> delDependency = sample(idPro);
            List<Integer> addDependency = getTestSet(retSet,delDependency);
            for(Integer add: addDependency){
                //删掉addDependency中包含的概率为0的元素
                if(!testSet.contains(add) && dPro[testSet.get(i)][add] != 0){
                    testSet.add(add);
                }
            }
        }
        return testSet;
    }

    //得到testSet及其所有可能的依赖（在一定概率下失活）
    public static List<Integer> getProDependencyWithEpsilon(List<Integer> testSet, Double[][] dPro, List<Integer> retSet){
        List<Integer> addProDependency = new ArrayList<>();
        for(int i = 0; i < testSet.size(); i++){
            List<Double> idPro = new ArrayList<>(Arrays.asList(dPro[testSet.get(i)]));
            List<Integer> delDependency = sample(idPro);
            List<Integer> addDependency = getTestSet(retSet,delDependency);
            for(Integer add: addDependency){
                //删掉addDependency中包含的概率为0的元素
                if(!testSet.contains(add) && !addProDependency.contains(add) && dPro[testSet.get(i)][add] != 0){
                    addProDependency.add(add);
                }
            }
        }
        if(addProDependency.size() == 0){
            return testSet;
        }
        //dependency元素失活的概率 = |𝒅𝒆𝒑𝒆𝒏𝒅𝒆𝒏𝒄𝒚|/|𝒓𝒆𝒕𝑺𝒆𝒕| * (𝟏−𝒎𝒂𝒙(元素被依赖的概率))
        double rate = (float)addProDependency.size() / (float)retSet.size();
        List<Integer> tmpAddProDependency = new ArrayList<>(addProDependency);
        for (Integer add : tmpAddProDependency){
            double maxDependency = 0.0;
            //取得被依赖的最大概率
            for (Double[] doubles : dPro) {
                if (doubles[add] > maxDependency) {
                    maxDependency = doubles[add];
                }
            }
            double epsilon = rate * (1 - maxDependency) + 0.1;
            if(epsilon == 0.0){
                continue;
            }
            double slice =  Math.random();
            if(slice <= epsilon){
                addProDependency.remove(add);
            }
        }
        testSet.addAll(addProDependency);

//        //加上addProDependency确定的依赖（dPro为1的元素）？
//        for(Integer addPro: addProDependency){
//            Set<Integer> dependency = new HashSet<>();
//            getDependency(dependency, dPro, addPro);
//            for (Integer d: dependency){
//                if(!testSet.contains(d)){
//                    testSet.add(d);
//                }
//            }
//        }
        return testSet;
    }

    //判断是否选择了全集，否则重新选
    //轮盘赌重新选择后是否需要带上所有可能的依赖 ——是
    public static List<Integer> getProTestSet(List<Integer> testSet, Double[][] dPro, List<Integer> retSet, List<Double> cPro){
        //getProDependency(testSet, dPro,  retSet);
        getProDependencyWithEpsilon(testSet, dPro, retSet);
        int loop = 0;
        while (testSet.size() == retSet.size()){
            loop++;
            testSet.clear();
            List<Double> cProTmp = new ArrayList<>(cPro);
            int s = 0;//概率不为1或0的元素的个数
            for(int i = 0; i < cPro.size(); i++){
                if(cPro.get(i) == 1.0) {
                    testSet.add(i);
                    cProTmp.set(i, 0.0);
                }else if(cPro.get(i) != 0.0){
                    s++;
                }
            }
            if(testSet.size() + 1 == retSet.size()){
                return testSet;
            }

            int selectNum = RandomUtils.nextInt(0, s);
            for(int sel: select(cProTmp,selectNum)){
                if(!testSet.contains(sel)){
                    testSet.add(sel);
                }
            }
            getProDependencyWithEpsilon(testSet, dPro, retSet);
        }
        return testSet;
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

    //得到某个元素确定的所有的递归依赖
    public static Set<Integer> getDependency(Set<Integer> dependency, Double[][] dPro, int test) {
        dependency.add(test);
        for (int dSet = 0; dSet < dPro[test].length; dSet++) {
            if (!dependency.contains(dSet) && dPro[test][dSet] == 1.0) {
                dependency.add(dSet);
                getDependency(dependency, dPro, dSet);
            }
        }
        return dependency;
    }
}
