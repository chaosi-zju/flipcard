package com.pcs.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.*;


@Controller
@RequestMapping("/cardinfo")
public class CardController {

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
    @Value("${cardInformTemplateId}")
    private String cardInformTemplateId;

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


            String sql = "select sendId from relation_info where recvId = ? and status = 0 order by updateTime desc";

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

            String sql = "update relation_info set status = 2, updateTime = now() where sendId = ? and recvId = ?";

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


            String sql = "update relation_info set status = 1, updateTime = now() where sendId = ? and recvId = ?";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, userids.get("sendId").getAsInt());
            preparedStatement.setInt(2, userids.get("recvId").getAsInt());
            preparedStatement.executeUpdate();

            informRecvWx(userids.get("sendId").getAsInt());

            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", null);

        } catch (Exception e) {
            errLog.error("1302: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("1302", e.getMessage(), null);
        }

    }

    //通知用户收到新微信
    private void informRecvWx(int userid) {
        JsonObject postParam = new JsonObject();

        try {

            //获取被审核用户的openid和formId
            String sql = "select wx_openid, cardFormId, gender from wx_info inner join user_info on " +
                    "wx_info.userid = ? && user_info.userid = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, userid);
            preparedStatement.setInt(2, userid);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int gender = resultSet.getInt("gender");
                //封装postParam基本信息
                postParam.addProperty("touser", resultSet.getString("wx_openid"));
                postParam.addProperty("template_id", cardInformTemplateId);
                postParam.addProperty("page", "/pages/index/index");
                postParam.addProperty("form_id", resultSet.getString("cardFormId"));

                //封装postParam的keywords
                JsonObject data = new JsonObject();

                JsonObject keyword1 = new JsonObject();
                keyword1.addProperty("value", "卡片被同意提醒");
                data.add("keyword1", keyword1);

                JsonObject keyword2 = new JsonObject();
                String tmp1[] = {"小姐姐", "小哥哥"};
                String tmp2[] = {"她", "他"};
                keyword2.addProperty("value", "您给某位" + tmp1[gender - 1] + "发送的卡片被对方同意啦！看来" +
                        tmp2[gender - 1] + "也很中意您噢！快去看看" + tmp2[gender - 1] + "是谁吧！说不定就是命中注定的" + tmp2[gender - 1] + "喔～");
                data.add("keyword2", keyword2);

                JsonObject keyword3 = new JsonObject();
                keyword3.addProperty("value", "脉言——沙脉科技有限公司");
                data.add("keyword3", keyword3);

                postParam.add("data", data);

            } else {
                errLog.error("1304: 没有获取到被审核用户的wx_openid和infoFormId，用户id为：" + userid);
                throw new Exception("没有获取到被审核用户的wx_openid和infoFormId");
            }
            sendTemplateMsg(postParam);

            resultSet.close();

        } catch (Exception e) {
            errLog.error("1303: " + e.getMessage() + "，postParam is: " + postParam.toString(), e);
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


            String sql = "select friendId from " +
                    "(select sendId as friendId, updateTime from relation_info where recvId = ? and status = 1 " +
                    "union " +
                    "select recvId as friendId, updateTime from relation_info where sendId = ? and status = 1) " +
                    "as newtable order by updateTime desc";

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


            String sql = "insert into relation_info values (0,?,?,0,now())";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, userids.get("sendId").getAsInt());
            preparedStatement.setInt(2, userids.get("recvId").getAsInt());
            preparedStatement.executeUpdate();

            informRecvCard(userids.get("recvId").getAsInt());

            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", null);

        } catch (Exception e) {
            errLog.error("1502: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("1502", e.getMessage(), null);
        }

    }

    //通知用户收到卡片
    private void informRecvCard(int userid) {
        JsonObject postParam = new JsonObject();

        try {

            //获取被审核用户的openid和formId
            String sql = "select wx_openid, cardFormId, gender from wx_info inner join user_info on " +
                    "wx_info.userid = ? && user_info.userid = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, userid);
            preparedStatement.setInt(2, userid);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int gender = resultSet.getInt("gender");
                //封装postParam基本信息
                postParam.addProperty("touser", resultSet.getString("wx_openid"));
                postParam.addProperty("template_id", cardInformTemplateId);
                postParam.addProperty("page", "/pages/index/index");
                postParam.addProperty("form_id", resultSet.getString("cardFormId"));

                //封装postParam的keywords
                JsonObject data = new JsonObject();

                JsonObject keyword1 = new JsonObject();
                keyword1.addProperty("value", "收到新卡片提醒");
                data.add("keyword1", keyword1);

                JsonObject keyword2 = new JsonObject();
                String tmp1[] = {"小姐姐", "小哥哥"};
                String tmp2[] = {"她", "他"};
                keyword2.addProperty("value", "有位" + tmp1[gender - 1] + "翻开了您的卡片，觉得您很不错噢，" +
                        "并向您发送了" + tmp2[gender - 1] + "的卡片！快去看看" + tmp2[gender - 1] + "是谁吧！说不定就是命中注定的" + tmp2[gender - 1] + "喔～");
                data.add("keyword2", keyword2);

                JsonObject keyword3 = new JsonObject();
                keyword3.addProperty("value", "脉言——沙脉科技有限公司");
                data.add("keyword3", keyword3);

                postParam.add("data", data);

            } else {
                errLog.error("1504: 没有获取到被审核用户的wx_openid和infoFormId，用户id为：" + userid);
                throw new Exception("没有获取到被审核用户的wx_openid和infoFormId");
            }
            sendTemplateMsg(postParam);

            resultSet.close();

        } catch (Exception e) {
            errLog.error("1503: " + e.getMessage() + "，postParam is: " + postParam.toString(), e);
        }
    }

    private void sendTemplateMsg(JsonObject postParam) {
        try {
            OkHttpClient client = new OkHttpClient();
            String access_token;

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
                okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(mediaType, postParam.toString());
                Request req2 = new Request.Builder().post(requestBody).url(url2).build();
                Response res2 = client.newCall(req2).execute();

                if (!res2.isSuccessful()) {
                    throw new Exception("发送模板消息 request not successful");
                }
            } else {
                throw new Exception("获取小程序token request not successful");
            }
        } catch (Exception e) {
            errLog.error("1601: " + e.getMessage() + "，发送模板消息失败，postParam:" + postParam.toString(), e);
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
