package think.rpgitems.power;

public interface AllTrigger
        extends Power,
                        PowerHit,
                        PowerHitTaken,
                        PowerLeftClick,
                        PowerRightClick,
                        PowerOffhandClick,
                        PowerProjectileHit,
                        PowerSneak,
                        PowerSprint,
                        PowerSwapToMainhand,
                        PowerSwapToOffhand,
                        PowerTick {
}
