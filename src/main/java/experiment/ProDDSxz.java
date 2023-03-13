package experiment;

import experiment.internal.DeltaDebugging;
import org.apache.commons.collections4. CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.RandomUtils;

import java.util.*;

import static java.lang.Math.log;
import static java.lang.Math.min;
import static utils.DDUtil.*;


public class ProDDSxz implements DeltaDebugging {
    final static double cSigma = 0.1;
    final static double dSigma = 0.1;
    final static double dRate = 0.1;
    final static int PASS =1;
    final static int FAL =0;
    final static int CE =-1;

    FuzzInput ddInput;
    MultiValuedMap<Integer, Integer> smellMap;

    public ProDDSxz(FuzzInput ddInput, MultiValuedMap<Integer, Integer> smellMap) {
        this.ddInput = ddInput;
        this.smellMap = smellMap;
    }

    @Override
    public DDOutputWithLoop run() {
        HashMap<String, List<Integer>> CEMap = new HashMap<>();
        List<Integer> retSet = new ArrayList<>(ddInput.fullSet);
        Set<String> his = new HashSet<>();
        List<Double> cPro = new ArrayList<>();
        Double[][] dPro = new Double[ddInput.fullSet.size()][ddInput.fullSet.size()];
        double cProSum = 0.0;
        double dProSum = 0.0;
        double lastcProSum = 0.0;
        double lastdProSum = 0.0;
        for (int i = 0; i < ddInput.fullSet.size(); i++) {
            cPro.add(cSigma);
            for(int j = 0; j < ddInput.fullSet.size(); j++){
                dPro[i][j] = dSigma;
                if(i == j){
                    dPro[i][j] = 0.0;
                }
                if(smellMap.containsMapping(i,j)){
                    dPro[i][j] = 0.9;
                }
            }
        }

        int loop = 0;
        int stayPro = 0;
        Set<String> totalSet = new HashSet<>();
        while (!testDone(cPro)){
            lastcProSum = cProSum;
            lastdProSum = dProSum;
            loop++;
            List<Integer> delSet = sample(cPro);
            if (delSet.size() == 0) {
                break;
            }

            List<Integer> testSet = getTestSet(retSet, delSet);
            resolveDependency(testSet,dPro,cPro);
            totalSet.add(getListToString(testSet));
            //检测编译
            if(!judgeCompile(testSet,CEMap)){ //检测能否编译
//                System.out.println("CE "+ Arrays.deepToString(testSet.toArray()));
                testSet = genNextList(testSet,delSet,dPro,cPro,CEMap,totalSet);
                if (testSet == null){
                    break;
                }
                resolveDependency(testSet,dPro,cPro); //如果不可以编译则生成
            }

            System.out.println("CE Size "+CEMap.size());
            delSet = getTestSet(retSet, testSet);
            List<List<Integer>> subCESet = getCESubset(testSet,CEMap);
            for (List<Integer> subset: subCESet){
                upDateDpro(subset,getTestSet(testSet,subset),dPro);
            }
            int result = getTestResult(testSet);
            System.out.println(loop + ": test: " + testSet + " : " + result );
            his.add(getListToString(testSet));
            if (result == PASS) {
                //PASS: cPro=0 dPro=0
                for (int set0 = 0; set0 < cPro.size(); set0++) {
                    if (!testSet.contains(set0)) {
                        cPro.set(set0, 0.0);
                        for(int i = 0; i < dPro.length; i++){
                            dPro[i][set0] = 0.0;
                            dPro[set0][i] = 0.0;
                        }
                    }
                }
                if (testSet.size()<retSet.size()) {
                    retSet = testSet;
                }
            } else if (result == FAL) {
                //FAIL: d_pro=0 c_pro++
                List<Double> cProTmp = new ArrayList<>(cPro);
                double cRadio = computRatio(delSet, cProTmp) - 1.0;
                for (int setd = 0; setd < cPro.size(); setd++) {
                    if (delSet.contains(setd) && (cPro.get(setd) != 0) && (cPro.get(setd) != 1)) {
                        double delta = cRadio * cProTmp.get(setd);
                        cPro.set(setd, min(cProTmp.get(setd) + delta, 1.0));
                        if(cPro.get(setd) >= 0.99){
                            cPro.set(setd, 1.0);
                        }
                    }
                }
                for (int setd = 0; setd < cPro.size(); setd++) {
                    if (testSet.contains(setd)) {
                        for(int i = 0; i < dPro.length; i++){
                            if(!testSet.contains(i)) {
                                dPro[setd][i] = 0.0;
                            }
                        }
                    }
                }
            } else {

            }
            System.out.println("cPro: " + cPro);
            for(int i = 0; i < dPro.length; i++){
                System.out.println(Arrays.deepToString(dPro[i]));
            }


            cProSum = listToSum(cPro);
            dProSum = arrayToSum(dPro);

            //判断结果和上一次是否相同
            if(cProSum == lastcProSum){
                stayPro++;
            } else {
                stayPro = 0;
            }

            //当dPro学习结束且cPro&dPro的值10次不改变，再传递依赖
            if(stayPro > 1000) {
                break;

            }
        }
        System.out.println("不重复的测试："+his.size());
        DDOutputWithLoop ddOutputWithLoop = new DDOutputWithLoop(retSet);
        ddOutputWithLoop.loop = his.size();
        ddOutputWithLoop.CE =CEMap.size();
        return ddOutputWithLoop;
    }

//    Map<Float,List<Integer>> getConsiderList(List<Integer> testSet, List<Integer> delSet,double[][] dpro){
//
//    }

