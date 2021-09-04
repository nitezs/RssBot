package com.nite07;

import com.nite07.Pojo.Entry;
import com.nite07.Pojo.WebDetails;
import kotlin.Pair;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class Rss {

    public static Pair<String, List<Entry>> parseXML(String xml) {
        try {
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
     * 获取简介,图片等信息
     *
     * @return WebDetails
     */
    public static WebDetails getWebDetails(String url) {
        WebDetails webDetails = new WebDetails();
        try {
            Connection conn = Jsoup.connect(url);
            org.jsoup.nodes.Document doc = conn.userAgent("User-Agent,Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36 Edg/92.0.902.84").get();
            Elements es = doc.select("meta[property]");
            for (org.jsoup.nodes.Element e : es) {
                if (e.attr("property").equals("og:description")) {
                    webDetails.description = e.attr("content");
                }
                if (e.attr("property").equals("og:image")) {
                    webDetails.imageUrl = e.attr("content");
                }
            }
            if (webDetails.description == null) {
                es = doc.select("meta[name=description]");
                webDetails.description = es.get(0).attr("content");
            }
        } catch (Exception ignore) {
        }
        return webDetails;
    }
}
