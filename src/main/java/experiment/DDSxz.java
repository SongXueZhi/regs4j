package experiment;

import experiment.internal.DDOutput;
import experiment.internal.TestRunner;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.RandomUtils;
import utils.DDUtil;
import utils.FuzzUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @Author: sxz
 * @Date: 2023/03/08/10:20
 * @Description:
 */
public class DDSxz {

    public static void main(String[] args) {
        int lm =0;
        int sumLoop=0;
        int sumCE=0;
//        for(int i = 0; i < 100; i++) {
//            System.out.println(i);
            DDContext ddContext = new DDContext();
//            int setSize = RandomUtils.nextInt(8, 150);
//            int relatedNum = RandomUtils.nextInt(2, (setSize / 2) + 1);
//            int criticalNum = RandomUtils.nextInt(1, 20);
//            FuzzInput fuzzInput = FuzzUtil.createFuzzInput(setSize, relatedNum, criticalNum);
//            List<Integer> cc = new ArrayList<Integer>(fuzzInput.criticalChanges);
//            DDUtil.getTestSetWithDependency(cc, fuzzInput.relatedMap);
//            MultiValuedMap smellMap = getSmellInput(fuzzInput.fullSet,(int)(relatedNum*.03));
            FuzzInput fuzzInput = getFuzzInputMan();
            List<Integer> cc = new ArrayList<Integer>(fuzzInput.criticalChanges);
            DDUtil.getTestSetWithDependency(cc, fuzzInput.relatedMap);
            MultiValuedMap smellMap =getSmellInputMan();
//            System.out.println("\n" + " dd input:");
//            System.out.println("fullSet " + fuzzInput.fullSet.size() + " " + fuzzInput.fullSet);
//            System.out.println("relatedMap " + fuzzInput.relatedMap.size() + " " + fuzzInput.relatedMap);
//            System.out.println("smellMap " + smellMap.size() + " " + smellMap);
//            System.out.println("criticalChanges " + fuzzInput.criticalChanges.size() + " " + fuzzInput.criticalChanges);
//
//            System.out.println("cc " + cc.size() + " " + cc);

            Map<String, DDOutput> ddOutputHashMap = ddContext.addDDStrategy(
                            new ProDDSxz(fuzzInput, smellMap)
                    )
                    .start();
            DDOutputWithLoop ProDDPlusMOut = (DDOutputWithLoop) ddOutputHashMap.get(ProDDSxz.class.getName());

            System.out.println("\n" + " dd input:");
            System.out.println("fullSet " + fuzzInput.fullSet.size() + " " + fuzzInput.fullSet);
            System.out.println("relatedMap " + fuzzInput.relatedMap.size() + " " + fuzzInput.relatedMap);
            System.out.println("smellMap " + smellMap.size() + " " + smellMap);
            System.out.println("criticalChanges " + fuzzInput.criticalChanges.size() + " " + fuzzInput.criticalChanges);
            System.out.println("cc " + cc.size() + " " + cc);

            if (ProDDPlusMOut.resultIndexList.size()>cc.size()){
                lm++;
            }
            sumLoop+= ProDDPlusMOut.loop;;
            sumCE+=ProDDPlusMOut.CE;
        }
//        System.out.println(lm+"----->"+sumLoop+"----->"+sumCE);
//    }
   static MultiValuedMap<Integer, Integer> getSmellInput(List<Integer> set, int relatedNum){
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

   static  FuzzInput getFuzzInputMan(){
        FuzzInput fuzzInput = new FuzzInput();
        fuzzInput.relatedMap = new ArrayListValuedHashMap<>();
        fuzzInput.relatedMap.put(0,2);
        fuzzInput.relatedMap.put(2,3);
        fuzzInput.relatedMap.put(3,6);
        fuzzInput.fullSet = new ArrayList<>();
        for (int i =0; i<=7;i++){
            fuzzInput.fullSet.add(i);
        }
        fuzzInput.criticalChanges =new HashSet<>();
       fuzzInput.criticalChanges.add(3);
       fuzzInput.criticalChanges.add(5);
        return fuzzInput;
   }
   static   MultiValuedMap<Integer,Integer> getSmellInputMan(){
        MultiValuedMap<Integer,Integer> map = new ArrayListValuedHashMap<>();
        map.put(5,0);
        map.put(5,7);
        return map;
   }
}
