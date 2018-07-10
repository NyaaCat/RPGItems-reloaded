/*
 *  This file is part of RPG Items.
 *
 *  RPG Items is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RPG Items is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RPG Items.  If not, see <http://www.gnu.org/licenses/>.
 */
package think.rpgitems.power;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import think.rpgitems.I18n;
import think.rpgitems.utils.Pair;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Power selector.
 * <p>
 * Not a triggerable power.
 * Provide a selector for some AOE power.
 * </p>
 */
public class PowerSelector extends Power {

    /**
     * Power(s) that it applies to
     */
    public String display;
    /**
     * Power(s) that it applies to
     */
    public String applyTo;

    /**
     * Type(s) of target entities
     */
    public String type;

    /**
     * Count of target entities
     */
    public Integer count = -1;

    /**
     * X-coordinate of reference position, tilde notation is available. Use player's current position if empty
     */
    public String x;

    /**
     * Y-coordinate of reference position, tilde notation is available. Use player's current position if empty
     */
    public String y;

    /**
     * Z-coordinate of reference position, tilde notation is available. Use player's current position if empty
     */
    public String z;

    /**
     * Selects only targets less than r blocks from reference position
     */
    public Integer r = -1;

    /**
     * Selects only targets more than rm blocks from reference position
     */
    public Integer rm = -1;

    /**
     * Selects only targets less than dx blocks in X-axis from reference position
     */
    public Integer dx = -1;

    /**
     * Selects only targets less than dx blocks in Y-axis from reference position
     */
    public Integer dy = -1;

    /**
     * Selects only targets less than dx blocks in Z-axis from reference position
     */
    public Integer dz = -1;

    /**
     * Selecting targets by score(s), According to the following format
     * `score_name:min,max another_score_name:min,max`
     */
    public String score;

    /**
     * Selecting targets by tag(s), According to the following format
     * `MUST_HAVE,!MUST_NOT_HAVE`
     * `` targets without any scoreboard tags
     * `!` targets with any scoreboard tags
     */
    public String tag;

    /**
     * Selecting targets by team(s), According to the following format
     * `MUST_ON,!MUST_NOT_ON`
     * `` targets not on any team
     * `!` targets on any team
     * Only first MUST_ON has effect
     */
    public String team;

    @Override
    public void init(ConfigurationSection s) {
        applyTo = s.getString("applyTo");
        type = s.getString("type");
        x = s.getString("x");
        y = s.getString("y");
        z = s.getString("z");
        score = s.getString("score");
        tag = s.getString("tag");
        team = s.getString("team");
        count = s.getInt("count");
        if (count < 0) count = null;
        dx = s.getInt("dx");
        if (dx < 0) dx = null;
        dy = s.getInt("dy");
        if (dy < 0) dy = null;
        dz = s.getInt("dz");
        if (dz < 0) dz = null;
        r = s.getInt("r");
        if (r < 0) r = null;
        rm = s.getInt("rm");
        if (rm < 0) rm = null;
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("applyTo", applyTo);
        s.set("type", type);
        s.set("x", x);
        s.set("y", y);
        s.set("z", z);
        s.set("score", score);
        s.set("tag", tag);
        s.set("team", team);
        s.set("count", count == null ? -1 : count);
        s.set("dx", dx == null ? -1 : dx);
        s.set("dy", dy == null ? -1 : dy);
        s.set("dz", dz == null ? -1 : dz);
        s.set("r", r == null ? -1 : r);
        s.set("rm", rm == null ? -1 : rm);

    }

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

    public static LoadingCache<String, Set<EntityType>> typeCache = CacheBuilder
                                                                            .newBuilder()
                                                                            .concurrencyLevel(1)
                                                                            .expireAfterAccess(1, TimeUnit.DAYS)
                                                                            .build(CacheLoader.from(PowerSelector::parseType));

    public static LoadingCache<String, Set<Class<? extends Power>>> applyToCache = CacheBuilder
                                                                                           .newBuilder()
                                                                                           .concurrencyLevel(1)
                                                                                           .expireAfterAccess(1, TimeUnit.DAYS)
                                                                                           .build(CacheLoader.from(PowerSelector::parseApplyTo));

    private double getCoordinate(String s, double base) {
        if (s == null) return base;
        if (s.startsWith("~")) {
            return Double.parseDouble(s.substring(1)) + base;
        }
        return Double.parseDouble(s);
    }

    private Location reference(Player p) {
        Location base = p.getLocation();
        return new Location(
                                   p.getWorld(),
                                   getCoordinate(x, base.getX()),
                                   getCoordinate(y, base.getY()),
                                   getCoordinate(z, base.getZ()));
    }

    private static Map<String, Pair<Integer, Integer>> parseScore(String limit) {
        Map<String, Pair<Integer, Integer>> result = new HashMap<>();
        Arrays.stream(limit.split("\\s")).forEach(s -> {
            String name = s.split(":")[0];
            String range = s.split(":")[1];
            String l = range.split(",", -1)[0];
            String u = range.split(",", -1)[1];
            Integer li = l.isEmpty() ? null : Integer.parseInt(l);
            Integer ui = u.isEmpty() ? null : Integer.parseInt(u);
            result.put(name, new Pair<>(li, ui));
        });
        return result;
    }

