package com.nite07;

import com.nite07.Pojo.RssItem;
import com.nite07.Pojo.Entry;
import com.nite07.Pojo.WebDetails;
import kotlin.Pair;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
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
        MessageChainBuilder messages = new MessageChainBuilder();
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
            time = "\n\t\uD83D\uDD5B更新：" + time;
        }
        if (webDetails.description != null) {
            p = new PlainText("\t\u270F\uFE0F标题：" + ne.title + time + "\n\t\uD83C\uDFF7简介：" + webDetails.description + "……\n\t\uD83D\uDD0D点击查看更多：" + ne.link + "\n");
        } else {
            p = new PlainText("\t\u270F\uFE0F标题：" + ne.title + time + "\n\t\uD83D\uDD0D点击查看更多：" + ne.link + "\n");
        }
        Image img = null;
        if (c.showImage) {
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
        }
        messages.append(p);
        if (img != null && c.showImage) {
            messages.append(img).append("\n");
        }
        return messages.build();
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
                MessageChainBuilder msg = new MessageChainBuilder();
                PlainText title = new PlainText("\uD83D\uDCAC " + c.title + " 更新了新的内容：\n");
                msg.append(title);
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
                        if (!exist) {
                            c.entries.add(ne);
                        }
                        msg.append(buildContent(contact, ne)).append(new PlainText("\n"));
                        if (counter == c.mergeNum) {
                            msg.remove(msg.size() - 1);
                            contact.sendMessage(msg.build());
                            counter = 0;
                            msg = new MessageChainBuilder().append(title);
                        }
                    }
                }
                if (counter != 0 && counter < c.mergeNum) {
                    msg.remove(msg.size() - 1);
                    contact.sendMessage(msg.build());
                }
                cfg.saveData();
            }
        } catch (Exception e) {
            if (cfg.debug()) {
                RssBot.logger.warning(e.getMessage());
                RssBot.logger.warning(Arrays.toString(e.getStackTrace()));
            }
        }
    }
}
