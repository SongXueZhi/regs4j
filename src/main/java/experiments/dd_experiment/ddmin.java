package experiments.dd_experiment;

import experiments.dd_experiment.internal.DDInput;
import experiments.dd_experiment.internal.DeltaDebugging;
import experiments.dd_experiment.internal.TestRunner;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author lsn
 * @date 2022/11/28 2:54 PM
 */
public class ddmin implements DeltaDebugging {

    DDInput ddInput;
    TestRunner testRunner;

    public ddmin(DDInput ddInput, TestRunner testRunner){
        this.ddInput = ddInput;
        this.testRunner = testRunner;
    }
    @Override
    public DDOutputWithLoop run() {
        List<Integer> retSet = new ArrayList<>(ddInput.fullSet);
        int loop = 0;
        int n = 2;
        while(retSet.size() >= 2){
            int start = 0;
            int subset_length = retSet.size() / n;
            boolean some_complement_is_failing = false;
            while (start < retSet.size()){
                loop += 1;
                List<Integer> complement = new ArrayList<>();
                for(int i = 0; i < retSet.size();i++ ){
                    if(i < start || i >= start + subset_length) {
                        complement.add(retSet.get(i));
                    }
                }
                TestRunner.status result = testRunner.getResult(complement,ddInput);

                System.out.println(loop + ": test: " + complement + " : " + result );

                if (result == TestRunner.status.PASS){
                    retSet = complement;
                    n = max(n - 1, 2);
                    some_complement_is_failing = true;
                    break;
                }
                start += subset_length;
            }
            if(!some_complement_is_failing){
                if (n == retSet.size()){
                    break;
                }
                n = min(n * 2, retSet.size());
            }
        }
        DDOutputWithLoop ddOutputWithLoop = new DDOutputWithLoop(retSet);
        ddOutputWithLoop.loop = loop;
        return ddOutputWithLoop;    }
}
