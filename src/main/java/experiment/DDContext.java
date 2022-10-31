package experiment;

import experiment.internal.DDOutput;
import experiment.internal.DeltaDebugging;

import java.util.*;

/**
 * @Author: sxz
 * @Date: 2022/10/21/16:07
 * @Description:
 */
public class DDContext {

    private List<DeltaDebugging> ddStrategyList = new ArrayList<>();

    public DDContext addDDStrategy(DeltaDebugging... dd){
        ddStrategyList.addAll(Arrays.asList(dd));
        return this;
    }

    public Map<String, DDOutput> start() {
        Map<String, DDOutput> ddOutputMap = new HashMap<>();
        for (DeltaDebugging dd : ddStrategyList){
//            System.out.println("\n--------" + dd.getClass().getName() + "--------");
           ddOutputMap.put(dd.getClass().getName(),dd.run());
        }
       return ddOutputMap;
    }
}
