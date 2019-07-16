package think.rpgitems.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LightContext {
    private static Map<UUID, Map<String, Object>> context = new HashMap<>();

    public static void putTemp(UUID uuid, String key, Object val){
        Map<String, Object> stringObjectMap = context.computeIfAbsent(uuid, uuid1 -> new HashMap<>());
        stringObjectMap.put(key, val);
    }

    public static <T> Optional<T> getTemp(UUID uuid, String key, T returnType){
        Map<String, Object> stringObjectMap = context.computeIfAbsent(uuid, uuid1 -> new HashMap<>());
        try{
            return Optional.of(((T) stringObjectMap.get(key)));
        } catch (Exception ex){
           return Optional.empty();
        }
    }

    public static void removeTemp(UUID uuid, String key){
        Map<String, Object> stringObjectMap = context.computeIfAbsent(uuid, uuid1 -> new HashMap<>());
        stringObjectMap.remove(key);
    }

    public static void clear(){
        context.clear();
    }
}
