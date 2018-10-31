package com.pcs.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;


@Controller
@RequestMapping("/userinfo")
public class UserController {

    private String driver = "com.mysql.jdbc.Driver";
    private String sqlUrl = "jdbc:mysql://localhost:3306/flipcardv2?useUnicode=true&characterEncoding=UTF-8";
    private String dbusername = "root";
    private String dbpassword = "moyan";

    private Connection connection = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;

    private static final int USER_STATUS_INIT = 0;
    private static final int MAX_FLOWER = 10;        //最大玫瑰数

    private static Logger errLog = Logger.getLogger("error-log");

    //几个变量整形转字符串
    private static final String genderArr[] = {"", "男", "女"};
    private static final String groupsArr[] = {"在校生", "上班族"};
    private static final String educationArr[] = {"本科在读", "硕士在读", "博士在读", "学士", "硕士", "博士"};
    private static final Map<String, String> districtMap = new HashMap<String, String>() {
        {
            put("330100", "杭州");
            put("310100", "上海");
            put("510100", "成都");
        }
    };


    //{ wx_openid:"1", status:1, gender:1, groups:1, education:1, district:"330100", school:"浙江大学", birthyear:"1994", star:"金牛座", hobby:"篮球", wxnum:"superpan", phonenum:"123456789", hobbyImage:"http://a.com", hobbyImageTitle:"安和桥", hobbyImageDes:"发牌的是上帝，打牌的是自己", faceImage:"http://b.com", identityImage:"http://c.com" }
    // 添加用户
    @RequestMapping(value = "/add", method = RequestMethod.GET)
    @ResponseBody
    String addUserInfo(String params) {

        JsonObject userinfo;
        try {
            userinfo = new JsonParser().parse(params).getAsJsonObject();
        } catch (Exception e) {
            errLog.error("0201: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("0201", "url参数传递错误", null);
        }

        JsonObject resData = new JsonObject();

        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);
            connection.setAutoCommit(false);//开启事务

            String sql1 = "insert into user_info values (0,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'',now())";
            String sql2 = "update wx_info set userid = ? where wx_openid = ?";
            String sql3 = "select count(*) as cnt from user_info where gender = 1 " +
                    "union all " +
                    "select count(*) as cnt from user_info where gender = 2";
            String sql4 = "insert into timerelied_info values (?,?,?,?,?,'','',now())";

            preparedStatement = connection.prepareStatement(sql1, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, USER_STATUS_INIT);    //审核状态
            preparedStatement.setInt(2, userinfo.get("gender").getAsInt());
            preparedStatement.setInt(3, userinfo.get("groups").getAsInt());
            preparedStatement.setInt(4, userinfo.get("education").getAsInt());
            preparedStatement.setString(5, userinfo.get("district").getAsString());
            preparedStatement.setString(6, userinfo.get("school").getAsString());
            preparedStatement.setString(7, userinfo.get("birthyear").getAsString());
            preparedStatement.setString(8, userinfo.get("star").getAsString());
            preparedStatement.setString(9, userinfo.get("hobby").getAsString());
            preparedStatement.setString(10, userinfo.get("wxnum").getAsString());
            preparedStatement.setString(11, userinfo.get("phonenum").getAsString());
            preparedStatement.setString(12, userinfo.get("hobbyImage").getAsString());
            preparedStatement.setString(13, userinfo.get("hobbyImageTitle").getAsString());
            preparedStatement.setString(14, userinfo.get("hobbyImageDes").getAsString());
            preparedStatement.setString(15, userinfo.get("faceImage").getAsString());
            preparedStatement.setString(16, userinfo.get("identityImage").getAsString());
            preparedStatement.executeUpdate();

            resultSet = preparedStatement.getGeneratedKeys();

            if (resultSet.next()) {
                int userid = resultSet.getInt(1);
                resData.addProperty("userid", userid);

                //插入wx_info
                preparedStatement = connection.prepareStatement(sql2);
                preparedStatement.setInt(1, userid);
                preparedStatement.setString(2, userinfo.get("wx_openid").getAsString());
                preparedStatement.executeUpdate();

                //查询男女人数
                preparedStatement = connection.prepareStatement(sql3);
                resultSet = preparedStatement.executeQuery();
                int menCnt, womenCnt;

                if (resultSet.next()) {
                    menCnt = resultSet.getInt(1);

                    if (resultSet.next()) {
                        womenCnt = resultSet.getInt(1);

                        //插入timerelied_info
                        preparedStatement = connection.prepareStatement(sql4);
                        preparedStatement.setInt(1, userid);
                        preparedStatement.setInt(2, MAX_FLOWER);
                        int gend = userinfo.get("gender").getAsInt();
                        if (gend == 1) {
                            preparedStatement.setInt(3, (womenCnt == 0 ? 0 : (menCnt % womenCnt)));
                            preparedStatement.setInt(4, 2);
                        } else {

                            preparedStatement.setInt(3, (menCnt == 0 ? 0 : (womenCnt % menCnt)));
                            preparedStatement.setInt(4, 1);
                        }
                        preparedStatement.setString(5, userinfo.get("district").getAsString());
                        preparedStatement.executeUpdate();

                    } else {
                        throw new Exception("获取女用户数失败");
                    }
                } else {
                    throw new Exception("获取男用户数失败");
                }
            } else {
                throw new Exception("插入的用户主键获取失败");
            }

            connection.commit();
            connection.setAutoCommit(true);

            resultSet.close();
            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", resData);

        } catch (Exception e) {
            try {
                if (connection != null) {
                    connection.rollback();
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e1) {
                errLog.error("0203: " + e1.getMessage() + "，params is: " + params, e1);
                return sendRespond("0203", e1.getMessage(), null);
            }
            errLog.error("0202: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("0202", e.getMessage(), null);
        }

    }

    //{ userid:1, status:1, groups:2, education:2, district:"330100", school:"吉林大学", star:"白羊座", hobby:"乒乓球", wxnum:"chaosi", phonenum:"978652341", hobbyImage:"http://a.com", hobbyImageTitle:"安和桥", hobbyImageDes:"打牌的是自己，发牌的是上帝", faceImage:"http://b.com", identityImage:"http://c.com" }
    //修改用户信息
    @RequestMapping(value = "/update", method = RequestMethod.GET)
    @ResponseBody
    String updateUserInfo(String params) {

        JsonObject userinfo;
        try {
            userinfo = new JsonParser().parse(params).getAsJsonObject();
        } catch (Exception e) {
            errLog.error("0301: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("0301", "url参数传递错误", null);
        }

        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);

            String sql = "select district from user_info where userid = ?";
            String sql2 = "update user_info set status = ?, gender = ?, groups = ?, education = ?, district = ?, school = ?, birthyear = ?, star = ?, " +
                    "hobby = ?,wxnum = ?, phonenum = ?, hobbyImage = ?,  hobbyImageTitle = ?, hobbyImageDes = ?, faceImage = ?, identityImage = ?, " +
                    "failReason = '' where userid = ?";
            String sql3 = "update timerelied_info set commendDist = ? where userid = ?";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, userinfo.get("userid").getAsInt());
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String oldDistrict = resultSet.getString("district");
                String newDistrict = userinfo.get("district").getAsString();

                preparedStatement = connection.prepareStatement(sql2);
                preparedStatement.setInt(1, USER_STATUS_INIT);    //审核状态
                preparedStatement.setInt(2, userinfo.get("gender").getAsInt());
                preparedStatement.setInt(3, userinfo.get("groups").getAsInt());
                preparedStatement.setInt(4, userinfo.get("education").getAsInt());
                preparedStatement.setString(5, userinfo.get("district").getAsString());
                preparedStatement.setString(6, userinfo.get("school").getAsString());
                preparedStatement.setString(7, userinfo.get("birthyear").getAsString());
                preparedStatement.setString(8, userinfo.get("star").getAsString());
                preparedStatement.setString(9, userinfo.get("hobby").getAsString());
                preparedStatement.setString(10, userinfo.get("wxnum").getAsString());
                preparedStatement.setString(11, userinfo.get("phonenum").getAsString());
                preparedStatement.setString(12, userinfo.get("hobbyImage").getAsString());
                preparedStatement.setString(13, userinfo.get("hobbyImageTitle").getAsString());
                preparedStatement.setString(14, userinfo.get("hobbyImageDes").getAsString());
                preparedStatement.setString(15, userinfo.get("faceImage").getAsString());
                preparedStatement.setString(16, userinfo.get("identityImage").getAsString());
                preparedStatement.setInt(17, userinfo.get("userid").getAsInt());
                preparedStatement.executeUpdate();

                if (!oldDistrict.equals(newDistrict)) {
                    preparedStatement = connection.prepareStatement(sql3);
                    preparedStatement.setString(1, newDistrict);
                    preparedStatement.setInt(2, userinfo.get("userid").getAsInt());
                    preparedStatement.executeUpdate();
                }

            } else {
                throw new Exception("该用户不存在");
            }

