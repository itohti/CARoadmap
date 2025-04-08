package com.caroadmap;

import java.util.HashMap;
import java.util.Map;

public class Boss {
    private String boss;
    private int kc;
    private double ehb;

    public Boss (String boss, int kc, double ehb) {
        this.boss = boss;
        this.kc = kc;
        this.ehb = ehb;
    }

    public Map<String, Object> formatBoss() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("Boss", boss);
        ret.put("kc", kc);
        ret.put("ehb", ehb);

        return ret;
    }

    public String getBossName() {
        return boss;
    }
}
