package think.rpgitems.power;

public class PowerResult<T> {
    private TriggerResult result;
    private T data;

    public static PowerResult<Void> of(TriggerResult result) {
        return new PowerResult<Void>().setResult(result);
    }

    public static <T> PowerResult<T> of(TriggerResult result, T data) {
        return new PowerResult<T>().setResult(result).setData(data);
    }

    public TriggerResult getResult() {
        return result;
    }

    public T getData() {
        return data;
    }

    protected PowerResult<T> setResult(TriggerResult result) {
        this.result = result;
        return this;
    }

    protected PowerResult<T> setData(T data) {
        this.data = data;
        return this;
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

    public static <T> PowerResult<T> cost() {
        return of(TriggerResult.COST, null);
    }

    public static <T> PowerResult<T> abort() {
        return of(TriggerResult.ABORT, null);
    }

    public static <T> PowerResult<T> condition() {
        return of(TriggerResult.CONDITION, null);
    }

    public boolean isOK(){
        return result == TriggerResult.OK;
    }

    public boolean isError(){
        return result == TriggerResult.FAIL || result == TriggerResult.COST || result == TriggerResult.ABORT;
    }

    public boolean notError(){
        return result == TriggerResult.OK || result == TriggerResult.NOOP || result == TriggerResult.COOLDOWN;
    }

    public boolean isAbort(){
        return result == TriggerResult.ABORT;
    }
}
