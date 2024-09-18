package think.rpgitems.power.cond;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Meta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;
import think.rpgitems.power.PropertyHolder;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Meta(marker = true)
public class PlaceholderAPICondition extends BaseCondition<Void>{
    @Property(order = 0, required = true)
    public String id;

    @Property(order = 1, required = true)
    public String operator = "==";

    @Property(order = 2, required = true)
    public String placeholder = null;

    @Property(order = 3, required = true)
    public String value = null;

    @Property
    public boolean isStatic = false;

    @Property
    public boolean isCritical = false;

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public boolean isCritical() {
        return isCritical;
    }

    @Override
    public PowerResult<Void> check(Player player, ItemStack stack, Map<PropertyHolder, PowerResult<?>> context) {
        String finalplaceholder = PlaceholderAPI.setPlaceholders(player, placeholder);
        String finalvalue = PlaceholderAPI.setPlaceholders(player, value);
        double placeholdervalue;
        double valuevalue;
        if(finalplaceholder.equals(placeholder)){
            throw new IllegalStateException("Placeholder "+placeholder+" failed to parse, please ensure expansion is downloaded and loaded or there is no typo in your placeholder");
        }
        else switch (operator){
            case "==":
                if(finalplaceholder.equals(finalvalue)){
                    return PowerResult.ok();
                }
                break;
            case "!=":
                if(!finalplaceholder.equals(finalvalue)){
                    return PowerResult.ok();
                }
                break;
            case "===":
                if(finalplaceholder.equalsIgnoreCase(finalvalue)){
                    return PowerResult.ok();
                }
                break;
            case "!==":
                if(!finalplaceholder.equalsIgnoreCase(finalvalue)){
                    return PowerResult.ok();
                }
                break;
            case "contains":
            case "contain":
                if(finalplaceholder.contains(finalvalue)){
                    return PowerResult.ok();
                }
                break;
            case "!contains":
            case "!contain":
                if(!finalplaceholder.contains(finalvalue)){
                    return PowerResult.ok();
                }
                break;
            case "startwith":
                if(finalplaceholder.startsWith(finalvalue)){
                    return PowerResult.ok();
                }
                break;
            case "!startwith":
                if(!finalplaceholder.startsWith(finalvalue)){
                    return PowerResult.ok();
                }
                break;
            case "endwith":
                if(finalplaceholder.endsWith(finalvalue)){
                    return PowerResult.ok();
                }
            case "!endwith":
                if(!finalplaceholder.endsWith(finalvalue)){
                    return PowerResult.ok();
                }
            case "matches":
            case "match":
                if(finalplaceholder.matches(finalvalue)){
                    return PowerResult.ok();
                }
            case "!matches":
            case "!match":
                if(!finalplaceholder.matches(finalvalue)){
                    return PowerResult.ok();
                }
            case ">=":
            case ">":
            case "<":
            case "<=":
                try{
                    placeholdervalue = Double.parseDouble(finalplaceholder.replaceAll(",",""));
                }catch (NumberFormatException e){
                    throw new NumberFormatException(finalplaceholder+"("+placeholder+") is not a number");
                }
                try{
                    valuevalue = Double.parseDouble(finalvalue);
                }catch (NumberFormatException e){
                    throw new NumberFormatException(finalvalue+"("+value+") is not a number");
                }
                switch (operator){
                    case ">=":
                        if(placeholdervalue>=valuevalue){
                            return PowerResult.ok();
                        }
                        else{
                            return PowerResult.fail();
                        }
                    case "<=":
                        if(placeholdervalue<=valuevalue){
                            return PowerResult.ok();
                        }
                        else{
                            return PowerResult.fail();
                        }
                    case ">":
                        if(placeholdervalue>valuevalue) {
                            return PowerResult.ok();
                        }
                        else{
                            return PowerResult.fail();
                        }
                    case "<":
                        if(placeholdervalue<valuevalue){
                            return PowerResult.ok();
                        }
                        else{
                            return PowerResult.fail();
                        }
                }
            default:
                throw new IllegalArgumentException("Unknown operator "+operator);
        }
        return PowerResult.fail();
    }

    @Override
    public Set<String> getConditions() {
        return Collections.emptySet();
    }

    @Override
    public String getName() {
        return "placeholdercondition";
    }

    @Override
    public String displayText() {
        return null;
    }
}
