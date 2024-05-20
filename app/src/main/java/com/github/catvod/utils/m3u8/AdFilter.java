package com.github.catvod.utils.m3u8;

import android.net.Uri;
import android.text.TextUtils;

import com.github.catvod.bean.Rule;
import com.github.catvod.net.OkHttp;
import com.github.catvod.spider.Proxy;
import com.github.catvod.spider.base.BaseSpider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdFilter {
    public static List<Rule> rules = new ArrayList<>();
    public static Map<String , Rule> ruleMap = new HashMap<>();

    private AdFilter(){}

    public static Map<String, String> getHeader() {
        Map<String, String> header = new HashMap<>();
        header.put("User-Agent", BaseSpider.CHROME);
        return header;
    }

    public static void setRuleMap(String extend) throws Exception {
        JSONObject jsonObject = new JSONObject(extend);
        String siteUrl = jsonObject.getString("siteUrl");
        Rule rule = new Rule();
        rule.setRegex(JSONArray2List(jsonObject.getJSONObject("rule").getJSONArray("regex")));
        ruleMap.put(siteUrl, rule);
    }

    public static void setRules(String url) throws Exception {
        if (!rules.isEmpty()) return;
        String content = OkHttp.string(url, getHeader());
        JSONObject jsonObject = new JSONObject(content);
        JSONArray rulesJsonArray = jsonObject.getJSONArray("rules");
        for (int i = 0; i < rulesJsonArray.length(); i++) {
            JSONObject obj = rulesJsonArray.getJSONObject(i);
            Rule rule = new Rule();
            rule.setName(obj.getString("name"));
            rule.setHosts(JSONArray2List(obj.getJSONArray("hosts")));
            rule.setRegex(JSONArray2List(obj.getJSONArray("regex")));
            rules.add(rule);
        }
    }

    private static List<String> JSONArray2List(JSONArray jsonArray) throws Exception {
        List<String> tmp = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) tmp.add(jsonArray.getString(i));
        return tmp;
    }

    public static Rule getRule(Uri uri) {
        if (uri.getHost() == null) return Rule.empty();
        String hosts = TextUtils.join(",", Arrays.asList(host(uri), host(uri.getQueryParameter("url"))));
        for (Rule rule : rules) for (String host : rule.getHosts()) if (containOrMatch(hosts, host)) return rule;
        return Rule.empty();
    }

    public static String host(String url) {
        return url == null ? "" : host(Uri.parse(url));
    }

    public static String host(Uri uri) {
        String host = uri.getHost();
        return host == null ? "" : host.toLowerCase().trim();
    }

    public static boolean containOrMatch(String text, String regex) {
        try {
            return text.contains(regex) || text.matches(regex);
        } catch (Exception e) {
            return false;
        }
    }

    public static List<String> getRegex(Uri uri) {
        return getRule(uri).getRegex();
    }

    public static List<String> getRegex(String siteUrl) {
        Rule rule = ruleMap.get(siteUrl);
        return rule == null ? Rule.empty().getRegex() : rule.getRegex();
    }

    public static String proxyM3U8(String url, String siteUrl) {
        if (!url.contains(".m3u8")) return url;
        /*boolean m3u8Ad = getRegex(Uri.parse(url)).size() > 0;
        return m3u8Ad ? Proxy.localProxyUrl() + "?do=m3u8&url=" + url : url;*/
        if (siteUrl == null) siteUrl = "";
        return Proxy.localProxyUrl() + "?do=m3u8&siteUrl=" + siteUrl + "&url=" + url;
    }
}
