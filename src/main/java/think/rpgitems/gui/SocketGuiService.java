package think.rpgitems.gui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.PlayerRPGInventoryCache;

import java.util.*;

public final class SocketGuiService {
    private static final int SIZE = 27;
    private static final int CONTAINER_SLOT = 10;
    private static final int STATUS_SLOT = 8;
    private static final int[] SOCKET_SLOTS = new int[]{3, 4, 5, 6, 7, 12, 13, 14, 15, 16, 21, 22, 23, 24, 25};
    private static final Set<Integer> FILLER_SLOTS = Set.of(0, 1, 2, 9, 11, 17, 18, 19, 20, 26);
    private static final Map<UUID, Session> sessions = new HashMap<>();

    private SocketGuiService() {
    }

    public static void open(Player player) {
        Session current = sessions.remove(player.getUniqueId());
        if (current != null) {
            player.closeInventory();
        }

        Inventory inventory = Bukkit.createInventory(new SocketHolder(player.getUniqueId()), SIZE, I18n.formatDefault("message.socket.title"));
        Session session = new Session(player.getUniqueId(), inventory);
        sessions.put(player.getUniqueId(), session);
        drawStatic(inventory);
        refresh(session, player, true);
        player.openInventory(inventory);
    }

    public static void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (event.getView().getTopInventory() != session.inventory) {
            return;
        }

        int rawSlot = event.getRawSlot();
        boolean topInventory = rawSlot < session.inventory.getSize();
        event.setCancelled(true);

        if (!topInventory) {
            handleBottomInventoryClick(event, session, player);
            return;
        }

        if (rawSlot == CONTAINER_SLOT) {
            handleContainerSlot(event, session, player);
            return;
        }

        if (rawSlot == STATUS_SLOT || FILLER_SLOTS.contains(rawSlot)) {
            return;
        }

