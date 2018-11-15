package com.pcs.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.*;


@Controller
@RequestMapping("/cardinfo")
public class CardController {

    @Value("${driver}") private String driver;
    @Value("${sqlUrl}") private String sqlUrl;
    @Value("${dbusername}") private String dbusername;
    @Value("${dbpassword}") private String dbpassword;

    private Connection connection = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;

    private static Logger errLog = Logger.getLogger("error-log");


    //获取收到的卡片
    @RequestMapping(value = "/getReceivedCard", method = RequestMethod.GET)
    @ResponseBody
    String getReceivedCard(String userid) {

        if (userid == null || userid.equals("") || userid.equals("undefined") || userid.equals("null")) {
            errLog.error("1101: url参数传递错误，params is: " + userid);
            return sendRespond("1101", "url参数传递错误", null);
        }

        JsonObject resData = new JsonObject();

        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);


            String sql = "select sendId from relation_info where recvId = ? and status = 0";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, Integer.parseInt(userid));
            resultSet = preparedStatement.executeQuery();

            JsonArray arr = new JsonArray();
            JsonParser parser = new JsonParser();
            while (resultSet.next()) {
                arr.add(parser.parse(resultSet.getObject(1).toString()));
            }
            resData.add("sendIds", arr);

            resultSet.close();
            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", resData);

        } catch (Exception e) {
            errLog.error("1102: " + e.getMessage() + "，params is: " + userid, e);
            return sendRespond("1102", e.getMessage(), null);
        }

    }

    //{sendId: 4, recvId:1}
    //删除卡片
    @RequestMapping(value = "/deleteCard", method = RequestMethod.GET)
    @ResponseBody
    String deleteCard(String params) {

        JsonObject userids;
        try {
            userids = new JsonParser().parse(params).getAsJsonObject();
        } catch (Exception e) {
            errLog.error("1201: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("1201", "url参数传递错误", null);
        }

        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);


            String sql = "update relation_info set status = 2 where sendId = ? and recvId = ?";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, userids.get("sendId").getAsInt());
            preparedStatement.setInt(2, userids.get("recvId").getAsInt());
            preparedStatement.executeUpdate();

            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", null);

        } catch (Exception e) {
            errLog.error("1202: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("1202", e.getMessage(), null);
        }

    }

    //{sendId: 3, recvId:1}
    //同意卡片
    @RequestMapping(value = "/agreeCard", method = RequestMethod.GET)
    @ResponseBody
    String agreeCard(String params) {

        JsonObject userids;
        try {
            userids = new JsonParser().parse(params).getAsJsonObject();
        } catch (Exception e) {
            errLog.error("1301: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("1301", "url参数传递错误", null);
        }

        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);


            String sql = "update relation_info set status = 1 where sendId = ? and recvId = ?";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, userids.get("sendId").getAsInt());
            preparedStatement.setInt(2, userids.get("recvId").getAsInt());
            preparedStatement.executeUpdate();


            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", null);

        } catch (Exception e) {
            errLog.error("1302: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("1302", e.getMessage(), null);
        }

    }

    //获取结成的好友
    @RequestMapping(value = "/getFriends", method = RequestMethod.GET)
    @ResponseBody
    String getFriends(String userid) {

        if (userid == null || userid.equals("") || userid.equals("undefined") || userid.equals("null")) {
            errLog.error("1401: url参数传递错误，params is: " + userid);
            return sendRespond("1401", "url参数传递错误", null);
        }
        JsonObject resData = new JsonObject();

        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);


            String sql = "select sendId as friendId from relation_info where recvId = ? and status = 1 " +
                    "union " +
                    "select recvId as friendId from relation_info where sendId = ? and status = 1";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, Integer.parseInt(userid));
            preparedStatement.setInt(2, Integer.parseInt(userid));
            resultSet = preparedStatement.executeQuery();

            JsonArray arr = new JsonArray();
            JsonParser parser = new JsonParser();
            while (resultSet.next()) {
                arr.add(parser.parse(resultSet.getObject(1).toString()));
            }
            resData.add("friendIds", arr);

            resultSet.close();
            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", resData);

        } catch (Exception e) {
            errLog.error("1402: " + e.getMessage() + "，params is: " + userid, e);
            return sendRespond("1402", e.getMessage(), null);
        }

    }

    //{sendId:1, recvId:2}
    //发送卡片
    @RequestMapping(value = "/sendCard", method = RequestMethod.GET)
    @ResponseBody
    String sendCard(String params) {

        JsonObject userids;
        try {
            userids = new JsonParser().parse(params).getAsJsonObject();
        } catch (Exception e) {
            errLog.error("1501: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("1501", "url参数传递错误", null);
        }

        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);


            String sql = "insert into relation_info values (0,?,?,0)";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, userids.get("sendId").getAsInt());
            preparedStatement.setInt(2, userids.get("recvId").getAsInt());
            preparedStatement.executeUpdate();

            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", null);

        } catch (Exception e) {
            errLog.error("1502: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("1502", e.getMessage(), null);
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
