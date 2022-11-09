package experiment;

import experiment.internal.DeltaDebugging;
import experiment.internal.TestRunner;
import experiment.internal.TestRunner.status;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;
import static utils.DDUtil.*;

//只有在加入dependency变成全集时才会进行随机；
public class ProDDProD implements DeltaDebugging {
    final static double cSigma = 0.1;
    final static double dSigma = 0.1;
    final static double dRate = 0.1;
    FuzzInput ddInput;
    TestRunner testRunner;

    public ProDDProD(FuzzInput ddInput, TestRunner testRunner) {
        this.ddInput = ddInput;
        this.testRunner = testRunner;
    }

    @Override
    public DDOutputWithLoop run() {
        List<Integer> retSet = new ArrayList<>(ddInput.fullSet);
        List<Double> cPro = new ArrayList<>();
        List<Double> dPro = new ArrayList<>();
        for (int i = 0; i < ddInput.fullSet.size(); i++) {
            cPro.add(cSigma);
            dPro.add(dSigma);
        }

        int loop = 0;
        while (!testDone(cPro) && loop < pow(ddInput.fullSet.size(), 2)){
            loop++;
            List<Integer> delSet = sample(cPro);
            if (delSet.size() == 0) {
                break;
            }
            List<Integer> testSet = getTestSet(retSet, delSet);
            //带上依赖关系
            testSet = realTestSet(retSet,testSet, ddInput.relatedMap, cPro);
            delSet = getTestSet(retSet, testSet);

            status result = testRunner.getResult(testSet,ddInput);
            //System.out.println(loop + ": test: " + testSet + " : " + result );
            if (result == status.PASS) {
                //PASS: cPro=0 dPro=0
                for (int set0 = 0; set0 < cPro.size() && set0 < dPro.size(); set0++) {
                    if (!testSet.contains(set0)) {
                        cPro.set(set0, 0.0);
                        dPro.set(set0, 0.0);
                    }
                }
                retSet = testSet;
            } else if (result == status.FAL) {
                //FAIL: d_pro-- c_pro++
                List<Double> cProTmp = new ArrayList<>(cPro);
                List<Double> dProTmp = new ArrayList<>(dPro);
                double cRadio = computRatio(delSet, cProTmp) - 1.0;
                double dDelta = dRate * testSet.size() / delSet.size();

                for (int setd = 0; setd < cPro.size(); setd++) {
                    if (delSet.contains(setd) && (cPro.get(setd) != 0) && (cPro.get(setd) != 1)) {
                        double delta = cRadio * cProTmp.get(setd);
                        cPro.set(setd, min(cProTmp.get(setd) + delta, 1.0));
                        //如果一个元素概率为1，那么它的依赖的概率也要设为1
                        //todo 如果依赖不确定，也只是概率的话这里要怎么处理呢
                        if(cPro.get(setd) == 1.0){
                            List<Integer> dependency = getDependency(ddInput.relatedMap, setd);
                            for(int j = 0; j < dependency.size(); j++){
                                cPro.set(dependency.get(j), 1.0);
                            }
                        }
                    }
                }

                for (int setd = 0; setd < dPro.size(); setd++) {
                    if (delSet.contains(setd)) {
                        dPro.set(setd, max(dProTmp.get(setd) - dDelta, dSigma));
                    }
                }
            } else {
                //CE: d_pro++
                List<Double> dProTmp = new ArrayList<>(dPro);
                double dDelta = dRate * testSet.size() / delSet.size();
                for (int setd = 0; setd < dPro.size(); setd++) {
                    if (delSet.contains(setd)) {
                        dPro.set(setd, min(dProTmp.get(setd) + dDelta, 1.0));
                    }
                }
            }
            //System.out.println("cPro: " + cPro);
            //System.out.println("dPro: " + dPro);

        }
        DDOutputWithLoop ddOutputWithLoop = new DDOutputWithLoop(retSet);
        ddOutputWithLoop.loop = loop;
        return ddOutputWithLoop;
    }

}
