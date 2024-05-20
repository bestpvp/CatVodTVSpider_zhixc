package com.github.catvod.spider;

import android.content.Context;

import com.github.catvod.spider.base.BaseSpider;

import com.github.catvod.utils.m3u8.AdFilter;
import com.github.catvod.utils.m3u8.Notify;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

public class Caiji extends BaseSpider {
    private String siteUrl;

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);
        JSONObject obj = new JSONObject(extend);
        siteUrl = obj.getString("siteUrl");
        if (obj.has("rulesUrl")) AdFilter.setRules(obj.getString("rulesUrl"));
        else AdFilter.setRuleMap(extend);
        Notify.show("采集对象初始化完成。。。");
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        String link = String.format("%s?ac=list", siteUrl);
        String content = req(link, getHeader());
        // JSONObject obj = new JSONObject(content);
        return content;
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String link = String.format("%s?ac=detail&t=%s&pg=%s", siteUrl, tid, pg);
        String content = req(link, getHeader());
        return content;
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String link = String.format("%s?ac=detail&ids=%s", siteUrl, ids.get(0));
        String content = req(link, getHeader());
        return content;
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        JSONObject result = new JSONObject();
        result.put("parse", 0);
        result.put("header", getHeader().toString());
        result.put("playUrl", "");
        result.put("url", AdFilter.proxyM3U8(id, siteUrl));
        return result.toString();
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        return searchContent(key, quick, "1");
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) throws Exception {
        String link = String.format("%s?ac=detail&wd=%s&pg=%s", siteUrl, URLEncoder.encode(key), pg);
        String content = req(link, getHeader());
        return content;
    }
}
