package core;

import java.io.File;

public class Configs {
    //    public   final  static  String workSpace= System.getProperty("user.home") + "/Documents/miner-space";
    public final static String workSpace = System.getProperty("user.home") + File.separator + "reg_space";;
    public final static String URL = "jdbc:mysql://10.177.21.179:3306/code_annotation?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF8";
    public final static String NAME = "root";
    public final static String PWD = "123456";
}
