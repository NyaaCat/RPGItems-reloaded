package think.rpgitems.utils;


import cat.nyaa.nyaacore.http.client.HttpClient;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class NetworkUtils {

    public enum Location {
        GIST
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> downloadGist(String id, String token) throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization:", "token " + token);
        CompletableFuture<FullHttpResponse> response = HttpClient.get("https://api.github.com/gists/" + id, headers);
        FullHttpResponse httpResponse = response.get(10, TimeUnit.SECONDS);
        String json = httpResponse.content().toString(UTF_8);

        Map result = new Gson().fromJson(json, LinkedTreeMap.class);
        Map files = (Map) result.get("files");
        Map<String, String> results = new HashMap<>();
        files.forEach((k, v) -> {
            Map<String, String> map = (Map<String, String>) v;
            if (map.get("filename").endsWith(".yml")) {
                results.put(map.get("filename"), map.get("content"));
            }
        });
        return results;
    }
}
