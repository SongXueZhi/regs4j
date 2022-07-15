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

import model.Regression;
import model.Revision;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static core.Configs.*;

public class MysqlManager {

    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private static Connection conn = null;
    private static Statement statement = null;

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
