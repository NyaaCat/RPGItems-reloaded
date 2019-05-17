package think.rpgitems.power;

import org.bukkit.Particle;

@SuppressWarnings("unused")
public enum DeprecatedEffect {
    CLOUD(Particle.CLOUD),
    COLOURED_DUST(Particle.FALLING_DUST),
    CRIT(Particle.CRIT),
    EXPLOSION(Particle.EXPLOSION_NORMAL),
    EXPLOSION_HUGE(Particle.EXPLOSION_HUGE),
    EXPLOSION_LARGE(Particle.EXPLOSION_LARGE),
    FIREWORKS_SPARK(Particle.FIREWORKS_SPARK),
    FLAME(Particle.FLAME),
    FLYING_GLYPH(Particle.ENCHANTMENT_TABLE),
    FOOTSTEP(Particle.NAUTILUS), // TODO: FOOTSTEP is gone
    HAPPY_VILLAGER(Particle.VILLAGER_HAPPY),
    HEART(Particle.HEART),
    INSTANT_SPELL(Particle.SPELL_INSTANT),
    ITEM_BREAK(Particle.ITEM_CRACK),
    LARGE_SMOKE(Particle.SMOKE_LARGE),
    LAVA_POP(Particle.LAVA),
    LAVADRIP(Particle.DRIP_LAVA),
    MAGIC_CRIT(Particle.CRIT_MAGIC),
    NOTE(Particle.NOTE),
    PARTICLE_SMOKE(Particle.SMOKE_NORMAL),
    PORTAL(Particle.PORTAL),
    POTION_SWIRL(Particle.SPELL_MOB),
    POTION_SWIRL_TRANSPARENT(Particle.SPELL_MOB_AMBIENT),
    SLIME(Particle.SLIME),
    SMALL_SMOKE(Particle.TOWN_AURA),
    SNOW_SHOVEL(Particle.SNOW_SHOVEL),
    SNOWBALL_BREAK(Particle.SNOWBALL),
    SPELL(Particle.SPELL),
    SPLASH(Particle.WATER_SPLASH),
    TILE_BREAK(Particle.BLOCK_CRACK),
    TILE_DUST(Particle.BLOCK_DUST),
    VILLAGER_THUNDERCLOUD(Particle.VILLAGER_ANGRY),
    VOID_FOG(Particle.SUSPENDED_DEPTH),
    WATERDRIP(Particle.DRIP_WATER),
    WITCH_MAGIC(Particle.SPELL_WITCH),
    ;

    private final Particle particle;

    DeprecatedEffect(Particle particle) {
        this.particle = particle;
    }

    public Particle getParticle() {
        return particle;
    }
}
