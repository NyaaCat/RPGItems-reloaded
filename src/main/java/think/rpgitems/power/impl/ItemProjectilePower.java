package think.rpgitems.power.impl;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import think.rpgitems.I18n;
import think.rpgitems.Events;
import think.rpgitems.RPGItems;
import think.rpgitems.power.AcceptedValue;
import think.rpgitems.power.Deserializer;
import think.rpgitems.power.Meta;
import think.rpgitems.power.PowerBeamHit;
import think.rpgitems.power.PowerBowShoot;
import think.rpgitems.power.PowerHit;
import think.rpgitems.power.PowerHitTaken;
import think.rpgitems.power.PowerHurt;
import think.rpgitems.power.PowerJump;
import think.rpgitems.power.PowerLeftClick;
import think.rpgitems.power.PowerLivingEntity;
import think.rpgitems.power.PowerLocation;
import think.rpgitems.power.PowerPlain;
import think.rpgitems.power.PowerRightClick;
import think.rpgitems.power.PowerSneak;
import think.rpgitems.power.PowerSprint;
import think.rpgitems.power.PowerSwim;
import think.rpgitems.power.Property;
import think.rpgitems.utils.MaterialUtils;

@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = {
        PowerLeftClick.class,
        PowerRightClick.class,
        PowerSwim.class,
        PowerJump.class,
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
}, implClass = ItemProjectilePower.Impl.class)
public class ItemProjectilePower extends ProjectilePower {
    private static final double MIN_DIRECTION_LENGTH_SQUARED = 1.0E-8;

    @Property
    @Deserializer(MaterialUtils.class)
    public Material itemMaterial = Material.PAPER;

    @Property
    public CustomModelData.Builder customModelData = null;

    @Property
    public String itemModel = "";

    @Property
    @AcceptedValue({
            "NONE",
            "THIRDPERSON_LEFTHAND",
            "THIRDPERSON_RIGHTHAND",
            "FIRSTPERSON_LEFTHAND",
            "FIRSTPERSON_RIGHTHAND",
            "HEAD",
            "GUI",
            "GROUND",
            "FIXED"
    })
    public ItemDisplay.ItemDisplayTransform displayTransform = ItemDisplay.ItemDisplayTransform.FIXED;

    @Property
    public double displayScale = 1.0;

    @Property
    public double displayOffsetX = 0.0;

    @Property
    public double displayOffsetY = 0.0;

    @Property
    public double displayOffsetZ = 0.0;

    @Property
    @AcceptedValue({"LAUNCH", "VELOCITY"})
    public RotationReference rotationReference = RotationReference.VELOCITY;

    @Property
    @AcceptedValue({"FIXED", "SPIN"})
    public RotationMode rotationMode = RotationMode.FIXED;

    @Property
    public double rotationYaw = 0.0;

    @Property
    public double rotationPitch = 0.0;

    @Property
    public double rotationRoll = 0.0;

    @Property
    @AcceptedValue({"X", "Y", "Z"})
    public SpinAxis spinAxis = SpinAxis.Z;

    @Property
    public double spinSpeed = 0.0;

    @Property
    public double spinInitial = 0.0;

    @Property
    public double maxRange = 0.0;

    @Property
    public int ttl = 0;

    @Property
    public Double damage = null;

    @Override
    public String getName() {
        return "item_projectile";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault(isCone() ? "power.item_projectile.cone" : "power.item_projectile.display", getItemMaterial().getKey().asString(), (double) getCooldown() / 20d);
    }

    public Material getItemMaterial() {
        if (itemMaterial == null || itemMaterial == Material.AIR || !itemMaterial.isItem()) {
            return Material.PAPER;
        }
        return itemMaterial;
    }

    public CustomModelData.Builder getCustomModelData() {
        return customModelData;
    }

    public String getItemModel() {
        return itemModel;
    }

    public ItemDisplay.ItemDisplayTransform getDisplayTransform() {
        return displayTransform == null ? ItemDisplay.ItemDisplayTransform.FIXED : displayTransform;
    }

    public double getDisplayScale() {
        return Double.isFinite(displayScale) ? Math.max(displayScale, 0.0) : 1.0;
    }

