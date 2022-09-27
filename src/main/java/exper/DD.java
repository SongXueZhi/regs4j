package exper;

import exper.fuzz.FuzzInput;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.Validate;

import java.util.*;

/**
 * @Author: sxz
 * @Date: 2022/09/26/10:13
 * @Description:
 */
public class DD {
    final static int PASS = 0;
    final static int FAL = 1;
    final static int UNRESOLVED = -1;
    final static int MIN_SET_SIZE = 3;
    final static int MAX_SET_SIZE = 200;
    static FuzzInput fuzzInput;

    public static void main(String[] args) {
        //fuzzInput = fuzz();
        fuzzInput = fuzz(5,1);
        //dd
    }

    //TODO shuning
    static List<Integer> proddPlus() {

        return null;
    }

    //TODO songxuezhi
    static List<Integer> geneticdd() {
        return null;
    }

    /**
     * @param setSize    min >=3,MAX <=200
     * @param relatedNum
     * @return
     */
    static FuzzInput fuzz(int setSize, int relatedNum) {
        if (setSize < MIN_SET_SIZE) {
            setSize = MIN_SET_SIZE;
        }
        if (setSize > MAX_SET_SIZE) {
            setSize = MAX_SET_SIZE;
        }

        FuzzInput fuzzInput = new FuzzInput();
        fuzzInput.set = new HashSet<>(setSize);
        initializeSet(fuzzInput.set,setSize);
        fuzzInput.relatedMap = createRelatedMap(fuzzInput.set, relatedNum);
        fuzzInput.criticalChanges = createCriticalChanges(fuzzInput.set);
        return fuzzInput;
    }

    static FuzzInput fuzz() {
        FuzzInput fuzzInput = new FuzzInput();
        int setSize = RandomUtils.nextInt(MIN_SET_SIZE, MAX_SET_SIZE);
        int maxRelatedNum = (setSize / 2) - 1;
        maxRelatedNum = 0 > maxRelatedNum ? 0 : maxRelatedNum;
        Validate.isTrue(maxRelatedNum >= 0);
        int relatedNum = RandomUtils.nextInt(0, maxRelatedNum);
        return fuzz(setSize, relatedNum);
    }

    //revert（1-subset）
    static int test(Set<Integer> subset) {
        //Return result according to different priorities
        //1) unresolved, if a->b, a in the subset , b is not .
       Collection<Integer> interParentSet = CollectionUtils.intersection(subset, fuzzInput.relatedMap.keySet());
       for (Integer item :interParentSet){
           Collection<Integer> childList = fuzzInput.relatedMap.get(item);
           if (!subset.containsAll(childList)){ //each father contains all child
               return UNRESOLVED;
           }
       }
       //2）PASS，contains all critical changes
        if (subset.containsAll(fuzzInput.criticalChanges)){
            return PASS;
        }else { //FAL
            return FAL;
        }
    }

    static void initializeSet(Set<Integer> set, int setSize) {
        for (int i = 0; i < setSize; i++) {
            set.add(i);
        }
    }

    static Set<Integer> createCriticalChanges(Set<Integer> set) {
        Set<Integer> criticalChanges = new HashSet<>(2);
        int c1 = RandomUtils.nextInt(0, set.size());
        int c2 = RandomUtils.nextInt(0, set.size());
        //Note that! there may be 1 cc(when c1 equals c2) or 2 cc.
        criticalChanges.add(c1);
        criticalChanges.add(c2);
        return  criticalChanges;
    }

    static MultiValuedMap<Integer, Integer> createRelatedMap(Set<Integer> set, int relatedNum) {
        MultiValuedMap<Integer, Integer> relatedMap = new ArrayListValuedHashMap<>(relatedNum);
        //rules:
        // 1) if a call b, then b can't call a, i.e. a->b, b \->a.
        // 2) a can call multiple elements, i.e. a -> b, a ->c, a->d.
        // 3) if a is child node, it also could be parent node, i.e.  e->a->f
        // 4) self dependent is not allowed, i.e a \->a.

        int parentIndex = RandomUtils.nextInt(0, set.size());
        int childIndex = RandomUtils.nextInt(0, set.size());
        while (parentIndex == childIndex) {
            childIndex = RandomUtils.nextInt(0, set.size());
        }
        relatedMap.put(parentIndex, childIndex);

        for (int i = 1; i < relatedNum; i++) {
            // judge relate mapping exits , note to fit rule 1,here a->b and b->a  is same mapping
            while (relatedMap.containsMapping(parentIndex, childIndex) || relatedMap.containsMapping(childIndex,
                    parentIndex)) {
                parentIndex = RandomUtils.nextInt(0, set.size());
                while (parentIndex == childIndex) {
                    childIndex = RandomUtils.nextInt(0, set.size());
                }
            }
            relatedMap.put(parentIndex, childIndex);
        }
        return relatedMap;
    }

}
