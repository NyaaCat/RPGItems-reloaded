package think.rpgitems.power;

public class PowerProperty {
    private String name;

    private boolean required;

    private int order;

    public PowerProperty(String name, boolean required, int order) {
        this.name = name;
        this.required = required;
        this.order = order;
    }

    public String name() {
        return name;
    }

    public boolean required() {
        return required;
    }

    public int order() {
        return order;
    }
}
