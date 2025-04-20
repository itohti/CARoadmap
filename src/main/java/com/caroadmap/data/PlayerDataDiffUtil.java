package com.caroadmap.data;

import java.util.*;

public class PlayerDataDiffUtil {
    public static List<Boss> filterChangedBosses(List<Object> newList, List<Object> cachedList) {
        Map<String, Boss> clientMap = toMapByKey(newList, Boss.class, Boss::getBoss);
        Map<String, Boss> cachedMap = toMapByKey(cachedList, Boss.class, Boss::getBoss);

        clientMap.entrySet().removeIf(e -> Objects.equals(e.getValue(), cachedMap.get(e.getKey())));
        return new ArrayList<>(clientMap.values());
    }

    public static List<Map<String, Object>> filterChangedTasks(List<Object> newList, List<Object> cachedList) {
        Map<String, Boolean> clientTasks = extractTaskMap(newList);
        Map<String, Boolean> cachedTasks = extractTaskMap(cachedList);

        clientTasks.entrySet().removeIf(e -> Objects.equals(e.getValue(), cachedTasks.get(e.getKey())));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Boolean> e : clientTasks.entrySet()) {
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("task_name", e.getKey());
            taskMap.put("Done", e.getValue());
            result.add(taskMap);
        }
        return result;
    }

    public static List<Map<String, Object>> filterChangedStats(List<Object> newList, List<Object> cachedList) {
        Map<String, Integer> clientStats = extractStatMap(newList);
        Map<String, Integer> cachedStats = extractStatMap(cachedList);

        clientStats.entrySet().removeIf(e -> Objects.equals(e.getValue(), cachedStats.get(e.getKey())));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : clientStats.entrySet()) {
            Map<String, Object> statMap = new HashMap<>();
            statMap.put(e.getKey(), e.getValue());
            result.add(statMap);
        }
        return result;
    }

    private static Map<String, Boolean> extractTaskMap(List<Object> rawList) {
        Map<String, Boolean> map = new HashMap<>();
        for (Object obj : rawList) {
            if (obj instanceof Map) {
                Map<?, ?> m = (Map<?, ?>) obj;
                Object name = m.get("task_name");
                Object done = m.get("Done");
                if (name instanceof String && done instanceof Boolean) {
                    map.put((String) name, (Boolean) done);
                }
            }
        }
        return map;
    }

    private static Map<String, Integer> extractStatMap(List<Object> rawList) {
        Map<String, Integer> map = new HashMap<>();
        for (Object obj : rawList) {
            if (obj instanceof Map) {
                Map<?, ?> rawMap = (Map<?, ?>) obj;
                for (Map.Entry<?, ?> e : rawMap.entrySet()) {
                    if (e.getKey() instanceof String && e.getValue() instanceof Integer) {
                        map.put((String) e.getKey(), (Integer) e.getValue());
                    }
                }
            }
        }
        return map;
    }

    public static <K, T> Map<K, T> toMapByKey(List<?> list, Class<T> clazz, java.util.function.Function<T, K> keyExtractor) {
        Map<K, T> result = new HashMap<>();
        for (Object o : list) {
            if (clazz.isInstance(o)) {
                T item = clazz.cast(o);
                result.put(keyExtractor.apply(item), item);
            }
        }
        return result;
    }

    public static List<Object> filterByKey(String key, List<Object> newVal, List<Object> cachedVal) {
        if ("boss_info".equals(key)) {
            return new ArrayList<Object>(filterChangedBosses(newVal, cachedVal));
        } else if ("tasks".equals(key)) {
            return new ArrayList<Object>(filterChangedTasks(newVal, cachedVal));
        } else if ("combat_stats".equals(key)) {
            return new ArrayList<Object>(filterChangedStats(newVal, cachedVal));
        } else {
            return new ArrayList<Object>();
        }
    }
}
