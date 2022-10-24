package experiment;

import experiment.internal.DDOutput;
import experiment.internal.DeltaDebugging;
import experiment.internal.TestRunner;
import experiment.internal.TestRunner.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
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
    public DDOutput run() {
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
        while (!testDone(cPro) && loop < 60){
            loop++;
            List<Integer> tmpSet = new ArrayList<>(testSet);
            for (int test : tmpSet) {
                if (ddInput.relatedMap.containsKey(test)) {
                    getDependency(testSet, test);
                }
            }
            delSet = getTestSet(retSet, testSet);

            status result = testRunner.getResult(testSet,ddInput);
            System.out.println(loop + ": test: " + testSet + " : " + result );
            if (result == status.PASS) {
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
            } else if (result == status.FAL) {
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
            System.out.println("cPro: " + cPro);
            System.out.println("dPro: " + dPro);
            if (testSet.size() == retSet.size()) {
                break;
            }
        }
        return new DDOutput(retSet);
    }

    public List<Integer> getDependency(List<Integer> testSet, int test) {
        for (int dSet : ddInput.relatedMap.get(test)) {
            if (!testSet.contains(dSet)) {
                testSet.add(dSet);
                getDependency(testSet, dSet);
            }
        }
        return testSet;
    }
}
