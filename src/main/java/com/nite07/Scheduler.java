package com.nite07;

import com.nite07.Pojo.RssItem;
import com.nite07.Pojo.Entry;
import com.nite07.Pojo.WebDetails;
import kotlin.Pair;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.MiraiLogger;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class Scheduler implements Runnable {

    RssItem c;
    Bot bot;
    MiraiLogger logger;
    Config cfg;

    public Scheduler(RssItem c, Bot bot, MiraiLogger logger, Config cfg) {
        this.c = c;
        this.bot = bot;
        this.logger = logger;
        this.cfg = cfg;
    }

    /**
     * 主动发送信息
     */
    public void sendMessage(String target, String type, String imageUrl, String title, String description, String link) {
        if (bot != null) {
            PlainText p1 = new PlainText("\uD83D\uDCAC " + c.title);
            PlainText p2 = new PlainText(" 更新了新的内容：\n");
            PlainText p3;
            if (description != null) {
                p3 = new PlainText("\t标题：" + title + "\n\t简介：" + description + "……\n点击查看更多：" + link);
            } else {
                p3 = new PlainText("\t标题：" + title + "\n点击查看更多：" + link);
            }
            MessageChain msg = p1.plus(p2);
            if (type.equals("Group")) {
                if (RssBot.strToLong(target) != -1) {
                    Group group = bot.getGroup(RssBot.strToLong(target));
                    if (group != null) {
                        Image img = null;
                        try {
                            img = Contact.uploadImage(group, new URL(imageUrl).openConnection().getInputStream());
                        } catch (Exception e) {
                            if (cfg.debug()) {
                                logger.warning(Arrays.toString(e.getStackTrace()));
                            }
                        }
                        msg = msg.plus(p3);
                        if (img != null) {
                            msg = msg.plus(img);
                        }
                        group.sendMessage(msg);
                    }
                }
            } else if (type.equals("Friend")) {
                if (RssBot.strToLong(target) != -1) {
                    Friend friend = bot.getFriend(RssBot.strToLong(target));
                    if (friend != null) {
                        Image img = null;
                        try {
                            img = Contact.uploadImage(friend, new URL(imageUrl).openConnection().getInputStream());
                        } catch (Exception e) {
                            if (cfg.debug()) {
                                logger.warning(Arrays.toString(e.getStackTrace()));
                            }
                        }
                        msg = msg.plus(p3);
                        if (img != null) {
                            msg = msg.plus(img);
                        }
                        friend.sendMessage(msg);
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            if (cfg.debug()) {
                logger.info("ID：" + c.id + "开始抓取");
            }
            String xml = Rss.get(c.url, cfg);
            if (xml == null) {
                if (cfg.debug()) {
                    logger.warning("ID：" + c.id + "抓取失败，抓取链接为 " + c.url);
                }
                return;
            }
            Pair<String, List<Entry>> p = Rss.parseXML(xml);
            if (p != null) {
                for (Entry ne : p.getSecond()) {
                    boolean exist = false;
                    for (Entry oe : c.entries) {
                        if (ne.title.equals(oe.title) || ne.link.equals(oe.link)) {
                            exist = true;
                            break;
                        }
                    }
                    if (!exist) {
                        c.entries.add(ne);
                        WebDetails webDetails = Rss.getWebDetails(Rss.get(ne.link, cfg));
                        sendMessage(c.target, c.type, webDetails.imageUrl, ne.title, webDetails.description, ne.link);
                    }
                }
                cfg.saveData();
            }
        } catch (Exception e) {
            if (cfg.debug()) {
                logger.warning(e.getMessage());
                logger.warning(Arrays.toString(e.getStackTrace()));
            }
        }
    }
}
