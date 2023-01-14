/*
 *
 *  * Copyright 2021 SongXueZhi
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package core;

import model.HunkEntity;
import model.Regression;
import model.Revision;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static core.Configs.*;
import static utils.FileUtilx.readListFromFile;

public class MysqlManager {

    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private static Connection conn = null;
    private static Statement statement = null;

    public static void main(String[] args) throws Exception {
        List<String> uuid = readListFromFile("uuid.txt");
        for (String id: uuid) {
            System.out.println(id);
            countCC("bic",id,"ProbDD");
            countCC("bic",id,"ddmin");
        }
    }

    private static void getConn() throws Exception {
        if (conn != null) {
            return;
        }
        Class.forName(DRIVER);
        conn = DriverManager.getConnection(URL, NAME, PWD);

    }

    public static void getStatement() throws Exception {
        if (conn == null) {
            getConn();
        }
        if (statement != null) {
            return;
        }
        statement = conn.createStatement();
    }

    public static void closed() {
        try {
            if (statement != null) {
                statement.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {

        } finally {
            try {
                if (statement != null) {
                    statement.close();
                    statement = null;
                }
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (Exception e) {
            }
        }
    }

    public static void executeUpdate(String sql) {
        try {
            getStatement();
            statement.executeUpdate(sql);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closed();
        }
    }

    public static List<Regression> selectRegressions(String sql) {
        List<Regression> regressionList = new ArrayList<>();
        try {
            getStatement();
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                Regression regression = new Regression();
                regression.setRfc(new Revision(rs.getString("bfc"), "rfc"));
                regression.setBuggy(new Revision(rs.getString("buggy"), "buggy"));
                regression.setRic(new Revision(rs.getString("bic"), "ric"));
                regression.setWork(new Revision(rs.getString("work"), "work"));
                regression.setTestCase(rs.getString("testcase").split(";")[0]);
                regressionList.add(regression);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closed();
        }
        return regressionList;
    }

    public static List<Regression> getRegressions(String sql) {
        List<Regression> regressionList = new ArrayList<>();
        try {
            getStatement();
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                Regression regression = new Regression();
                regression.setId(rs.getString("regression_uuid"));
                regression.setRfc(new Revision(rs.getString("bfc"), "rfc"));
                regression.setBuggy(new Revision(rs.getString("buggy"), "buggy"));
                regression.setRic(new Revision(rs.getString("bic"), "ric"));
                regression.setWork(new Revision(rs.getString("work"), "work"));
                regression.setTestCase(rs.getString("testcase").split(";")[0]);
                regression.setProjectFullName(rs.getString("project_full_name"));
                regression.setErrorType(rs.getString("error_type"));
                regressionList.add(regression);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closed();
        }
        return regressionList;
    }

    public static List<Regression> getRegressionsWithoutError(String sql) {
        List<Regression> regressionList = new ArrayList<>();
        try {
            getStatement();
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                Regression regression = new Regression();
                regression.setId(rs.getString("regression_uuid"));
                regression.setRfc(new Revision(rs.getString("bfc"), "rfc"));
                regression.setBuggy(new Revision(rs.getString("buggy"), "buggy"));
                regression.setRic(new Revision(rs.getString("bic"), "ric"));
                regression.setWork(new Revision(rs.getString("work"), "work"));
                regression.setTestCase(rs.getString("testcase").split(";")[0]);
                regression.setProjectFullName(rs.getString("project_full_name"));
                regressionList.add(regression);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closed();
        }
        return regressionList;
    }

    public static void insertCC(String revision_name, String regression_uuid, String tool, List<HunkEntity> hunks) throws Exception {
        if (conn == null) {
            getConn();
        }
        PreparedStatement pstmt = null;
        try {
            for(HunkEntity hunk: hunks) {
                pstmt = conn.prepareStatement("insert IGNORE into critical_change_dd(revision_name, regression_uuid, " +
                        "new_path, old_path, " +
                        "beginA, beginB, endA, endB, type, tool) values(?,?,?,?,?,?,?,?,?,?)");
                pstmt.setString(1, revision_name);
                pstmt.setString(2, regression_uuid);
                pstmt.setString(3, hunk.getNewPath());
                pstmt.setString(4, hunk.getOldPath());
                pstmt.setInt(5, hunk.getBeginA());
                pstmt.setInt(6, hunk.getBeginB());
                pstmt.setInt(7, hunk.getEndA());
                pstmt.setInt(8, hunk.getEndB());
                pstmt.setString(9, hunk.getType().toString());
                pstmt.setString(10, tool);
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (pstmt!=null){
                pstmt.close();
            }
        }
    }

    public static List<HunkEntity> selectCC(String revision_name, String regression_uuid, String tool) throws Exception{
        List<HunkEntity> hunkList = new ArrayList<>();
        if (conn == null) {
            getConn();
        }
        PreparedStatement pstmt = null;
        try {
            getStatement();
            ResultSet rs = statement.executeQuery("select count(*) from critical_change_dd where revision_name='" + revision_name +
                    "'and regression_uuid='" + regression_uuid +"'" + "and tool='" + tool +"'");
            while (rs.next()){
                HunkEntity hunkEntity = new HunkEntity();
                hunkEntity.setType(rs.getString("type"));
                hunkEntity.setOldPath(rs.getString("old_path"));
                hunkEntity.setNewPath(rs.getString("new_path"));
                hunkEntity.setBeginA(rs.getInt("beginA"));
                hunkEntity.setBeginB(rs.getInt("beginB"));
                hunkEntity.setEndA(rs.getInt("endA"));
                hunkEntity.setEndB(rs.getInt("endB"));
                hunkList.add(hunkEntity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (pstmt!=null){
                pstmt.close();
            }
        }
        return hunkList;
    }

    public static void countCC(String revision_name, String regression_uuid, String tool) throws Exception{
        if (conn == null) {
            getConn();
        }
        PreparedStatement pstmt = null;
        try {
            getStatement();
            ResultSet rs = statement.executeQuery("select count(*) from critical_change_dd where revision_name='" + revision_name +
                    "'and regression_uuid='" + regression_uuid +"'" + "and tool='" + tool +"'");
            while (rs.next()) {
                System.out.println(rs.getInt(1));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (pstmt!=null){
                pstmt.close();
            }
        }
    }

    public static void  insertDD(String uuid,String revision,String cc_ddmin,String cc_ddj) throws Exception {
        if (conn == null) {
            getConn();
        }
        PreparedStatement pstmt = null;
        try {
            pstmt =conn.prepareStatement("insert into dd(regressionId,revision," +
                    "cc_ddmin,cc_ddj) values(?," +
                    "?,?,?)");
            pstmt.setString(1,uuid);
            pstmt.setString(2,revision);
            pstmt.setString(3,cc_ddmin);
            pstmt.setString(4,cc_ddj);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            if (pstmt!=null){
                pstmt.close();
            }
        }
    }
    
    public static List<String> selectProjects(String sql) {
    	List<String> result = new ArrayList<>();
    	try {
    		getStatement();
    		ResultSet rs = statement.executeQuery(sql);
    		while (rs.next()) {
    			result.add(rs.getString("project_full_name"));
    		}
    	} catch (Exception e) {
            e.printStackTrace();
        } finally {
            closed();
        }
    	return result;
    }
}