    String getListToString(List<Integer> set){
        Collections.sort(set);
        StringBuilder sb = new StringBuilder();
        for (Integer item : set){
            sb.append(item);
        }
        return sb.toString();
    }

    boolean judgeCompile(List<Integer> set, HashMap<String,List<Integer>> CESet){
        if (CESet.containsKey(getListToString(set))){
            return false;
        }
        Collection<Integer> interParentSet = CollectionUtils.intersection(set, this.ddInput.relatedMap.keySet());
        for (Integer item : interParentSet) {
            Collection<Integer> childList = this.ddInput.relatedMap.get(item);
            if (!set.containsAll(childList)) { //each father contains all child
                CESet.put(getListToString(set),set);
                return false;
            }
        }
        return true;
    }
     int getTestResult(List<Integer> set){
        //2）PASS，contains all critical changes
        if (set.containsAll(ddInput.criticalChanges)) {
            return PASS;
        } else { //FAL
            return FAL;
        }
    }

    void  upDateDpro(List<Integer> testSet, List<Integer> delSet,Double[][] dPro){
        //CE: d_pro++
        double tmplog = 0.0;
        for (int i = 0; i < testSet.size(); i++) {
            for (int j = 0; j < delSet.size(); j++) {
                if ((dPro[testSet.get(i)][delSet.get(j)] != 0)) {
//                            tmplog *= (1.0 - dPro[testSet.get(i)][delSet.get(j)]);
                    //采用取对数的方式，将连乘转化为连加，以避免数值下溢
                    tmplog += Math.log(1.0 - dPro[testSet.get(i)][delSet.get(j)]);
                }
            }
        }
        tmplog = Math.pow(Math.E, tmplog);
        //放大，概率变为10^n/10
        tmplog = Math.pow(10.0, tmplog) / 10.0;
        for (int i = 0; i < testSet.size(); i++) {
            for (int j = 0; j < delSet.size(); j++) {
                if ((dPro[testSet.get(i)][delSet.get(j)] != 0)) {
                    dPro[testSet.get(i)][delSet.get(j)] = min(dPro[testSet.get(i)][delSet.get(j)] / (1.0 - tmplog), 1.0);
                    // 因为有不能停下的情况和精度问题，暂时将大于0.99的情况视为1
                    if(dPro[testSet.get(i)][delSet.get(j)] >= 0.99){
                        dPro[testSet.get(i)][delSet.get(j)] = 1.0;
                    }
                }
            }
        }
    }

    List<Integer> genCompileList(List<Integer> testSet,Double[][] dPro, List<Integer> delSet){
        List<Integer> result = new ArrayList<>();
        result.addAll(testSet);
        Map<Integer,Double> dScoreMap = new HashMap<>();
        for (Integer del : delSet){
            double sum =0d;
            for (Integer item: testSet){
                sum+=dPro[item][del];
            }
            dScoreMap.put(del,sum);
        }
        for (Map.Entry<Integer,Double> entry: dScoreMap.entrySet()){
            if (RandomUtils.nextDouble(0.0,1.0)< entry.getValue()/testSet.size()){
                result.add(entry.getKey());
            }
        }
        result = new ArrayList<>(new HashSet<>(result));
        return  result;
    }
    List<Integer> genConsiderList(List<Integer> testSet, List<Double> cPro){
        int size = RandomUtils.nextInt(1, testSet.size()-1);;
        List<Integer> result;
        Collections.shuffle(testSet);
        result = testSet.subList(0, size);
        return result;
    }
    List<Integer> genNextList(List<Integer> testSet,List<Integer> delSet,Double[][] dPro,
                              List<Double> cPro, HashMap<String,List<Integer>> CESet, Set<String> totalSet){
        int j=0;
        int complete = 0;
        int total = testSet.size()+delSet.size();
        List<Integer> cList = new ArrayList<>();
        List<Integer> backTestList = new ArrayList<>();
        backTestList.addAll(testSet);
        boolean isCompile=false;
        int  m =0 ;
        Set<String> historySet = new HashSet<>();
        do {
            cList = genCompileList(backTestList, dPro, delSet); //动 delSet
            if (cList.size() == testSet.size()+delSet.size()){
                complete++;
            }
             if (complete>9 && complete % 10 == 0){
                 do{
                     backTestList = genConsiderList(testSet, cPro);//动 testSet
                 }while (backTestList.size() == testSet.size());
                cList =backTestList;
            }
            String s = getListToString(cList);
            if (!historySet.contains(s)){
                m++;
                historySet.add(getListToString(cList));
            }

            if (m>total*total){
                return null;
            }
            isCompile = judgeCompile(cList,CESet);

        } while (cList.size() == 0 || cList.size() == testSet.size()+delSet.size() || totalSet.contains(cList)|| !isCompile );

        return cList;
    }

      List<List<Integer>> getCESubset(List<Integer> testSet,HashMap<String,List<Integer>> CESet){
          List<List<Integer>> result  = new ArrayList<>();
          for (List<Integer> list : CESet.values()){
            if (testSet.containsAll(list)){
                result.add(list);
            }
        }
          return result;
      }
      void resolveDependency(List<Integer> testSet,Double[][] dPro,List<Double> cPro){
        List<Integer> copy = new ArrayList<>();
        copy.addAll(testSet);
        for (Integer item : copy){
            for (int i =0;i<dPro.length;i++){
                if (dPro[item][i] ==1){
                if (!testSet.contains(i)) {
                    testSet.add(i);
                }
                cPro.set(i,cPro.get(item) );
                }
            }
        }
      }
}
