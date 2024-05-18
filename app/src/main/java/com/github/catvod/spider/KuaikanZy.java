package com.github.catvod.spider;

//import com.github.catvod.utils.FileUtil;
import com.github.catvod.spider.base.BaseSpider;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zhixc
 * 快看资源(搜索播放)
 * 部分地区无法正常播放
 */
public class KuaikanZy extends BaseSpider {
    private final String OCR_API = "https://api.nn.ci/ocr/b64/text";
    private String cookie;
    private final String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36";
    private final Pattern detailPattern = Pattern.compile("/index.php/vod/detail/id/(\\d+)\\.html");

    private Map<String, String> getHeaderForSearch() {
        Map<String, String> header = new HashMap<>();
        header.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        header.put("accept-language", "zh-CN,zh;q=0.9");
        header.put("cache-control", "no-cache");
        if (cookie != null) header.put("Cookie", cookie);
        header.put("pragma", "no-cache");
        header.put("sec-ch-ua", "\"Chromium\";v=\"116\", \"Not)A;Brand\";v=\"24\", \"Google Chrome\";v=\"116\"");
        header.put("sec-ch-ua-mobile", "?0");
        header.put("sec-ch-ua-platform", "\"macOS\"");
        header.put("sec-fetch-dest", "document");
        header.put("sec-fetch-mode", "navigate");
        header.put("sec-fetch-site", "none");
        header.put("sec-fetch-user", "?1");
        header.put("upgrade-insecure-requests", "1");
        header.put("User-Agent", userAgent);
        return header;
    }

