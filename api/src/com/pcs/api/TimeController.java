package com.pcs.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.*;
import java.util.Random;


@Controller
@RequestMapping("/timerelate")
public class TimeController {

    @Value("${driver}")
    private String driver;
    @Value("${sqlUrl}")
    private String sqlUrl;
    @Value("${dbusername}")
    private String dbusername;
    @Value("${dbpassword}")
    private String dbpassword;

    private Connection connection = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;

    @Value("${MAX_FLOWER}")
    private int MAX_FLOWER;              //最大玫瑰数
    @Value("${FLOWER_COST_EVE}")
    private int FLOWER_COST_EVE;    //每次翻牌消耗玫瑰数
    @Value("${COMMEND_NUM_EVE}")
    private int COMMEND_NUM_EVE;    //每次推荐数目
    @Value("${CHANGE_NUM_LITTLE}")
    private int CHANGE_NUM_LITTLE;  //换一批次数（少）
    @Value("${CHANGE_NUM_MIDDLE}")
    private int CHANGE_NUM_MIDDLE;  //换一批次数（中）
    @Value("${GIVE_FLOWER_EVE_NUM}")
    private int GIVE_FLOWER_EVE_NUM;  //每次赠送玫瑰数

    private static Logger errLog = Logger.getLogger("error-log");
    private static Logger commonLog = Logger.getLogger("common-log");


    //获取玫瑰数
    @RequestMapping(value = "/getFlowersAndflippedIds", method = RequestMethod.GET)
    @ResponseBody
    String getFlowersAndflippedIds(String userid) {

        if (userid == null || userid.equals("") || userid.equals("undefined") || userid.equals("null")) {
            errLog.error("3101: url参数传递错误，params is: " + userid);
            return sendRespond("3101", "url参数传递错误", null);
        }

        JsonObject resData = new JsonObject();

        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);


