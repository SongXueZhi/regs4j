package experiments.dd_experiment;

import experiments.dd_experiment.internal.DDInput;
import experiments.dd_experiment.internal.DeltaDebugging;
import experiments.dd_experiment.internal.TestRunner;

import java.util.*;

import static utils.DDUtil.*;

/**
 * @Author: sxz
 * @Date: 2022/10/21/16:26
 * @Description:
 */
public class ProDD implements DeltaDebugging {
    final static double cSigma = 0.1;
    DDInput ddInput;
    TestRunner testRunner;

    public ProDD(DDInput ddInput, TestRunner testRunner){
        this.ddInput = ddInput;
        this.testRunner = testRunner;
    }

    @Override
    public DDOutputWithLoop run() {
        List<Integer> retSet = new ArrayList<>(ddInput.fullSet);
        List<Double> cPro = new ArrayList<>();
        for (int i = 0; i < ddInput.fullSet.size(); i++) {
            cPro.add(cSigma);
        }
        int loop = 0;
        while (!testDone(cPro)) {
            loop++;
            List<Integer> delSet = sample(cPro);
            if (delSet.size() == 0) {
                break;
            }
            List<Integer> testSet = getTestSet(retSet, delSet);
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
            //System.out.println("cPro: " + cPro);
        }
        DDOutputWithLoop ddOutputWithLoop = new DDOutputWithLoop(retSet);
        ddOutputWithLoop.loop = loop;
        return ddOutputWithLoop;
    }
}
