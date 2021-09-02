package com.nite07;

import com.nite07.Pojo.ConfigItem;
import com.nite07.Pojo.Entry;
import kotlin.Pair;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.Listener;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class RssBot extends JavaPlugin {
    public static final RssBot INSTANCE = new RssBot();
    public Config cfg = new Config();
    Map<Long, ScheduledFuture<?>> tasks = new HashMap<>();
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
    Bot myBot = null;

    private RssBot() {
        super(new JvmPluginDescriptionBuilder("com.nite07.RssBot", "1.2")
                .name("RssBot")
                .info("A Rss Bot")
                .author("Nite07")
                .build());
    }

    /**
     * 将 String 转换为 long,如果不能转换返回 -1
     *
     * @param s String
     * @return long
     */
    static public long strToLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 插件加载事件(监听消息)
     */
    @Override
    public void onEnable() {
        getLogger().info("RssBot 插件已加载");
        Listener<MessageEvent> messageEventListener = GlobalEventChannel.INSTANCE.subscribeAlways(MessageEvent.class, g -> {
            runCMD(g.getMessage().contentToString(), g);
        });
        Listener<BotOnlineEvent> botOnlineEventListener = GlobalEventChannel.INSTANCE.subscribeOnce(BotOnlineEvent.class, g -> {
            myBot = getBotInstance();
            if (myBot != null) {
                loadTasks();
            } else {
                getLogger().info("RssBot使用账号未登录");
            }
        });
        Listener<NewFriendRequestEvent> newFriendRequestEventListener = GlobalEventChannel.INSTANCE.subscribeAlways(NewFriendRequestEvent.class, g -> {
            if (cfg.getAutoAcceptFriendApplication()) {
                g.accept();
            }
        });
        Listener<BotInvitedJoinGroupRequestEvent> botInvitedJoinGroupRequestEventListener = GlobalEventChannel.INSTANCE.subscribeAlways(BotInvitedJoinGroupRequestEvent.class, g -> {
            if (cfg.getAutoAcceptGroupApplication()) {
                g.accept();
            }
        });
        Listener<BotLeaveEvent> botLeaveEventListener = GlobalEventChannel.INSTANCE.subscribeAlways(BotLeaveEvent.class, g -> {
            cfg.clearConfigItem(String.valueOf(g.getGroup().getId()), "Group");
        });
        Listener<FriendDeleteEvent> friendDeleteEventListener = GlobalEventChannel.INSTANCE.subscribeAlways(FriendDeleteEvent.class, g -> {
            cfg.clearConfigItem(String.valueOf(g.getFriend().getId()), "Friend");
        });
    }

    /**
     * 获取消息的来源类型
     *
     * @param g MessageEvent
     * @return 好友消息返回"Friend",群消息返回"Group",其他消息返回"Other"
     */
    public String getMessageType(MessageEvent g) {
        if (g.getSubject() instanceof Group) {
            return "Group";
        } else if (g.getSubject() instanceof Friend) {
            return "Friend";
        }
        return "Other";
    }

    /**
     * 检查发送者是否有管理权限
     *
     * @param g MessageEvent
     * @return boolean
     */
    public boolean checkSenderPerm(MessageEvent g) {
        if (g.getSubject() instanceof Group) {
            MemberPermission sender = ((GroupMessageEvent) g).getPermission();
            return sender == MemberPermission.OWNER || sender == MemberPermission.ADMINISTRATOR;
        } else return g.getSubject() instanceof Friend;
    }

    /**
     * 检测 RssUrl 是否可访问
     *
     * @param url RssUrl
     * @return boolean
     */
    public boolean checkUrl(String url) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception ignore) {
            return false;
        }
    }

    /**
     * 判断参数是否为数字
     *
     * @param s String
     * @return boolean
     */
    public boolean isDigit(String s) {
        boolean flag = true;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                flag = false;
            }
        }
        return flag;
    }

    /**
     * 判断发信者是否是订阅的合法管理者
     *
     * @param g MessageEvent
     * @param c ConfigItem
     * @return boolean
     */
    public boolean isSubOwner(MessageEvent g, ConfigItem c) {
        return String.valueOf(g.getSubject().getId()).equals(c.target);
    }

    /**
     * 被动发送信息
     *
     * @param g   MessageEvent
     * @param msg String 信息内容
     */
    public void sendMessage(MessageEvent g, String msg) {
        if (getMessageType(g).equals("Group")) {
            PlainText t = new PlainText("\n" + msg);
            At at = new At(g.getSender().getId());
            MessageChain mc = at.plus(t);
            g.getSubject().sendMessage(mc);
        }
        if (getMessageType(g).equals("Friend")) {
            PlainText t = new PlainText(msg);
            g.getSender().sendMessage(t);
        }
    }

    public Bot getBotInstance() {
        for (Bot bot : Bot.getInstances()) {
            if (String.valueOf(bot.getId()).equals(cfg.getBotId())) {
                return bot;
            }
        }
        return null;
    }

    /**
     * 插件启用后添加 Tasks
     */
    public void loadTasks() {
        List<ConfigItem> cis = cfg.getConfigItems();
        if (cis != null) {
            for (ConfigItem c : cis) {
                tasks.put(c.id, executor.scheduleWithFixedDelay(new Scheduler(c, myBot, getLogger(), cfg), 0, c.interval, TimeUnit.MINUTES));
            }
        }
    }

    /**
     * 命令处理方法
     *
     * @param cmd 命令行
     * @param g   MessageEvent
     */
    public void runCMD(String cmd, MessageEvent g) {
        if (cmd.startsWith("#")) {
            String[] slice = cmd.split(" ");
            int paramNum = slice.length - 1;
            if (cmd.startsWith("#sub")) {
                if (checkSenderPerm(g)) {
                    if (checkUrl(slice[1])) {
                        if (paramNum == 1) {
                            if (cfg.canAddSub()) {
                                long id = cfg.getNewId();
                                Pair<String, List<Entry>> p = Rss.parseXML(slice[1]);
                                ConfigItem c = new ConfigItem(id, getMessageType(g), String.valueOf(g.getSubject().getId()), slice[1], 10, p.getFirst(), p.getSecond());
                                cfg.addConfigItem(c);
                                tasks.put(id, executor.scheduleWithFixedDelay(new Scheduler(c, myBot, getLogger(), cfg), 0, 10, TimeUnit.MINUTES));
                                sendMessage(g, "订阅设置成功\nID：" + id + "\n标题：" + c.title + "\nUrl：" + c.url + "\n抓取频率：10分钟");
                            } else {
                                sendMessage(g, "总订阅量已达上限，你可以自己搭建一个RssBot\nhttps://github.com/NiTian1207/RssBot");
                            }
                        } else if (paramNum == 2) {
                            if (isDigit(slice[2])) {
                                if (cfg.canAddSub()) {
                                    long id = cfg.getNewId();
                                    Pair<String, List<Entry>> p = Rss.parseXML(slice[1]);
                                    ConfigItem c = new ConfigItem(id, getMessageType(g), String.valueOf(g.getSubject().getId()), slice[1], Integer.parseInt(slice[2]), p.getFirst(), p.getSecond());
                                    cfg.addConfigItem(c);
                                    tasks.put(id, executor.scheduleWithFixedDelay(new Scheduler(c, myBot, getLogger(), cfg), 0, Integer.parseInt(slice[2]), TimeUnit.MINUTES));
                                    sendMessage(g, "订阅设置成功\nID：" + id + "\n标题：" + c.title + "\nUrl：" + c.url + "\n抓取频率：" + slice[2] + "分钟");
                                } else {
                                    sendMessage(g, "总订阅量已达上限，你可以自己搭建一个RssBot\nhttps://github.com/NiTian1207/RssBot");
                                }
                            } else {
                                sendMessage(g, "参数错误");
                            }
                        } else {
                            sendMessage(g, "参数错误");
                        }
                    } else {
                        sendMessage(g, "链接无法访问");
                    }
                } else {
                    sendMessage(g, "没有操作权限");
                }
            } else if (cmd.startsWith("#unsub")) {
                if (checkSenderPerm(g)) {
                    if (paramNum == 1 && strToLong(slice[1]) != -1) {
                        ConfigItem c = cfg.getConfigItem(Long.parseLong(slice[1]));
                        if (c != null) {
                            if (isSubOwner(g, c)) {
                                tasks.get(strToLong(slice[1])).cancel(true);
                                tasks.remove(strToLong(slice[1]));
                                cfg.removeConfigItem(c);
                                sendMessage(g, "订阅已取消\n" + "ID：" + c.id + "\n标题：" + c.title + "\nUrl：" + c.url);
                            } else {
                                sendMessage(g, "没有操作权限");
                            }
                        } else {
                            sendMessage(g, "未找到订阅");
                        }
                    } else {
                        sendMessage(g, "参数错误");
                    }
                } else {
                    sendMessage(g, "没有操作权限");
                }
            } else if (cmd.startsWith("#setinterval")) {
                if (checkSenderPerm(g)) {
                    if (paramNum == 2 && strToLong(slice[1]) != -1 && strToLong(slice[2]) != -1) {
                        ConfigItem c = cfg.getConfigItem(Long.parseLong(slice[1]));
                        if (c != null) {
                            tasks.get(strToLong(slice[1])).cancel(true);
                            tasks.put(strToLong(slice[1]), executor.scheduleWithFixedDelay(new Scheduler(c, myBot, getLogger(), cfg), 0, strToLong(slice[2]), TimeUnit.MINUTES));
                            c.interval = Integer.parseInt(slice[2]);
                            cfg.saveConfig();
                            sendMessage(g, "抓取频率已修改\n" + "ID：" + c.id + "\n标题：" + c.title + "\nUrl：" + c.url + "\n抓取频率：" + c.interval + "分钟");
                        } else {
                            sendMessage(g, "未找到订阅");
                        }
                    } else {
                        sendMessage(g, "参数错误");
                    }
                } else {
                    sendMessage(g, "没有操作权限");
                }
            } else if (cmd.equals("#list")) {
                if (checkSenderPerm(g)) {
                    if (paramNum == 0) {
                        List<ConfigItem> cs = cfg.getOnesConfigItems(String.valueOf(g.getSubject().getId()));
                        if (cs.size() != 0) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("当前有").append(cs.size()).append("条订阅:\n");
                            for (ConfigItem c : cs) {
                                stringBuilder.append("\nID：").append(c.id).append("\n标题：").append(c.title).append("\nUrl").append(c.url).append("\n");
                            }
                            sendMessage(g, stringBuilder.toString());
                        } else {
                            sendMessage(g, "当前无订阅");
                        }
                    } else {
                        sendMessage(g, "参数错误");
                    }
                } else {
                    sendMessage(g, "没有操作权限");
                }
            } else if (cmd.equals("#help")) {
                String t = "#sub <url> [interval(minute)]\n" +
                        "#unsub <id>\n" +
                        "#setinterval <id> <interval(minute)>\n" +
                        "#list\n" +
                        "#detail <id>\n" +
                        "<>为必须参数，[]为可选参数";
                sendMessage(g, t);
            } else if (cmd.startsWith("#detail")) {
                if (checkSenderPerm(g)) {
                    if (paramNum == 1) {
                        if (strToLong(slice[1]) != -1) {
                            ConfigItem c = cfg.getConfigItem(strToLong(slice[1]));
                            sendMessage(g, "ID：" + c.id + "\n标题：" + c.title + "\nUrl：" + c.url + "\n抓取频率：" + c.interval + "分钟\n");
                        } else {
                            sendMessage(g, "参数错误");
                        }
                    } else {
                        sendMessage(g, "参数错误");
                    }
                } else {
                    sendMessage(g, "没有操作权限");
                }
            }
        }
    }
}

