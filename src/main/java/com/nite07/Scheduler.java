package com.nite07;

import com.nite07.Pojo.ConfigItem;
import com.nite07.Pojo.Entry;
import com.nite07.Pojo.WebDetails;
import kotlin.Pair;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;
import net.mamoe.mirai.utils.MiraiLogger;

import java.net.URL;
import java.util.List;

public class Scheduler implements Runnable {

    ConfigItem c;
    Bot bot;
    MiraiLogger logger;
    Config cfg;

    public Scheduler(ConfigItem c, Bot bot, MiraiLogger logger, Config cfg) {
        this.c = c;
        this.bot = bot;
        this.logger = logger;
        this.cfg = cfg;
        //logger.info("任务初始化完成/" + c.id + "/" + c.url);
    }

    /**
     * 主动发送信息
     */
    public void sendMessage(String target, String type, String imageUrl, String title, String description, String link) {
        if (bot != null) {
            PlainText p1 = new PlainText(c.title);
            PlainText p2 = new PlainText(" 更新了新的内容：\n\t");
            MessageChain msg = p1.plus(p2);
            if (type.equals("Group")) {
                if (RssBot.strToLong(target) != -1) {
                    Group group = bot.getGroup(RssBot.strToLong(target));
                    if (group != null) {
                        Image img = null;
                        try {
                            img = Contact.uploadImage(group, new URL(imageUrl).openConnection().getInputStream());
                        } catch (Exception ignore) {
                        }
                        msg = msg.plus(new PlainText(title + "\n\t" + description + "\n\t" + link));
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
                        } catch (Exception ignore) {
                        }
                        msg = msg.plus(new PlainText(title + "\n\t" + description + "\n\t" + link));
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
        //logger.info("开始抓取" + c.id);
        Pair<String, List<Entry>> p = Rss.parseXML(c.url);
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
                    //logger.info(c.title + "更新了新的内容\n" + ne.title + "\n" + ne.link);
                    WebDetails webDetails = Rss.getWebDetails(ne.link);
                    sendMessage(c.target, c.type, webDetails.imageUrl, ne.title, webDetails.description, ne.link);
                }
            }
            c.entries = p.getSecond();
            cfg.saveConfig();
        }
    }
}
