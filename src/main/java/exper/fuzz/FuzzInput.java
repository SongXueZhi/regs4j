package exper.fuzz;

import org.apache.commons.collections4.MultiValuedMap;

import java.util.List;
import java.util.Set;

/**
 * @Author: sxz
 * @Date: 2022/09/23/14:17
 * @Description:
 */
public class FuzzInput {

    public List<Integer> set;
    public MultiValuedMap<Integer,Integer> relatedMap;
    public Set<Integer> criticalChanges; //note set not have order ,but List have.
}