            String sql = "select flowerNum, changeNum, flippedIds from timerelied_info where userid = ?";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, Integer.parseInt(userid));
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int flowerNum = resultSet.getInt("flowerNum");
                int changeNum = resultSet.getInt("changeNum");
                String flippedIds = resultSet.getString("flippedIds");

                resData.addProperty("flowerNum", flowerNum);
                resData.addProperty("changeNum", changeNum);
                resData.add("flippedIds", strToArray(flippedIds));
            }

            resultSet.close();
            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", resData);

        } catch (Exception e) {

            errLog.error("3102: " + e.getMessage() + "，params is: " + userid, e);
            return sendRespond("3102", e.getMessage(), null);
        }

    }

    //进入游戏，判断是否要更新玫瑰数和推荐id
    @RequestMapping(value = "/getOrUpdateCommend", method = RequestMethod.GET)
    @ResponseBody
    String getOrUpdateCommend(String userid, String cardFormId) {

        if (userid == null || userid.equals("") || userid.equals("undefined") || userid.equals("null")) {
            errLog.error("3301: url参数传递错误，params is: " + userid);
            return sendRespond("3301", "url参数传递错误", null);
        }

        JsonObject resData = new JsonObject();

        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);

            String sql0 = "update wx_info set cardFormId = ? where userid = ?";
            String sql = "select * from timerelied_info where userid = ?";
            String sql2 = "update timerelied_info set flowerNum = ?, changeNum = ?, commendId = ?, commendIds = ?, flippedIds = '', updateTime = now() where userid = ?";
            String sql3 = "update timerelied_info set commendIds = ? where userid = ?";

            int myid = Integer.parseInt(userid);

            preparedStatement = connection.prepareStatement(sql0);
            preparedStatement.setString(1, cardFormId);
            preparedStatement.setInt(2, myid);
            preparedStatement.executeUpdate();

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, myid);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {

                int flowerNum = resultSet.getInt("flowerNum");
                int changeNum = resultSet.getInt("changeNum");
                int commendId = resultSet.getInt("commendId");
                int commendGender = resultSet.getInt("commendGender");
                int commendGroups = resultSet.getInt("commendGroups");
                String commendDist = resultSet.getString("commendDist");
                JsonArray commendIds = strToArray(resultSet.getString("commendIds"));

                Date lastTime = resultSet.getDate("updateTime");
                Date curTime = new Date(System.currentTimeMillis());

                Date lastTimeTmp = Date.valueOf(lastTime.toString());
                Date curTimeTmp = Date.valueOf(curTime.toString());

                //数据库里commendId字段本是多余的，这里暂且表示昨天推的最后一个id
                int size = commendIds.size();
                int lastId = (size == 0 ? commendId : commendIds.get(size - 1).getAsInt());

                //如果这是今天第一次获取
                if (lastTimeTmp.before(curTimeTmp)) {

                    JsonObject ids = getAllCommendIds(lastId, commendGender, commendGroups, commendDist, myid, COMMEND_NUM_EVE);
                    if (ids.get("num").getAsInt() == -1) {
                        throw new Exception("数据库中获取推荐用户id失败");
                    }

                    preparedStatement = connection.prepareStatement(sql2);
                    flowerNum = flowerNum < MAX_FLOWER ? MAX_FLOWER : flowerNum;
                    preparedStatement.setInt(1, flowerNum);
                    if (commendDist.equals("330100")) {
                        //如果是杭州，换一批次数更新为1
                        changeNum = changeNum < CHANGE_NUM_MIDDLE ? CHANGE_NUM_MIDDLE : changeNum;
                    } else {
                        changeNum = changeNum < CHANGE_NUM_LITTLE ? CHANGE_NUM_LITTLE : changeNum;
                    }
                    preparedStatement.setInt(2, changeNum);
                    preparedStatement.setInt(3, lastId);
                    commendIds = ids.get("commendIds").getAsJsonArray();
                    preparedStatement.setString(4, arrayToStr(commendIds));
                    preparedStatement.setInt(5, myid);
                    preparedStatement.executeUpdate();
                } else {
                    //如果不是第一次推，需要保证今天推满10个
                    if (size < COMMEND_NUM_EVE) {
                        JsonObject ids = getAllCommendIds(lastId, commendGender, commendGroups, commendDist, myid, COMMEND_NUM_EVE - size);
                        if (ids.get("num").getAsInt() == -1) {
                            throw new Exception("数据库中获取推荐用户id失败");
                        }

                        //把新获取的加到原来的后面，注意去重
                        JsonParser parser = new JsonParser();
                        JsonArray plusArr = ids.get("commendIds").getAsJsonArray();

                        for (int i = 0; i < plusArr.size(); i++) {
                            String plusVal = plusArr.get(i).getAsString();
                            boolean hasVal = false;
                            for (int j = 0; j < commendIds.size(); j++) {
                                String comVal = commendIds.get(j).getAsString();
                                if (comVal.equals(plusVal)) hasVal = true;
                            }
                            if (!hasVal) {
                                commendIds.add(parser.parse(plusVal));
                            }
                        }

                        preparedStatement = connection.prepareStatement(sql3);
                        preparedStatement.setString(1, arrayToStr(commendIds));
                        preparedStatement.setInt(2, Integer.parseInt(userid));
                        preparedStatement.executeUpdate();
                    }
                }

                resData.add("commendIds", commendIds);
                resData.addProperty("flowerNum", flowerNum);
            }

            resultSet.close();
            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", resData);

        } catch (Exception e) {

            errLog.error("3302: " + e.getMessage() + "，params is: " + userid, e);
            return sendRespond("3302", e.getMessage(), null);
        }
    }

    //换一批，相当于到了新的一天
    @RequestMapping(value = "/changeBatch", method = RequestMethod.GET)
    @ResponseBody
    String changeBatch(String userid) {

        int myid;
        try {
            myid = Integer.parseInt(userid);
        } catch (Exception e) {
            errLog.error("3701: url参数传递错误，params is: " + userid);
            return sendRespond("3701", "url参数传递错误", null);
        }

        JsonObject resData = new JsonObject();

        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);

            String sql = "select * from timerelied_info where userid = ?";
            String sql2 = "update timerelied_info set changeNum = changeNum-1, commendId = ?, commendIds = ?, flippedIds = '', updateTime = now() where userid = ?";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, myid);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {

                int changeNum = resultSet.getInt("changeNum");
                if (changeNum <= 0) {
                    throw new Exception("changeNum不足，为" + changeNum);
                }
                int commendId = resultSet.getInt("commendId");
                int commendGender = resultSet.getInt("commendGender");
                int commendGroups = resultSet.getInt("commendGroups");
                String commendDist = resultSet.getString("commendDist");
                JsonArray commendIds = strToArray(resultSet.getString("commendIds"));

                int size = commendIds.size();
                int lastId = (size == 0 ? commendId : commendIds.get(size - 1).getAsInt());

                JsonObject ids = getAllCommendIds(lastId, commendGender, commendGroups, commendDist, myid, COMMEND_NUM_EVE);
                if (ids.get("num").getAsInt() == -1) {
                    throw new Exception("数据库中获取推荐用户id失败");
                }

                preparedStatement = connection.prepareStatement(sql2);
                preparedStatement.setInt(1, lastId);
                commendIds = ids.get("commendIds").getAsJsonArray();
                preparedStatement.setString(2, arrayToStr(commendIds));
                preparedStatement.setInt(3, myid);
                preparedStatement.executeUpdate();

                resData.add("commendIds", commendIds);
                resData.addProperty("changeNum", changeNum - 1);

            } else {
                throw new Exception("用户不存在，userid为" + userid);
            }

            resultSet.close();
            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", resData);

        } catch (Exception e) {
            errLog.error("3702: " + e.getMessage() + "，params is: " + userid, e);
            return sendRespond("3702", e.getMessage(), null);
        }
    }


    //获取所有推荐Id
    //获取异性、获取通过审核的人、不能是自己、不能是自己已经发过卡片的人
    private JsonObject getAllCommendIds(int commendId, int commendGender, int commendGroups, String commendDist,
                                        int myid, int limitNum) {
        JsonObject obj = new JsonObject();
        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);

            String sql = "select userid from user_info where status = 1 and gender = ? and groups = ? and district = ? and userid > 0 and userid < ? and userid != ? and userid not in " +
                    "(select recvId as userid from relation_info where sendId = ?) order by userid desc limit ? ";
            String sql2 = "select userid from user_info where status = 1 and gender = ? and groups = ? and district = ? and userid >= ? and userid != ? and userid not in " +
                    "(select recvId as userid from relation_info where sendId = ?) order by userid desc limit ? ";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, commendGender);
            preparedStatement.setInt(2, commendGroups);
            preparedStatement.setString(3, commendDist);
            preparedStatement.setInt(4, commendId);
            preparedStatement.setInt(5, myid);
            preparedStatement.setInt(6, myid);
            preparedStatement.setInt(7, limitNum);

