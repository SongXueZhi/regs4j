package experiments.dd_experiment.exper.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: sxz
 * @Date: 2022/10/20/17:05
 * @Description:
 */
public class StepLine implements Serializable {
    List<Integer> subset = new ArrayList<>();
    List<Double> c_pro = new ArrayList<>();
    int status;

    public List<Integer> getSubset() {
        return subset;
    }

    public void setSubset(List<Integer> subset) {
        this.subset = subset;
    }

    public List<Double> getC_pro() {
        return c_pro;
    }

    public void setC_pro(List<Double> c_pro) {
        this.c_pro = c_pro;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
