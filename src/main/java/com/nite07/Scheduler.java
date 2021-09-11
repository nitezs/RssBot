package com.nite07;

import com.nite07.Pojo.RssItem;
import com.nite07.Pojo.Entry;
import com.nite07.Pojo.WebDetails;
import kotlin.Pair;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.*;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Scheduler implements Runnable {

    RssItem c;
    Bot bot;
    Config cfg;

    public Scheduler(RssItem c) {
        this.c = c;
        this.bot = RssBot.myBot;
        this.cfg = RssBot.cfg;
    }

    public MessageChain buildContent(Contact contact, Entry ne) {
        MessageChain res = null;
        WebDetails webDetails = Rss.getWebDetails(Rss.get(ne.link));
        PlainText p;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d = null;
        try {
            d = simpleDateFormat.parse("1970-1-1 00:00:00");
        } catch (ParseException ignore) {
        }
        String time = simpleDateFormat.format(ne.updated);
        assert d != null;
        if (d.toString().equals(ne.updated.toString())) {
            time = "";
        } else {
            time = "\n\t更新：" + time;
        }
        if (webDetails.description != null) {
            p = new PlainText("\t标题：" + ne.title + time + "\n\t简介：" + webDetails.description + "……\n点击查看更多：" + ne.link);
        } else {
            p = new PlainText("\t标题：" + ne.title + time + "\n点击查看更多：" + ne.link);
        }
        if (RssBot.strToLong(c.target) != -1) {
            Group group = bot.getGroup(RssBot.strToLong(c.target));
            if (group != null) {
                Image img = null;
                try {
                    if (webDetails.imageUrl != null) {
                        img = Contact.uploadImage(contact, new URL(webDetails.imageUrl).openConnection().getInputStream());
                    }
                } catch (Exception e) {
                    if (cfg.debug()) {
                        RssBot.logger.warning(e.getMessage());
                        RssBot.logger.warning(Arrays.toString(e.getStackTrace()));
                    }
                }
                res = p.plus(new PlainText(""));
                if (img != null) {
                    res = res.plus(img);
                }
            }
        }
        return res;
    }

    @Override
    public void run() {
        try {
            int counter = 0;
            if (c.mergeNum == 0) {
                c.mergeNum = 1;
            }
            if (c.updateMode == null || c.updateMode.isEmpty()) {
                c.updateMode = "date";
            }
            c.refreshTime = new Date();
            if (cfg.debug()) {
                RssBot.logger.info("ID：" + c.id + "开始抓取");
            }
            String xml = Rss.get(c.url);
            if (xml == null) {
                if (cfg.debug()) {
                    RssBot.logger.warning("ID：" + c.id + "抓取失败，抓取链接为 " + c.url);
                }
                return;
            }
            Pair<String, List<Entry>> p = Rss.parseXML(xml);
            if (p != null) {
                MessageChain msg;
                PlainText p1 = new PlainText("\uD83D\uDCAC " + c.title);
                PlainText p2 = new PlainText(" 更新了新的内容：\n");
                msg = p1.plus(p2);
                Contact contact = null;
                if (c.type.equals("Group")) {
                    if (RssBot.strToLong(c.target) != -1) {
                        contact = bot.getGroup(RssBot.strToLong(c.target));
                    }
                } else if (c.type.equals("Friend")) {
                    if (RssBot.strToLong(c.target) != -1) {
                        contact = bot.getFriend(RssBot.strToLong(c.target));
                    }
                }
                if (contact == null) {
                    return;
                }
                for (Entry ne : p.getSecond()) {
                    boolean exist = false;
                    boolean update = false;
                    for (Entry oe : c.entries) {
                        if (ne.title.equals(oe.title) || ne.link.equals(oe.link)) {
                            exist = true;
                            if (c.updateMode.equals("updated") && ne.updated.getTime() != oe.updated.getTime()) {
                                update = true;
                            }
                            oe.updated = ne.updated;
                            break;
                        }
                    }
                    if (!exist || update) {
                        counter++;
                        c.entries.add(ne);
                        msg.plus(buildContent(contact, ne));
                        if (counter == c.mergeNum) {
                            contact.sendMessage(msg);
                            counter = 0;
                            msg = p1.plus(p2);
                        }
                    }
                }
                if (counter != 0 && counter < c.mergeNum) {
                    contact.sendMessage(msg);
                }
                cfg.saveData();
            }
        } catch (Exception e) {
            if (cfg.debug()) {
                RssBot.logger.warning(e.getMessage());
                RssBot.logger.warning(Arrays.toString(e.getStackTrace()));
            }
        }
        if (cfg.debug()) {
            RssBot.logger.info("ID：" + c.id + "执行完成");
        }
    }
}
