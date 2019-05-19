package think.rpgitems.power;

import org.bukkit.command.CommandSender;

import javax.annotation.CheckReturnValue;

public class PowerResult<T> {
    private TriggerResult result;
    private T data;
    private String message;

    @CheckReturnValue
    public static PowerResult<Void> of(TriggerResult result) {
        return new PowerResult<Void>().result(result);
    }

    @CheckReturnValue
    public static <T> PowerResult<T> of(TriggerResult result, T data) {
        return new PowerResult<T>().result(result).data(data);
    }

    @CheckReturnValue
    public static <T> PowerResult<T> of(TriggerResult result, T data, String message) {
        return new PowerResult<T>().result(result).data(data).message(message);
    }

    public TriggerResult result() {
        return result;
    }

    public T data() {
        return data;
    }

    public String message() {
        return message;
    }

    protected PowerResult<T> result(TriggerResult result) {
        this.result = result;
        return this;
    }

    protected PowerResult<T> data(T data) {
        this.data = data;
        return this;
    }

    protected PowerResult<T> message(String message) {
        this.message = message;
        return this;
    }

    public PowerResult<T> sendMessage(CommandSender sender) {
        if (message != null) {
            sender.sendMessage(message);
        }
        return this;
    }

    public <TP> PowerResult<TP> with(TP data) {
        return of(result, data, message);
    }

    public static PowerResult<Void> ok() {
        return of(TriggerResult.OK);
    }

    public static <T> PowerResult<T> ok(T value) {
        return of(TriggerResult.OK, value);
    }

    public static <T> PowerResult<T> noop() {
        return of(TriggerResult.NOOP, null);
    }

    public static <T> PowerResult<T> cd() {
        return of(TriggerResult.COOLDOWN, null);
    }

    public static <T> PowerResult<T> fail() {
        return of(TriggerResult.FAIL, null);
    }

    public static <T> PowerResult<T> fail(T value) {
        return of(TriggerResult.FAIL, value);
    }

    public static <T> PowerResult<T> cost() {
        return of(TriggerResult.COST, null);
    }

    public static <T> PowerResult<T> abort() {
        return of(TriggerResult.ABORT, null);
    }

    public static <T> PowerResult<T> condition() {
        return of(TriggerResult.CONDITION, null);
    }

    public static <T> PowerResult<T> context() {
        return of(TriggerResult.CONTEXT, null);
    }

    public boolean isOK() {
        return result == TriggerResult.OK;
    }

    public boolean isError() {
        return result == TriggerResult.CONDITION
                       || result == TriggerResult.FAIL
                       || result == TriggerResult.COST
                       || result == TriggerResult.CONTEXT
                       || result == TriggerResult.ABORT;
    }

    public boolean notError() {
        return result == TriggerResult.OK || result == TriggerResult.NOOP || result == TriggerResult.COOLDOWN;
    }

    public boolean isAbort() {
        return result == TriggerResult.ABORT;
    }
}