        int socketIndex = toSocketIndex(rawSlot);
        if (socketIndex >= 0) {
            handleSocketSlot(event, session, player, socketIndex);
        }
    }

    public static void handleDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (event.getView().getTopInventory() != session.inventory) {
            return;
        }

        boolean touchesTop = event.getRawSlots().stream().anyMatch(slot -> slot < session.inventory.getSize());
        if (touchesTop) {
            event.setCancelled(true);
        }
    }

    public static void handleClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (event.getInventory() != session.inventory) {
            return;
        }

        sessions.remove(player.getUniqueId());
        if (session.container == null) {
            return;
        }

        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(session.container);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private static void handleContainerSlot(InventoryClickEvent event, Session session, Player player) {
        ItemStack cursor = event.getCursor();

        if (isAir(cursor)) {
            if (session.container == null) {
                return;
            }
            event.setCursor(session.container);
            session.container = null;
            session.baseItem = null;
            Arrays.fill(session.socketUids, null);
            refresh(session, player, false);
            Bukkit.getScheduler().runTask(RPGItems.plugin, (Runnable) player::closeInventory);
            return;
        }

        if (!isSingleItem(cursor)) {
            session.statusLines = List.of(I18n.formatDefault("message.socket.error.single_item"));
            refresh(session, player, false);
            return;
        }

        Optional<RPGItem> containerOpt = ItemManager.toBaseRPGItem(cursor, false);
        if (containerOpt.isEmpty()) {
            session.statusLines = List.of(I18n.formatDefault("message.socket.error.container_required"));
            refresh(session, player, false);
            return;
        }

        if (session.container != null) {
            ItemStack old = session.container;
            session.container = null;
            session.baseItem = null;
            Arrays.fill(session.socketUids, null);
            giveItemBack(player, old);
        }

        session.baseItem = containerOpt.get();
        session.container = cloneSingle(cursor);
        removeOne(event);

        Arrays.fill(session.socketUids, null);
        List<Integer> existingSockets = session.baseItem.getSocketedItemUids(session.container);
        for (int i = 0; i < SOCKET_SLOTS.length && i < existingSockets.size(); i++) {
            session.socketUids[i] = existingSockets.get(i);
        }

        refresh(session, player, true);
    }

    private static void handleSocketSlot(InventoryClickEvent event, Session session, Player player, int socketIndex) {
        if (session.container == null || session.baseItem == null) {
            session.statusLines = List.of(I18n.formatDefault("message.socket.error.no_container"));
            refresh(session, player, false);
            return;
        }

        ItemStack cursor = event.getCursor();
        Integer existingUid = session.socketUids[socketIndex];

        if (isAir(cursor)) {
            if (existingUid == null) {
                return;
            }
            Optional<RPGItem> socketOpt = ItemManager.getItem(existingUid);
            socketOpt.map(RPGItem::toItemStack).ifPresent(event::setCursor);
            session.socketUids[socketIndex] = null;
            refresh(session, player, true);
            return;
        }

        if (!isSingleItem(cursor)) {
            session.statusLines = List.of(I18n.formatDefault("message.socket.error.single_item"));
            refresh(session, player, false);
            return;
        }

        Optional<RPGItem> socketItemOpt = ItemManager.toBaseRPGItem(cursor, false);
        if (socketItemOpt.isEmpty()) {
            session.statusLines = List.of(I18n.formatDefault("message.socket.error.socket_required"));
            refresh(session, player, false);
            return;
        }

        RPGItem socketItem = socketItemOpt.get();
        Integer[] next = Arrays.copyOf(session.socketUids, session.socketUids.length);
        next[socketIndex] = socketItem.getUid();

        ValidationResult validation = validateSockets(session.baseItem, session.container, next);
        String reason = validation.reasons.get(socketIndex);
        if (reason != null) {
            session.statusLines = List.of(reason);
            refresh(session, player, false);
            return;
        }

        if (existingUid != null) {
            Optional<RPGItem> oldSocket = ItemManager.getItem(existingUid);
            oldSocket.map(RPGItem::toItemStack).ifPresent(item -> giveItemBack(player, item));
        }

        session.socketUids = next;
        removeOne(event);
        refresh(session, player, true);
    }

    private static void handleBottomInventoryClick(InventoryClickEvent event, Session session, Player player) {
        if (!event.isShiftClick()) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        if (isAir(current)) {
            return;
        }

        if (session.container == null) {
            Optional<RPGItem> containerOpt = ItemManager.toBaseRPGItem(current, false);
            if (containerOpt.isEmpty()) {
                return;
            }
            session.baseItem = containerOpt.get();
            session.container = cloneSingle(current);
            subtractOneFromBottom(event);
            Arrays.fill(session.socketUids, null);
            List<Integer> existingSockets = session.baseItem.getSocketedItemUids(session.container);
            for (int i = 0; i < SOCKET_SLOTS.length && i < existingSockets.size(); i++) {
                session.socketUids[i] = existingSockets.get(i);
            }
            refresh(session, player, true);
            return;
        }

        Optional<RPGItem> socketOpt = ItemManager.toBaseRPGItem(current, false);
        if (socketOpt.isEmpty()) {
            return;
        }

        int firstEmpty = firstEmptySocket(session.socketUids);
        if (firstEmpty < 0) {
            return;
        }

        Integer[] next = Arrays.copyOf(session.socketUids, session.socketUids.length);
        next[firstEmpty] = socketOpt.get().getUid();
        ValidationResult validation = validateSockets(session.baseItem, session.container, next);
        if (validation.reasons.get(firstEmpty) != null) {
            session.statusLines = List.of(validation.reasons.get(firstEmpty));
            refresh(session, player, false);
            return;
        }

        session.socketUids = next;
        subtractOneFromBottom(event);
        refresh(session, player, true);
    }

    private static void refresh(Session session, Player player, boolean applyToContainer) {
        drawStatic(session.inventory);

        if (session.container != null && session.baseItem != null) {
            session.inventory.setItem(CONTAINER_SLOT, session.container);
            for (int i = 0; i < SOCKET_SLOTS.length; i++) {
                Integer uid = session.socketUids[i];
                if (uid == null) {
                    session.inventory.setItem(SOCKET_SLOTS[i], null);
                    continue;
                }
                Optional<RPGItem> socket = ItemManager.getItem(uid);
                session.inventory.setItem(SOCKET_SLOTS[i], socket.map(RPGItem::toItemStack).orElseGet(() -> invalidSocketItem(uid)));
            }

            ValidationResult validation = validateSockets(session.baseItem, session.container, session.socketUids);
            if (session.statusLines.isEmpty()) {
                session.statusLines = validation.summary;
            }

            if (applyToContainer) {
                List<Integer> compactSockets = compactSocketIds(session.socketUids);
                session.baseItem.setSocketedItemUids(session.container, compactSockets);
                session.baseItem.updateItem(session.container, false, player);
                session.inventory.setItem(CONTAINER_SLOT, session.container);
                ItemManager.enqueuePlayerUpdate(player);
                PlayerRPGInventoryCache.getInstance().invalidate(player.getUniqueId());
            }
        } else {
            Arrays.fill(session.socketUids, null);
            session.statusLines = List.of(I18n.formatDefault("message.socket.hint.put_container"));
        }

        session.inventory.setItem(STATUS_SLOT, createStatusItem(session));
        session.statusLines = new ArrayList<>();
    }

    private static ValidationResult validateSockets(RPGItem container, ItemStack containerStack, Integer[] sockets) {
        Map<Integer, String> reasons = new HashMap<>();
        List<String> summary = new ArrayList<>();
        int level = container.getItemLevel(containerStack);
        int usedWeight = 0;

        summary.add(I18n.formatDefault("message.socket.status.level", level));

        for (int i = 0; i < sockets.length; i++) {
            Integer uid = sockets[i];
            if (uid == null) {
                continue;
            }
            Optional<RPGItem> socketOpt = ItemManager.getItem(uid);
            if (socketOpt.isEmpty()) {
                String reason = I18n.formatDefault("message.socket.error.invalid_uid", uid);
                reasons.put(i, reason);
                summary.add(reason);
                continue;
            }

            RPGItem socket = socketOpt.get();
            String reason = validateOneSocket(container, socket, level, usedWeight);
            if (reason != null) {
                reasons.put(i, reason);
                summary.add(reason);
                continue;
            }
            usedWeight += socket.getSocketWeight();
        }

        summary.add(I18n.formatDefault("message.socket.status.weight", usedWeight, container.getSocketMaxWeight()));
        if (summary.size() == 2) {
            summary.add(I18n.formatDefault("message.socket.status.ok"));
        }

        return new ValidationResult(reasons, summary);
    }

    private static String validateOneSocket(RPGItem container, RPGItem socket, int level, int usedWeight) {
        Set<String> acceptTags = container.getSocketAcceptTags();
        Set<String> socketTags = socket.getSocketTags();
        if (acceptTags.isEmpty() || socketTags.isEmpty()) {
            return I18n.formatDefault("message.socket.error.tag_mismatch", socket.getName());
        }

        boolean tagMatch = acceptTags.contains("ANY")
                || socketTags.contains("ANY")
                || socketTags.stream().anyMatch(acceptTags::contains);
        if (!tagMatch) {
            return I18n.formatDefault("message.socket.error.tag_mismatch", socket.getName());
        }

        if (level < socket.getSocketMinLevel()) {
            return I18n.formatDefault("message.socket.error.level_low", socket.getName(), socket.getSocketMinLevel());
        }

        if (usedWeight + socket.getSocketWeight() > container.getSocketMaxWeight()) {
            return I18n.formatDefault("message.socket.error.weight_overflow", socket.getName(), socket.getSocketWeight());
        }

        return null;
    }

    private static ItemStack createStatusItem(Session session) {
        ItemStack status = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = status.getItemMeta();
        if (meta == null) {
            return status;
        }
        meta.setDisplayName(ChatColor.GOLD + I18n.formatDefault("message.socket.status.title"));
        meta.setLore(session.statusLines.stream().map(line -> ChatColor.GRAY + line).toList());
        status.setItemMeta(meta);
        return status;
    }

    private static void drawStatic(Inventory inventory) {
        for (int slot : FILLER_SLOTS) {
            inventory.setItem(slot, fillerItem());
        }
    }

    private static ItemStack fillerItem() {
        ItemStack item = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack invalidSocketItem(int uid) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + I18n.formatDefault("message.socket.error.invalid_uid_short", uid));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static List<Integer> compactSocketIds(Integer[] socketUids) {
        List<Integer> compact = new ArrayList<>();
        for (Integer socketUid : socketUids) {
            if (socketUid != null) {
                compact.add(socketUid);
            }
        }
        return compact;
    }

    private static int firstEmptySocket(Integer[] socketUids) {
        for (int i = 0; i < socketUids.length; i++) {
            if (socketUids[i] == null) {
                return i;
            }
        }
        return -1;
    }

    private static int toSocketIndex(int rawSlot) {
        for (int i = 0; i < SOCKET_SLOTS.length; i++) {
            if (SOCKET_SLOTS[i] == rawSlot) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isSingleItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.getAmount() == 1;
    }

    private static boolean isAir(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    private static ItemStack cloneSingle(ItemStack item) {
        ItemStack cloned = item.clone();
        cloned.setAmount(1);
        return cloned;
    }

    private static void removeOne(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        if (isAir(cursor)) {
            return;
        }
        int amount = cursor.getAmount();
        if (amount <= 1) {
            event.setCursor(null);
            return;
        }
        cursor.setAmount(amount - 1);
        event.setCursor(cursor);
    }

    private static void subtractOneFromBottom(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        if (isAir(current)) {
            return;
        }
        int amount = current.getAmount();
        if (amount <= 1) {
            event.setCurrentItem(null);
            return;
        }
        current.setAmount(amount - 1);
        event.setCurrentItem(current);
    }

    private static void giveItemBack(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        overflow.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
    }

    private record ValidationResult(Map<Integer, String> reasons, List<String> summary) {
    }

    private static final class SocketHolder implements InventoryHolder {
        private final UUID owner;

        private SocketHolder(UUID owner) {
            this.owner = owner;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }

        public UUID getOwner() {
            return owner;
        }
    }

    private static final class Session {
        private final UUID playerId;
        private final Inventory inventory;
        private ItemStack container;
        private RPGItem baseItem;
        private Integer[] socketUids = new Integer[SOCKET_SLOTS.length];
        private List<String> statusLines = new ArrayList<>();

        private Session(UUID playerId, Inventory inventory) {
            this.playerId = playerId;
            this.inventory = inventory;
        }
    }
}
