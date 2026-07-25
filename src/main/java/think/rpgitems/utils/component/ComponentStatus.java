package think.rpgitems.utils.component;

public enum ComponentStatus {
    /** 配置里写了 unset: true，应用时调用 item.unsetData()。 */
    UNSET,
    /** 无值组件（glider / intangible_projectile 这类），应用时调用 setData(NonValued)。 */
    NON_VALUED
}
