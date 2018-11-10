package com.pcs.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.*;


@Controller
@RequestMapping("/admin")
public class AdminController {

    private String driver = "com.mysql.jdbc.Driver";
    private String sqlUrl = "jdbc:mysql://localhost:3306/flipcardv2";
    private String dbusername = "root";
    private String dbpassword = "moyan";

    private Connection connection = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;

    private static Logger errLog = Logger.getLogger("error-log");


    //0未通过管理员  1通过管理员
    @RequestMapping(value = "/isAdmin", method = RequestMethod.GET)
    @ResponseBody
    String isAdmin(String params) {

        JsonObject wxinfo;
        try {
            wxinfo = new JsonParser().parse(params).getAsJsonObject();
        } catch (Exception e) {
            errLog.error("5101: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("5101", "url参数传递错误", null);
        }
        if (wxinfo.get("wx_openid") == null) {
            errLog.error("5101: wx_openid不存在，params is: " + params);
            return sendRespond("5102", "wx_openid不存在", null);
        }

        JsonObject resData = new JsonObject();

        try {
            //数据库初始化
            Class.forName(driver);
            //校验用户名密码
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);

            String sql = "select status from admin_info where wx_openid = ?";
            String sql2 = "insert into admin_info values (0,0,?,?)";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, wxinfo.get("wx_openid").getAsString());
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                //申请过管理员
                int status = resultSet.getInt("status");
                resData.addProperty("status", status);

            } else {
                //还没申请管理员，先申请
                preparedStatement = connection.prepareStatement(sql2);
                preparedStatement.setString(1, wxinfo.get("wx_openid").getAsString());
                preparedStatement.setString(2, wxinfo.get("wx_nickName").getAsString());
                preparedStatement.executeUpdate();

                resData.addProperty("status", 0);
            }

