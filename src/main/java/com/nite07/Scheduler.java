package com.nite07;

import com.nite07.Pojo.ConfigData;
import com.nite07.Pojo.ConfigItem;
import com.nite07.Pojo.Entry;
import kotlin.Pair;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.utils.MiraiLogger;

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
     *
     * @param target 发送目标Id(group或friend)
     * @param type   发送类型(group或friend)
     * @param msg    发送内容
     * @return 是否成功发送
     */
    public void sendMessage(String target, String type, PlainText msg) {
        if (bot != null) {
            if (type.equals("Group")) {
                if (RssBot.strToLong(target) != -1) {
                    Group group = bot.getGroup(RssBot.strToLong(target));
                    if (group != null) {
                        group.sendMessage(msg);
                    }
                }
            } else if (type.equals("Friend")) {
                if (RssBot.strToLong(target) != -1) {
                    Friend friend = bot.getFriend(RssBot.strToLong(target));
                    if (friend != null) {
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
                    sendMessage(c.target, c.type, new PlainText(c.title + "更新了新的内容\n" + ne.title + "\n" + ne.link));
                }
            }
            c.entries = p.getSecond();
            cfg.saveConfig();
        }
    }
}
