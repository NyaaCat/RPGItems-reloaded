package think.rpgitems.utils;

import cat.nyaa.nyaacore.http.client.HttpClient;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import think.rpgitems.Handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class NetworkUtils {

    public enum Location {
        URL, GIST
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> downloadGist(String id, String token) throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, String> headers = new HashMap<>();
        if (Strings.isNullOrEmpty(token)) {
            headers.put("Authorization", "token " + token);
        }
        CompletableFuture<FullHttpResponse> response = HttpClient.get("https://api.github.com/gists/" + id, headers);
        FullHttpResponse httpResponse = response.get(10, TimeUnit.SECONDS);
        try {

            if (httpResponse.status().code() == 404) {
                throw new Handler.CommandException("message.import.gist.notfound", id);
            } else if (httpResponse.status().code() != 200) {
                throw new Handler.CommandException("message.import.gist.code", httpResponse.status().code());
            }

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
        } finally {
            ReferenceCountUtil.release(httpResponse);
        }
    }

    public static Map<String, String> downloadUrl(String url) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<FullHttpResponse> response = HttpClient.get(url, null);
        FullHttpResponse httpResponse = response.get(10, TimeUnit.SECONDS);
        try {

            if (httpResponse.status().code() == 404) {
                throw new Handler.CommandException("message.import.url.notfound", url);
            } else if (httpResponse.status().code() != 200) {
                throw new Handler.CommandException("message.import.url.code", httpResponse.status().code());
            }

            String yamlStr = httpResponse.content().toString(UTF_8);
            return Collections.singletonMap(null, yamlStr);
        } finally {
            ReferenceCountUtil.release(httpResponse);
        }
    }

    public static String publishGist(Map<String, Map<String, String>> files, String token, String description, boolean isPublic) throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "token " + token);
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("description", description);
        jsonMap.put("public", isPublic);
        jsonMap.put("files", files);

        String json = new Gson().toJson(jsonMap);
        CompletableFuture<FullHttpResponse> response = HttpClient.postJson("https://api.github.com/gists", headers, json);
        FullHttpResponse httpResponse = response.get(10, TimeUnit.SECONDS);
        if (httpResponse.status().code() != 201) {
            System.out.println(httpResponse.content().toString(UTF_8));
            throw new Handler.CommandException("message.export.gist.code", httpResponse.status().code());
        }
        String location = httpResponse.headers().get("Location");
        String[] split = location.split("/");
        return split[split.length - 1];
    }
}
