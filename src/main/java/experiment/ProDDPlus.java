package experiment;

import experiment.internal.DDInput;
import experiment.internal.DDOutput;
import experiment.internal.DeltaDebugging;
import experiment.internal.TestRunner;
import experiment.internal.TestRunner.status;
import org.apache.commons.lang3.RandomUtils;

import java.util.*;

import static java.lang.Math.*;
import static utils.DDUtil.*;

/**
 * @Author: sxz
 * @Date: 2022/10/21/16:55
 * @Description:
 */
public class ProDDPlus implements DeltaDebugging {
    final static double cSigma = 0.1;
    final static double dSigma = 0.1;
    final static double dRate = 0.1;
    DDInput ddInput;
    TestRunner testRunner;
    Map<List<Integer>, status> record = new HashMap<>();

    public ProDDPlus(DDInput ddInput, TestRunner testRunner) {
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
        double cProSum = 0.0;
        double dProSum = 0.0;
        double lastcProSum = 0.0;
        double lastdProSum = 0.0;

        int loop = 0;
        int stayPro = 0;
        List<Integer> delSet = sample(cPro);
        List<Integer> testSet = getTestSet(retSet, delSet);
        status result = status.PASS;
        while (!testDone(cPro)){
            lastcProSum = cProSum;
            lastdProSum = dProSum;
//            if(!result.equals(status.PASS) && inRecord(testSet)){
//                continue;
//            }
            loop++;
            result = testRunner.getResult(testSet,ddInput);
            record.put(testSet, result);
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
                delSet = sample(cPro);
                testSet = getTestSet(retSet, delSet);
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
                    if(cPro.get(setd) >= 0.99){
                        cPro.set(setd, 1.0);
                    }
                }
                for (int setd = 0; setd < dPro.size(); setd++) {
                    if (delSet.contains(setd)) {
                        dPro.set(setd, max(dProTmp.get(setd) - dDelta, dSigma));
                    }
                }
                delSet = sample(cPro);
                testSet = getTestSet(retSet, delSet);
            } else {
                //CE: d_pro++
                List<Double> dProTmp = new ArrayList<>(dPro);
                double dDelta = dRate * testSet.size() / delSet.size();
                for (int setd = 0; setd < dPro.size(); setd++) {
                    if (delSet.contains(setd)) {
                        dPro.set(setd, dProTmp.get(setd) + dDelta);
                    }
                }
                //int selectSetSize = retSet.size() - sample(cPro).size();
                int selectSetSize = RandomUtils.nextInt(1, retSet.size());
                List<Double> avgPro = getAvgPro(cPro, dPro);
                testSet = select(avgPro, selectSetSize);
                Collections.sort(testSet);
                delSet = getTestSet(retSet, testSet);
            }
            System.out.println("cPro: " + cPro);
            System.out.println("dPro: " + dPro);
            if (delSet.size() == 0) {
                break;
            }

            cProSum = listToSum(cPro);
            dProSum = listToSum(dPro);

            //判断结果和上一次是否相同
            if(cProSum == lastcProSum){
                stayPro++;
            } else {
                stayPro = 0;
            }
            //当cPro的值10次不改变，结束
            if(stayPro > 10 ){
                System.out.println("keep 10 break");
                break;
            }
//            else if(loop > pow(ddInput.fullSet.size(), 2)){
//                System.out.println("n^2 break");
//                break;
//            }
        }
        DDOutputWithLoop ddOutputWithLoop = new DDOutputWithLoop(retSet);
        ddOutputWithLoop.loop = loop;
        return ddOutputWithLoop;
    }
}
