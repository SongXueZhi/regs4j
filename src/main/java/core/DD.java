package core;

import model.DDOutput;
import model.HunkEntity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author lsn
 * @date 2023/11/21 6:33 PM
 */
public class DD {
    String filename = "detail" + new SimpleDateFormat("_yyyyMMdd").format(new Date());
    File file = new File(filename);
    BufferedWriter bw;

    public DD() throws IOException {
        if (file.exists()) {
            // 如果文件存在，则追加内容到已有文件中
            bw = new BufferedWriter(new FileWriter(file, true));
        } else {
            // 如果文件不存在，则创建新文件并构建 BufferedWriter
            bw = new BufferedWriter(new FileWriter(file));
        }
    }

    public DDOutput ddmin(String path, List<HunkEntity> hunkEntities) throws IOException {
        return null;
    }
    public DDOutput ProbDD(String path, List<HunkEntity> hunkEntities) throws IOException {
        return null;
    }
}
