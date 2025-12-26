package com.caroadmap.data;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class Boss {
    @Getter
    private String boss;
    @Getter
    private int kc;
    @Getter
    private double ehb;
    @Getter
    @Setter
    private int killTime;

    public Boss (String boss, int kc, double ehb) {
        this.boss = boss;
        this.kc = kc;
        this.ehb = ehb;
    }

    public Map<String, Object> formatBoss() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("boss", boss);
        ret.put("kc", kc);
        ret.put("ehb", ehb);
        ret.put("pb", killTime);
        return ret;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Boss otherBoss = (Boss) obj;

        return (this.boss.equals(otherBoss.getBoss())
                && this.kc == otherBoss.getKc()
                && this.ehb == otherBoss.getEhb()
                && this.killTime == otherBoss.getKillTime()
        );
    }
}
