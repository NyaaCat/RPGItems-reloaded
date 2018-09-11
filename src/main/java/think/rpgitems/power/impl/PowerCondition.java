package think.rpgitems.power.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.Property;
import think.rpgitems.utils.Pair;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static think.rpgitems.power.impl.PowerSelector.*;

@PowerMeta(marker = true)
public class PowerCondition extends BasePower {

    @Property(order = 0, required = true)
    public String id;

    @Property
    public boolean isStatic = false;

    @Property
    public boolean isCritical = false;

    @Property
    public int durabilityMin = Integer.MIN_VALUE;

    @Property
    public int durabilityMax = Integer.MAX_VALUE;

    /**
     * Selecting targets by score(s), According to the following format
     * `score_name:min,max another_score_name:min,max`
     */
    @Property
    public String score;

    /**
     * Selecting targets by tag(s), According to the following format
     * `MUST_HAVE,!MUST_NOT_HAVE`
     * `` targets without any scoreboard tags
     * `!` targets with any scoreboard tags
     */
    @Property
    public String tag;

    /**
     * Selecting targets by team(s), According to the following format
     * `MUST_ON,!MUST_NOT_ON`
     * `` targets not on any team
     * `!` targets on any team
     * Only first MUST_ON has effect
     */
    @Property
    public String team;

    @Property
    public double chancePercentage;

    public static LoadingCache<String, Map<String, Pair<Integer, Integer>>> scoreCache = CacheBuilder
                                                                                                 .newBuilder()
                                                                                                 .concurrencyLevel(1)
                                                                                                 .expireAfterAccess(1, TimeUnit.DAYS)
                                                                                                 .build(CacheLoader.from(PowerSelector::parseScore));

    public static LoadingCache<String, Pair<Set<String>, Set<String>>> teamCache = CacheBuilder
                                                                                           .newBuilder()
                                                                                           .concurrencyLevel(1)
                                                                                           .expireAfterAccess(1, TimeUnit.DAYS)
                                                                                           .build(CacheLoader.from(PowerSelector::parse));

    public static LoadingCache<String, Pair<Set<String>, Set<String>>> tagCache = CacheBuilder
                                                                                          .newBuilder()
                                                                                          .concurrencyLevel(1)
                                                                                          .expireAfterAccess(1, TimeUnit.DAYS)
                                                                                          .build(CacheLoader.from(PowerSelector::parse));

    public boolean check(Player player, ItemStack stack) {
        if (ThreadLocalRandom.current().nextDouble(0, 100) > chancePercentage) return false;
        int durability = getItem().getDurability(stack);
        if (durability > durabilityMax || durability < durabilityMin) return false;
        if (tag != null) {
            Pair<Set<String>, Set<String>> t = tagCache.getUnchecked(tag);
            if (!matchTag(player, t)) {
                return false;
            }
        }
        if (team != null) {
            Pair<Set<String>, Set<String>> t = teamCache.getUnchecked(team);
            if (!matchTeam(player, player.getScoreboard(), t)) {
                return false;
            }
        }
        if (score != null) {
            Map<String, Pair<Integer, Integer>> t = scoreCache.getUnchecked(score);
            if (!matchScore(player, player.getScoreboard(), t)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return "condition";
    }

    @Override
    public String displayText() {
        return null;
    }
}
