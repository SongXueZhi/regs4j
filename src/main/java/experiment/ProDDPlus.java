package experiment;

import experiment.internal.DDInput;
import experiment.internal.DDOutput;
import experiment.internal.DeltaDebugging;
import experiment.internal.TestRunner;

/**
 * @Author: sxz
 * @Date: 2022/10/21/16:55
 * @Description:
 */
public class ProDDPlus implements DeltaDebugging {
    DDInput ddInput;
    TestRunner testRunner;

    public ProDDPlus(DDInput ddInput, TestRunner testRunner) {
        this.ddInput = ddInput;
        this.testRunner = testRunner;
    }

    @Override
    public DDOutput run() {
        return null;
    }
}
