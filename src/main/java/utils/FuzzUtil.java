package utils;

import experiment.FuzzInput;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Math.max;

/**
 * @Author: sxz
 * @Date: 2022/10/21/17:01
 * @Description:
 */
public class FuzzUtil {
    final static int MIN_SET_SIZE = 3;
    final static int MAX_SET_SIZE = 20;

    public static FuzzInput createFuzzInput(int setSize, int relatedNum, int criticalNum) {
        if (setSize < MIN_SET_SIZE) {
            setSize = MIN_SET_SIZE;
        }
        if (setSize > MAX_SET_SIZE) {
            setSize = MAX_SET_SIZE;
        }

        FuzzInput fuzzInput = new FuzzInput();
        fuzzInput.fullSet = new ArrayList<>(setSize);
        initializeSet(fuzzInput.fullSet, setSize);
        fuzzInput.relatedMap = createRelatedMap(fuzzInput.fullSet, relatedNum);
        fuzzInput.criticalChanges = createCriticalChanges(fuzzInput.fullSet, criticalNum);
        return fuzzInput;
    }

    public static FuzzInput createFuzzInput() {
        int setSize = RandomUtils.nextInt(MIN_SET_SIZE, MAX_SET_SIZE);
        int maxRelatedNum = max((setSize / 2) - 1, 0);
        int maxCriticalNum = max((setSize / 2) - 1, 0);
        int relatedNum = RandomUtils.nextInt(0, maxRelatedNum);
        int criticalNum = RandomUtils.nextInt(0, maxCriticalNum);

        return createFuzzInput(setSize, relatedNum, criticalNum);
    }


    static void initializeSet(List<Integer> set, int setSize) {
        for (int i = 0; i < setSize; i++) {
            set.add(i);
        }
    }

    static Set<Integer> createCriticalChanges(List<Integer> set, int criticalNum) {
        Set<Integer> criticalChanges = new HashSet<>(criticalNum);
        //Note that! there may be 1 cc(when c1 equals c2) or 2 cc.
        for (int i = 0; i < criticalNum; i++) {
            int c = RandomUtils.nextInt(0, set.size());
            criticalChanges.add(c);
        }
        return criticalChanges;
    }

    static MultiValuedMap<Integer, Integer> createRelatedMap(List<Integer> set, int relatedNum) {
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
                childIndex = RandomUtils.nextInt(0, set.size());
                while (parentIndex == childIndex) {
                    childIndex = RandomUtils.nextInt(0, set.size());
                }
            }
            relatedMap.put(parentIndex, childIndex);
        }
        return relatedMap;
    }

}
