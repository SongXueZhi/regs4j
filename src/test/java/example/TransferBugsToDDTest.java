package example;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @Author: sxz
 * @Date: 2022/07/14/21:16
 * @Description:
 */
public class TransferBugsToDDTest {

    @Test
    public  void checkout() throws Exception{
        TransferBugsToDD.checkout("uniVocity/univocity-parsers");
    }

}