    private static Set<EntityType> parseType(String type) {
        return Arrays.stream(type.split(",")).map(String::toUpperCase).map(EntityType::valueOf).collect(Collectors.toSet());
    }

    private static Set<Class<? extends Power>> parseApplyTo(String type) {
        return Arrays.stream(type.split(",")).map(powers::get).collect(Collectors.toSet());
    }

    private static Pair<Set<String>, Set<String>> parse(String limit) {
        if (limit.isEmpty()) {
            return new Pair<>(Collections.emptySet(), null);
        }
        if (limit.equals("!")) {
            return new Pair<>(null, Collections.emptySet());
        }
        Set<String> must = new HashSet<>();
        Set<String> mustNot = new HashSet<>();
        Arrays.stream(limit.split(",")).forEach(s -> {
            if (s.startsWith("!")) {
                mustNot.add(s.substring(1));
            } else {
                must.add(s);
            }
        });
        return new Pair<>(must, mustNot);
    }

    public boolean canApplyTo(Class<? extends Power> power) {
        return applyTo != null && applyToCache.getUnchecked(applyTo).contains(power);
    }

    private boolean matchTeam(Entity e, Scoreboard s, Pair<Set<String>, Set<String>> teamLimit) {
        Team t = s.getEntryTeam(e.getUniqueId().toString());
        if (teamLimit.getValue() == null) return t == null;
        if (teamLimit.getKey() == null) return t != null;
        return teamLimit.getKey().stream().findFirst().map(l -> t != null && l.equals(t.getName())).orElse(true)
                       && (t == null || !teamLimit.getValue().contains(t.getName()));
    }

    private boolean matchScore(Entity e, Scoreboard s, Map<String, Pair<Integer, Integer>> scoreLimit) {
        Set<Score> scores = s.getScores(e.getUniqueId().toString());
        Map<String, Integer> smap = new HashMap<>();
        scores.forEach(sc -> smap.put(sc.getObjective().getName(), sc.getScore()));
        return scoreLimit.entrySet().stream().allMatch(sl -> Optional.ofNullable(smap.get(sl.getKey())).map(
                sco -> {
                    boolean result = true;
                    Pair<Integer, Integer> limit = sl.getValue();
                    if (limit.getValue() != null) result = sco < limit.getValue();
                    if (limit.getKey() != null) result &= sco >= limit.getKey();
                    return result;
                }).orElse(false));
    }

    private boolean matchTag(Entity e, Pair<Set<String>, Set<String>> tagLimit) {
        if (tagLimit.getValue() == null) return e.getScoreboardTags().isEmpty();
        if (tagLimit.getKey() == null) return !e.getScoreboardTags().isEmpty();
        return e.getScoreboardTags().stream().allMatch(t -> tagLimit.getKey().contains(t))
                       && e.getScoreboardTags().stream().noneMatch(t -> tagLimit.getValue().contains(t));
    }

    private boolean inRadius(Entity e, Location l, Integer r, Integer rm) {
        boolean result = true;
        double dis = e.getLocation().distance(l);
        if (r != null && r > 0) result = dis < r;
        if (rm != null && rm > 0) result &= dis >= rm;
        return result;
    }

    private boolean inVolume(Entity e, Location l, Integer dx, Integer dy, Integer dz) {
        boolean result = true;
        Location loc = e.getLocation();
        if (dx != null && dx > 0) result = loc.getX() - l.getX() < dx;
        if (dy != null && dy > 0) result &= loc.getY() - l.getY() < dy;
        if (dz != null && dz > 0) result &= loc.getZ() - l.getZ() < dz;
        return result;
    }

    public void inPlaceFilter(Player p, List<Entity> entities) {
        Location ref = reference(p);
        List<Entity> ents = entities.stream().filter(entity -> inRadius(entity, ref, r, rm)).collect(Collectors.toList());
        if (Stream.of(dx, dy, dz).anyMatch(Objects::nonNull))
            ents = ents.stream().filter(entity -> inVolume(entity, ref, dx, dy, dz)).collect(Collectors.toList());
        if (type != null) {
            Set<EntityType> allowType = typeCache.getUnchecked(type);
            ents = ents.stream().filter(entity -> allowType.contains(entity.getType())).collect(Collectors.toList());
        }
        if (tag != null) {
            Pair<Set<String>, Set<String>> t = tagCache.getUnchecked(tag);
            ents = ents.stream().filter(entity -> matchTag(entity, t)).collect(Collectors.toList());
        }
        if (team != null) {
            Pair<Set<String>, Set<String>> t = teamCache.getUnchecked(team);
            ents = ents.stream().filter(entity -> matchTeam(entity, p.getScoreboard(), t)).collect(Collectors.toList());
        }
        if (score != null) {
            Map<String, Pair<Integer, Integer>> t = scoreCache.getUnchecked(score);
            ents = ents.stream().filter(entity -> matchScore(entity, p.getScoreboard(), t)).collect(Collectors.toList());
        }
        entities.clear();
        entities.addAll(ents);
    }

    @Override
    public String getName() {
        return "selector";
    }

    @Override
    public String displayText() {
        return display == null ? I18n.format("power.selector") : display;
    }
}