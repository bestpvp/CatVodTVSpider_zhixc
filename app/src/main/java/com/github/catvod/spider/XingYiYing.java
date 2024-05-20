package com.github.catvod.spider;

import com.github.catvod.spider.base.BaseSpider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author zhixc
 * 星易影
 */
public class XingYiYing extends BaseSpider {
    private final String siteUrl = "https://www.xingyiying.com";

    private String parseVodInfo(Element element) {
        StringBuilder sb = new StringBuilder();
        for (Element a : element.select("a")) sb.append(a.text()).append(" / ");
        return sb.toString();
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String vodId = ids.get(0);
        // https://www.xingyiying.com/index.php/vod/detail/id/217717.html
        // https://www.xingyiying.com/index.php/vod/detail/id/183491.html
        // https://www.xingyiying.com/index.php/vod/detail/id/31606.html
        String detailUrl = siteUrl + "/index.php/vod/detail/id/" + vodId + ".html";
        String html = req(detailUrl, getHeader());
        Document doc = Jsoup.parse(html);
        String name = doc.select("h1").text();
        String pic = doc.select(".module-info-poster img").attr("data-original");
        String typeName = doc.select(".module-info-tag-link").text();
        String year = "";
        String area = "";

        String actor = "";
        String director = "";
        String remark = "";
        Elements elements = doc.select(".module-info-items > .module-info-item");
        for (Element element : elements) {
            String text = element.text();
            if (text.startsWith("导演")) director = parseVodInfo(element);
            if (text.startsWith("主演")) actor = parseVodInfo(element);
            if (text.startsWith("集数")) remark = text;
            if (text.startsWith("备注")) remark = text.replaceAll("备注：", "");
        }
        String description = doc.select(".module-info-introduction").text();

        Elements circuits = doc.select("#y-playList > .tab-item");
        Elements sourceList = doc.select("#panel1");
        Map<String, String> playMap = new LinkedHashMap<>();
        for (int i = 0; i < sourceList.size(); i++) {
            String spanText = circuits.get(i).select("span").text();
            String smallText = circuits.get(i).select("small").text();
            String circuitName = spanText + "【共" + smallText + "集】";
            List<String> vodItems = new ArrayList<>();
            Elements aList = sourceList.get(i).select("a");
            for (Element a : aList) {
                // https://www.xingyiying.com/index.php/vod/play/id/217405/sid/1/nid/1.html
                String episodeUrl = siteUrl + a.attr("href");
                String episodeName = a.text();
                vodItems.add(episodeName + "$" + episodeUrl);
            }
            if (vodItems.size() > 0) {
                playMap.put(circuitName, String.join("#", vodItems));
            }
        }

        JSONObject vod = new JSONObject();
        vod.put("vod_id", ids.get(0));
        vod.put("vod_name", name); // 影片名称
        vod.put("vod_pic", pic); // 图片
        vod.put("type_name", typeName); // 影片类型 选填
        vod.put("vod_year", year); // 年份 选填
        vod.put("vod_area", area); // 地区 选填
        vod.put("vod_remarks", remark); // 备注 选填
        vod.put("vod_actor", actor); // 主演 选填
        vod.put("vod_director", director); // 导演 选填
        vod.put("vod_content", description); // 简介 选填
        if (playMap.size() > 0) {
            vod.put("vod_play_from", String.join("$$$", playMap.keySet()));
            vod.put("vod_play_url", String.join("$$$", playMap.values()));
        }
        JSONArray jsonArray = new JSONArray().put(vod);
        JSONObject result = new JSONObject().put("list", jsonArray);
        return result.toString();
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        return searchContent(key, quick, "1");
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) throws Exception {
        String keyword = URLEncoder.encode(key);
        // https://www.xingyiying.com/index.php/ajax/suggest?mid=1&wd=斗破
        String searchUrl = siteUrl + "/index.php/ajax/suggest?mid=1&wd=" + keyword;
//        if (!pg.equals("1")) searchUrl = siteUrl + "/s-" + keyword + "---------" + pg + ".html";
        if (!pg.equals("1")) return "";
        JSONArray videos = new JSONArray();
        JSONObject searchResult = new JSONObject(req(searchUrl, getHeader(siteUrl + "/")));
        JSONArray items = searchResult.optJSONArray("list");
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String vodId = item.optString("id");
            String name = item.optString("name");
            String pic = item.optString("pic");
            String remark = "";

            JSONObject vod = new JSONObject();
            vod.put("vod_id", vodId);
            vod.put("vod_name", name);
            vod.put("vod_pic", pic);
            vod.put("vod_remarks", remark);
            videos.put(vod);
        }
        JSONObject result = new JSONObject();
        result.put("list", videos);
        return result.toString();
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        String lastUrl = id;
        int parse = 1;
        String headerStr = getHeader().toString();
        String html = req(lastUrl, getHeader(siteUrl + "/"));
        String player_aaaa = find(Pattern.compile("player_aaaa=(.*?)</script>"), html);
        JSONObject jsonObject = new JSONObject(player_aaaa);
        String url = jsonObject.optString("url");
        if (url.contains(".m3u8") || url.contains(".mp4")) {
            lastUrl = url;
            parse = 0;
            Map<String, String> header = new HashMap<>();
            header.put("Accept", "*/*");
            header.put("User-Agent", CHROME);
            headerStr = header.toString();
        }

        JSONObject result = new JSONObject();
        result.put("parse", parse);
        result.put("header", headerStr);
        result.put("playUrl", "");
        result.put("url", lastUrl);
        return result.toString();
    }
}
