package think.rpgitems.power;

public class PowerProperty {
    private String name;

    private boolean required;

    private int order;

    private String[] alias;

    public PowerProperty(String name, boolean required, int order, String[] alias) {
        this.name = name;
        this.required = required;
        this.order = order;
        this.alias = alias;
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

    public String[] alias() {
        return alias;
    }
}
