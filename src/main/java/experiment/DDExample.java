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
        DDContext ddContext = new DDContext();
        DDInput fuzzInput = FuzzUtil.createFuzzInput();
        TestRunner testRunner = new TestRunner4Fuzz();
        Map<String, DDOutput> ddOutputHashMap = ddContext.addDDStrategy(new ProDD(fuzzInput,testRunner),
                        new ProDDPlus(fuzzInput,testRunner))
                .start();
        DDOutput proDDOutput = ddOutputHashMap.get(ProDD.class.getName());
        DDOutput proDDPlusOut = ddOutputHashMap.get(ProDDPlus.class.getName());
        //to show
    }
}
