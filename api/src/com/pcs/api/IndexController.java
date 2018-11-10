package com.pcs.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;


@Controller
@RequestMapping("/index")
public class IndexController {

    private static final String wxAppid = "wx67aa7336c73cb84a";
    private static final String wxAppsecret = "0e0ff76e5e3a53f837993044dbcb7287";

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

//        System.out.println(code);
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
                resData.add("openid",data.get("openid"));
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
        resData.addProperty("netOk", true);
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
