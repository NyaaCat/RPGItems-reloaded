package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Pair;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import think.rpgitems.RPGItems;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.power.*;
import think.rpgitems.power.marker.Selector;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = {
        PowerLeftClick.class,
        PowerRightClick.class,
        PowerPlain.class,
        PowerSneak.class,
        PowerLivingEntity.class,
        PowerSprint.class,
        PowerHurt.class,
        PowerHit.class,
        PowerHitTaken.class,
        PowerBowShoot.class,
        PowerBeamHit.class,
        PowerLocation.class
}, implClass = Scoreboard.Impl.class)
public class Scoreboard extends BasePower {

    private static LoadingCache<String, Pair<Set<String>, Set<String>>> teamCache = CacheBuilder
                                                                                            .newBuilder()
                                                                                            .concurrencyLevel(1)
                                                                                            .expireAfterAccess(1, TimeUnit.DAYS)
                                                                                            .build(CacheLoader.from(Selector::parse));
    private static LoadingCache<String, Pair<Set<String>, Set<String>>> tagCache = CacheBuilder
                                                                                           .newBuilder()
                                                                                           .concurrencyLevel(1)
                                                                                           .expireAfterAccess(1, TimeUnit.DAYS)
                                                                                           .build(CacheLoader.from(Selector::parse));
    @Property
    public String tag;
    @Property
    public String team;
    @Property
    public ScoreboardOperation scoreOperation = ScoreboardOperation.NO_OP;
    @Property
    public int value = 0;
    @Property
    public String objective = "";
    @Property
    public boolean reverseTagAfterDelay = false;
    @Property
    public long delay = 20;
    @Property
    public boolean abortOnSuccess = false;
    @Property
    public boolean requireHurtByEntity = true;

    public BukkitRunnable getRemoveTask() {
        return removeTask;
    }

    private BukkitRunnable removeTask;

    public long getDelay() {
        return delay;
    }

    @Override
    public String getName() {
        return "scoreboard";
    }

    @Override
    public String displayText() {
        return null;
    }

    public String getObjective() {
        return objective;
    }

    public ScoreboardOperation getScoreOperation() {
        return scoreOperation;
    }

    /**
     * Tag(s) to add and remove, according to the following format
     * `TO_ADD,!TO_REMOVE`
     */
    public String getTag() {
        return tag;
    }

    /**
     * Team(s) to join and leave, according to the following format
     * `JOIN,!LEAVE`
     */
    public String getTeam() {
        return team;
    }

    public int getValue() {
        return value;
    }

    public boolean isAbortOnSuccess() {
        return abortOnSuccess;
    }

    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    public boolean isReverseTagAfterDelay() {
        return reverseTagAfterDelay;
    }

    public enum ScoreboardOperation {
        NO_OP, ADD_SCORE, SET_SCORE, RESET_SCORE
    }

    private static TagReverser tagReverser = new TagReverser();
    private static boolean tagReverserInited = false;
    public static class TagReverser implements Listener {
        private Map<UUID, List<TagReverseTask>> reverseTaskMap = new HashMap<>();
        private void init(){
            Bukkit.getPluginManager().registerEvents(this, RPGItems.plugin);
            tagReverserInited = true;
        }

        @EventHandler
        public void onLogout(PlayerQuitEvent event){
            Player player = event.getPlayer();
            UUID uniqueId = player.getUniqueId();
            List<TagReverseTask> tagReverseTasks = reverseTaskMap.computeIfAbsent(uniqueId, uuid -> new ArrayList<>());
            tagReverseTasks.forEach(TagReverseTask::revert);
            tagReverseTasks.clear();
        }

        public void submitReverse(List<String> added, List<String> removed, Player player, int delay){
            if (!tagReverserInited){
                init();
            }
            List<TagReverseTask> tagReverseTasks = reverseTaskMap.computeIfAbsent(player.getUniqueId(), uuid -> new ArrayList<>());
            tagReverseTasks.add(new TagReverseTask(added, removed, player).runLater(delay, tagReverseTasks));
        }
    }
    private static class TagReverseTask{
        private final List<String>added;
        private final List<String>removed;
        private final Player player;
        boolean reverted = false;

        public TagReverseTask(List<String> added, List<String> removed, Player player) {
            this.added = added;
            this.removed = removed;
            this.player = player;
        }

        public void revert() {
            if(reverted){return;}
            added.forEach(player::removeScoreboardTag);
            removed.forEach(player::addScoreboardTag);
            reverted = true;
        }

        public TagReverseTask runLater(int delay, List<TagReverseTask> tagReverseTasks){
            new BukkitRunnable(){
                @Override
                public void run() {
                    revert();
                    tagReverseTasks.remove(TagReverseTask.this);
                }
            }.runTaskLater(RPGItems.plugin, delay);
            return this;
        }
    }


