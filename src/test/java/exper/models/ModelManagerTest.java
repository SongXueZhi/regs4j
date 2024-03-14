package exper.models;

import experiments.dd_experiment.exper.models.DDModel;
import experiments.dd_experiment.exper.models.ModelManager;
import org.junit.Test;

import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;

/**
 * @Author: sxz
 * @Date: 2022/10/20/19:09
 * @Description:
 */
public class ModelManagerTest {

    @Test
    public void loadModel() throws FileNotFoundException {
        ModelManager modelManager = new ModelManager();
        DDModel ddModel = modelManager.loadModel("output/2022-10-20-21-46-02.ser");
        assertEquals(ddModel.fuzzInput.relatedMap.size(),2);
    }
}
