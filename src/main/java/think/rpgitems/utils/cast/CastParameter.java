package think.rpgitems.utils.cast;

/**
 * @see think.rpgitems.power.impl.Beam:
 */
public class CastParameter {
    private boolean castEnabled = false;
    private double castRange = 10;
    private RangedDoubleValue castR = RangedDoubleValue.of("10:20");
    private RangedDoubleValue castTheta = RangedDoubleValue.of("0:15");
    private RangedDoubleValue castPhi = RangedDoubleValue.of("0:360");
}
