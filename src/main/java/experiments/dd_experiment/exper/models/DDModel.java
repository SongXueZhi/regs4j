package experiments.dd_experiment.exper.models;

import experiments.dd_experiment.exper.fuzz.FuzzInput;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: sxz
 * @Date: 2022/10/20/15:43
 * @Description:
 */
public class DDModel implements Serializable {
    FuzzInput fuzzInput;
    List<Integer> result = new ArrayList<>();
    int[][] relatedMatrix;
    List<StepLine> stepLines = new ArrayList<>();

    public FuzzInput getFuzzInput() {
        return fuzzInput;
    }

    public void setFuzzInput(FuzzInput fuzzInput) {
        this.fuzzInput = fuzzInput;
    }

    public List<Integer> getResult() {
        return result;
    }

    public void setResult(List<Integer> result) {
        this.result = result;
    }

    public int[][] getRelatedMatrix() {
        return relatedMatrix;
    }

    public void setRelatedMatrix(int[][] relatedMatrix) {
        this.relatedMatrix = relatedMatrix;
    }

    public List<StepLine> getStepLines() {
        return stepLines;
    }

    public void setStepLines(List<StepLine> stepLines) {
        this.stepLines = stepLines;
    }
}
