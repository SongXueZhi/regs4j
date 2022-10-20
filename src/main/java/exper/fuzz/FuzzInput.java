package exper.fuzz;

import org.apache.commons.collections4.MultiValuedMap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author: sxz
 * @Date: 2022/09/23/14:17
 * @Description:
 */
public class FuzzInput implements Serializable {

    public List<Integer> set;
    public MultiValuedMap<Integer,Integer> relatedMap;
    public Set<Integer> criticalChanges; //note set not have order ,but List have.
}
