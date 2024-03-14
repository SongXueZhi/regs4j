package experiments.dd_experiment.internal;

import java.util.List;

/**
 * @Author: sxz
 * @Date: 2022/10/21/16:48
 * @Description:
 */
public interface TestRunner {
    status getResult(List<Integer> subSet, DDInput ddInput);

    enum status {
        FAL, PASS, CE
    }
}
