package com.nite07;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.fastjson.JSON;
import com.nite07.Pojo.ConfigData;
import com.nite07.Pojo.ConfigItem;

public class Config {
    String configPath = "config/RssBot/data.json";
    private ConfigData data;
    ReentrantLock lock = new ReentrantLock();

    public Config() {
        File file = new File(configPath);
        String cfg = null;
        if (!configExist()) {
            try {
                file.createNewFile();
                initConfigData();
            } catch (IOException ignored) {
            }
        } else {
            try (FileReader fileReader = new FileReader(file);
                 BufferedReader bufferedReader = new BufferedReader(fileReader);) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                cfg = stringBuilder.toString();
            } catch (Exception ignored) {
            }
            lock.lock();
            data = JSON.parseObject(cfg, ConfigData.class);
            lock.unlock();
        }
    }

    public List<ConfigItem> getConfigItems() {
        return data.data;
    }

    public void initConfigData() {
        lock.lock();
        data = new ConfigData();
        data.botId = "123456789";
        data.autoAcceptFriendApplication = true;
        data.autoAcceptGroupApplication = true;
        data.data = new ArrayList<>();
        data.maxSub = 100;
        lock.unlock();
        saveConfig();
    }

    public boolean configExist() {
        File file = new File(configPath);
        return file.exists();
    }

    public void saveConfig() {
        lock.lock();
        String json = JSON.toJSONString(data);
        File file = new File(configPath);
        if (!file.exists()) {
            String dirs[] = configPath.split("/");
            StringBuilder dir = new StringBuilder();
            for (int i = 0; i < dirs.length - 1; i++) {
                dir.append(dirs[i]).append("/");
            }
            File f = new File(dir.toString());
            f.mkdirs();
        }
        try (FileWriter fileWriter = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);) {
            bufferedWriter.write(json);
            bufferedWriter.flush();
        } catch (IOException ignored) {
        } finally {
            lock.unlock();
        }
    }

    public ConfigItem getConfigItem(long id) {
        lock.lock();
        for (ConfigItem c : data.data) {
            if (c.id == id) {
                lock.unlock();
                return c;
            }
        }
        lock.unlock();
        return null;
    }

    public void addConfigItem(ConfigItem c) {
        lock.lock();
        if (data.data == null) {
            data.data = new ArrayList<>();
        }
        data.data.add(c);
        lock.unlock();
        saveConfig();
    }

    public void removeConfigItem(ConfigItem c) {
        data.data.remove(c);
        saveConfig();
    }

    public List<ConfigItem> getOnesConfigItems(String target) {
        List<ConfigItem> res = new ArrayList<>();
        lock.lock();
        if (data.data == null) {
            return new ArrayList<ConfigItem>();
        }
        for (ConfigItem c : data.data) {
            if (c.target.equals(target)) {
                res.add(c);
            }
        }
        lock.unlock();
        return res;
    }

    public String getBotId() {
        return data.botId;
    }

    public long getNewId() {
        lock.lock();
        long id;
        boolean exist = false;
        do {
            exist = false;
            Random r = new Random(new Date().getTime());
            id = r.nextInt(99999);
            for (ConfigItem c : data.data) {
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
        return data.autoAcceptFriendApplication;
    }

    public boolean getAutoAcceptGroupApplication() {
        return data.autoAcceptGroupApplication;
    }

    public boolean canAddSub() {
        return data.data.size() < data.maxSub;
    }

    public void clearConfigItem(String target, String type) {
        lock.lock();
        data.data.removeIf(c -> c.target.equals(target) && c.type.equals(type));
        saveConfig();
        lock.unlock();
    }
}