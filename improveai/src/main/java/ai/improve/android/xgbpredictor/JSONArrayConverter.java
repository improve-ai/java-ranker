package ai.improve.android.xgbpredictor;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedList;
import java.util.List;

public class JSONArrayConverter {


    public static List toList(JSONArray array) throws JSONException {
        List result = new LinkedList();
        for(int i = 0; i < array.length(); ++i) {
            Object o = array.get(i);
            if(o instanceof JSONArray) {
                result.add(toList((JSONArray) o));
            } else {
                result.add(o);
            }
        }
        return result;
    }
}
