package com.github.catvod.utils.m3u8;

import android.net.Uri;
import android.text.TextUtils;

import com.github.catvod.utils.okhttp.OkHttpUtil;
import com.google.common.net.HttpHeaders;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author FongMi
 */
public class M3U8 {

    private static final String TAG_DISCONTINUITY = "#EXT-X-DISCONTINUITY";
    private static final String TAG_MEDIA_DURATION = "#EXTINF";
    private static final String TAG_ENDLIST = "#EXT-X-ENDLIST";
    private static final String TAG_KEY = "#EXT-X-KEY";

    private static final Pattern REGEX_X_DISCONTINUITY = Pattern.compile("#EXT-X-DISCONTINUITY[\\s\\S]*?(?=#EXT-X-DISCONTINUITY|$)");
    private static final Pattern REGEX_MEDIA_DURATION = Pattern.compile(TAG_MEDIA_DURATION + ":([\\d\\.]+)\\b");
    private static final Pattern REGEX_URI = Pattern.compile("URI=\"(.+?)\"");

    private static final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36";

    public static boolean isAd(String regex) {
        return regex.contains(TAG_DISCONTINUITY) || regex.contains(TAG_MEDIA_DURATION) || regex.contains(TAG_ENDLIST) || regex.contains(TAG_KEY) || isDouble(regex);
    }

    private static OkHttpClient getOkHttpClient() {
        return OkHttpUtil.defaultClient();
    }

    public static Object[] proxy(Map<String, String> params) throws Exception {
        Map<String, String> header = new HashMap<>();
        header.put("User-Agent", userAgent);
        String m3u8Content = get(params.get("url"), header);
        Object[] result = new Object[3];
        result[0] = 200;
        result[1] = "text/plain; charset=utf-8";
        ByteArrayInputStream baos = new ByteArrayInputStream(m3u8Content.getBytes("UTF-8"));
        result[2] = baos;
        return result;
    }

    public static String get(String url, Map<String, String> headers) {
        try {
            if (TextUtils.isEmpty(url)) return "";
            Response response = getOkHttpClient().newCall(new Request.Builder().url(url).headers(getHeader(headers)).build()).execute();
            String result = response.body().string();
            result = result.replaceAll("\r\n", "\n");
            Matcher matcher = Pattern.compile("#EXT-X-STREAM-INF(.*)\\n?(.*)").matcher(result);
            if (matcher.find() && matcher.groupCount() > 1) return get(UriUtil.resolve(url, matcher.group(2)), headers);
            StringBuilder sb = new StringBuilder();
            for (String line : result.split("\n")) sb.append(shouldResolve(line) ? resolve(url, line) : line).append("\n");
            List<String> ads = AdFilter.getRegex(Uri.parse(url));
            String s1 = sb.toString();
            String s2 = clean(s1, ads);
            if (s1.length() != s2.length()) Notify.show("净化成功！");
            return s2;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String clean(String line, List<String> ads) {
        boolean scan = false;
        for (String ad : ads) {
            if (ad.contains(TAG_DISCONTINUITY) || ad.contains(TAG_MEDIA_DURATION)) line = line.replaceAll(ad, "");
            else if (isDouble(ad)) scan = true;
        }
        return scan ? scan(line, ads) : line;
    }

    private static String scan(String line, List<String> ads) {
        Matcher m1 = REGEX_X_DISCONTINUITY.matcher(line);
        while (m1.find()) {
            String group = m1.group();
            BigDecimal t = BigDecimal.ZERO;
            Matcher m2 = REGEX_MEDIA_DURATION.matcher(group);
            while (m2.find()) t = t.add(new BigDecimal(m2.group(1)));
            for (String ad : ads) if (t.toString().startsWith(ad)) line = line.replace(group.replace(TAG_ENDLIST, ""), "");
        }
        return line;
    }

    private static Headers getHeader(Map<String, String> headers) {
        Headers.Builder builder = new Headers.Builder();
        for (Map.Entry<String, String> header : headers.entrySet()) if (HttpHeaders.USER_AGENT.equalsIgnoreCase(header.getKey()) || HttpHeaders.REFERER.equalsIgnoreCase(header.getKey()) || HttpHeaders.COOKIE.equalsIgnoreCase(header.getKey())) builder.add(header.getKey(), header.getValue());
        builder.add(HttpHeaders.RANGE, "bytes=0-");
        return builder.build();
    }

    private static boolean isDouble(String ad) {
        try {
            return Double.parseDouble(ad) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean shouldResolve(String line) {
        return (!line.startsWith("#") && !line.startsWith("http")) || line.startsWith(TAG_KEY);
    }

    private static String resolve(String base, String line) {
        if (line.startsWith(TAG_KEY)) {
            Matcher matcher = REGEX_URI.matcher(line);
            String value = matcher.find() ? matcher.group(1) : null;
            return value == null ? line : line.replace(value, UriUtil.resolve(base, value));
        } else {
            return UriUtil.resolve(base, line);
        }
    }
}
