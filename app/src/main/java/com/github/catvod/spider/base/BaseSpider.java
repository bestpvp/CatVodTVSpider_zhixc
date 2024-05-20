package com.github.catvod.spider.base;

import com.github.catvod.crawler.Spider;
import com.github.catvod.utils.okhttp.OkHttpUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BaseSpider extends Spider {
    public static final String CHROME = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36";
    public static final String IPHONE = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Mobile/15E148 Safari/604.1";

    public Map<String, String> getHeader() {
        Map<String, String> header = new HashMap<>();
        header.put("User-Agent", CHROME);
        return header;
    }

    public Map<String, String> getMobileHeader() {
        Map<String, String> header = new HashMap<>();
        header.put("User-Agent", IPHONE);
        return header;
    }

    public Map<String, String> getHeader(String referer) {
        Map<String, String> header = new HashMap<>();
        header.put("User-Agent", CHROME);
        header.put("Referer", referer);
        return header;
    }

    public static OkHttpClient getOkHttpClient() {
        //return OkHttp.client();
        return OkHttpUtil.defaultClient();
    }

    public static Response newCall(Request request) throws IOException {
        return getOkHttpClient().newCall(request).execute();
    }

    public static Response newCall(String url) throws IOException {
        return getOkHttpClient().newCall(new Request.Builder().url(url).build()).execute();
    }

    public static Response newCall(String url, Map<String, String> header) throws IOException {
        return getOkHttpClient().newCall(new Request.Builder().url(url).headers(Headers.of(header)).build()).execute();
    }

    public String req(String url, Map<String, String> header) throws Exception {
        return req(newCall(url, header), null);
    }

    public String req(Response response, String encoding) throws Exception {
        if (!response.isSuccessful()) return "";
        byte[] bytes = response.body().bytes();
        response.close();
        return new String(bytes, encoding == null ? "UTF-8" : encoding);
    }

    public String find(Pattern pattern, String html) {
        Matcher m = pattern.matcher(html);
        return m.find() ? m.group(1) : "";
    }

    public String find(String regexStr, String htmlStr) {
        Pattern pattern = Pattern.compile(regexStr, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(htmlStr);
        if (matcher.find()) return matcher.group(1);
        return "";
    }

    public String removeHtmlTag(String str) {
        return str.replaceAll("</?[^>]+>", "");
    }


    public static String substring(String text) {
        return substring(text, 1);
    }

    public static String substring(String text, int num) {
        if (text != null && text.length() > num) {
            return text.substring(0, text.length() - num);
        } else {
            return text;
        }
    }
}
