package think.rpgitems.power.impl;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.librazy.nclangchecker.LangKey;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

import java.util.List;
import java.util.Optional;

@PowerMeta(defaultTrigger = "RIGHT_CLICK")
public class PowerEffect extends BasePower implements PowerRightClick, PowerLeftClick, PowerSneak, PowerSneaking, PowerHurt, PowerTick {
    EffectManager manager = new EffectManager(RPGItems.plugin);

    @CommandAttribute("effect")
    @Property(required = true)
    public Effects effect;

    @Completer(completer = ParamCompleter.class)
    @Serializer(ParamSerializer.class)
    @Deserializer(ParamSerializer.class)
    @Property()
    public ConfigurationSection params;

    enum Effects {
        ANIMATEDBALL("Animatedball", AnimatedBallEffect.class),
        ARC("Arc", ArcEffect.class),
        ATOM("Atom", AtomEffect.class),
        BIGBANG("Bigbang", BigBangEffect.class),
        BLEED("Bleed", BleedEffect.class),
        CIRCLE("Circle", CircleEffect.class),
        CLOUD("Cloud", CloudEffect.class),
        COLOREDIMAGE("Coloredimage", ColoredImageEffect.class),
        CONE("Cone", ConeEffect.class),
        CUBE("Cube", CubeEffect.class),
        CYLINDER("Cylinder", CylinderEffect.class),
        DISCOBALL("Discoball", DiscoBallEffect.class),
        DNA("Dna", DnaEffect.class),
        DONUT("Donut", DonutEffect.class),
        DRAGON("Dragon", DragonEffect.class),
        EARTH("Earth", EarthEffect.class),
        EQUATION("Equation", EquationEffect.class),
        EXPLODE("Explode", ExplodeEffect.class),
        FLAME("Flame", FlameEffect.class),
        FOUNTAIN("Fountain", FountainEffect.class),
        GRID("Grid", GridEffect.class),
        HEART("Heart", HeartEffect.class),
        HELIX("Helix", HelixEffect.class),
        HILL("Hill", HillEffect.class),
        ICON("Icon", IconEffect.class),
        IMAGE("Image", ImageEffect.class),
        JUMP("Jump", JumpEffect.class),
        LINE("Line", LineEffect.class),
        LOVE("Love", LoveEffect.class),
        MODIFIED("Modified", ModifiedEffect.class),
        MUSIC("Music", MusicEffect.class),
        PLOT("Plot", PlotEffect.class),
        SHIELD("Shield", ShieldEffect.class),
        SKYROCKET("Skyrocket", SkyRocketEffect.class),
        SMOKE("Smoke", SmokeEffect.class),
        SPHERE("Sphere", SphereEffect.class),
        STAR("Star", StarEffect.class),
        TEXT("Text", TextEffect.class),
        TORNADO("Tornado", TornadoEffect.class),
        TRACE("Trace", TraceEffect.class),
        TURN("Turn", TurnEffect.class),
        VORTEX("Vortex", VortexEffect.class),
        WARP("Warp", WarpEffect.class),
        WAVE("Wave", WaveEffect.class);

        String effectName;
        Class<? extends Effect> effectClass;

        Effects(String effect, Class<? extends Effect> effectClass) {
            this.effectName = effect;
            this.effectClass = effectClass;
        }

    }

    @Override
    public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
//        manager.start(effect.effectName, );
        return null;
    }

    private Effect getEffect() {
        try {
//            manager.start()
        }catch (Exception e){

        }
        return null;
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return null;
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return null;
    }

    @Override
    public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
        return null;
    }

    @Override
    public PowerResult<Void> sneaking(Player player, ItemStack stack) {
        return null;
    }

    @Override
    public PowerResult<Void> tick(Player player, ItemStack stack) {
        return null;
    }

    @Override
    public String getName() {
        return "effect";
    }

    @Override
    public String displayText() {
        return "power.effect";
    }

    class ParamSerializer implements Getter<String>, Setter<String>{

        @Override
        public String get(String object) {
            return null;
        }

        @Override
        public Optional<String> set(String value) throws IllegalArgumentException {
            return Optional.empty();
        }
    }

    public class ParamCompleter implements Getter<List<String>> {
        Object[] params;

        public ParamCompleter(Object[] params){
            this.params = params;
        }

        @Override
        public String get(List<String> object) {
            Object param = params[0];
            if (!(param instanceof String))throw new IllegalArgumentException();
            String effectName = (String) param;
            return null;
        }
    }
}
