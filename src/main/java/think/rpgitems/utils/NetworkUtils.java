package think.rpgitems.utils;


import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class NetworkUtils {

    public enum Location {
        GIST
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> downloadGist(String id, String token) {
        try {
            Map<String, String> results = new HashMap<>();
            HttpURLConnection urlConnection = (HttpURLConnection) new URL("https://api.github.com/gists/" + id).openConnection();
            if (!Strings.isNullOrEmpty(token)) {
                urlConnection.setRequestProperty("Authorization:", "token " + token);
            }
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            Map result = new Gson().fromJson(new InputStreamReader(in), LinkedTreeMap.class);
            Map files = (Map) result.get("files");
            files.forEach((k, v) -> {
                Map<String, String> map = (Map<String, String>) v;
                if (map.get("filename").endsWith(".yml")) {
                    results.put(map.get("filename"), map.get("content"));
                }
            });
            return results;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
