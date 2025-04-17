package data;

import java.util.HashMap;
import java.util.Map;

public class Boss {
    private String boss;
    private int kc;
    private double ehb;
    private double killTime;

    public Boss (String boss, int kc, double ehb) {
        this.boss = boss;
        this.kc = kc;
        this.ehb = ehb;
    }

    public void setKillTime (Double killTime) {
        this.killTime = killTime;
    }

    public Map<String, Object> formatBoss() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("boss", boss);
        ret.put("kc", kc);
        ret.put("ehb", ehb);
        ret.put("pb", killTime);
        return ret;
    }

    public String getBossName() {
        return boss;
    }
}
