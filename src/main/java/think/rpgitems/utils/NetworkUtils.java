package think.rpgitems.utils;

import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.http.client.HttpClient;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.ReferenceCountUtil;
import think.rpgitems.Handler;
import think.rpgitems.RPGItems;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
        try {
            if (httpResponse.status().code() != 201) {
                RPGItems.plugin.getLogger().warning("Code " + httpResponse.status().code() + " publishing gist");
                RPGItems.plugin.getLogger().warning(httpResponse.content().toString(UTF_8));
                throw new Handler.CommandException("message.export.gist.code", httpResponse.status().code());
            }
            String location = httpResponse.headers().get("Location");
            String[] split = location.split("/");
            return split[split.length - 1];
        } finally {
            ReferenceCountUtil.release(httpResponse);
        }
    }

    @SuppressWarnings("unchecked")
    public static Pair<String, List<String>> updateCommand(String item, String command) throws InterruptedException, ExecutionException, TimeoutException {
        String endPoint = RPGItems.plugin.cfg.spuEndpoint;
        if (Strings.isNullOrEmpty(endPoint)) {
            throw new Handler.CommandException("message.spu.no_endpoint");
        }
        CompletableFuture<FullHttpResponse> response = HttpClient.request(endPoint + "/command/112/113", HttpMethod.POST, null, Unpooled.wrappedBuffer(command.getBytes(UTF_8)), HttpHeaderValues.TEXT_PLAIN);
        FullHttpResponse httpResponse = response.get(10, TimeUnit.SECONDS);
        try {
            String body = httpResponse.content().toString(UTF_8);
            if (httpResponse.status().code() != 200) {
                RPGItems.plugin.getLogger().warning("Code " + httpResponse.status().code() + " updating command " + item + ": " + command);
                RPGItems.plugin.getLogger().warning(body);
                throw new Handler.CommandException("message.spu.command.code", item, httpResponse.status().code(), body, command);
            }
            Map result = new Gson().fromJson(body, LinkedTreeMap.class);
            String updated = (String) result.get("command");
            List<String> warnings = (List<String>) result.get("warnings");
            return Pair.of(updated, warnings);
        } finally {
            ReferenceCountUtil.release(httpResponse);
        }
    }

    public static String updateEntity(String item, String entity, boolean name) throws InterruptedException, ExecutionException, TimeoutException {
        String endPoint = RPGItems.plugin.cfg.spuEndpoint;
        if (Strings.isNullOrEmpty(endPoint)) {
            throw new Handler.CommandException("message.spu.no_endpoint");
        }
        CompletableFuture<FullHttpResponse> response = HttpClient.request(endPoint + (name ? "/entityname/112/113" : "/entitynbt/112/113"), HttpMethod.POST, null, Unpooled.wrappedBuffer(entity.getBytes(UTF_8)), HttpHeaderValues.TEXT_PLAIN);
        FullHttpResponse httpResponse = response.get(10, TimeUnit.SECONDS);
        try {
            String result = httpResponse.content().toString(UTF_8);
            if (httpResponse.status().code() != 200) {
                RPGItems.plugin.getLogger().warning("Code " + httpResponse.status().code() + " updating entity in " + item + ": " + entity);
                RPGItems.plugin.getLogger().warning(result);
                throw new Handler.CommandException("message.spu.entity.code", item, httpResponse.status().code(), result, entity);
            }
            return new Gson().fromJson(result, String.class);
        } finally {
            ReferenceCountUtil.release(httpResponse);
        }
    }
}
