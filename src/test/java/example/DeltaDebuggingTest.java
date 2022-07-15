package example;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @Author: sxz
 * @Date: 2022/07/14/23:42
 * @Description:
 */
public class DeltaDebuggingTest {

    @Test
    public void getDDMinResult() throws IOException {
        System.out.println(DeltaDebugging.getDDMinResult(new File("/Users/sxz/reg_space/uniVocity_univocity-parsers")
                ,"1b",
                "1g"));
    }

    @Test
    public void getDDJResult() throws IOException {
        System.out.println(DeltaDebugging.getDDJResult(new File("/Users/sxz/reg_space/logback_ddj")));
    }

    @Test
    public void cleanCache() throws IOException {
        DeltaDebugging.cleanCache(new File("/Users/sxz/reg_space/logback_ddj"));
    }
}