    public double getDisplayOffsetX() {
        return displayOffsetX;
    }

    public double getDisplayOffsetY() {
        return displayOffsetY;
    }

    public double getDisplayOffsetZ() {
        return displayOffsetZ;
    }

    public RotationReference getRotationReference() {
        return rotationReference == null ? RotationReference.VELOCITY : rotationReference;
    }

    public RotationMode getRotationMode() {
        return rotationMode == null ? RotationMode.FIXED : rotationMode;
    }

    public double getRotationYaw() {
        return rotationYaw;
    }

    public double getRotationPitch() {
        return rotationPitch;
    }

    public double getRotationRoll() {
        return rotationRoll;
    }

    public SpinAxis getSpinAxis() {
        return spinAxis == null ? SpinAxis.Z : spinAxis;
    }

    public double getSpinSpeed() {
        return spinSpeed;
    }

    public double getSpinInitial() {
        return spinInitial;
    }

    public double getMaxRange() {
        return Double.isFinite(maxRange) ? Math.max(maxRange, 0.0) : 0.0;
    }

    public int getTtl() {
        return Math.max(ttl, 0);
    }

    public Double getDamage() {
        if (damage == null || !Double.isFinite(damage) || damage < 0.0) {
            return null;
        }
        return damage;
    }

    public enum RotationReference {
        LAUNCH,
        VELOCITY
    }

    public enum RotationMode {
        FIXED,
        SPIN
    }

    public enum SpinAxis {
        X,
        Y,
        Z
    }

    public class Impl extends ProjectilePower.Impl {
        @Override
        protected void handleProjectile(Vector v, Projectile projectile) {
            super.handleProjectile(v, projectile);
            Double customDamage = getDamage();
            if (customDamage != null) {
                projectile.getPersistentDataContainer().set(new NamespacedKey(RPGItems.plugin, Events.PROJECTILE_DAMAGE), PersistentDataType.DOUBLE, customDamage);
            }
        }

        @Override
        protected int getProjectileTtl() {
            return getTtl() > 0 ? 0 : super.getProjectileTtl();
        }

        @Override
        protected void afterProjectileLaunch(Player player, ItemStack stack, Vector v, Projectile projectile) {
            projectile.setInvisible(true);
            projectile.setVisibleByDefault(false);
            projectile.addScoreboardTag("rgi_item_projectile_carrier");

            Vector launchDirection = normalizedOrDefault(v);
            Location launchLocation = projectile.getLocation().clone();
            ItemDisplay display = projectile.getWorld().spawn(projectile.getLocation(), ItemDisplay.class, itemDisplay -> {
                itemDisplay.setItemStack(createDisplayItem());
                itemDisplay.setItemDisplayTransform(getDisplayTransform());
                itemDisplay.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
                itemDisplay.setGravity(false);
                itemDisplay.setPersistent(false);
                itemDisplay.setInterpolationDelay(0);
                itemDisplay.setInterpolationDuration(1);
                itemDisplay.setTeleportDuration(1);
                itemDisplay.addScoreboardTag("rgi_item_projectile_display");
            });
            updateDisplay(projectile, display, launchDirection, 0);

            new BukkitRunnable() {
                private int age = 1;
                private int rotationTick = 1;
                private Vector lastDirection = launchDirection.clone();
                private boolean rotationStopped = false;

                @Override
                public void run() {
                    if (!projectile.isValid() || projectile.isDead()) {
                        display.remove();
                        cancel();
                        return;
                    }
                    if (!display.isValid() || display.isDead()) {
                        cancel();
                        return;
                    }
                    if (shouldExpire(projectile, launchLocation, age)) {
                        projectile.remove();
                        display.remove();
                        cancel();
                        return;
                    }

                    if (!rotationStopped && shouldStopRotation(projectile)) {
                        rotationStopped = true;
                    }
                    Vector direction = rotationStopped ? lastDirection.clone() : getReferenceDirection(projectile, lastDirection);
                    updateDisplay(projectile, display, direction, rotationTick);
                    if (!rotationStopped) {
                        lastDirection = direction.clone();
                        rotationTick++;
                    }
                    age++;
                }
            }.runTaskTimer(RPGItems.plugin, 1, 1);
        }

