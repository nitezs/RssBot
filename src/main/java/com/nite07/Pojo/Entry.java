package com.nite07.Pojo;

import java.util.Date;

public class Entry {
    public String title;
    public String link;
    public Date updated;

    public Entry(String title, String link, Date updated) {
        this.title = title;
        this.link = link;
        this.updated = updated;
    }
}
