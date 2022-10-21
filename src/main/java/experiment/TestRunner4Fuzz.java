package experiment;

import experiment.internal.DDInput;
import experiment.internal.TestRunner;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 * @Author: sxz
 * @Date: 2022/10/21/16:52
 * @Description:
 */
public class TestRunner4Fuzz implements TestRunner {

    @Override
    public status getResult(List<Integer> subset, DDInput fuzzInput) {
        return test(subset, (FuzzInput) fuzzInput);
    }

    status test(List<Integer> subset, FuzzInput fuzzInput) {
        //Return result according to different priorities
        //1) unresolved, if a->b, a in the subset , b is not .
        Collection<Integer> interParentSet = CollectionUtils.intersection(subset, fuzzInput.relatedMap.keySet());
        for (Integer item : interParentSet) {
            Collection<Integer> childList = fuzzInput.relatedMap.get(item);
            if (!subset.containsAll(childList)) { //each father contains all child
                return status.CE;
            }
        }
        //2）PASS，contains all critical changes
        if (subset.containsAll(fuzzInput.criticalChanges)) {
            return status.PASS;
        } else { //FAL
            return status.FAL;
        }
    }
}
