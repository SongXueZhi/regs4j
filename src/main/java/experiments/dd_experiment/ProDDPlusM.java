package experiments.dd_experiment;

import experiments.dd_experiment.internal.DeltaDebugging;
import experiments.dd_experiment.internal.TestRunner;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;

import java.util.*;

import static java.lang.Math.min;
import static utils.DDUtil.*;


public class ProDDPlusM implements DeltaDebugging {
    final static double cSigma = 0.1;
    final static double dSigma = 0.1;
    final static double dRate = 0.1;
    FuzzInput ddInput;
    TestRunner testRunner;

    public ProDDPlusM(FuzzInput ddInput, TestRunner testRunner) {
        this.ddInput = ddInput;
        this.testRunner = testRunner;
    }

    @Override
    public DDOutputWithLoop run() {
        List<Integer> CE = new ArrayList<>();
        List<Integer> retSet = new ArrayList<>(ddInput.fullSet);
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
                if(ddInput.dependencies.containsMapping(i,j)){
                    dPro[i][j] = 1.0;
                }
            }
        }

        int loop = 0;
        int stayPro = 0;
        while (!testDone(cPro)){
            lastcProSum = cProSum;
            lastdProSum = dProSum;

            loop++;
            List<Integer> delSet = sample(cPro);
            if (delSet.size() == 0) {
                break;
            }

            List<Integer> testSet = getTestSet(retSet, delSet);
            //使用增益公式带上一些可能的依赖
            getProTestSet(testSet, dPro, retSet, cPro);
            //testSet和CE完全一样，随机选择元素
            if(CE.size() != 0 && CollectionUtils.isEqualCollection(testSet, CE)){
                int num = RandomUtils.nextInt(1, retSet.size());
                testSet = select(cPro, num);
            }
            delSet = getTestSet(retSet, testSet);

            TestRunner.status result = testRunner.getResult(testSet,ddInput);
            System.out.println(loop + ": test: " + testSet + " : " + result );
            CE.clear();
            if (result == TestRunner.status.PASS) {
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
                retSet = testSet;

            } else if (result == TestRunner.status.FAL) {
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
                        //如果一个dPro为1，即确定了依赖关系，也将其cPro设为1
//                        if(cPro.get(setd) == 1.0){
//                            //获取所有确定的依赖关系
//                            Set<Integer> tmpDependency = new HashSet<>();
//                            getDependency(tmpDependency, dPro, setd);
//                            List<Integer> dependency = new ArrayList<>(tmpDependency);
//                            for(int j = 0; j < dependency.size(); j++){
//                                cPro.set(dependency.get(j), 1.0);
//                            }
//                        }
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
                CE.addAll(testSet);
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
                            //确定了一个依赖，依赖的元素如果cPro已经为1，那么被依赖的元素概率设为1
//                            if(dPro[testSet.get(i)][delSet.get(j)] == 1.0 && cPro.get(testSet.get(i)) == 1.0){
//                                cPro.set(delSet.get(j), 1.0);
//                            }
                        }
                    }
                }
            }
            System.out.println("cPro: " + cPro);
            for(int i = 0; i < dPro.length; i++){
                System.out.println(Arrays.deepToString(dPro[i]));
            }

            //优雅输出
//            System.out.print("cPro: ");
//            for(int i = 0; i < cPro.size(); i++){
//                if(cPro.get(i) != 0.0)
//                System.out.print(i + ":" +cPro.get(i)  + ", ");
//            }
//            System.out.println("\ndPro: ");
//            for(int i = 0; i < dPro.length; i++){
//                for(int j = 0; j < dPro[i].length; j++){
//                    if(dPro[i][j] != 0.0){
//                        System.out.println("[" + i + "," + j + "]: " + dPro[i][j]);
//                    }
//                }
//            }

            cProSum = listToSum(cPro);
            dProSum = arrayToSum(dPro);

            //判断结果和上一次是否相同
            if(cProSum == lastcProSum && dProSum == lastdProSum){
                stayPro++;
            } else {
                stayPro = 0;
            }

            //当dPro学习结束且cPro&dPro的值10次不改变，再传递依赖
            if(testDone(dPro) && stayPro > 10){
                for (int setd = 0; setd < cPro.size(); setd++) {
                    if (cPro.get(setd) == 1.0) {
                        //获取所有确定的依赖关系
                        Set<Integer> tmpDependency = new HashSet<>();
                        getDependency(tmpDependency, dPro, setd);
                        List<Integer> dependency = new ArrayList<>(tmpDependency);
                        for (int j = 0; j < dependency.size(); j++) {
                            cPro.set(dependency.get(j), 1.0);
                        }
                    }
                }
            }
        }

        DDOutputWithLoop ddOutputWithLoop = new DDOutputWithLoop(retSet);
        ddOutputWithLoop.loop = loop;
        return ddOutputWithLoop;
    }
}
