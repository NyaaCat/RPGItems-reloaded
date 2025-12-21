package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Pair;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import think.rpgitems.RPGItems;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;
import think.rpgitems.power.marker.Selector;
import think.rpgitems.power.proxy.Interceptor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static think.rpgitems.power.Utils.checkCooldown;

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

    private static final LoadingCache<String, Pair<Set<String>, Set<String>>> teamCache = CacheBuilder
            .newBuilder()
            .concurrencyLevel(1)
            .expireAfterAccess(1, TimeUnit.DAYS)
            .build(CacheLoader.from(Selector::parse));
    private static final LoadingCache<String, Pair<Set<String>, Set<String>>> tagCache = CacheBuilder
            .newBuilder()
            .concurrencyLevel(1)
            .expireAfterAccess(1, TimeUnit.DAYS)
            .build(CacheLoader.from(Selector::parse));
    private static final TagReverser tagReverser = new TagReverser();
    private static boolean tagReverserInited = false;
    @Property
    public String tag;
    @Property
    public String team;
    @Property
    public int cooldown = 0;
    @Property
    public ScoreboardOperation scoreOperation = ScoreboardOperation.NO_OP;
    @Property
    public int value = 0;
    @Property
    public String objective = "";
    @Property
    public int cost = 0;
    @Property
    public boolean reverseTagAfterDelay = false;
    @Property
    public long delay = 20;
    @Property
    public boolean abortOnSuccess = false;
    @Property
    public boolean requireHurtByEntity = true;
    private BukkitRunnable removeTask;

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

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

    public static class TagReverser implements Listener {
        private final Map<UUID, List<TagReverseTask>> reverseTaskMap = new HashMap<>();

        private void init() {
            Bukkit.getPluginManager().registerEvents(this, RPGItems.plugin);
            tagReverserInited = true;
        }

        @EventHandler
        public void onLogout(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            UUID uniqueId = player.getUniqueId();
            List<TagReverseTask> tagReverseTasks = reverseTaskMap.computeIfAbsent(uniqueId, uuid -> new ArrayList<>());
            tagReverseTasks.forEach(TagReverseTask::revert);
            tagReverseTasks.clear();
        }

        public void submitReverse(List<String> added, List<String> removed, Player player, int delay) {
            if (!tagReverserInited) {
                init();
            }
            List<TagReverseTask> tagReverseTasks = reverseTaskMap.computeIfAbsent(player.getUniqueId(), uuid -> new ArrayList<>());
            tagReverseTasks.add(new TagReverseTask(added, removed, player).runLater(delay, tagReverseTasks));
        }
    }

    private static class TagReverseTask {
        private final List<String> added;
        private final List<String> removed;
        private final Player player;
        boolean reverted = false;

        public TagReverseTask(List<String> added, List<String> removed, Player player) {
            this.added = added;
            this.removed = removed;
            this.player = player;
        }

        public void revert() {
            if (reverted) {
                return;
            }
            added.forEach(player::removeScoreboardTag);
            removed.forEach(player::addScoreboardTag);
            reverted = true;
        }

        public TagReverseTask runLater(int delay, List<TagReverseTask> tagReverseTasks) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    revert();
                    tagReverseTasks.remove(TagReverseTask.this);
                }
            }.runTaskLater(RPGItems.plugin, delay);
            return this;
        }
    }


    public class Impl implements PowerHit, PowerHitTaken, PowerHurt, PowerLeftClick, PowerRightClick, PowerOffhandClick, PowerProjectileHit, PowerSneak, PowerSprint, PowerOffhandItem, PowerMainhandItem, PowerTick, PowerSneaking, PowerPlain, PowerBowShoot, PowerBeamHit, PowerLivingEntity, PowerLocation, PowerConsume, PowerJump, PowerSwim {

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            // Bypass ByteBuddy proxy to get actual configured values
            Scoreboard original = Interceptor.getOriginal(Scoreboard.this);

            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("objective",player.getScoreboard().getObjective(original.getObjective()));
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkCooldown(getPower(), player, original.getCooldown(), original.showCooldownWarning(), true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, original.getCost())) return PowerResult.cost();

            org.bukkit.scoreboard.Scoreboard scoreboard = player.getScoreboard();

            Objective objective = scoreboard.getObjective(original.getObjective());
            if (objective != null) {
                Score sc = objective.getScore(player.getName());
                int ori = sc.getScore();
                switch (original.getScoreOperation()) {
                    case ADD_SCORE:
                        sc.setScore(ori + original.getValue());
                        break;
                    case SET_SCORE:
                        sc.setScore(original.getValue());
                        break;
                    case RESET_SCORE:
                        sc.setScore(0);
                        break;
                    default:
                }
            }
            if (original.getTeam() != null) {
                Pair<Set<String>, Set<String>> team = teamCache.getUnchecked(original.getTeam());
                team.getKey().stream().map(scoreboard::getTeam).forEach(t -> t.addEntry(player.getName()));
                team.getValue().stream().map(scoreboard::getTeam).forEach(t -> t.removeEntry(player.getName()));
            }

            if (original.getTag() != null) {
                Pair<Set<String>, Set<String>> tag = tagCache.getUnchecked(original.getTag());
                List<String> addedTags = new ArrayList<>();
                List<String> removedTags = new ArrayList<>();
                if (removeTask != null) {
                    if (!removeTask.isCancelled()) {
                        removeTask.cancel();
                        removeTask.run();
                    }
                }
                tag.getKey().forEach(tag1 -> {
                    tag1 = tag1.replaceAll("\\{player}", player.getName());
                    if (player.addScoreboardTag(tag1)) {
                        addedTags.add(tag1);
                    }
                });
                tag.getValue().forEach(tag1 -> {
                    tag1 = tag1.replaceAll("\\{player}", player.getName());
                    if (player.removeScoreboardTag(tag1)) {
                        removedTags.add(tag1);
                    }
                });
                if (original.isReverseTagAfterDelay()) {
                    tagReverser.submitReverse(addedTags, removedTags, player, (int) original.getDelay());
                }
            }
            return original.isAbortOnSuccess() ? PowerResult.abort() : PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return Scoreboard.this;
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack).with(damage);
        }

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> offhandClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> projectileHit(Player player, ItemStack stack, ProjectileHitEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(player, itemStack).with(e.getForce());
        }

        @Override
        public PowerResult<Boolean> swapToMainhand(Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
            return fire(player, stack).with(true);
        }

        @Override
        public PowerResult<Boolean> swapToOffhand(Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
            return fire(player, stack).with(true);
        }

        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sneaking(Player player, ItemStack stack) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Double> hitEntity(Player player, ItemStack stack, LivingEntity entity, double damage, BeamHitEntityEvent event) {
            return fire(player, stack).with(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(Player player, ItemStack stack, Location location, BeamHitBlockEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> beamEnd(Player player, ItemStack stack, Location location, BeamEndEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, @Nullable Double value) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, Location location) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> consume(Player player, ItemStack stack, PlayerItemConsumeEvent event) {
            return fire(player,stack);
        }

        @Override
        public PowerResult<Void> jump(Player player, ItemStack stack, PlayerJumpEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> swim(Player player, ItemStack stack, EntityToggleSwimEvent event) {
            return fire(player, stack);
        }
    }
}