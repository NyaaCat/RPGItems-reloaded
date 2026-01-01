package think.rpgitems.power;

public interface PowerPotion {
    @Property
    boolean isAmbient = false;
    @Property
    boolean showParticles = true;
    @Property
    boolean showIcon = true;

    default boolean isAmbient() {
        return isAmbient;
    }

    default boolean isShowParticles(){
        return showParticles;
    }

    default boolean isShowIcon(){
        return showIcon;
    }
}
