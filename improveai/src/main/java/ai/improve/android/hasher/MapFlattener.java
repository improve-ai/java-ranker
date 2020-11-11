package ai.improve.android.hasher;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.*;

public class MapFlattener {


    private static final int MAX_DEPTH = 100;

    private String separator;
    int depth = 0;

    public static Map<String, Object> flatten(Map<String, Object> input, String separator) {
        return new MapFlattener(separator).flattenMap(input, null);
    }

    public MapFlattener(String separator) {
        this.separator = separator;
    }

    private Map<String, Object> flattenMap(Map<String, Object> input, String prefix) {
        if (depth++ > MAX_DEPTH) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String newPrefix = extendPrefix(prefix, entry.getKey());
            Object value = entry.getValue();

            if (value instanceof Map) {
                result.putAll(flattenMap((Map<String, Object>) value, newPrefix));
            } else if (value instanceof JSONObject) {
                JSONObject json = (JSONObject) value;
                Map<String, Object> next = new HashMap<>();
                for (Iterator<String> i = json.keys(); i.hasNext(); ) {
                    try {
                        String jsonKey = i.next();
                        Object jsonValue = json.get(jsonKey);
                        next.put(jsonKey, jsonValue);
                    } catch (JSONException ex) {
                        //Do nothing
                    }
                }
                result.putAll(flattenMap(next, newPrefix));
            } else if (value instanceof Collection) {
                Collection c = (Collection) value;
                int index = 0;
                for (Object o : c) {
                    result.put(extendPrefix(newPrefix, String.valueOf(index++)), o);
                }
            } else if (value != null && value.getClass().isArray()) {
                int len = Array.getLength(value);
                for (int i = 0; i < len; ++i) {
                    result.put(extendPrefix(newPrefix, String.valueOf(i)), Array.get(value, i));
                }
            } else {
                result.put(newPrefix, value);
            }
        }
        depth--;
        return result;
    }

    private String extendPrefix(String prefix, String key) {
        return (prefix == null) ? key : prefix + separator + key;
    }

}
