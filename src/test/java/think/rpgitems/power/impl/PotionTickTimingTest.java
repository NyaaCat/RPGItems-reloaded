package think.rpgitems.power.impl;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ObjIntConsumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * potiontick timing: when the effect is applied, for how long it is there and when the cooldown is bypassed.
 * <p>
 * {@link Sim} plays one player holding one potiontick item, tick by tick: the RPGItems Ticker fires TICK at the
 * start of every server tick (scheduler), then the world tick counts the effect down and removes it at 0. The
 * gate is {@code PotionTick.Impl#fire}'s: {@link PotionTick#removedBeforeExpiry} decides the bypass, and the
 * normal path is {@code Utils#checkAndSetCooldown} (ready when there is no entry or it has passed; applying sets
 * it to now + interval).
 */
class PotionTickTimingTest {

    static final class Sim {
        final int interval;
        final int duration;
        final boolean clear;
        final double msPerTick;
        final long startMillis = 1_000_000L; // the monotonic clock does not start at 0
        Long cooldown;                        // Context entry of the power
        Integer effect;                       // remaining ticks of the player's effect, null = none
        int applications;                     // successful firings (each one charges cost / durability)
        final StringBuilder timeline = new StringBuilder(); // per tick, after the power ran: 1 = effect present
        int tick;

        Sim(int interval, int duration, boolean clear, double msPerTick) {
            this.interval = interval;
            this.duration = duration;
            this.clear = clear;
            this.msPerTick = msPerTick;
        }

        Sim(int interval, int duration) {
            this(interval, duration, false, 50);
        }

        long now() {
            return startMillis + Math.round(tick * msPerTick);
        }

        void fire() {
            long now = now();
            boolean ready = cooldown == null || cooldown <= now;
            boolean bypass = !clear && !ready && PotionTick.removedBeforeExpiry(now, cooldown, effect, interval, duration);
            if (!bypass) {
                if (!ready) return;
                cooldown = now + interval * 50L;
            }
            applications++;
            if (clear) {
                effect = null;
            } else {
                // vanilla: the same amplifier with a longer duration replaces the running effect
                effect = effect == null ? duration : Math.max(effect, duration);
            }
            if (bypass) cooldown = now + interval * 50L;
        }

        /** Runs {@code ticks} server ticks; {@code beforeTick} may change the player first (milk, another plugin). */
        Sim run(int ticks, ObjIntConsumer<Sim> beforeTick) {
            for (int i = 0; i < ticks; i++, tick++) {
                beforeTick.accept(this, tick);
                fire();
                timeline.append(effect == null ? '0' : '1');
                if (effect != null && effect != Integer.MAX_VALUE && --effect <= 0) effect = null;
            }
            return this;
        }

        Sim run(int ticks) {
            return run(ticks, (sim, t) -> {
            });
        }
    }

    /** "on40 off60 on40 ..." */
    static String runs(CharSequence timeline) {
        List<String> runs = new ArrayList<>();
        for (int i = 0; i < timeline.length(); ) {
            int j = i;
            while (j < timeline.length() && timeline.charAt(j) == timeline.charAt(i)) j++;
            runs.add((timeline.charAt(i) == '1' ? "on" : "off") + (j - i));
            i = j;
        }
        return String.join(" ", runs);
    }

    // ------------------------------------------------------------------ the report

    @Test
    void intervalLongerThanDurationGivesTheEffectThenAGap() {
        // player report: interval 100, duration 40 must be 40 ticks with the effect, then 60 without, repeating
        Sim sim = new Sim(100, 40).run(600);
        assertEquals("on40 off60 on40 off60 on40 off60 on40 off60 on40 off60 on40 off60", runs(sim.timeline));
        assertEquals(6, sim.applications, "cost and durability are charged once per interval");
    }

    @Test
    void gapsAlsoForOtherLongIntervals() {
        assertEquals("on20 off10 on20 off10 on20 off10", runs(new Sim(30, 20).run(90).timeline));
        assertEquals("on1 off19 on1 off19", runs(new Sim(20, 1).run(40).timeline));
        assertEquals("on60 off140 on60 off140", runs(new Sim(200, 60).run(400).timeline));
    }

    @Test
    void gapsSurviveServerLag() {
        // 10 TPS: the cooldown (wall clock) ends after 50 ticks, the effect (ticks) after 40
        assertEquals("on40 off10 on40 off10 on40 off10", runs(new Sim(100, 40, false, 100).run(150).timeline));
    }

    @Test
    void gapsSurviveTheServerCatchingUp() {
        // ticks closer together than 50ms after a lag spike: the effect runs out early by the wall clock
        Sim sim = new Sim(100, 40, false, 30).run(500);
        String r = runs(sim.timeline);
        assertTrue(r.startsWith("on40 off127 on40 off127"), r);
    }

    // ------------------------------------------------------------------ kept behaviours

    @Test
    void intervalShorterThanDurationIsSeamless() {
        // real configs: speed / haste / night vision / slow falling / regeneration, interval 80, duration 340, TICK_INVENTORY
        Sim sim = new Sim(80, 340).run(2000);
        assertEquals("on2000", runs(sim.timeline));
        assertEquals(25, sim.applications, "re-applied every 80 ticks");
    }

    @Test
    void intervalOneTickShorterThanDurationIsSeamless() {
        // real config: regeneration, interval 48, duration 49
        Sim sim = new Sim(48, 49).run(1000);
        assertEquals("on1000", runs(sim.timeline));
        assertEquals(21, sim.applications);
    }

    @Test
    void equalIntervalAndDurationIsSeamless() {
        assertEquals("on500", runs(new Sim(40, 40).run(500).timeline));
    }

    @Test
    void effectRemovedEarlyIsGivenBackOnTheNextTick() {
        // milk / death / effect clear 20 ticks after the last application (interval 80, duration 340)
        Sim sim = new Sim(80, 340).run(400, (s, t) -> {
            if (t == 100) s.effect = null;
        });
        assertEquals("on400", runs(sim.timeline), "re-applied in the same tick the removal is noticed");
        assertEquals(6, sim.applications, "applied at 0, 80, by the bypass at 100, then at 180, 260, 340");
    }

    @Test
    void effectRemovedEarlyIsGivenBackAlsoWhenTheIntervalIsLonger() {
        Sim sim = new Sim(100, 40).run(300, (s, t) -> {
            if (t == 10) s.effect = null;
        });
        // removed at 10, re-applied at 10; the next interval counts from there
        assertEquals("on50 off60 on40 off60 on40 off50", runs(sim.timeline));
    }

    @Test
    void effectCutShortBySomethingElseIsToppedUp() {
        // another plugin replaced our 340 tick effect with a 5 tick one
        Sim sim = new Sim(80, 340).run(300, (s, t) -> {
            if (t == 30) s.effect = 5;
        });
        assertEquals("on300", runs(sim.timeline));
    }

    @Test
    void clearModeRemovesTheEffectOncePerIntervalAndNeverBypasses() {
        // isClear: the power removes the effect; something else gives it at tick 0 and again at tick 10
        Sim sim = new Sim(100, 40, true, 50).run(210, (s, t) -> {
            if (t == 0 || t == 10 || t == 150) s.effect = 1000;
        });
        assertEquals("off10 on90 off50 on50 off10", runs(sim.timeline));
        assertEquals(3, sim.applications);
    }

    // ------------------------------------------------------------------ the decision itself

    private static final long T0 = 1_000_000L;

    /** cooldown that started with an application at T0 */
    private static long cooldownFrom(int interval) {
        return T0 + interval * 50L;
    }

    @Test
    void naturalCountdownIsNotARemoval() {
        // 20 ticks after applying a 40 tick effect, 20 ticks are left: that is our own effect running down
        assertFalse(PotionTick.removedBeforeExpiry(T0 + 20 * 50, cooldownFrom(100), 20, 100, 40));
        assertFalse(PotionTick.removedBeforeExpiry(T0 + 39 * 50, cooldownFrom(100), 1, 100, 40));
    }

    @Test
    void missingEffectWhileItShouldRunIsARemoval() {
        assertTrue(PotionTick.removedBeforeExpiry(T0 + 10 * 50, cooldownFrom(100), null, 100, 40));
        assertTrue(PotionTick.removedBeforeExpiry(T0 + 20 * 50, cooldownFrom(80), null, 80, 340));
    }

    @Test
    void missingEffectAfterItsNaturalEndIsNotARemoval() {
        assertFalse(PotionTick.removedBeforeExpiry(T0 + 40 * 50, cooldownFrom(100), null, 100, 40));
        assertFalse(PotionTick.removedBeforeExpiry(T0 + 70 * 50, cooldownFrom(100), null, 100, 40));
    }

    @Test
    void endingABitEarlyByTheWallClockIsNotARemoval() {
        // effect ran its 40 ticks in 30 wall-clock ticks (catch-up after lag)
        assertFalse(PotionTick.removedBeforeExpiry(T0 + 30 * 50, cooldownFrom(100), null, 100, 40));
    }

    @Test
    void shortenedEffectIsARemoval() {
        assertTrue(PotionTick.removedBeforeExpiry(T0 + 30 * 50, cooldownFrom(80), 5, 80, 340));
    }

    @Test
    void infiniteEffectFromElsewhereIsNotARemoval() {
        assertFalse(PotionTick.removedBeforeExpiry(T0 + 10 * 50, cooldownFrom(100), Integer.MAX_VALUE, 100, 40));
    }
}
