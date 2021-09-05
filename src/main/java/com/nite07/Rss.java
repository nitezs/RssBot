package com.nite07;

import com.nite07.Pojo.Entry;
import com.nite07.Pojo.WebDetails;
import kotlin.Pair;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class Rss {

    /**
     * 处理 XML 数据
     *
     * @param xml XML
     * @return 第一个参数是站点标题, 第二个参数是文章列表
     */
    public static Pair<String, List<Entry>> parseXML(String xml) {
        try {
            if (xml == null) {
                return null;
            }
            Document doc = DocumentHelper.parseText(xml);
            Element root = doc.getRootElement();
            String type = root.getName();
            if (type.equals("rss")) {
                return rssType(root);
            } else if (type.equals("feed")) {
                return feedType(root);
            }
        } catch (DocumentException ignore) {
        }
        return null;
    }

    /**
     * 处理 feed 格式的数据
     *
     * @param root 根节点
     * @return 第一个参数是站点标题, 第二个参数是文章列表
     */
    public static Pair<String, List<Entry>> feedType(Element root) {
        String title = String.valueOf(root.elements("title").get(0).getData());
        List<Element> elements = root.elements("entry");
        List<Entry> entries = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
        for (Element e : elements) {
            String t = (String) e.elements("title").get(0).getData();
            String l = (String) e.elements("link").get(0).attribute("href").getData();
            Date d = null;
            try {
                d = simpleDateFormat.parse(String.valueOf(e.elements("updated").get(0).getData()));
            } catch (Exception ignore) {
                try {
                    d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("1970-01-01 00:00:00");
                } catch (ParseException ignore1) {
                }
            }
            entries.add(new Entry(t, l, d));
        }
        return new Pair<>(title, entries);
    }

    /**
     * 处理 rss 格式的数据
     *
     * @param root 根节点
     * @return 第一个参数是站点标题, 第二个参数是文章列表
     */
    public static Pair<String, List<Entry>> rssType(Element root) {
        Element channel = root.elements("channel").get(0);
        String title = String.valueOf(channel.elements("title").get(0).getData());
        List<Element> elements = channel.elements("item");
        List<Entry> entries = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        for (Element e : elements) {
            String t = (String) e.elements("title").get(0).getData();
            String l = (String) e.elements("link").get(0).getData();
            Date d = null;
            try {
                d = simpleDateFormat.parse(String.valueOf(e.elements("pubDate").get(0).getData()));
            } catch (Exception ignore) {
                try {
                    d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("1970-01-01 00:00:00");
                } catch (ParseException ignore1) {
                }
            }
            entries.add(new Entry(t, l, d));
        }
        return new Pair<>(title, entries);
    }

    /**
     * 获取网页数据
     *
     * @param url 网页链接
     * @param cfg 配置实例（获取代理信息）
     * @return html
     */
    public static String get(String url, Config cfg) {
        StringBuilder stringBuilder = new StringBuilder();
        String credential = cfg.getProxyCredential();
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder().connectTimeout(5, TimeUnit.SECONDS).proxy(cfg.getProxy()).build();
        Request request;
        if (credential != null) {
            request = new Request.Builder().url(url).addHeader("Proxy-Authorization", credential).build();
        } else {
            request = new Request.Builder().url(url).build();
        }
        Response response;
        try {
            response = okHttpClient.newCall(request).execute();
            BufferedReader bufferedReader = new BufferedReader(Objects.requireNonNull(response.body()).charStream());
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 获取简介,图片等信息
     *
     * @return WebDetails
     */
    public static WebDetails getWebDetails(String html) {
        WebDetails webDetails = new WebDetails();
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        Elements es = doc.select("meta[property]");
        if (es.size() == 0) {
            es = doc.select("meta[name=description]");
            webDetails.description = es.get(0).attr("content");
        } else {
            for (org.jsoup.nodes.Element e : es) {
                if (e.attr("property").equals("og:description")) {
                    webDetails.description = e.attr("content");
                }
                if (e.attr("property").equals("og:image")) {
                    webDetails.imageUrl = e.attr("content");
                }
            }
        }
        return webDetails;
    }
}
