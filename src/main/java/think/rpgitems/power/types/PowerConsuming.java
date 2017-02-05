package think.rpgitems.power.types;

public interface PowerConsuming extends Power {
    public void setConsumption(int cost);
    public int getConsumption();
}
