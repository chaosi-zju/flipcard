package com.pcs.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.*;
import java.text.SimpleDateFormat;


@Controller
@RequestMapping("/admin")
public class AdminController {

    @Value("${driver}")
    private String driver;
    @Value("${sqlUrl}")
    private String sqlUrl;
    @Value("${dbusername}")
    private String dbusername;
    @Value("${dbpassword}")
    private String dbpassword;

    @Value("${wxAppid}")
    private String wxAppid;
    @Value("${wxAppsecret}")
    private String wxAppsecret;
    @Value("${infoInformTemplateId}")
    private String infoInformTemplateId;

    private Connection connection = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;

    private static final String statusArr[] = {"正在审核中", "已通过", "未通过"};
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

        int userid, status;
        String failReason;

        try {
            JsonObject user = new JsonParser().parse(params).getAsJsonObject();
            userid = user.get("userid").getAsInt();
            status = user.get("status").getAsInt();
            failReason = user.get("failReason").getAsString();
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
            preparedStatement.setInt(1, status);
            preparedStatement.setString(2, failReason);
            preparedStatement.setInt(3, userid);
            preparedStatement.executeUpdate();

            informUserPassed(userid, status, failReason);

            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", null);

        } catch (Exception e) {
            errLog.error("5302: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("5302", e.getMessage(), null);
        }
    }

    private void informUserPassed(int userid, int status, String failReason) {
        JsonObject postParam = new JsonObject();

        try {
            String access_token;

            //获取被审核用户的openid和formId
            String sql = "select wx_openid, infoFormId from wx_info inner join user_info on " +
                    "wx_info.userid = ? && user_info.userid = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, userid);
            preparedStatement.setInt(2, userid);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                //封装postParam基本信息
                postParam.addProperty("touser", resultSet.getString("wx_openid"));
                postParam.addProperty("template_id", infoInformTemplateId);
                postParam.addProperty("page", "pages/index/index");
                postParam.addProperty("form_id", resultSet.getString("infoFormId"));

                //封装postParam的keywords
                JsonObject data = new JsonObject();

                JsonObject keyword1 = new JsonObject();
                keyword1.addProperty("value", "您的资料审核" + statusArr[status]);
                data.add("keyword1", keyword1);

                if (status == 2) {
                    JsonObject keyword2 = new JsonObject();
                    keyword2.addProperty("value", failReason);
                    data.add("keyword2", keyword2);
                }

                JsonObject keyword3 = new JsonObject();
                keyword3.addProperty("value", "脉言——沙脉科技有限公司");
                data.add("keyword3", keyword3);

                JsonObject keyword4 = new JsonObject();
                if (status == 1) {
                    keyword4.addProperty("value", "恭喜您的资料审核通过，马上开始您的翻牌子之旅吧！");
                } else {
                    keyword4.addProperty("value", "您离成功只差一点点啦，资料稍稍更新一下就可以一起来翻牌子啦！");
                }
                data.add("keyword4", keyword4);
                postParam.add("data", data);

            } else {
                errLog.error("5304: 没有获取到被审核用户的wx_openid和infoFormId，用户id为：" + userid);
                throw new Exception("没有获取到被审核用户的wx_openid和infoFormId");
            }

            OkHttpClient client = new OkHttpClient();

            //获取小程序token
            String url1 = "https://api.weixin.qq.com/cgi-bin/token?" +
                    "grant_type=client_credential&appid=" + wxAppid + "&secret=" + wxAppsecret;
            Request req1 = new Request.Builder().url(url1).build();
            Response res1 = client.newCall(req1).execute();

            if (res1.isSuccessful()) {
                JsonObject data = new JsonParser().parse(res1.body().string()).getAsJsonObject();
                access_token = data.get("access_token").getAsString();

                String url2 = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?" +
                        "access_token=" + access_token;
                MediaType mediaType = MediaType.parse("application/json");
                RequestBody requestBody = RequestBody.create(mediaType, postParam.toString());
                Request req2 = new Request.Builder().post(requestBody).url(url2).build();
                Response res2 = client.newCall(req2).execute();

                if (!res2.isSuccessful()) {
                    throw new Exception("发送模板消息 request not successful");
                } else {
                    JsonObject jo = new JsonParser().parse(res2.body().string()).getAsJsonObject();
                    int errcode = jo.get("errcode").getAsInt();
                    if (errcode != 0 && errcode != 40003 && errcode != 41028 && errcode != 41029) {
                        errLog.error("5304: 发送模板消息失败，返回值:" + jo.toString() + "，postParam:" + postParam.toString());
                    }
                }
            } else {
                throw new Exception("获取小程序token request not successful");
            }
        } catch (Exception e) {
            errLog.error("5303: " + e.getMessage() + "，postParam is: " + postParam.toString(), e);
        }
    }

    //删除用户
    @RequestMapping(value = "/deleteUser", method = RequestMethod.GET)
    @ResponseBody
    String deleteUser(String userid) {

        int uid;
        try {
            uid = Integer.parseInt(userid);
        } catch (Exception e) {
            errLog.error("5601: " + e.getMessage() + "，userid is: " + userid, e);
            return sendRespond("5601", "url参数传递错误", null);
        }

        try {
            //数据库初始化
            Class.forName(driver);
            //校验用户名密码
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);
            connection.setAutoCommit(false);//开启事务

            String sql0 = "insert into quit_user select * from user_info where userid = ?";
            String sql = "delete from user_info where userid = ?";
            String sql2 = "update wx_info set userid = -1 where userid = ?";
            String sql3 = "delete from timerelied_info where userid = ?";
            String sql4 = "delete from relation_info where sendId = ? or recvId = ?";

            preparedStatement = connection.prepareStatement(sql0);
            preparedStatement.setInt(1, uid);
            preparedStatement.executeUpdate();

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, uid);
            preparedStatement.executeUpdate();

            preparedStatement = connection.prepareStatement(sql2);
            preparedStatement.setInt(1, uid);
            preparedStatement.executeUpdate();

            preparedStatement = connection.prepareStatement(sql3);
            preparedStatement.setInt(1, uid);
            preparedStatement.executeUpdate();

            preparedStatement = connection.prepareStatement(sql4);
            preparedStatement.setInt(1, uid);
            preparedStatement.setInt(2, uid);
            preparedStatement.executeUpdate();

            connection.commit();
            connection.setAutoCommit(true);

            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", null);

        } catch (Exception e) {
            try {
                if (connection != null) {
                    connection.rollback();
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e1) {
                errLog.error("5603: " + e1.getMessage() + "，userid is: " + userid, e1);
                return sendRespond("5603", e1.getMessage(), null);
            }
            errLog.error("5602: " + e.getMessage() + "，userid is: " + userid, e);
            return sendRespond("5602", e.getMessage(), null);
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
