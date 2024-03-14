package experiments.dd_experiment;

import experiments.dd_experiment.internal.DDOutput;

import java.util.List;

public class DDOutputWithLoop extends DDOutput {

    public int loop;

    public DDOutputWithLoop(List<Integer> resultIndexList) {
        super(resultIndexList);
    }
}
