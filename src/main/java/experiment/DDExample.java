package experiment;

import experiment.internal.DDInput;
import experiment.internal.DDOutput;
import experiment.internal.TestRunner;
import utils.FuzzUtil;

import java.util.Map;

/**
 * @Author: sxz
 * @Date: 2022/10/21/16:54
 * @Description:
 */
public class DDExample {
    public static void main(String[] args) {
        Map<String, DDOutput> ddOutputHashMap = fuzz();
    }
    public static Map<String, DDOutput> fuzz(){
        DDContext ddContext = new DDContext();
        FuzzInput fuzzInput = FuzzUtil.createFuzzInput();
        System.out.println("dd input:");
        System.out.println(fuzzInput.fullSet);
        System.out.println(fuzzInput.relatedMap);
        System.out.println(fuzzInput.criticalChanges);
        TestRunner testRunner = new TestRunner4Fuzz();
        Map<String, DDOutput> ddOutputHashMap = ddContext.addDDStrategy(new ProDD(fuzzInput,testRunner),
                        new ProDDPlus(fuzzInput,testRunner), new ProDDPlusD(fuzzInput,testRunner),
                        new ProDDD(fuzzInput,testRunner), new ProDDPlusM(fuzzInput,testRunner))
                .start();
        DDOutput proDDOutput = ddOutputHashMap.get(ProDD.class.getName());
        DDOutput proDDPlusOut = ddOutputHashMap.get(ProDDPlus.class.getName());
        DDOutput proDDPlusDOut = ddOutputHashMap.get(ProDDPlusD.class.getName());
        DDOutput proDDDOut = ddOutputHashMap.get(ProDDD.class.getName());
        DDOutput proDDPlusMOut = ddOutputHashMap.get(ProDDPlusM.class.getName());

        System.out.println("proDDOutput: " + proDDOutput.resultIndexList);
        System.out.println("proDDPlusOut: " + proDDPlusOut.resultIndexList);
        System.out.println("proDDPlusDOut: " + proDDPlusDOut.resultIndexList);
        System.out.println("proDDDOut: " + proDDDOut.resultIndexList);
        System.out.println("proDDPlusMOut: " + proDDPlusMOut.resultIndexList);
        return ddOutputHashMap;
    }
}
