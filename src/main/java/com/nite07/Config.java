package com.nite07;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.nite07.Pojo.ConfigData;
import com.nite07.Pojo.RssItem;
import net.mamoe.mirai.utils.MiraiLogger;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Config {
    String configDir = "config/RssBot/";
    String configName = "config.json";
    String configPath = configDir + configName;
    String datafileDir = "data/RssBot/";
    String dataName = "data.json";
    String dataPath = datafileDir + dataName;
    ReentrantLock lock = new ReentrantLock();
    MiraiLogger logger;
    private ConfigData cfg;
    private List<RssItem> rssItems;

    public Config(MiraiLogger logger) {
        this.logger = logger;
        if (!configExist()) {
            initConfig();
        } else {
            File file = new File(configPath);
            String cfg = null;
            try (FileReader fileReader = new FileReader(file);
                 BufferedReader bufferedReader = new BufferedReader(fileReader)) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                cfg = stringBuilder.toString();
            } catch (Exception ignored) {
            }
            this.cfg = JSON.parseObject(cfg, ConfigData.class);
        }
        if (!dataExist()) {
            initData();
        } else {
            File file = new File(dataPath);
            String data = null;
            try (FileReader fileReader = new FileReader(file);
                 BufferedReader bufferedReader = new BufferedReader(fileReader)) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                data = stringBuilder.toString();
            } catch (Exception ignored) {
            }
            this.rssItems = JSON.parseArray(data, RssItem.class);
            if (this.rssItems == null) {
                this.rssItems = new ArrayList<>();
            }
        }
        logConfig();
    }

    public void logConfig() {
        StringBuilder log = new StringBuilder("\nRssBotID：\t\t")
                .append(cfg.botId)
                .append("\n自动接受好友请求：\t")
                .append(cfg.autoAcceptFriendApplication)
                .append("\n自动接受群邀请：\t")
                .append(cfg.autoAcceptGroupApplication)
                .append("\n最大订阅数量：\t\t")
                .append(cfg.maxSub);
        if ((cfg.proxy_type.equalsIgnoreCase("http") || cfg.proxy_type.equalsIgnoreCase("socks"))
                && !cfg.proxy_address.isEmpty() && cfg.proxy_port != 0) {
            log.append("\n代理类型：\t\t")
                    .append(cfg.proxy_type)
                    .append("\n代理地址：\t\t")
                    .append(cfg.proxy_address)
                    .append("\n代理端口：\t\t")
                    .append(cfg.proxy_port);
            if (!cfg.proxy_username.isEmpty() && !cfg.proxy_password.isEmpty()) {
                log.append("\n代理用户名：\t\t")
                        .append(cfg.proxy_username)
                        .append("\n代理密码：\t\t")
                        .append(cfg.proxy_password);
            }
        } else {
            log.append("\n未设置代理");
        }
        logger.info(log.toString());
    }

    public List<RssItem> getRssItems() {
        return rssItems;
    }

    public void initConfig() {
        cfg = new ConfigData();
        cfg.botId = "123456789";
        cfg.autoAcceptFriendApplication = true;
        cfg.autoAcceptGroupApplication = true;
        cfg.maxSub = 100;
        cfg.proxy_address = "";
        cfg.proxy_port = 0;
        cfg.proxy_type = "";
        cfg.proxy_password = "";
        cfg.proxy_username = "";
        cfg.enableWhiteList = false;
        cfg.whiteList = new ArrayList<>();
        String json = JSON.toJSONString(cfg, SerializerFeature.PrettyFormat);
        File file = new File(configPath);
        if (!file.exists()) {
            File f = new File(configDir);
            if (!f.exists()) {
                if (!f.mkdirs()) {
                    logger.warning("创建配置文件夹失败");
                }
            }
        }
        try (FileWriter fileWriter = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(json);
            bufferedWriter.flush();
        } catch (IOException ignored) {
        }
    }

    public void initData() {
        lock.lock();
        rssItems = new ArrayList<>();
        lock.unlock();
        saveData();
    }

    public boolean configExist() {
        File file = new File(configPath);
        return file.exists();
    }

    public boolean dataExist() {
        File file = new File(dataPath);
        return file.exists();
    }

    public void saveData() {
        lock.lock();
        String json = JSON.toJSONString(rssItems);
        File file = new File(dataPath);
        if (!file.exists()) {
            File f = new File(datafileDir);
            if (!f.exists()) {
                if (!f.mkdirs()) {
                    logger.warning("创建数据文件夹失败");
                }
            }
        }
        try (FileWriter fileWriter = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(json);
            bufferedWriter.flush();
        } catch (IOException ignored) {
        } finally {
            lock.unlock();
        }
    }

    public RssItem getConfigItem(long id) {
        lock.lock();
        for (RssItem c : rssItems) {
            if (c.id == id) {
                lock.unlock();
                return c;
            }
        }
        lock.unlock();
        return null;
    }

    public void addRssItem(RssItem c) {
        lock.lock();
        if (rssItems == null) {
            rssItems = new ArrayList<>();
        }
        rssItems.add(c);
        lock.unlock();
        saveData();
    }

    public void removeConfigItem(RssItem c) {
        lock.lock();
        rssItems.remove(c);
        lock.unlock();
        saveData();
    }

    public List<RssItem> getOnesConfigItems(String target) {
        List<RssItem> res = new ArrayList<>();
        lock.lock();
        if (rssItems == null) {
            return new ArrayList<>();
        }
        for (RssItem c : rssItems) {
            if (c.target.equals(target)) {
                res.add(c);
            }
        }
        lock.unlock();
        return res;
    }

    public String getBotId() {
        return cfg.botId;
    }

    public long getNewId() {
        lock.lock();
        long id;
        boolean exist;
        do {
            exist = false;
            Random r = new Random(new Date().getTime());
            id = r.nextInt(99999);
            for (RssItem c : rssItems) {
                if (id == c.id) {
                    exist = true;
                    break;
                }
            }
        }
        while (exist);
        lock.unlock();
        return id;
    }

    public boolean getAutoAcceptFriendApplication() {
        return cfg.autoAcceptFriendApplication;
    }

    public boolean getAutoAcceptGroupApplication() {
        return cfg.autoAcceptGroupApplication;
    }

    public boolean canAddSub() {
        return rssItems.size() < cfg.maxSub;
    }

    public void clearConfigItem(String target, String type) {
        lock.lock();
        rssItems.removeIf(c -> c.target.equals(target) && c.type.equals(type));
        saveData();
        lock.unlock();
    }

    public Proxy getProxy() {
        String proxy_type = cfg.proxy_type;
        String proxy_address = cfg.proxy_address;
        int proxy_port = cfg.proxy_port;
        if (!proxy_type.isEmpty()) {
            Proxy.Type type = null;
            if (proxy_type.equalsIgnoreCase("http")) {
                type = Proxy.Type.HTTP;
            } else if (proxy_type.equalsIgnoreCase("socks")) {
                type = Proxy.Type.SOCKS;
            }
            if (type != null && !proxy_address.isEmpty() && proxy_port != 0) {
                return new Proxy(type, new InetSocketAddress(proxy_address, proxy_port));
            }
        }
        return null;
    }

    public String getProxyCredential() {
        String credential = null;
        String proxy_username = cfg.proxy_username;
        String proxy_password = cfg.proxy_password;
        if (!proxy_username.isEmpty() && !proxy_password.isEmpty()) {
            credential = Credentials.basic(proxy_username, proxy_password);
        }
        return credential;
    }

    public String get(String url) {
        StringBuilder stringBuilder = new StringBuilder();
        String credential = getProxyCredential();
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder().connectTimeout(5, TimeUnit.SECONDS).proxy(getProxy()).build();
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

    public boolean whiteList() {
        return cfg.enableWhiteList;
    }

    public boolean inWhiteList(String id) {
        if (cfg.whiteList != null) {
            for (String s : cfg.whiteList) {
                if (s.equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }
}