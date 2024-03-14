package experiments.dd_experiment;

import experiments.dd_experiment.internal.DDInput;
import org.apache.commons.collections4.MultiValuedMap;

import java.util.Set;

/**
 * @Author: sxz
 * @Date: 2022/10/21/16:36
 * @Description:
 */
public class FuzzInput extends DDInput<Integer> {
    public MultiValuedMap<Integer,Integer> relatedMap;
    public Set<Integer> criticalChanges; //note set not have order ,but List have.
    public MultiValuedMap<Integer,Integer> dependencies;
}
