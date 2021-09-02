package com.nite07.Pojo;

import java.util.List;

public class ConfigItem {
    public boolean enable;
    public long id;
    public String type;
    public String target;
    public int interval;
    public String url;
    public String title;
    public List<Entry> entries;

    public ConfigItem(long id, String type, String target, String url, int interval, String title, List<Entry> entries) {
        this.id = id;
        this.type = type;
        this.target = target;
        this.interval = interval;
        this.url = url;
        this.enable = true;
        this.title = title;
        this.entries = entries;
    }
}
