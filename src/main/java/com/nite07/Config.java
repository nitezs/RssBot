package com.nite07;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        data.data = new ArrayList<>();
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
        c.enable = false;
        saveConfig();
    }

    public List<ConfigItem> getOnesConfigItems(String target) {
        List<ConfigItem> res = new ArrayList<>();
        lock.lock();
        if (data.data == null) {
            return new ArrayList<ConfigItem>();
        }
        for (ConfigItem c : data.data) {
            if (c.target.equals(target) && c.enable) {
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
        if (data.data == null) {
            return 1;
        } else {
            return data.data.size() + 1;
        }
    }


}