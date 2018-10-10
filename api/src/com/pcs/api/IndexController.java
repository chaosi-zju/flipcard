package com.pcs.api;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


@Controller
@RequestMapping("/")
public class IndexController {

    private String driver = "com.mysql.jdbc.Driver";
    private String sqlUrl = "jdbc:mysql://localhost:3306/flipcord?useUnicode=true&characterEncoding=UTF-8";
    private String dbusername = "root";
    private String dbpassword = "moyan";

    @Autowired HttpServletRequest req;


    @RequestMapping(value = "/", method = RequestMethod.GET)
    @ResponseBody
    String index() {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(new JsonParser().parse("'杭州'"));

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("place","安庆");

        jsonArray.add(jsonObject);

        return jsonArray.toString();
    }




    private String sendRespond(String code, String info, JsonObject result){
        JsonObject res = new JsonObject();
        res.addProperty("code", code);
        res.addProperty("info", info);
        res.add("result",result);
        return res.toString();
    }

}
