package experiment;

import experiment.internal.DDInput;
import experiment.internal.DDOutput;
import experiment.internal.DeltaDebugging;
import experiment.internal.TestRunner;
import experiment.internal.TestRunner.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static utils.DDUtil.*;


public class ProDDPlusM implements DeltaDebugging {
    final static double cSigma = 0.1;
    final static double dSigma = 0.1;
    final static double dRate = 0.1;
    DDInput ddInput;
    TestRunner testRunner;

    public ProDDPlusM(DDInput ddInput, TestRunner testRunner) {
        this.ddInput = ddInput;
        this.testRunner = testRunner;
    }

    @Override
    public DDOutput run() {
        List<Integer> retSet = new ArrayList<>(ddInput.fullSet);
        List<Double> cPro = new ArrayList<>();
        Double[][] dPro = new Double[ddInput.fullSet.size()][ddInput.fullSet.size()];
        for (int i = 0; i < ddInput.fullSet.size(); i++) {
            cPro.add(cSigma);
            for(int j = 0; j < ddInput.fullSet.size(); j++){
                dPro[i][j] = dSigma;
            }        }

        int loop = 0;
        List<Integer> delSet = sample(cPro);
        List<Integer> testSet = getTestSet(retSet, delSet);
        while (!testDone(cPro) && loop < 60){
            loop++;
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
                //FAIL: d_pro-- c_pro++
                List<Double> cProTmp = new ArrayList<>(cPro);
                double cRadio = computRatio(delSet, cProTmp) - 1;
                for (int setd = 0; setd < cPro.size(); setd++) {
                    if (delSet.contains(setd) && (cPro.get(setd) != 0) && (cPro.get(setd) != 1)) {
                        double delta = cRadio * cProTmp.get(setd);
                        cPro.set(setd, min(cProTmp.get(setd) + delta, 1.0));
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
                            tmplog *= (1 - dPro[testSet.get(i)][delSet.get(j)]);
                        }
                    }
                }
                for (int i = 0; i < testSet.size(); i++) {
                    for (int j = 0; j < delSet.size(); j++) {
                        if ((dPro[testSet.get(i)][delSet.get(j)] != 0)) {
                            dPro[testSet.get(i)][delSet.get(j)] = min(dPro[testSet.get(i)][delSet.get(j)] / (1 - tmplog), 1.0);
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
            testSet = select(avgPro, selectSetSize);
            Collections.sort(testSet);
            delSet = getTestSet(retSet, testSet);

            System.out.println("cPro: " + cPro);
            System.out.println("dPro: " + Arrays.deepToString(dPro));
            if (delSet.size() == 0) {
                break;
            }
        }
        return new DDOutput(retSet);
    }
}