//            commonLog.info(preparedStatement.toString());

            resultSet = preparedStatement.executeQuery();
            JsonArray arr = new JsonArray();
            JsonParser parser = new JsonParser();
            while (resultSet.next()) {
                arr.add(parser.parse(resultSet.getObject(1).toString()));
            }
            int cnt = arr.size();
            if (cnt < limitNum) {
                preparedStatement = connection.prepareStatement(sql2);
                preparedStatement.setInt(1, commendGender);
                preparedStatement.setInt(2, commendGroups);
                preparedStatement.setString(3, commendDist);
                preparedStatement.setInt(4, commendId);
                preparedStatement.setInt(5, myid);
                preparedStatement.setInt(6, myid);
                preparedStatement.setInt(7, limitNum - cnt);
//                commonLog.info(preparedStatement.toString());

                resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    arr.add(parser.parse(resultSet.getObject(1).toString()));
                }
            }

            obj.addProperty("num", arr.size());
            obj.add("commendIds", arr);
            obj.addProperty("lastId", (arr.size() == 0 ? 0 : arr.get(arr.size() - 1).getAsInt()));

            return obj;

        } catch (Exception e) {

            errLog.error(e.getMessage() + "，getAllCommendIds: " + commendId + "," + commendGender + "," + myid, e);
            obj.addProperty("num", -1);
            return obj;
        }
    }

    //游客模式，随即分配10张卡片
    @RequestMapping(value = "/getRandomCommend", method = RequestMethod.GET)
    @ResponseBody
    String getRandomCommend(int commendGender) {

        if (commendGender != 1 && commendGender != 2) {
            errLog.error("3601: url参数传递错误，gender is: " + commendGender);
            return sendRespond("3601", "url参数传递错误", null);
        }

        JsonObject resData = new JsonObject();

        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);


            String sql1 = "select userid from user_info order by userid desc limit 1";
            String sql2 = "select userid from user_info where status = 1 and gender = ? " +
                    "and userid > 0 and userid < ? order by userid desc limit ? ";
            String sql3 = "select userid from user_info where status = 1 and gender = ? " +
                    "and userid >= ? order by userid desc limit ? ";

            preparedStatement = connection.prepareStatement(sql1);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int maxId = resultSet.getInt("userid");
                int randomId = new Random().nextInt(maxId) + 1;

                preparedStatement = connection.prepareStatement(sql2);
                preparedStatement.setInt(1, commendGender);
                preparedStatement.setInt(2, randomId);
                preparedStatement.setInt(3, COMMEND_NUM_EVE);
                resultSet = preparedStatement.executeQuery();

                JsonArray commendIds = new JsonArray();
                JsonParser parser = new JsonParser();
                while (resultSet.next()) {
                    commendIds.add(parser.parse(resultSet.getObject(1).toString()));
                }
                int cnt = commendIds.size();
                if (cnt < COMMEND_NUM_EVE) {
                    preparedStatement = connection.prepareStatement(sql3);
                    preparedStatement.setInt(1, commendGender);
                    preparedStatement.setInt(2, randomId);
                    preparedStatement.setInt(3, COMMEND_NUM_EVE - cnt);
                    resultSet = preparedStatement.executeQuery();

                    while (resultSet.next()) {
                        commendIds.add(parser.parse(resultSet.getObject(1).toString()));
                    }
                }

                resData.add("commendIds", commendIds);

            } else {
                throw new Exception("sql error , sql: " + sql1);
            }

            resultSet.close();
            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", resData);

        } catch (Exception e) {
            errLog.error("3602: " + e.getMessage() + "，gender is: " + commendGender, e);
            return sendRespond("3602", e.getMessage(), null);
        }
    }

    //params={userid:50, objid:27}
    //翻牌子
    //返回值：
    /* {
        firstFlip: true,       //是否是第一次翻
        flowerEnough: true,    //玫瑰是否够
        curRelation: -1,       //和所翻的人的卡片关系
        flowerNum: 10,         //剩余玫瑰数量
        flippedIds: [27,28,29] //已翻开的ids
    }*/
    @RequestMapping(value = "/flipcard", method = RequestMethod.GET)
    @ResponseBody
    String flipcard(String params) {

        JsonObject ids;
        try {
            ids = new JsonParser().parse(params).getAsJsonObject();
        } catch (Exception e) {
            errLog.error("3401: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("3401", "url参数传递错误", null);
        }
        int userid = ids.get("userid").getAsInt();
        int objid = ids.get("objid").getAsInt();

        JsonObject resData = new JsonObject();

        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);

            String sql = "select flowerNum, flippedIds from timerelied_info  where userid = ?";
            String sql2 = "update timerelied_info set flowerNum = ?, flippedIds = ? where userid = ?";
            String sql3 = "select status from relation_info where sendId = ? and recvId = ?";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, userid);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {

                int flowerNum = resultSet.getInt("flowerNum");
                String flippedIds = resultSet.getString("flippedIds");

                //判断已翻开数组中是否有objid，即是否第一次翻
                JsonArray flippedIdsArr = strToArray(flippedIds);
                boolean firstFlip = true;
                for (JsonElement e : flippedIdsArr) {
                    if (e.getAsInt() == objid) firstFlip = false;
                }
                resData.addProperty("firstFlip", firstFlip);
                //判断玫瑰是否够
                resData.addProperty("flowerEnough", (flowerNum >= FLOWER_COST_EVE));

                //如果第一次翻且玫瑰够,翻牌更新玫瑰
                if (firstFlip && flowerNum >= FLOWER_COST_EVE) {
                    flowerNum -= FLOWER_COST_EVE;
                    flippedIds += (flippedIds.equals("") ? objid : ("#" + objid));

                    preparedStatement = connection.prepareStatement(sql2);
                    preparedStatement.setInt(1, flowerNum);
                    preparedStatement.setString(2, flippedIds);
                    preparedStatement.setInt(3, userid);
                    preparedStatement.executeUpdate();
                }

                resData.addProperty("flowerNum", flowerNum);
                resData.add("flippedIds", strToArray(flippedIds));
            } else {
                throw new Exception("该userid用户不存在");
            }

            preparedStatement = connection.prepareStatement(sql3);
            preparedStatement.setInt(1, userid);
            preparedStatement.setInt(2, objid);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int status = resultSet.getInt("status");
                resData.addProperty("curRelation", status);
            } else {
                resData.addProperty("curRelation", -1);
            }

            resultSet.close();
            preparedStatement.close();
            connection.close();

            return sendRespond("0000", "success", resData);

        } catch (Exception e) {

            errLog.error("3402: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("3402", e.getMessage(), null);
        }

    }

    //赠送玫瑰
    @RequestMapping(value = "/giveFlower", method = RequestMethod.GET)
    @ResponseBody
    String giveFlower(String params) {

        JsonObject resData = new JsonObject();

        JsonObject ids;
        String openid;
        int toid;

        try {
            ids = new JsonParser().parse(params).getAsJsonObject();
            openid = ids.get("openid").getAsString();
        } catch (Exception e) {
            errLog.error("3501: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("3501", "url参数传递错误", null);
        }

        try {
            toid = ids.get("toid").getAsInt();
        } catch (Exception e) {
            errLog.error("3504: " + e.getMessage() + "，params is: " + params, e);
            resData.addProperty("toidExist", false);
            return sendRespond("0000", "success", resData);
        }

        //openid补救措施
//        openid = ids.get("openid").toString();
//        if (openid.equals("null") || openid.equals("")) {
//            openid = UUID.randomUUID().toString().replace("-", "");
//            errLog.error("3505: wx_openid不存在，params is: " + params + "现在分配一个临时openid: " + openid);
//        }


        boolean hasRecord;
        boolean isTodayRecord;

        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(sqlUrl, dbusername, dbpassword);
            connection.setAutoCommit(false);//开启事务

            String sql0 = "select status from user_info where userid = ?";
            String sql = "select updateTime from flower_given where openid = ?";
            String sql2 = "insert into flower_given values (?,?,now())";
            String sql3 = "update flower_given set toid = ?, updateTime = now() where openid = ?";
            String sql4 = "update timerelied_info set flowerNum = flowerNum + ? where userid = ?";

            //判断所赠送的人是否已存在
            preparedStatement = connection.prepareStatement(sql0);
            preparedStatement.setInt(1, toid);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, openid);
                resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    Date lastTime = resultSet.getDate("updateTime");
                    Date curTime = new Date(System.currentTimeMillis());

                    Date lastTimeTmp = Date.valueOf(lastTime.toString());
                    Date curTimeTmp = Date.valueOf(curTime.toString());
                    if (lastTimeTmp.before(curTimeTmp)) {
                        //如果有记录，且上次时间是今天以前，说明这个人今天以前赠送过玫瑰
                        preparedStatement = connection.prepareStatement(sql3);
                        preparedStatement.setInt(1, toid);
                        preparedStatement.setString(2, openid);
                        preparedStatement.executeUpdate();

                        hasRecord = true;
                        isTodayRecord = false;
                    } else {
                        //如果有记录，但上次时间还是今天，说明这个人今天赠送过玫瑰
                        hasRecord = true;
                        isTodayRecord = true;
                    }
                } else {
                    //如果没有记录，说明这个人今天还没赠送玫瑰
                    preparedStatement = connection.prepareStatement(sql2);
                    preparedStatement.setString(1, openid);
                    preparedStatement.setInt(2, toid);
                    preparedStatement.executeUpdate();
                    hasRecord = false;
                    isTodayRecord = false;
                }

                if (!hasRecord || !isTodayRecord) {
                    preparedStatement = connection.prepareStatement(sql4);
                    preparedStatement.setInt(1, GIVE_FLOWER_EVE_NUM);
                    preparedStatement.setInt(2, toid);
                    preparedStatement.executeUpdate();
                }

                resData.addProperty("toidExist", true);
                resData.addProperty("hasRecord", hasRecord);
                resData.addProperty("isTodayRecord", isTodayRecord);
            } else {
                errLog.error("3505: 该userid不存在数据库中，params is: " + params);
                resData.addProperty("toidExist", false);
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
                errLog.error("3503: " + e1.getMessage() + "，params is: " + params, e1);
                return sendRespond("3503", e1.getMessage(), null);
            }

            errLog.error("3502: " + e.getMessage() + "，params is: " + params, e);
            return sendRespond("3502", e.getMessage(), null);
        }

    }


    //字符串转JsonArray
    private JsonArray strToArray(String str) {
        JsonArray jsonArray = new JsonArray();
        if (str == null || str.equals("")) return jsonArray;

        JsonParser parser = new JsonParser();
        String strArray[] = str.split("#");
        for (String s : strArray) {
            jsonArray.add(parser.parse(s));
        }
        return jsonArray;
    }

    //JsonArray
    private String arrayToStr(JsonArray jsonArray) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < jsonArray.size(); i++) {
            sb.append(jsonArray.get(i).getAsString());
            if (i != jsonArray.size() - 1) sb.append("#");
        }
        return sb.toString();
    }

    private String sendRespond(String code, String info, JsonObject result) {
        JsonObject res = new JsonObject();
        res.addProperty("code", code);
        res.addProperty("info", info);
        res.add("result", result);
        return res.toString();
    }

}
