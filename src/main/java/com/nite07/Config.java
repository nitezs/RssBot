package com.nite07;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.nite07.Pojo.ConfigData;
import com.nite07.Pojo.RssItem;
import net.mamoe.mirai.utils.MiraiLogger;
import okhttp3.Credentials;

public class Config {
    String configDir = "config/com.nite07.RssBot/";
    String configName = "config.json";
    String configPath = configDir + configName;
    String datafileDir = "data/com.nite07.RssBot/";
    String dataName = "data.json";
    String dataPath = datafileDir + dataName;
    ReentrantLock lock = new ReentrantLock();
    MiraiLogger logger;
    private ConfigData cfg;
    private List<RssItem> rssItems;

    public Config() {
        this.logger = RssBot.logger;
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
            } catch (Exception e) {
                logger.warning(e.getMessage());
                logger.warning(Arrays.toString(e.getStackTrace()));
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
            } catch (Exception e) {
                logger.warning(e.getMessage());
                logger.warning(Arrays.toString(e.getStackTrace()));
            }
            this.rssItems = JSON.parseArray(data, RssItem.class);
            if (this.rssItems == null) {
                this.rssItems = new ArrayList<>();
            }
        }
        checkConfig();
        logger.info("\n" + getLog());
    }

    private void checkConfig() {
        if (cfg.botAdmins == null) {
            cfg.botAdmins = new ArrayList<>();
        }
        if (cfg.whiteList == null) {
            cfg.whiteList = new ArrayList<>();
        }
        if (cfg.proxy_password == null) {
            cfg.proxy_password = "";
        }
        if (cfg.proxy_username == null) {
            cfg.proxy_username = "";
        }
        if (cfg.proxy_type == null) {
            cfg.proxy_type = "";
        }
        if (cfg.botId == null) {
            cfg.botId = "123456789";
        }
        if (cfg.proxy_address == null) {
            cfg.proxy_address = "";
        }
        saveConfig();
    }

    /**
     * ??? Mirai ????????????????????????
     */
    public String getLog() {
        StringBuilder log = new StringBuilder("RssBotID???")
                .append(cfg.botId)
                .append("\n???????????????????????????")
                .append(cfg.autoAcceptFriendApplication)
                .append("\n????????????????????????")
                .append(cfg.autoAcceptGroupApplication)
                .append("\n?????????????????????")
                .append(cfg.maxSub)
                .append("\n?????????????????????")
                .append(rssItems.size())
                .append("\n??????????????????[");
        if (cfg.botAdmins.size() != 0) {
            for (String id : cfg.botAdmins) {
                log.append(id).append(",");
            }
            log.deleteCharAt(log.length() - 1);
        }
        log.append("]");
        if ((cfg.proxy_type.equalsIgnoreCase("http") || cfg.proxy_type.equalsIgnoreCase("socks"))
                && !cfg.proxy_address.isEmpty() && cfg.proxy_port != 0) {
            log.append("\n???????????????")
                    .append(cfg.proxy_type)
                    .append("\n???????????????")
                    .append(cfg.proxy_address)
                    .append("\n???????????????")
                    .append(cfg.proxy_port);
            if (!cfg.proxy_username.isEmpty() && !cfg.proxy_password.isEmpty()) {
                log.append("\n??????????????????")
                        .append(cfg.proxy_username)
                        .append("\n???????????????")
                        .append(cfg.proxy_password);
            }
        } else {
            log.append("\n???????????????");
        }
        return log.toString();
    }

    /**
     * ??????????????????
     *
     * @return List<RssItem>
     */
    public List<RssItem> getRssItems() {
        return rssItems;
    }

    /**
     * ?????????Config??????
     */
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
        cfg.groupPermissionRestrictions = true;
        cfg.whiteList = new ArrayList<>();
        cfg.botAdmins = new ArrayList<>();
        cfg.deBug = false;
        saveConfig();
    }

    /**
     * ????????????
     */
    public void saveConfig() {
        String json = JSON.toJSONString(cfg, SerializerFeature.PrettyFormat);
        File file = new File(configPath);
        if (!file.exists()) {
            File f = new File(configDir);
            if (!f.exists()) {
                if (!f.mkdirs()) {
                    logger.warning("???????????????????????????");
                }
            }
        }
        try (FileWriter fileWriter = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(json);
            bufferedWriter.flush();
        } catch (IOException e) {
            logger.warning(e.getMessage());
            logger.warning(Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * ?????????????????????
     */
    public void initData() {
        lock.lock();
        rssItems = new ArrayList<>();
        lock.unlock();
        saveData();
    }

    /**
     * ??????????????????????????????
     *
     * @return boolean
     */
    public boolean configExist() {
        File file = new File(configPath);
        return file.exists();
    }

    /**
     * ??????????????????????????????
     *
     * @return boolean
     */
    public boolean dataExist() {
        File file = new File(dataPath);
        return file.exists();
    }

    /**
     * ??????????????????
     */
    public void saveData() {
        lock.lock();
        String json = JSON.toJSONString(rssItems);
        File file = new File(dataPath);
        if (!file.exists()) {
            File f = new File(datafileDir);
            if (!f.exists()) {
                if (!f.mkdirs()) {
                    logger.warning("???????????????????????????");
                }
            }
        }
        try (FileWriter fileWriter = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(json);
            bufferedWriter.flush();
        } catch (IOException e) {
            logger.warning(e.getMessage());
            logger.warning(Arrays.toString(e.getStackTrace()));
        } finally {
            lock.unlock();
        }
    }

    /**
     * ??????ID???????????????
     *
     * @param id ID
     * @return RssItem
     */
    public RssItem getRssItem(long id) {
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

    /**
     * ???????????????
     *
     * @param c RssItem
     */
    public void addRssItem(RssItem c) {
        lock.lock();
        if (rssItems == null) {
            rssItems = new ArrayList<>();
        }
        rssItems.add(c);
        lock.unlock();
        saveData();
    }

    /**
     * ???????????????
     *
     * @param c RssItem
     */
    public void removeConfigItem(RssItem c) {
        lock.lock();
        rssItems.remove(c);
        lock.unlock();
        saveData();
    }

    /**
     * ???????????????????????????
     *
     * @param target ??????ID
     * @return List<RssItem>
     */
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

    /**
     * ???????????????BotId
     *
     * @return BotId
     */
    public String getBotId() {
        return cfg.botId;
    }

    /**
     * ???????????????Id
     *
     * @return ??????id
     */
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

    /**
     * ????????????????????????????????????
     *
     * @return boolean
     */
    public boolean getAutoAcceptFriendApplication() {
        return cfg.autoAcceptFriendApplication;
    }

    /**
     * ?????????????????????????????????
     *
     * @return boolean
     */
    public boolean getAutoAcceptGroupApplication() {
        return cfg.autoAcceptGroupApplication;
    }

    /**
     * ??????????????????????????????
     *
     * @return boolean
     */
    public boolean notReachLimit() {
        return rssItems.size() < cfg.maxSub;
    }

    /**
     * ??????????????????
     *
     * @param target ??????Id
     * @param type   ???????????????Friend/Group???
     */
    public void clearConfigItem(String target, String type) {
        lock.lock();
        rssItems.removeIf(c -> c.target.equals(target) && c.type.equals(type));
        saveData();
        lock.unlock();
    }

    /**
     * ??????????????????
     *
     * @return Proxy
     */
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

    /**
     * ??????????????????
     *
     * @return String
     */
    public String getProxyCredential() {
        String credential = null;
        String proxy_username = cfg.proxy_username;
        String proxy_password = cfg.proxy_password;
        if (!proxy_username.isEmpty() && !proxy_password.isEmpty()) {
            credential = Credentials.basic(proxy_username, proxy_password);
        }
        return credential;
    }

    /**
     * ?????????????????????
     *
     * @return boolean
     */
    public boolean whiteList() {
        return cfg.enableWhiteList;
    }

    /**
     * ?????????????????????????????????
     *
     * @param id ??????Id
     * @return boolean
     */
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

    /**
     * ??????????????????????????????
     *
     * @return boolean
     */
    public boolean groupPermissionRestrictions() {
        return cfg.groupPermissionRestrictions;
    }

    /**
     * ?????????????????????????????????
     *
     * @param id ID
     * @return boolean
     */
    public boolean isBotAdmin(String id) {
        return cfg.botAdmins.contains(id);
    }

    public boolean debug() {
        return cfg.deBug;
    }
}