            resultSet.close();
            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", null);

        } catch (Exception e) {
            errLog.error("0302: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("0302", e.getMessage(), null);
        }
    }

    //查询用户信息
    @RequestMapping(value = "/query", method = RequestMethod.GET)
    @ResponseBody
    String queryUser(String userid) {

        JsonObject resData = new JsonObject();
        if (userid == null || userid.equals("") || userid.equals("undefined") || userid.equals("null")) {
            resData.addProperty("exist", false);
            return sendRespond("0000", "未传递userid", resData);
        }

        try {
            //数据库初始化
            Class.forName(driver);
            //校验用户名密码
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);

            String sql = "select * from user_info where userid = ?";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, Integer.parseInt(userid));
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                ResultSetMetaData rsmd = resultSet.getMetaData();
                int column = rsmd.getColumnCount();

                JsonObject userinfo = new JsonObject();
                for (int i = 1; i <= column; i++) {
                    Object obj = resultSet.getObject(i);
                    userinfo.addProperty(rsmd.getColumnName(i), obj == null ? "" : obj.toString());

                }
                userinfo.addProperty("gender", genderArr[userinfo.get("gender").getAsInt()]);
                userinfo.addProperty("groups", groupsArr[userinfo.get("groups").getAsInt()]);
                userinfo.addProperty("education", educationArr[userinfo.get("education").getAsInt()]);
                userinfo.addProperty("district", districtMap.get(userinfo.get("district").getAsString()));

                resData.addProperty("exist", true);
                resData.add("userinfo", userinfo);
            } else {
                resData.addProperty("exist", false);
            }

            resultSet.close();
            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", resData);

        } catch (Exception e) {
            errLog.error("0101: " + e.getMessage() + "，params is: " + userid, e);
            return sendRespond("0101", e.getMessage(), null);
        }

    }

    //查询一组用户信息
    @RequestMapping(value = "/queryAll", method = RequestMethod.GET)
    @ResponseBody
    String queryAllUsers(String params) {

        JsonArray objsIds;
        try {
            objsIds = new JsonParser().parse(params).getAsJsonArray();
        } catch (Exception e) {
            errLog.error("0501: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("0501", "url参数传递错误", null);
        }
//        System.out.println(objsIds);

        JsonArray objsInfo = new JsonArray();
        JsonObject res = new JsonObject();

        //如果objsIds为空数组
        if (objsIds.size() == 0) {
            res.addProperty("code", "0000");
            res.addProperty("info", "success");
            res.add("result", objsInfo);
            return res.toString();
        }

        try {
            //数据库初始化
            Class.forName(driver);
            //校验用户名密码
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);

            //拼接sql语句
            StringBuilder ques = new StringBuilder();
            for (int i = 0; i < objsIds.size(); i++) {
                ques.append("?");
                if (i != objsIds.size() - 1) ques.append(",");
            }
            String sql = "select * from user_info where userid in (" + ques + ")";

            preparedStatement = connection.prepareStatement(sql);
            //拼接preparedStatement
            for (int i = 0; i < objsIds.size(); i++) {
                preparedStatement.setInt(i + 1, objsIds.get(i).getAsInt());
            }
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                ResultSetMetaData rsmd = resultSet.getMetaData();
                int column = rsmd.getColumnCount();

                //获取单个userinfo，加入数组
                JsonObject objInfo = new JsonObject();
                for (int i = 1; i <= column; i++) {
                    Object prop = resultSet.getObject(i);
                    objInfo.addProperty(rsmd.getColumnName(i), prop == null ? "" : prop.toString());
                }
                objInfo.addProperty("gender", genderArr[objInfo.get("gender").getAsInt()]);
                objInfo.addProperty("groups", groupsArr[objInfo.get("groups").getAsInt()]);
                objInfo.addProperty("education", educationArr[objInfo.get("education").getAsInt()]);
                objInfo.addProperty("district", objInfo.get("district").getAsString().equals("330100") ? "杭州" : "非杭州");
                objsInfo.add(objInfo);

            }
            resultSet.close();
            preparedStatement.close();
            connection.close();

            res.addProperty("code", "0000");
            res.addProperty("info", "success");
            res.add("result", objsInfo);
            return res.toString();


        } catch (Exception e) {
            errLog.error("0502: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("0502", e.getMessage(), null);
        }

    }

    //判断用户是否注册：无 不存在，-1 存在但没注册，N 存在且注册
    //返回值，userid userstatus(-1未注册 0未审核 1正常)
    //{ wx_openid: "wx1234", wx_nickName:"wxchaosi", wx_gender:"1", wx_province:"zhejiang", wx_city:"hangzhou", wx_avatarUrl:"http://d.com" }
    @RequestMapping(value = "/isUserReg", method = RequestMethod.GET)
    @ResponseBody
    String isUserReg(String params) {

        JsonObject wxinfo;
        try {
            wxinfo = new JsonParser().parse(params).getAsJsonObject();
        } catch (Exception e) {
            errLog.error("0401: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("0401", "url参数传递错误", null);
        }
        if (wxinfo.get("wx_openid") == null) {
            errLog.error("0402: wx_openid不存在，params is: " + params);
            return sendRespond("0402", "wx_openid不存在", null);
        }

        JsonObject resData = new JsonObject();

        try {
            //数据库初始化
            Class.forName(driver);
            //校验用户名密码
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);

            String sql = "select userid from wx_info where wx_openid = ?";
            String sql2 = "insert into wx_info values (0,?,-1,?,?,?,?,?,now())";
            String sql3 = "select status,failReason from user_info where userid = ?";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, wxinfo.get("wx_openid").getAsString());
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int userid = resultSet.getInt("userid");
                if (userid == -1) {
                    //记录了wx_info，但没注册
                    resData.add("userid", null);
                    resData.addProperty("userstatus", -1);
                    resData.add("failReason", null);
                } else {
                    //记录了wx_info，且注册了
                    preparedStatement = connection.prepareStatement(sql3);
                    preparedStatement.setInt(1, userid);
                    resultSet = preparedStatement.executeQuery();
                    if (resultSet.next()) {
                        int status = resultSet.getInt("status");
                        String failReason = resultSet.getString("failReason");
                        resData.addProperty("userid", userid);
                        resData.addProperty("userstatus", status);
                        resData.addProperty("failReason", failReason);
                    } else {
                        throw new Exception("已查询的userid不存在");
                    }
                }
            } else {
                //还没记录wx_info，直接记录
                preparedStatement = connection.prepareStatement(sql2);
                preparedStatement.setString(1, wxinfo.get("wx_openid").getAsString());
                preparedStatement.setString(2, wxinfo.get("wx_nickName").getAsString());
                preparedStatement.setString(3, wxinfo.get("wx_gender").getAsString());
                preparedStatement.setString(4, wxinfo.get("wx_province").getAsString());
                preparedStatement.setString(5, wxinfo.get("wx_city").getAsString());
                preparedStatement.setString(6, wxinfo.get("wx_avatarUrl").getAsString());
                preparedStatement.executeUpdate();
                resData.add("userid", null);
                resData.addProperty("userstatus", -1);
                resData.add("failReason", null);
            }

            resultSet.close();
            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", resData);

        } catch (Exception e) {
            errLog.error("0403: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("0403", e.getMessage(), null);
        }
    }

    /*
     *采用spring提供的上传文件的方法
     */
    @RequestMapping(value = "/uploadImg")
    @ResponseBody
    String uploadImg(@RequestParam("file") MultipartFile file, String userid, String kind, HttpServletRequest request) {

        JsonObject resData = new JsonObject();
        String oriPath = "";
        String subPath;
        try {
            String dir = request.getServletContext().getRealPath("");
            dir = dir.substring(0, dir.lastIndexOf("/"));
            dir = dir.substring(0, dir.lastIndexOf("/"));

            oriPath = "/uploadImg/flipcard/" + userid + "/" + kind + "/" + System.currentTimeMillis() + "." + file.getContentType().split("/")[1];
            subPath = "/uploadImg/flipcard/" + userid + "/" + kind + "/" + System.currentTimeMillis() + "_sub";

            File image = new File(dir + oriPath);
            if (!image.getParentFile().exists()) {
                image.getParentFile().mkdirs();
            }

            //生成上传的图
            file.transferTo(image);
            //生成压缩图
            Thumbnails.of(dir + oriPath).scale(1f).outputFormat("jpg").toFile(dir + subPath);

            resData.addProperty("oriPath", oriPath);
            resData.addProperty("subPath", subPath + ".jpg");

            return sendRespond("0000", "success", resData);
        } catch (Exception e) {
            errLog.error("0601: " + e.getMessage() + "，path is: " + oriPath, e);
            return sendRespond("0601", e.getMessage(), null);
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
