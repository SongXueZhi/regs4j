package exper;

import exper.fuzz.FuzzInput;
import exper.models.DDModel;
import exper.models.ModelManager;
import exper.models.StepLine;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.RandomUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.*;

/**
 * @Author: sxz
 * @Date: 2022/09/26/10:13
 * @Description:
 */
public class DD {
    final static int PASS = 0;
    final static int FAL = 1;
    final static int UNRESOLVED = -1;
    final static int MIN_SET_SIZE = 3;
    final static int MAX_SET_SIZE = 200;
    final static double cSigma = 0.1;
    final static double dSigma = 0.1;
    final static double dRate = 0.1;
    final static int setSize = 6;
    final static int relatedNum = 5;
    final static int criticalNum = 2;
    static FuzzInput fuzzInput;
    static BufferedWriter bw;
    static ModelManager modelManager = new ModelManager();
    static {
        try {
            bw = new BufferedWriter(new FileWriter("detail", true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //fuzzInput = fuzz();
        fuzzInput = fuzz(setSize,relatedNum, criticalNum);
        System.out.println("dd input:");
        System.out.println(fuzzInput.set);
        System.out.println(fuzzInput.relatedMap);
        System.out.println(fuzzInput.criticalChanges);

        System.out.println("dd output:\n" + proddPlusMatrix(fuzzInput.set));

        //System.out.println("prodd output:\n" + prodd(fuzzInput.set));
        //batchTest();
    }

    public static void batchTest() {
        int num1 = 0;
        int num2 = 0;
        int num3 = 0;
        int num4 = 0;
        int num5 = 0;
        int equal = 0;

        for (int i = 0; i < 100; i++) {
            fuzzInput = fuzz(setSize, relatedNum, criticalNum);
            System.out.println("------------dd input---------------");
            System.out.println(fuzzInput.set);
            System.out.println(fuzzInput.relatedMap);
            System.out.println(fuzzInput.criticalChanges);

            List<Integer> list1 = proddPlusD(fuzzInput.set);
            List<Integer> list2  = proddPlus(fuzzInput.set);
            List<Integer> list3  = prodd(fuzzInput.set);
            List<Integer> list4  = proddD(fuzzInput.set);
            List<Integer> list5  = proddPlusMatrix(fuzzInput.set);


            int size1 = list1.size();
            int size2 = list2.size();
            int size3 = list3.size();
            int size4 = list4.size();
            int size5 = list5.size();

            if(size2 < size5){
                num2++;
            } else if(size2 > size5){
                num5++;
            }

            System.out.println("equal: " + equal);
            System.out.println("dd+D: " + num1);
            System.out.println("dd+: " + num2);
            System.out.println("dd: " + num3);
            System.out.println("ddD: " + num4);
            System.out.println("dd+M: " + num5);

        }
    }

    static List<Integer> proddPlus(List<Integer> set) {
        List<Integer> retSet = set;
        List<Double> cPro = new ArrayList<>();
        List<Double> dPro = new ArrayList<>();

        for (int i = 0; i < set.size(); i++) {
            cPro.add(cSigma);
            dPro.add(dSigma);
        }

        List<Integer> delSet = sample(cPro);

        int loop = 0;
        while (!testDone(cPro) && loop < 60){
            loop++;

            List<Integer> testSet = getTestSet(retSet, delSet);
            int result = test(testSet);
            System.out.println(loop + ": " + result + ": test: " + testSet);
            if (result == PASS) {
                //PASS: cPro=0 dPro=0
                for (int set0 = 0; set0 < cPro.size() && set0 < dPro.size(); set0++) {
                    if (!testSet.contains(set0)) {
                        cPro.set(set0, 0.0);
                        dPro.set(set0, 0.0);
                    }
                }
                retSet = testSet;
                //delSet = sample(cPro);
                int selectSetSize = retSet.size() - sample(cPro).size();
                List<Double> avgPro = getAvgPro(cPro, dPro);
                delSet = getTestSet(retSet, select(avgPro, selectSetSize));
            } else if (result == FAL) {
                //FAIL: d_pro-- c_pro++
                List<Double> cProTmp = new ArrayList<>(cPro);
                List<Double> dProTmp = new ArrayList<>(dPro);
                double cRadio = computRatio(delSet, cProTmp) - 1;
                double dDelta = dRate * testSet.size() / delSet.size();

                for (int setd = 0; setd < cPro.size(); setd++) {
                    if (delSet.contains(setd) && (cPro.get(setd) != 0) && (cPro.get(setd) != 1)) {
                        double delta = cRadio * cProTmp.get(setd);
                        cPro.set(setd, min(cProTmp.get(setd) + delta, 1.0));
                    }
                }
                for (int setd = 0; setd < dPro.size(); setd++) {
                    if (delSet.contains(setd)) {
                        dPro.set(setd, max(dProTmp.get(setd) - dDelta, dSigma));
                    }
                }
                int selectSetSize = retSet.size() - sample(cPro).size();
                List<Double> avgPro = getAvgPro(cPro, dPro);
                delSet = getTestSet(retSet, select(avgPro, selectSetSize));

            } else {
                //CE: d_pro++
                List<Double> dProTmp = new ArrayList<>(dPro);
                double dDelta = dRate * testSet.size() / delSet.size();
                for (int setd = 0; setd < dPro.size(); setd++) {
                    if (delSet.contains(setd)) {
                        dPro.set(setd, min(dProTmp.get(setd) + dDelta, 1.0));

                    }
                }
                int selectSetSize = retSet.size() - sample(cPro).size();
                List<Double> avgPro = getAvgPro(cPro, dPro);
                delSet = getTestSet(retSet, select(avgPro, selectSetSize));

            }
            System.out.println("cPro: " + cPro);
            System.out.println("dPro: " + dPro);

            if (delSet.size() == 0) {
                break;
            }
        }
        return retSet;
    }

    static List<Integer> proddPlusD(List<Integer> set) {
        List<Integer> retSet = set;
        List<Double> cPro = new ArrayList<>();
        List<Double> dPro = new ArrayList<>();

        for (int i = 0; i < set.size(); i++) {
            cPro.add(cSigma);
            dPro.add(dSigma);
        }

        List<Integer> delSet = sample(cPro);

        int loop = 0;
        while (!testDone(cPro) && loop < 60){
            loop++;

            List<Integer> testSet = getTestSet(retSet, delSet);
            List<Integer> tmpSet = new ArrayList<>(testSet);
            for (int test : tmpSet) {
                if (fuzzInput.relatedMap.containsKey(test)) {
                    getDependency(testSet, test);
                }
            }
            delSet = getTestSet(retSet, testSet);

            int result = test(testSet);
            System.out.println(loop + ": " + result + ": test: " + testSet);
            if (result == PASS) {
                //PASS: cPro=0 dPro=0
                for (int set0 = 0; set0 < cPro.size() && set0 < dPro.size(); set0++) {
                    if (!testSet.contains(set0)) {
                        cPro.set(set0, 0.0);
                        dPro.set(set0, 0.0);
                    }
                }
                retSet = testSet;
                //delSet = sample(cPro);
                int selectSetSize = retSet.size() - sample(cPro).size();
                List<Double> avgPro = getAvgPro(cPro, dPro);
                delSet = getTestSet(retSet, select(avgPro, selectSetSize));
            } else if (result == FAL) {
                //FAIL: d_pro-- c_pro++
                List<Double> cProTmp = new ArrayList<>(cPro);
                List<Double> dProTmp = new ArrayList<>(dPro);
                double cRadio = computRatio(delSet, cProTmp) - 1;
                double dDelta = dRate * testSet.size() / delSet.size();

                for (int setd = 0; setd < cPro.size(); setd++) {
                    if (delSet.contains(setd) && (cPro.get(setd) != 0) && (cPro.get(setd) != 1)) {
                        double delta = cRadio * cProTmp.get(setd);
                        cPro.set(setd, min(cProTmp.get(setd) + delta, 1.0));
                    }
                }
                for (int setd = 0; setd < dPro.size(); setd++) {
                    if (delSet.contains(setd)) {
                        dPro.set(setd, max(dProTmp.get(setd) - dDelta, dSigma));
                    }
                }
                int selectSetSize = retSet.size() - sample(cPro).size();
                List<Double> avgPro = getAvgPro(cPro, dPro);
                delSet = getTestSet(retSet, select(avgPro, selectSetSize));

            } else {
                //CE: d_pro++
                List<Double> dProTmp = new ArrayList<>(dPro);
                double dDelta = dRate * testSet.size() / delSet.size();
                for (int setd = 0; setd < dPro.size(); setd++) {
                    if (delSet.contains(setd)) {
                        dPro.set(setd, min(dProTmp.get(setd) + dDelta, 1.0));

                    }
                }
                int selectSetSize = retSet.size() - sample(cPro).size();
                List<Double> avgPro = getAvgPro(cPro, dPro);
                delSet = getTestSet(retSet, select(avgPro, selectSetSize));

            }
            System.out.println("cPro: " + cPro);
            System.out.println("dPro: " + dPro);

            if (delSet.size() == 0) {
                break;
            }
        }
        return retSet;
    }

    static List<Integer> prodd(List<Integer> set) {
        List<Integer> retSet = set;
        List<Double> cPro = new ArrayList<>();

        for (int i = 0; i < set.size(); i++) {
            cPro.add(cSigma);
        }

        int loop = 0;
        while (!testDone(cPro) || loop < 60){
            loop++;
            List<Integer> delSet = sample(cPro);
            if (delSet.size() == 0) {
                break;
            }
            List<Integer> testSet = getTestSet(retSet, delSet);
            int result = test(testSet);
            System.out.println(loop + ": " + result + ": test: " + testSet);
            if (result == PASS) {
                //PASS: cPro=0 dPro=0
                for (int set0 = 0; set0 < cPro.size(); set0++) {
                    if (!testSet.contains(set0)) {
                        cPro.set(set0, 0.0);
                    }
                }
                retSet = testSet;
            } else {
                //c_pro++
                List<Double> cProTmp = new ArrayList<>(cPro);
                double cRadio = computRatio(delSet, cProTmp) - 1;

                for (int setd = 0; setd < cPro.size(); setd++) {
                    if (delSet.contains(setd) && (cPro.get(setd) != 0) && (cPro.get(setd) != 1)) {
                        double delta = cRadio * cProTmp.get(setd);
                        cPro.set(setd, cProTmp.get(setd) + delta);
                    }
                }
            }
            System.out.println("cPro: " + cPro);

        }
        return retSet;
    }

    static List<Integer> proddD(List<Integer> set) {
        List<Integer> retSet = set;
        List<Double> cPro = new ArrayList<>();

        for(int i = 0; i < set.size(); i++){
            cPro.add(cSigma);
        }

        int loop = 0;
        while (!testDone(cPro) && loop < 60){
            loop++;
            List<Integer> delSet = sample(cPro);
            if(delSet.size() == 0){
                break;
            }
            List<Integer> testSet = getTestSet(retSet,delSet);
            List<Integer> tmpSet = new ArrayList<>(testSet);
            for (int test: tmpSet) {
                if(fuzzInput.relatedMap.containsKey(test)){
                    getDependency(testSet, test);
                }
            }
            delSet =  getTestSet(retSet,testSet);

            int result = test(testSet);
            System.out.println(loop + ": " + result + ": test: " + testSet);
            if(result == PASS){
                //PASS: cPro=0 dPro=0
                for(int set0 = 0; set0 < cPro.size() ; set0++){
                    if(!testSet.contains(set0)){
                        cPro.set(set0,0.0);
                    }
                }
                retSet = testSet;
            }else {
                //c_pro++
                List<Double> cProTmp = new ArrayList<>(cPro);
                double cRadio = computRatio(delSet,cProTmp) - 1;

                for(int setd = 0; setd < cPro.size(); setd++){
                    if(delSet.contains(setd) && (cPro.get(setd) != 0) && (cPro.get(setd) != 1)){
                        double delta = cRadio * cProTmp.get(setd);
                        cPro.set(setd,cProTmp.get(setd) + delta);
                    }
                }
            }
            System.out.println("cPro: " + cPro);

        }
        return retSet;
    }

    static List<Integer> proddPlusMatrix(List<Integer> set) {
        List<Integer> retSet = set;
        List<Double> cPro = new ArrayList<>();
        Double[][] dPro = new Double[set.size()][set.size()];

        for (int i = 0; i < set.size(); i++) {
            cPro.add(cSigma);
            for(int j = 0; j < set.size(); j++){
                dPro[i][j] = dSigma;
            }
        }

        List<Integer> delSet = sample(cPro);

        int loop = 0;
        while (!testDone(cPro) && loop < 60){
            loop++;

            List<Integer> testSet = getTestSet(retSet, delSet);
            int result = test(testSet);
            System.out.println(loop + ": " + result + ": test: " + testSet);
            if (result == PASS) {
                //PASS: cPro=0 dPro=0
                for (int set0 = 0; set0 < cPro.size() ; set0++) {
                    if (!testSet.contains(set0)) {
                        cPro.set(set0, 0.0);
                        for(int i = 0; i < dPro.length; i++){
                            dPro[i][set0] = 0.0;
                            dPro[set0][i] = 0.0;
                        }
                    }
                }
                retSet = testSet;
                //delSet = sample(cPro);
                int selectSetSize = retSet.size() - sample(cPro).size();
                List<Double> tmpProList = new ArrayList<>();
                for(int i = 0; i < dPro.length; i ++){
                    double tmpPro = 0;
                    for(int j = 0; j < dPro.length; j++){
                        tmpPro += dPro[j][i];
                    }
                    tmpProList.add(i,tmpPro);
                }
                List<Double> avgPro = getAvgPro(cPro, tmpProList);
                delSet = getTestSet(retSet, select(avgPro, selectSetSize));
            } else if (result == FAL) {
                //FAIL: d_pro=0 c_pro++
                List<Double> cProTmp = new ArrayList<>(cPro);
                double cRadio = computRatio(delSet, cProTmp) - 1;
                for (int setd = 0; setd < cPro.size(); setd++) {
                    if (delSet.contains(setd) && (cPro.get(setd) != 0) && (cPro.get(setd) != 1)) {
                        double delta = cRadio * cProTmp.get(setd);
                        cPro.set(setd, min(cProTmp.get(setd) + delta, 1.0));
                    }
                }
                for (int set0 = 0; set0 < cPro.size() ; set0++) {
                    if (testSet.contains(set0)) {
                        for(int i = 0; i < dPro.length; i++){
                            if(!testSet.contains(i)) {
                                dPro[set0][i] = 0.0;
                            }
                        }
                    }
                }
                int selectSetSize = retSet.size() - sample(cPro).size();
                List<Double> tmpProList = new ArrayList<>();
                for(int i = 0; i < dPro.length; i ++){
                    double tmpPro = 0;
                    for(int j = 0; j < dPro.length; j++){
                        tmpPro += dPro[j][i];
                    }
                    tmpProList.add(i,tmpPro);
                }
                List<Double> avgPro = getAvgPro(cPro, tmpProList);
                delSet = getTestSet(retSet, select(avgPro, selectSetSize));

            } else {
                //CE: d_pro++
                double tmplog = 1;
                for(int i = 0; i < testSet.size(); i++){
                    for(int j = 0; j < delSet.size(); j++){
                        //有没可能等于1
                        if((dPro[testSet.get(i)][delSet.get(j)] != 0) ){
                            tmplog *= (1 - dPro[testSet.get(i)][delSet.get(j)]);
                        }
                    }
                }
                for(int i = 0; i < testSet.size(); i++){
                    for(int j = 0; j < delSet.size(); j++){
                        if((dPro[testSet.get(i)][delSet.get(j)] != 0) ){
                            dPro[testSet.get(i)][delSet.get(j)] = min(dPro[testSet.get(i)][delSet.get(j)] / (1 - tmplog), 1.0);
                        }
                    }
                }

                int selectSetSize = retSet.size() - sample(cPro).size();
                List<Double> tmpProList = new ArrayList<>();
                for(int i = 0; i < dPro.length; i ++){
                    double tmpPro = 0;
                    for(int j = 0; j < dPro.length; j++){
                        tmpPro += dPro[j][i];
                    }
                    tmpProList.add(i,tmpPro);
                }
                List<Double> avgPro = getAvgPro(cPro, tmpProList);
                delSet = getTestSet(retSet, select(avgPro, selectSetSize));
            }
            System.out.println("cPro: " + cPro);
            System.out.println("dPro: " + Arrays.deepToString(dPro));

            if (delSet.size() == 0) {
                break;
            }
        }
        return retSet;
    }

    static void saveModel(FuzzInput fuzzinput, List<Integer> retSet) throws IOException {
        DDModel outputModel = new DDModel();
        outputModel.setFuzzInput(fuzzInput);
        outputModel.setResult(retSet);
        modelManager.saveModel(outputModel);
    }

    static void loadModel(String path) throws IOException {
        DDModel ddModel = modelManager.loadModel(path);
        fuzzInput = ddModel.getFuzzInput();
    }

    static List<Integer> proddToSaveModelDemo(List<Integer> set) throws IOException {
        List<Integer> retSet = set;

        List<Double> cPro = new ArrayList<>();
        DDModel outputModel = new DDModel();
        outputModel.setFuzzInput(fuzzInput);

        for (int i = 0; i < set.size(); i++) {
            cPro.add(cSigma);
        }

        int loop = 0;
        while (!testDone(cPro) && loop < 20) {
            loop++;
            List<Integer> delSet = sample(cPro);
            if (delSet.size() == 0) {
                break;
            }
            List<Integer> testSet = getTestSet(retSet, delSet);
            int result = test(testSet);

            StepLine stepLine = new StepLine();
            stepLine.setSubset(testSet);
            stepLine.setStatus(result);

            System.out.println(loop + ": " + result + ": test: " + testSet);
            if (result == PASS) {
                //PASS: cPro=0 dPro=0
                for (int set0 = 0; set0 < cPro.size(); set0++) {
                    if (!testSet.contains(set0)) {
                        cPro.set(set0, 0.0);
                    }
                }
                retSet = testSet;
            } else {
                //c_pro++
                List<Double> cProTmp = new ArrayList<>(cPro);
                double cRadio = computRatio(delSet, cProTmp) - 1;

                for (int setd = 0; setd < cPro.size(); setd++) {
                    if (delSet.contains(setd) && (cPro.get(setd) != 0) && (cPro.get(setd) != 1)) {
                        double delta = cRadio * cProTmp.get(setd);
                        cPro.set(setd, cProTmp.get(setd) + delta);
                    }
                }
            }
            stepLine.setC_pro(cPro.stream().collect(Collectors.toList()));
            outputModel.getStepLines().add(stepLine);

            System.out.println("cPro: " + cPro);

        }
        outputModel.setResult(retSet);
        modelManager.saveModel(outputModel);
        return retSet;
    }
    //TODO songxuezhi
    static List<Integer> geneticdd() {
        return null;
    }

    /**
     * @param setSize    min >=3,MAX <=200
     * @param relatedNum
     * @return
     */
    public static FuzzInput fuzz(int setSize, int relatedNum, int criticalNum) {
        if (setSize < MIN_SET_SIZE) {
            setSize = MIN_SET_SIZE;
        }
        if (setSize > MAX_SET_SIZE) {
            setSize = MAX_SET_SIZE;
        }

        FuzzInput fuzzInput = new FuzzInput();
        fuzzInput.set = new ArrayList<>(setSize);
        initializeSet(fuzzInput.set, setSize);
        fuzzInput.relatedMap = createRelatedMap(fuzzInput.set, relatedNum);
        fuzzInput.criticalChanges = createCriticalChanges(fuzzInput.set, criticalNum);
        return fuzzInput;
    }

    static FuzzInput fuzz() {
        FuzzInput fuzzInput = new FuzzInput();
        int setSize = RandomUtils.nextInt(MIN_SET_SIZE, MAX_SET_SIZE);
        int maxRelatedNum = max((setSize / 2) - 1, 0);
        int maxCriticalNum = max((setSize / 2) - 1, 0);
        int relatedNum = RandomUtils.nextInt(0, maxRelatedNum);
        int criticalNum = RandomUtils.nextInt(0, maxCriticalNum);

        return fuzz(setSize, relatedNum, criticalNum);
    }

    static int test(List<Integer> subset) {
        //Return result according to different priorities
        //1) unresolved, if a->b, a in the subset , b is not .
        Collection<Integer> interParentSet = CollectionUtils.intersection(subset, fuzzInput.relatedMap.keySet());
        for (Integer item : interParentSet) {
            Collection<Integer> childList = fuzzInput.relatedMap.get(item);
            if (!subset.containsAll(childList)) { //each father contains all child
                return UNRESOLVED;
            }
        }
        //2）PASS，contains all critical changes
        if (subset.containsAll(fuzzInput.criticalChanges)) {
            return PASS;
        } else { //FAL
            return FAL;
        }
    }

    static void initializeSet(List<Integer> set, int setSize) {
        for (int i = 0; i < setSize; i++) {
            set.add(i);
        }
    }

    static Set<Integer> createCriticalChanges(List<Integer> set, int criticalNum) {
        Set<Integer> criticalChanges = new HashSet<>(criticalNum);
        //Note that! there may be 1 cc(when c1 equals c2) or 2 cc.
        for (int i = 0; i < criticalNum; i++) {
            int c = RandomUtils.nextInt(0, set.size());
            criticalChanges.add(c);
        }
        return criticalChanges;
    }

    static MultiValuedMap<Integer, Integer> createRelatedMap(List<Integer> set, int relatedNum) {
        MultiValuedMap<Integer, Integer> relatedMap = new ArrayListValuedHashMap<>(relatedNum);
        //rules:
        // 1) if a call b, then b can't call a, i.e. a->b, b \->a.
        // 2) a can call multiple elements, i.e. a -> b, a ->c, a->d.
        // 3) if a is child node, it also could be parent node, i.e.  e->a->f
        // 4) self dependent is not allowed, i.e a \->a.

        int parentIndex = RandomUtils.nextInt(0, set.size());
        int childIndex = RandomUtils.nextInt(0, set.size());
        while (parentIndex == childIndex) {
            childIndex = RandomUtils.nextInt(0, set.size());
        }
        relatedMap.put(parentIndex, childIndex);

        for (int i = 1; i < relatedNum; i++) {
            // judge relate mapping exits , note to fit rule 1,here a->b and b->a  is same mapping
            while (relatedMap.containsMapping(parentIndex, childIndex) || relatedMap.containsMapping(childIndex,
                    parentIndex)) {
                parentIndex = RandomUtils.nextInt(0, set.size());
                childIndex = RandomUtils.nextInt(0, set.size());
                while (parentIndex == childIndex) {
                    childIndex = RandomUtils.nextInt(0, set.size());
                }
            }
            relatedMap.put(parentIndex, childIndex);
        }
        return relatedMap;
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

    public static boolean testDone(List<Double> cPro, List<Double> dPro) {
        for (int i = 0; i < cPro.size() && i < dPro.size(); i++) {
            //abs(prob-1.0)>1e-6相当于(prob-1)!=0
            //也即是返回p!=1
            //两个 p!=1，就继续运行
            if (abs(cPro.get(i) - 1.0) > 1e-6 && min(cPro.get(i), 1) < 1.0 && abs(dPro.get(i) - 1.0) > 1e-6 && min(dPro.get(i), 1) < 1.0) {
                return false;
            }
        }
        //终止条件是所有的p都是1
        return true;
    }

    public static double computRatio(List<Integer> deleteconfig, List<Double> p) {
        double res = 0;
        double tmplog = 1;
        for(int delc: deleteconfig){
            if((p.get(delc) != 0) ){
                tmplog *= (1 - p.get(delc));
            }
        }
        res = 1 / (1 - tmplog);
        return res;
    }

    // 如何取样，取得的是pro最小，被排除掉的
    public static List<Integer> sample(List<Double> cPro, List<Double> dPro) {
        List<Integer> delSet = new ArrayList<>();
        List<Double> avgPro = new ArrayList<>();
        for (int i = 0; i < cPro.size() && i < dPro.size(); i++) {
            avgPro.add(i, (cPro.get(i) + dPro.get(i)) / 2);
        }

        List<Integer> idxlist = sortToIndex(avgPro);

        int k = 0;
        double tmp = 1;
        double last = -9999;
        int i = 0;
        while (i < avgPro.size()) {
            if (avgPro.get(idxlist.get(i)) == 0) {
                k = k + 1;
                i = i + 1;
                continue;
            }
            if (!(avgPro.get(idxlist.get(i)) < 1)) {
                break;
            }
            for (int j = k; j < i + 1; j++) {
                tmp *= (1 - avgPro.get(idxlist.get(j)));
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

    public static List<Integer> getDependency(List<Integer> testSet, int test) {
        for (int dSet : fuzzInput.relatedMap.get(test)) {
            if (!testSet.contains(dSet)) {
                testSet.add(dSet);
                getDependency(testSet, dSet);
            }
        }
        return testSet;
    }
}