        private ItemStack createDisplayItem() {
            ItemStack item = new ItemStack(getItemMaterial());
            NamespacedKey model = getParsedItemModel();
            if (model != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setItemModel(model);
                    item.setItemMeta(meta);
                }
            }
            if (getCustomModelData() != null) {
                item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, getCustomModelData().build());
            }
            return item;
        }

        private NamespacedKey getParsedItemModel() {
            if (getItemModel() == null || getItemModel().isBlank()) {
                return null;
            }
            return NamespacedKey.fromString(getItemModel());
        }

        private Vector getReferenceDirection(Projectile projectile, Vector fallback) {
            if (getRotationReference() == RotationReference.LAUNCH) {
                return fallback.clone();
            }
            return normalizedOrDefault(projectile.getVelocity(), fallback);
        }

        private Vector normalizedOrDefault(Vector vector) {
            return normalizedOrDefault(vector, new Vector(0, 0, 1));
        }

        private Vector normalizedOrDefault(Vector vector, Vector fallback) {
            if (vector != null && vector.lengthSquared() > MIN_DIRECTION_LENGTH_SQUARED) {
                return vector.clone().normalize();
            }
            return fallback.clone().normalize();
        }

        private boolean shouldExpire(Projectile projectile, Location launchLocation, int age) {
            int ttl = getTtl();
            if (ttl > 0 && age >= ttl) {
                return true;
            }
            double maxRange = getMaxRange();
            if (maxRange <= 0.0) {
                return false;
            }
            Location location = projectile.getLocation();
            if (!location.getWorld().equals(launchLocation.getWorld())) {
                return true;
            }
            return location.distanceSquared(launchLocation) >= maxRange * maxRange;
        }

        private boolean shouldStopRotation(Projectile projectile) {
            if (projectile instanceof AbstractArrow arrow && arrow.isInBlock()) {
                return true;
            }
            return projectile.isOnGround() && projectile.getVelocity().lengthSquared() <= MIN_DIRECTION_LENGTH_SQUARED;
        }

        private void updateDisplay(Projectile projectile, ItemDisplay display, Vector direction, int tick) {
            Quaternionf directionRotation = directionRotation(direction);
            Vector3f offset = new Vector3f((float) getDisplayOffsetX(), (float) getDisplayOffsetY(), (float) getDisplayOffsetZ());
            directionRotation.transform(offset);

            org.bukkit.Location location = projectile.getLocation().clone().add(offset.x(), offset.y(), offset.z());
            location.setYaw(0);
            location.setPitch(0);
            display.teleport(location);

            Quaternionf displayRotation = directionRotation.mul(localRotation());
            if (getRotationMode() == RotationMode.SPIN) {
                displayRotation.mul(spinRotation(tick));
            }
            float scale = Math.max((float) getDisplayScale(), 0.0f);
            display.setTransformation(new Transformation(new Vector3f(), displayRotation, new Vector3f(scale, scale, scale), new Quaternionf()));
        }

        private Quaternionf directionRotation(Vector direction) {
            Vector normalized = normalizedOrDefault(direction);
            float yaw = (float) Math.atan2(normalized.getX(), normalized.getZ());
            float pitch = (float) -Math.asin(Math.max(-1.0, Math.min(1.0, normalized.getY())));
            return new Quaternionf().rotationYXZ(yaw, pitch, 0.0f);
        }

        private Quaternionf localRotation() {
            return new Quaternionf().rotationYXZ(toRadians(getRotationYaw()), toRadians(getRotationPitch()), toRadians(getRotationRoll()));
        }

        private Quaternionf spinRotation(int tick) {
            float angle = toRadians(getSpinInitial() + getSpinSpeed() * tick);
            return switch (getSpinAxis()) {
                case X -> new Quaternionf().rotationX(angle);
                case Y -> new Quaternionf().rotationY(angle);
                case Z -> new Quaternionf().rotationZ(angle);
            };
        }

        private float toRadians(double degrees) {
            return (float) Math.toRadians(degrees);
        }
    }
}
