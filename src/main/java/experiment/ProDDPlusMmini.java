package experiment;

import experiment.internal.DDInput;
import experiment.internal.DeltaDebugging;
import experiment.internal.TestRunner;
import experiment.internal.TestRunner.status;

import java.util.*;

import static java.lang.Math.min;
import static utils.DDUtil.*;


public class ProDDPlusMmini implements DeltaDebugging {
    final static double cSigma = 0.1;
    final static double dSigma = 0.1;
    final static double dRate = 0.1;
    DDInput ddInput;
    TestRunner testRunner;

    public ProDDPlusMmini(DDInput ddInput, TestRunner testRunner) {
        this.ddInput = ddInput;
        this.testRunner = testRunner;
    }

    @Override
    public DDOutputWithLoop run() {
        List<Integer> retSet = new ArrayList<>(ddInput.fullSet);
        List<Double> cPro = new ArrayList<>();
        Double[][] dPro = new Double[ddInput.fullSet.size()][ddInput.fullSet.size()];
        for (int i = 0; i < ddInput.fullSet.size(); i++) {
            cPro.add(cSigma);
            for(int j = 0; j < ddInput.fullSet.size(); j++){
                dPro[i][j] = dSigma;
                if(i == j){
                    dPro[i][j] = 0.0;
                }
            }
        }

        int loop = 0;

        //while (!testDone(cPro) && loop < pow(ddInput.fullSet.size(), 2)){
        while (!testDone(cPro)){
                loop++;
            List<Integer> delSet = sample(cPro);
            if (delSet.size() == 0) {
                break;
            }
            List<Integer> testSet = getTestSet(retSet, delSet);
            //使用增益公式带上一些可能的依赖，感觉这里很容易带上所有的元素
            getProTestSet(testSet, dPro, retSet, cPro);
            delSet = getTestSet(retSet, testSet);

            status result = testRunner.getResult(testSet,ddInput);
            System.out.println(loop + ": test: " + testSet + " : " + result );
            if (result == status.PASS) {
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

            } else if (result == status.FAL) {
                //FAIL: d_pro=0 c_pro++
                List<Double> cProTmp = new ArrayList<>(cPro);
                double cRadio = computRatio(delSet, cProTmp) - 1.0;
                for (int setd = 0; setd < cPro.size(); setd++) {
                    if (delSet.contains(setd) && (cPro.get(setd) != 0) && (cPro.get(setd) != 1)) {
                        double delta = cRadio * cProTmp.get(setd);
                        cPro.set(setd, min(cProTmp.get(setd) + delta, 1.0));
//                        if(cPro.get(setd) >= 0.99){
//                            cPro.set(setd, 1.0);
//                        }
                        //如果一个dPro为1，即确定了依赖关系，也将其cPro设为1
                        if(cPro.get(setd) == 1.0){
                            //获取所有确定的依赖关系
                            Set<Integer> tmpDependency = new HashSet<>();
                            getDependency(tmpDependency, dPro, setd);
                            List<Integer> dependency = new ArrayList<>(tmpDependency);
//                            List<Integer> dependency = new ArrayList<>(getDependency(tmpDependency, dPro, setd));
                            for(int j = 0; j < dependency.size(); j++){
                                cPro.set(dependency.get(j), 1.0);
                            }
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
                //CE: d_pro++
                double tmplog = 1;
                for (int i = 0; i < testSet.size(); i++) {
                    for (int j = 0; j < delSet.size(); j++) {
                        //有没可能等于1
                        if ((dPro[testSet.get(i)][delSet.get(j)] != 0)) {
                            tmplog *= (1.0 - dPro[testSet.get(i)][delSet.get(j)]);
                        }
                    }
                }
                //放大，概率变为10^n/10
                //tmplog = Math.pow(10.0, tmplog) / 10.0;
                for (int i = 0; i < testSet.size(); i++) {
                    for (int j = 0; j < delSet.size(); j++) {
                        if ((dPro[testSet.get(i)][delSet.get(j)] != 0)) {
                            dPro[testSet.get(i)][delSet.get(j)] = min(dPro[testSet.get(i)][delSet.get(j)] / (1.0 - tmplog), 1.0);
                            //todo 因为有不能停下的情况和精度问题，暂时将大于0.99的情况视为1
//                            if(dPro[testSet.get(i)][delSet.get(j)] >= 0.99){
//                                dPro[testSet.get(i)][delSet.get(j)] = 1.0;
//                            }
                            //确定了一个依赖，依赖的元素如果cPro已经为1，那么被依赖的元素概率设为1
                            if(dPro[testSet.get(i)][delSet.get(j)] == 1.0 && cPro.get(testSet.get(i)) == 1.0){
                                cPro.set(delSet.get(j), 1.0);
                            }
                        }
                    }
                }
            }
            System.out.println("cPro: " + cPro);
            for(int i = 0; i < dPro.length; i++){
                System.out.println(Arrays.deepToString(dPro[i]));
            }
        }
        DDOutputWithLoop ddOutputWithLoop = new DDOutputWithLoop(retSet);
        ddOutputWithLoop.loop = loop;
        return ddOutputWithLoop;
    }
}
