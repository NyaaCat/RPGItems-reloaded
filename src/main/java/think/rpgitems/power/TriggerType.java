package think.rpgitems.power;

import com.google.common.base.CaseFormat;

public enum TriggerType {
    HIT(PowerHit.class),
    HIT_TAKEN(PowerHitTaken.class),
    HURT(PowerHurt.class),
    LEFT_CLICK(PowerLeftClick.class),
    RIGHT_CLICK(PowerRightClick.class),
    PROJECTILE_HIT(PowerProjectileHit.class),
    TICK(PowerTick.class),
    OFFHAND_CLICK(PowerOffhandClick.class),
    SNEAK(PowerSneak.class),
    SPRINT(PowerSprint.class),
    SWAP_TO_OFFHAND(PowerSwapToOffhand.class),
    SWAP_TO_MAINHAND(PowerSwapToMainhand.class),
    PICKUP_OFF_HAND(PowerSwapToOffhand.class),
    PLACE_OFF_HAND(PowerSwapToMainhand.class);


    Class<? extends Power> powerInterface;

    TriggerType(Class<? extends Power> powerInterface) {
        this.powerInterface = powerInterface;
    }

    public Class<? extends Power> getPowerInterface() {
        return powerInterface;
    }

    public static TriggerType fromInterface(Class<? extends Power> powerInterface){
        return TriggerType.valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, powerInterface.getSimpleName()).replace("POWER_", ""));
    }
}
