package think.rpgitems.power;

public class TriggerPreset {
  public static final Class<? extends Pimpl>[] GENERAL_TRIGGERS =
      new Class[] {
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
      };
}
