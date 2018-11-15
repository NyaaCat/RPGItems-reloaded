package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Pair;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static think.rpgitems.power.impl.PowerSelector.*;

@PowerMeta(marker = true)
public class PowerScoreboardCondition extends BasePower implements PowerCondition<Void> {

    @Property(order = 0, required = true)
    public String id;

    @Property
    public boolean isStatic = false;

    @Property
    public boolean isCritical = false;

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

    private static LoadingCache<String, Map<String, Pair<Integer, Integer>>> scoreCache = CacheBuilder
                                                                                                 .newBuilder()
                                                                                                 .concurrencyLevel(1)
                                                                                                 .expireAfterAccess(1, TimeUnit.DAYS)
                                                                                                 .build(CacheLoader.from(PowerSelector::parseScore));

    private static LoadingCache<String, Pair<Set<String>, Set<String>>> teamCache = CacheBuilder
                                                                                           .newBuilder()
                                                                                           .concurrencyLevel(1)
                                                                                           .expireAfterAccess(1, TimeUnit.DAYS)
                                                                                           .build(CacheLoader.from(PowerSelector::parse));

    private static LoadingCache<String, Pair<Set<String>, Set<String>>> tagCache = CacheBuilder
                                                                                          .newBuilder()
                                                                                          .concurrencyLevel(1)
                                                                                          .expireAfterAccess(1, TimeUnit.DAYS)
                                                                                          .build(CacheLoader.from(PowerSelector::parse));

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public boolean isCritical() {
        return isCritical;
    }

    @Override
    public PowerResult<Void> check(Player player, ItemStack stack, Map<Power, PowerResult> context) {
        if (tag != null) {
            Pair<Set<String>, Set<String>> t = tagCache.getUnchecked(tag);
            if (!matchTag(player, t)) {
                return PowerResult.fail();
            }
        }
        if (team != null) {
            Pair<Set<String>, Set<String>> t = teamCache.getUnchecked(team);
            if (!matchTeam(player, player.getScoreboard(), t)) {
                return PowerResult.fail();
            }
        }
        if (score != null) {
            Map<String, Pair<Integer, Integer>> t = scoreCache.getUnchecked(score);
            if (!matchScore(player, player.getScoreboard(), t)) {
                return PowerResult.fail();
            }
        }
        return PowerResult.ok();
    }

    @Override
    public String getName() {
        return "scoreboardcondition";
    }

    @Override
    public String displayText() {
        return null;
    }

    @Override
    public Set<String> getConditions() {
        return Collections.emptySet();
    }
}
