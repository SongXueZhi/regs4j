package experiment;

import experiment.internal.DDOutput;

import java.util.List;

public class DDOutputWithLoop extends DDOutput {

    public int loop;

    public DDOutputWithLoop(List<Integer> resultIndexList) {
        super(resultIndexList);
    }
}
