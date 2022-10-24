package utils;

import java.util.*;

import static java.lang.Math.abs;
import static java.lang.Math.min;

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

    private static List<Integer> sortToIndex(List<Double> p) {
        List<Integer> idxlist = new ArrayList<>();
        Map<Integer, Double> pidxMap = new HashMap<>();
        for (int j = 0; j < p.size(); j++) {
            pidxMap.put(j, p.get(j));
        }
        List<Map.Entry<Integer, Double>> entrys = new ArrayList<>(pidxMap.entrySet());
        entrys.sort(new Comparator<Map.Entry>() {
            public int compare(Map.Entry o1, Map.Entry o2) {
                return (int) ((double) o1.getValue() * 100000 - (double) o2.getValue() * 100000);
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
        double res = 0;
        double tmplog = 1;
        for(int delc: deleteconfig){
            //todo
            if((p.get(delc) != 0)){
                tmplog *= (1 - p.get(delc));
            }
        }
        res = 1 / (1 - tmplog);
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



}
