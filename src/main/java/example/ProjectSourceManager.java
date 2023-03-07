package example;

import core.MysqlManager;
import core.git.GitUtils;
import model.Regression;
import model.RegressionDO;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @Author: sxz
 * @Date: 2023/02/24/15:28
 * @Description:
 */
public class ProjectSourceManager {
    //  使用95 服务器中的数据库，将regressions_all中的数据填进regression

    final static String DIR = "/Users/sxz/miner_space/meta_projects";
    public static void main(String[] args) {
        // 首先查询出99中的regression
        String sql = "select * from regressions_all";
        List<RegressionDO>  regressionDOS = MysqlManager.selectRegressionDO(sql);
        System.out.println(regressionDOS.size());
        //处理每一个案例
        File target=null;
        File newTarget=null;
        String insertSQl="";
        Map<String,String> projMap= new HashMap<>();
        for (RegressionDO regressionDO : regressionDOS){
            //生成projectUUID
            String projectUUID ="";
            if (!projMap.containsKey(regressionDO.getProjectFullName())){
                projectUUID = UUID.randomUUID().toString();
                projMap.put(regressionDO.getProjectFullName(),projectUUID);

                //对项目目录进行重命名
                target =  new File(DIR+File.separator+regressionDO.getProjectFullName());

                if (target!=null && target.isDirectory()){
                    newTarget = new File(DIR+File.separator+projectUUID);
                    target.renameTo(newTarget);
                }
                insertSQl =
                        "insert into project_name(uuid,project_full_name) values ('"+projectUUID+"','"+regressionDO.getProjectFullName().replace("_","/")+"')";
                MysqlManager.executeUpdate(insertSQl);
            }else {
                projectUUID = projMap.get(regressionDO.getProjectFullName());
                newTarget = new File(DIR+File.separator+projectUUID);
            }
            String regressionUUID = UUID.randomUUID().toString();

            //获取buggy版本的的hash
            String buggyHash = GitUtils.getBuggyIDByBfc1(regressionDO.getRfc()+"~1",newTarget);
            //插入到数据库表


            //将更新的数据插入到regression表
            insertSQl = "insert into regression (regression_uuid,project_uuid,project_full_name,bfc,buggy,bic,work," +
                    "testcase) values ('"+regressionUUID+"',+'"+projectUUID+"','"+regressionDO.getProjectFullName().replace("_","/")+"','"+regressionDO.getRfc()+"','"+buggyHash+"','"+regressionDO.getRic()+"','"+regressionDO.getWork()+"','"+regressionDO.getTestCase()+"')";
            MysqlManager.executeUpdate(insertSQl);

        }
    }
}
