package experiments.dd_experiment.exper.models;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.*;


/**
 * @Author: sxz
 * @Date: 2022/10/20/15:39
 * @Description:
 */
public class ModelManager {
    final static String OUT_PATH = "output";
    final static String SUFFIX = ".ser";
    final static String DATE_PATTERN = "yyyy-MM-dd-HH-mm-ss";

    public void saveModel(DDModel model) throws IOException {
        try (FileOutputStream fileOut =
                     new FileOutputStream(OUT_PATH + File.separator + DateFormatUtils.format(System.currentTimeMillis(), DATE_PATTERN) + SUFFIX);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(model);
        }
    }

    public DDModel loadModel(String path) {
        try (FileInputStream fileIn = new FileInputStream(path);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            return (DDModel) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