    public static class Impl implements PowerHit<Scoreboard>, PowerHitTaken<Scoreboard>, PowerHurt<Scoreboard>, PowerLeftClick<Scoreboard>, PowerRightClick<Scoreboard>, PowerOffhandClick<Scoreboard>, PowerProjectileHit<Scoreboard>, PowerSneak<Scoreboard>, PowerSprint<Scoreboard>, PowerOffhandItem<Scoreboard>, PowerMainhandItem<Scoreboard>, PowerTick<Scoreboard>, PowerSneaking<Scoreboard>, PowerPlain<Scoreboard>, PowerBowShoot<Scoreboard>, PowerBeamHit<Scoreboard>, PowerLivingEntity<Scoreboard>, PowerLocation<Scoreboard> {

        @Override
        public PowerResult<Void> leftClick(Scoreboard power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(Scoreboard power, Player player, ItemStack stack) {
            org.bukkit.scoreboard.Scoreboard scoreboard = player.getScoreboard();

            Objective objective = scoreboard.getObjective(power.getObjective());
            if (objective != null) {
                Score sc = objective.getScore(player.getName());
                int ori = sc.getScore();
                switch (power.getScoreOperation()) {
                    case ADD_SCORE:
                        sc.setScore(ori + power.getValue());
                        break;
                    case SET_SCORE:
                        sc.setScore(power.getValue());
                        break;
                    case RESET_SCORE:
                        sc.setScore(0);
                        break;
                    default:
                }
            }
            if (power.getTeam() != null) {
                Pair<Set<String>, Set<String>> team = teamCache.getUnchecked(power.getTeam());
                team.getKey().stream().map(scoreboard::getTeam).forEach(t -> t.addEntry(player.getName()));
                team.getValue().stream().map(scoreboard::getTeam).forEach(t -> t.removeEntry(player.getName()));
            }

            if (power.getTag() != null) {
                Pair<Set<String>, Set<String>> tag = tagCache.getUnchecked(power.getTag());
                List<String> addedTags = new ArrayList<>();
                List<String> removedTags = new ArrayList<>();
                if (power.getRemoveTask() != null) {
                    if (!power.getRemoveTask().isCancelled()) {
                        power.getRemoveTask().cancel();
                        power.getRemoveTask().run();
                    }
                }
                tag.getKey().forEach(tag1 -> {
                    if (player.addScoreboardTag(tag1)) {
                        addedTags.add(tag1);
                    }
                });
                tag.getValue().forEach(tag1 -> {
                    if (player.removeScoreboardTag(tag1)) {
                        removedTags.add(tag1);
                    }
                });
                if (power.isReverseTagAfterDelay()) {
                    tagReverser.submitReverse(addedTags, removedTags, player, (int) power.getDelay());
                }
            }
            return power.isAbortOnSuccess() ? PowerResult.abort() : PowerResult.ok();
        }

        @Override
        public Class<? extends Scoreboard> getPowerClass() {
            return Scoreboard.class;
        }

        @Override
        public PowerResult<Void> rightClick(Scoreboard power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Double> hit(Scoreboard power, Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(power, player, stack).with(damage);
        }

        @Override
        public PowerResult<Double> takeHit(Scoreboard power, Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> hurt(Scoreboard power, Player target, ItemStack stack, EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> offhandClick(Scoreboard power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> projectileHit(Scoreboard power, Player player, ItemStack stack, ProjectileHitEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sneak(Scoreboard power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sprint(Scoreboard power, Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Scoreboard power, Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(power, player, itemStack).with(e.getForce());
        }

        @Override
        public PowerResult<Boolean> swapToMainhand(Scoreboard power, Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
            return fire(power, player, stack).with(true);
        }

        @Override
        public PowerResult<Boolean> swapToOffhand(Scoreboard power, Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
            return fire(power, player, stack).with(true);
        }

        @Override
        public PowerResult<Void> tick(Scoreboard power, Player player, ItemStack stack) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sneaking(Scoreboard power, Player player, ItemStack stack) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Double> hitEntity(Scoreboard power, Player player, ItemStack stack, LivingEntity entity, double damage, BeamHitEntityEvent event) {
            return fire(power, player, stack).with(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(Scoreboard power, Player player, ItemStack stack, Location location, BeamHitBlockEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> beamEnd(Scoreboard power, Player player, ItemStack stack, Location location, BeamEndEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(Scoreboard power, Player player, ItemStack stack, LivingEntity entity, @Nullable Double value) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(Scoreboard power, Player player, ItemStack stack, Location location) {
            return fire(power, player, stack);
        }
    }
}