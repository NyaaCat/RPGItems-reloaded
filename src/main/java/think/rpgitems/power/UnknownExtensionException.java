package think.rpgitems.power;

public class UnknownExtensionException extends Exception {
    private String name;

    UnknownExtensionException(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
