package com.pcs.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping("/index")
public class IndexController {

    @Value("${wxAppid}") private String wxAppid;
    @Value("${wxAppsecret}") private String wxAppsecret;
    @Value("${isNetWorking}") private boolean isNetWorking;

    private static Logger errLog = Logger.getLogger("error-log");

    //首页
    @RequestMapping(value = "/", method = RequestMethod.GET)
    @ResponseBody
    String Index() {
        return sendRespond("0000", "success", null);
    }

    //获取wx_openid
    @RequestMapping(value = "/getOpenId", method = RequestMethod.GET)
    @ResponseBody
    String getOpenId(String code) {

        if (code == null || code.equals("")) {
            errLog.error("0101: url参数传递错误，params is: " + code);
            return sendRespond("0101", "url参数传递错误", null);
        }
        JsonObject resData = new JsonObject();

        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + wxAppid + "&secret=" + wxAppsecret +
                "&js_code=" + code + "&grant_type=authorization_code";

        OkHttpClient client = new OkHttpClient();
        try {
            Request req = new Request.Builder().url(url).build();
            Response res = client.newCall(req).execute();
            if (res.isSuccessful()) {
                JsonObject data = new JsonParser().parse(res.body().string()).getAsJsonObject();
                resData.add("openid", data.get("openid"));
            }
        } catch (Exception e) {
            errLog.error("0102: " + e.getMessage() + "，params is: " + code, e);
            return sendRespond("0102", "获取openid失败", null);
        }

        return sendRespond("0000", "success", resData);
    }

    //判断是否在逃避审核
    @RequestMapping(value = "/isNetWorking", method = RequestMethod.GET)
    @ResponseBody
    String isNetWorking() {
        JsonObject resData = new JsonObject();
        resData.addProperty("netOk", isNetWorking);
        return sendRespond("0000", "success", resData);
    }

    private String sendRespond(String code, String info, JsonObject result) {
        JsonObject res = new JsonObject();
        res.addProperty("code", code);
        res.addProperty("info", info);
        res.add("result", result);
        return res.toString();
    }

}
