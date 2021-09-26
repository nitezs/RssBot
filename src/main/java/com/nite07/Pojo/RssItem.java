package com.nite07.Pojo;

import java.util.Date;
import java.util.List;

public class RssItem {
    public long id;
    public String type;
    public String target;
    public int interval;
    public String url;
    public String title;
    public List<Entry> entries;
    public Date refreshTime;
    public String updateMode;
    public int mergeNum;
    public boolean showImage;
    public boolean forwardMessage;

    public RssItem() {
    }

    public RssItem(long id, String type, String target, String url, int interval, String title, List<Entry> entries) {
        this.id = id;
        this.type = type;
        this.target = target;
        this.interval = interval;
        this.url = url;
        this.title = title;
        this.entries = entries;
        this.mergeNum = 1;
        this.updateMode = "date";
        this.showImage = true;
        this.forwardMessage = false;
    }
}