    private Map<String, String> getHeaderForImage() {
        Map<String, String> header = new HashMap<>();
        header.put("accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
        header.put("accept-language", "zh-CN,zh;q=0.9");
        header.put("cache-control", "no-cache");
//        header.put("cookie", "PHPSESSID=j2t21q5691apso30jllt77ham9");
        header.put("pragma", "no-cache");
        header.put("referer", "https://kkzy.tv/");
        header.put("sec-ch-ua", "\"Chromium\";v=\"116\", \"Not)A;Brand\";v=\"24\", \"Google Chrome\";v=\"116\"");
        header.put("sec-ch-ua-mobile", "?0");
        header.put("sec-ch-ua-platform", "\"macOS\"");
        header.put("sec-fetch-dest", "image");
        header.put("sec-fetch-mode", "no-cors");
        header.put("sec-fetch-site", "same-origin");
        header.put("user-agent", userAgent);
        return header;
    }

    private Map<String, String> getHeaderForOCR() {
        Map<String, String> header = new HashMap<>();
        header.put("User-Agent", userAgent);
        return header;
    }

    private Map<String, String> getHeaderForVerify() {
        Map<String, String> header = new HashMap<>();
        header.put("accept", "*/*");
        header.put("accept-language", "zh-CN,zh;q=0.9");
        header.put("Origin", "https://kkzy.tv");
        header.put("Referer", "https://kkzy.tv/");
        header.put("User-Agent", userAgent);
        return header;
    }

    private String getCookie() {
        if (cookie != null) return cookie;
        for (int i = 0; i < 5; i++) {
            try {
                System.out.println("第" + (i + 1) + "次尝试OCR过验证码......");
                if (verifyByOCR()) break;
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(3 * 1000); // 使当前线程休眠3秒
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return cookie;
    }

    private boolean verifyByOCR() throws Exception {
        // 先获取验证图片
        String verifyImgUrl = "https://kkzy.tv/index.php/verify/index.html";
        Request.Builder builder = new Request.Builder().url(verifyImgUrl).get();
        Map<String, String> headerForImage = getHeaderForImage();
        for (String key : headerForImage.keySet()) {
            builder.addHeader(key, headerForImage.get(key));
        }
        Response rsp = getOkHttpClient().newCall(builder.build()).execute();
        cookie = rsp.header("Set-Cookie").split(";")[0];

        // 将图片进行 ocr 验证，得到识别后的验证码字符串
        byte[] bytes = rsp.body().bytes();
        rsp.close();
        String imgBase64 = Base64.getEncoder().encodeToString(bytes); // iVBORw0KGgoAAAANSUhEUgAAAIAAAAAoBAMAAADEX+97AAAAG1BMVEXz+/4xWYza5u+qvtNJbZrC0uF5lbaSqsVhgai7FUlpAAABgUlEQVRIie1UzW+CMBytfAyvuFA9ds6wK2yLetSMQY+aDMcRDMk4wvyIR6YM/bM3W7oPpjLnaQnv0sePvtf3a5sCUKLEUeDQaXqxXyt2uLzZP0eHt26Rnpfl+3yN8/GAEBPBuMhgZHeCQa6mzpMVMaqCxf54GaYrCYe5WjA+i7ZCTg9BsYElGS77EK/JkCrVF5KqXutAd6fsE1dL1dcyzqsymf7oCtRg7SvQKooA1gvGFthpUFbZkEHvosjL71AeUv2VNaAo4AFtWadNVcImFpKiHoYem9EKXCElzJdphYcKX+8VGBgJylg01So+pc6MjgkGZnxYL0JFpA4cdJTm85ZNkP7E4mlO/pBzaHuhQHs4w70RWU0ywmma9YAtGe0WMpgGEOgaF542nBCnZBCww2vD5WE9MM9tld6DLgaY3GBpDNIa+98s0IPIk2WaYP6+41QXNSztkOYbuKZtI9qC2/Jjwvh0/Gv9F7TWkYf+IvzI0r/78TIcB3F2UoASJf4d3gA+HEC8A47zvgAAAABJRU5ErkJggg==
//        FileUtil.writeByteArrayToFile(bytes, "image.jpg"); // 写到本地文件

        Request.Builder builder2 = new Request.Builder().url(OCR_API).post(RequestBody.create(MediaType.parse("text/plain"), imgBase64));
        Map<String, String> headerForOCR = getHeaderForOCR();
        for (String key : headerForOCR.keySet()) {
            builder2.addHeader(key, headerForOCR.get(key));
        }
        Response rsp2 = getOkHttpClient().newCall(builder2.build()).execute();
        String code = rsp2.body().string();
        rsp2.close();
        System.out.println("ocr接口识别图片结果：" + code);

        // 将验证码字符串对网站尝试进行验证
        String verifyUrl = "https://kkzy.tv/index.php/ajax/verify_check?type=search&verify=" + code;
        Request.Builder builder3 = new Request.Builder().url(verifyUrl).post(RequestBody.create(null, new byte[0]));
        Map<String, String> headerForVerify = getHeaderForVerify();
        for (String key : headerForVerify.keySet()) {
            builder3.addHeader(key, headerForVerify.get(key));
        }
        builder3.addHeader("Cookie", cookie);
        Response rsp3 = getOkHttpClient().newCall(builder3.build()).execute();
        String verifiedResult = rsp3.body().string();
        rsp3.close();
        JSONObject result = new JSONObject(verifiedResult);
        if (result.optString("msg").equals("ok")) return true;
        return false;
    }


    /**
     * 影片详情方法
     *
     * @param ids ids.get(0) 来源于分类方法或搜索方法的 vod_id
     * @return 返回字符串
     */
    @Override
    public String detailContent(List<String> ids) throws Exception {
        // 这个api接口晚上时经常不可用
        /*String vodId = ids.get(0);
        // https://kuaikan-api.com/api.php/provide/vod/?ac=detail&ids=66351
        String detailUrl = "https://kuaikan-api.com/api.php/provide/vod/?ac=detail&ids=" + vodId;
        return req(detailUrl, getHeader());*/

        // 所以改成网页源码抓取了
        String vodId = ids.get(0);
        String link = "https://kkzy.tv/index.php/vod/detail/id/" + vodId + ".html";
        String html = req(link, getHeader());
        Document doc = Jsoup.parse(html);
        String name = doc.select(".countlist > h1").first().ownText();
        String pic = doc.select(".countimg > img").attr("src");
        String typeName = "";
        String year = "";
        String area = "";
        String remark = "";
        String director = "";
        String actor = "";
        String brief = doc.select("div[class=js row] p").text();
        Elements elements = doc.select(".countlist > div");
        for (Element element : elements) {
            String text = element.text();
            if (text.startsWith("类型：")) typeName = text.replaceAll("类型：", "").trim();
            if (text.startsWith("上映日期：")) year = text.replaceAll("上映日期：", "").trim();
            if (text.startsWith("地区：")) area = text.replaceAll("地区：", "").trim();
            if (text.startsWith("状态：")) remark = text.replaceAll("状态：", "").trim();
            if (text.startsWith("导演：")) director = text.replaceAll("导演：", "").trim();
            if (text.startsWith("主演：")) actor = text.replaceAll("主演：", "").trim();
        }

        Map<String, String> playMap = new LinkedHashMap<>();
        Elements circuits = doc.select("[class=tabletitle row]");
        Elements sourceList = doc.select("[class=listcount col]");
        for (int i = 0; i < sourceList.size(); i++) {
            String circuitName = circuits.get(i).selectFirst("h1").text().replace("播放类型：", "");
            if (!circuitName.contains("m3u8")) continue;
            Element source = sourceList.get(i);
            Elements items = source.select("div[class=listitems row]");
            List<String> vodItems = new ArrayList<>();
            for (Element item : items) vodItems.add(item.select("a.play-item").text());
            if (vodItems.size() > 0) playMap.put(circuitName, String.join("#", vodItems));
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
        vod.put("vod_content", brief); // 简介 选填
        if (playMap.size() > 0) {
            vod.put("vod_play_from", String.join("$$$", playMap.keySet()));
            vod.put("vod_play_url", String.join("$$$", playMap.values()));
        }
        JSONArray jsonArray = new JSONArray().put(vod);
        JSONObject result = new JSONObject().put("list", jsonArray);
        return result.toString();
    }

    /**
     * 搜索
     *
     * @param key 关键字/词
     * @return 返回值
     */
    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        String searchUrl = "https://kkzy.tv/index.php/vod/search.html?wd=" + URLEncoder.encode(key);
        String html = req(searchUrl, getHeaderForSearch());
        if (html.contains("安全验证")) {
            Map<String, String> headerForSearch = getHeaderForSearch();
            headerForSearch.put("Cookie", getCookie());
            html = req(searchUrl, headerForSearch);
        }
        Elements rows = Jsoup.parse(html).select("table[class=table align-middle] tbody > tr");
        JSONArray videos = new JSONArray();
        for (Element row : rows) {
            Element a = row.select("a").get(0);
            Matcher matcher = detailPattern.matcher(a.attr("href"));
            String vodId = "";
            if (matcher.find()) vodId = matcher.group(1);
            if (vodId.equals("")) continue;
            String name = a.attr("title");
            String pic = "";
            String remark = row.select(".badge-info").text();

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
        JSONObject result = new JSONObject();
        result.put("parse", 0);
        result.put("header", getHeader().toString());
        result.put("playUrl", "");
        result.put("url", id);
        return result.toString();
    }
}
