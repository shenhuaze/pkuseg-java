package com.huaze.shen.util;

import java.util.*;

/**
 * @author Huaze Shen
 * @date 2019-05-04
 *
 * 对Map排序
 */
public class SortMapUtil {
    public static Map<String, Integer> sortByKey(Map<String, Integer> map, boolean ascending) {
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(map.entrySet());
        entryList.sort(new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                if (ascending) {
                    return o1.getKey().compareTo(o2.getKey());
                } else {
                    return o2.getKey().compareTo(o1.getKey());
                }
            }
        });
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry: entryList) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }
}
