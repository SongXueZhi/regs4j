package experiments.dd_experiment;

import experiments.dd_experiment.internal.DDOutput;
import experiments.dd_experiment.internal.DeltaDebugging;
import experiments.dd_experiment.internal.TestRunner;
import utils.DDUtil;

import java.util.*;

import static java.lang.Math.pow;
import static utils.DDUtil.*;


public class ProDDD implements DeltaDebugging {
    final static double cSigma = 0.1;
    FuzzInput ddInput;
    TestRunner testRunner;

    public ProDDD(FuzzInput ddInput, TestRunner testRunner){
        this.ddInput = ddInput;
        this.testRunner = testRunner;
    }

    @Override
    public DDOutput run() {
        List<Integer> retSet = new ArrayList<>(ddInput.fullSet);
        List<Double> cPro = new ArrayList<>();
        for (int i = 0; i < ddInput.fullSet.size(); i++) {
            cPro.add(cSigma);
        }
        int loop = 0;
        while (!testDone(cPro) && loop < pow(ddInput.fullSet.size(), 2)) {
            loop++;
            List<Integer> delSet = sample(cPro);
            if (delSet.size() == 0) {
                break;
            }
            List<Integer> testSet = getTestSet(retSet, delSet);
            DDUtil.getTestSetWithDependency(testSet, ddInput.relatedMap);
            delSet =  getTestSet(retSet,testSet);

            TestRunner.status result = testRunner.getResult(testSet,ddInput);
            System.out.println(loop + ": test: " + testSet + " : " + result );
            if (result == TestRunner.status.PASS) {
                //PASS: cPro=0 dPro=0
                for (int set0 = 0; set0 < cPro.size(); set0++) {
                    if (!testSet.contains(set0)) {
                        cPro.set(set0, 0.0);
                    }
                }
                retSet = testSet;
                Collections.sort(retSet);
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
            if (delSet.size() == 0) {
                break;
            }
        }
        return new DDOutput(retSet);
    }

}