            resultSet.close();
            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", resData);

        } catch (Exception e) {

            errLog.error("5103: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("5103", e.getMessage(), null);
        }
    }

    //获取待审核用户
    @RequestMapping(value = "/getNotCheckUsers", method = RequestMethod.GET)
    @ResponseBody
    String getNotCheckUsers() {

        JsonObject resData = new JsonObject();

        try {
            //数据库初始化
            Class.forName(driver);
            //校验用户名密码
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);

            String sql = "select userid, date_format(updateTime, '%Y年%m月%d日%H时%i分') as updateTime from user_info " +
                    "where status = 0 order by userid desc ";
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();

            JsonArray users = new JsonArray();
            while (resultSet.next()) {
                JsonObject user = new JsonObject();
                user.addProperty("userid", resultSet.getInt("userid"));
                user.addProperty("updateTime", resultSet.getString("updateTime"));

                users.add(user);
            }

            String sql2 = "select gender, count(*) as count from user_info where status = 0 group by gender";
            preparedStatement = connection.prepareStatement(sql2);
            resultSet = preparedStatement.executeQuery();

            JsonObject userNum = new JsonObject();
            userNum.addProperty("men", 0);
            userNum.addProperty("women", 0);
            while (resultSet.next()) {
                int gender = resultSet.getInt("gender");
                int count = resultSet.getInt("count");
                if (gender == 1) {
                    userNum.addProperty("men", count);
                } else {
                    userNum.addProperty("women", count);
                }
            }
            userNum.addProperty("all", userNum.get("men").getAsInt() + userNum.get("women").getAsInt());

            resData.add("users", users);
            resData.add("userNum", userNum);

            resultSet.close();
            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", resData);

        } catch (Exception e) {
            errLog.error("5201: " + e.getMessage(), e);
            return sendRespond("5201", e.getMessage(), null);
        }
    }

    //获取已通过用户
    @RequestMapping(value = "/getCheckUsers", method = RequestMethod.GET)
    @ResponseBody
    String getCheckUsers() {

        JsonObject resData = new JsonObject();

        try {
            //数据库初始化
            Class.forName(driver);
            //校验用户名密码
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);

            String sql = "select userid, date_format(updateTime, '%Y年%m月%d日%H时%i分') as updateTime from user_info " +
                    "where status = 1 order by userid desc ";
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();

            JsonArray users = new JsonArray();
            while (resultSet.next()) {
                JsonObject user = new JsonObject();
                user.addProperty("userid", resultSet.getInt("userid"));
                user.addProperty("updateTime", resultSet.getString("updateTime"));

                users.add(user);
            }

            String sql2 = "select gender, count(*) as count from user_info where status = 1 group by gender";
            preparedStatement = connection.prepareStatement(sql2);
            resultSet = preparedStatement.executeQuery();

            JsonObject userNum = new JsonObject();
            userNum.addProperty("men", 0);
            userNum.addProperty("women", 0);
            while (resultSet.next()) {
                int gender = resultSet.getInt("gender");
                int count = resultSet.getInt("count");
                if (gender == 1) {
                    userNum.addProperty("men", count);
                } else {
                    userNum.addProperty("women", count);
                }
            }
            userNum.addProperty("all", userNum.get("men").getAsInt() + userNum.get("women").getAsInt());

            resData.add("users", users);
            resData.add("userNum", userNum);


            resultSet.close();
            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", resData);

        } catch (Exception e) {
            errLog.error("5401: " + e.getMessage(), e);
            return sendRespond("5401", e.getMessage(), null);
        }
    }

    //获取审核不通过用户
    @RequestMapping(value = "/getNotPassUsers", method = RequestMethod.GET)
    @ResponseBody
    String getNotPassUsers() {

        JsonObject resData = new JsonObject();

        try {
            //数据库初始化
            Class.forName(driver);
            //校验用户名密码
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);

            String sql = "select userid, date_format(updateTime, '%Y年%m月%d日%H时%i分') as updateTime from user_info " +
                    "where status = 2 order by userid desc ";
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();

            JsonArray users = new JsonArray();
            while (resultSet.next()) {
                JsonObject user = new JsonObject();
                user.addProperty("userid", resultSet.getInt("userid"));
                user.addProperty("updateTime", resultSet.getString("updateTime"));

                users.add(user);
            }

            String sql2 = "select gender, count(*) as count from user_info where status = 2 group by gender";
            preparedStatement = connection.prepareStatement(sql2);
            resultSet = preparedStatement.executeQuery();

            JsonObject userNum = new JsonObject();
            userNum.addProperty("men", 0);
            userNum.addProperty("women", 0);
            while (resultSet.next()) {
                int gender = resultSet.getInt("gender");
                int count = resultSet.getInt("count");
                if (gender == 1) {
                    userNum.addProperty("men", count);
                } else {
                    userNum.addProperty("women", count);
                }
            }
            userNum.addProperty("all", userNum.get("men").getAsInt() + userNum.get("women").getAsInt());

            resData.add("users", users);
            resData.add("userNum", userNum);


            resultSet.close();
            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", resData);

        } catch (Exception e) {
            errLog.error("5501: " + e.getMessage(), e);
            return sendRespond("5501", e.getMessage(), null);
        }
    }

    //审核用户
    @RequestMapping(value = "/checkUser", method = RequestMethod.GET)
    @ResponseBody
    String checkUser(String params) {

        JsonObject user;
        try {
            user = new JsonParser().parse(params).getAsJsonObject();
        } catch (Exception e) {
            errLog.error("5301: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("5301", "url参数传递错误", null);
        }

        try {
            //数据库初始化
            Class.forName(driver);
            //校验用户名密码
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);

            String sql = "update user_info set status = ?, failReason = ? where userid = ?";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, user.get("status").getAsInt());
            preparedStatement.setString(2, user.get("failReason").getAsString());
            preparedStatement.setInt(3, user.get("userid").getAsInt());
            preparedStatement.executeUpdate();

            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", null);

        } catch (Exception e) {
            errLog.error("5302: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("5302", e.getMessage(), null);
        }
    }


    private String sendRespond(String code, String info, JsonObject result) {
        JsonObject res = new JsonObject();
        res.addProperty("code", code);
        res.addProperty("info", info);
        res.add("result", result);
        return res.toString();
    }

}
