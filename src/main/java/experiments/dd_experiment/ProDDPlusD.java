package experiments.dd_experiment;

import experiments.dd_experiment.internal.DeltaDebugging;
import experiments.dd_experiment.internal.TestRunner;
import utils.DDUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.*;
import static utils.DDUtil.*;


public class ProDDPlusD implements DeltaDebugging {
    final static double cSigma = 0.1;
    final static double dSigma = 0.1;
    final static double dRate = 0.1;
    FuzzInput ddInput;
    TestRunner testRunner;

    public ProDDPlusD(FuzzInput ddInput, TestRunner testRunner) {
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
        List<Integer> delSet = sample(cPro);
        List<Integer> testSet = getTestSet(retSet, delSet);
        while (!testDone(cPro) && loop < pow(ddInput.fullSet.size(), 2)){
            loop++;
            DDUtil.getTestSetWithDependency(testSet, ddInput.relatedMap);
            delSet = getTestSet(retSet, testSet);

            TestRunner.status result = testRunner.getResult(testSet,ddInput);
            //System.out.println(loop + ": test: " + testSet + " : " + result );
            if (result == TestRunner.status.PASS) {
                //PASS: cPro=0 dPro=0
                for (int set0 = 0; set0 < cPro.size() && set0 < dPro.size(); set0++) {
                    if (!testSet.contains(set0)) {
                        cPro.set(set0, 0.0);
                        dPro.set(set0, 0.0);
                    }
                }
                retSet = testSet;
                Collections.sort(retSet);
                int selectSetSize = retSet.size() - sample(cPro).size();
                List<Double> avgPro = getAvgPro(cPro, dPro);
                testSet = select(avgPro, selectSetSize);
                Collections.sort(testSet);
            } else if (result == TestRunner.status.FAL) {
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
                testSet = select(avgPro, selectSetSize);
                Collections.sort(testSet);
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
                testSet = select(avgPro, selectSetSize);
                Collections.sort(testSet);
            }
            //System.out.println("cPro: " + cPro);
            //System.out.println("dPro: " + dPro);
            if (testSet.size() == retSet.size()) {
                break;
            }
        }
        DDOutputWithLoop ddOutputWithLoop = new DDOutputWithLoop(retSet);
        ddOutputWithLoop.loop = loop;
        return ddOutputWithLoop;
    }

}
