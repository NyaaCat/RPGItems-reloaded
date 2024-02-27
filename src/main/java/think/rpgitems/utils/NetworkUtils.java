package think.rpgitems.utils;

import cat.nyaa.nyaacore.Pair;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import think.rpgitems.AdminCommands;
import think.rpgitems.RPGItems;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class NetworkUtils {
    private static final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10)).build();

    @SuppressWarnings("unchecked")
    public static Map<String, String> downloadGist(String id, String token) throws InterruptedException, ExecutionException, TimeoutException, URISyntaxException, IOException {
        Map<String, String> headers = new HashMap<>();
        if (Strings.isNullOrEmpty(token)) {
            headers.put("Authorization", "token " + token);
        }
        var requestBuilder = HttpRequest.newBuilder().GET().uri(new URI("https://api.github.com/gists/" + id));
        headers.forEach(requestBuilder::header);
        var request = requestBuilder.build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));

        if (response.statusCode() == 404) {
            throw new AdminCommands.CommandException("message.import.gist.notfound", id);
        } else if (response.statusCode() != 200) {
            throw new AdminCommands.CommandException("message.import.gist.code", response.statusCode());
        }
        var responseJson = response.body();
        var result = new Gson().fromJson(responseJson, LinkedTreeMap.class);
        var files = (Map<String, Object>) result.get("files");
        var results = new HashMap<String, String>();
        files.forEach((k, v) -> {
            Map<String, String> map = (Map<String, String>) v;
            if (map.get("filename").endsWith(".yml")) {
                results.put(map.get("filename"), map.get("content"));
            }
        });
        return results;
    }

    public static Map<String, String> downloadUrl(String url) throws InterruptedException, ExecutionException, TimeoutException, URISyntaxException, IOException {
        var request = HttpRequest.newBuilder().GET().uri(new URI(url)).build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));

        if (response.statusCode() == 404) {
            throw new AdminCommands.CommandException("message.import.gist.notfound", url);
        } else if (response.statusCode() != 200) {
            throw new AdminCommands.CommandException("message.import.gist.code", response.statusCode());
        }
        return Collections.singletonMap(null, response.body());
    }

    public static String publishGist(Map<String, Map<String, String>> files, String token, String description, boolean isPublic) throws InterruptedException, ExecutionException, TimeoutException, URISyntaxException, IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "token " + token);
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("description", description);
        jsonMap.put("public", isPublic);
        jsonMap.put("files", files);

        var json = new Gson().toJson(jsonMap);
        var requestBuilder = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(json, UTF_8)).uri(new URI("https://api.github.com/gists"));
        headers.forEach(requestBuilder::header);
        var request = requestBuilder.build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
        if (response.statusCode() != 201) {
            RPGItems.plugin.getLogger().warning("Code " + response.statusCode() + " publishing gist");
            RPGItems.plugin.getLogger().warning(response.body());
            throw new AdminCommands.CommandException("message.export.gist.code", response.statusCode());
        }
        var location = response.headers().firstValue("Location");
        if (location.isEmpty()) {
            throw new RuntimeException("No location info exist in response headers, please note if there is a gist api change.");
        }
        String[] split = location.get().split("/");
        return split[split.length - 1];
    }

    @SuppressWarnings("unchecked")
    public static Pair<String, List<String>> updateCommand(String item, String command) throws InterruptedException, ExecutionException, TimeoutException, URISyntaxException, IOException {
        String endPoint = RPGItems.plugin.cfg.spuEndpoint;
        if (Strings.isNullOrEmpty(endPoint)) {
            throw new AdminCommands.CommandException("message.spu.no_endpoint");
        }

        var request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(command, UTF_8)).uri(new URI(endPoint + "/command/112/113")).build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
        var body = response.body();
        if (response.statusCode() != 200) {
            RPGItems.plugin.getLogger().warning("Code " + response.statusCode() + " updating command " + item + ": " + command);
            RPGItems.plugin.getLogger().warning(body);
            throw new AdminCommands.CommandException("message.spu.command.code", item, response.statusCode(), body, command);
        }
        var result = new Gson().fromJson(body, LinkedTreeMap.class);
        var updated = (String) result.get("command");
        var warnings = (List<String>) result.get("warnings");
        return Pair.of(updated, warnings);
    }

    public static String updateEntity(String item, String entity, boolean name) throws InterruptedException, ExecutionException, TimeoutException, URISyntaxException, IOException {
        String endPoint = RPGItems.plugin.cfg.spuEndpoint;
        if (Strings.isNullOrEmpty(endPoint)) {
            throw new AdminCommands.CommandException("message.spu.no_endpoint");
        }

        var request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(entity, UTF_8)).uri(new URI(endPoint + (name ? "/entityname/112/113" : "/entitynbt/112/113"))).build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
        var result = response.body();
        if (response.statusCode() != 200) {
            RPGItems.plugin.getLogger().warning("Code " + response.statusCode() + " updating entity in " + item + ": " + entity);
            RPGItems.plugin.getLogger().warning(result);
            throw new AdminCommands.CommandException("message.spu.entity.code", item, response.statusCode(), result, entity);
        }
        return new Gson().fromJson(result, String.class);
    }

    public enum Location {
        URL, GIST
    }
}
