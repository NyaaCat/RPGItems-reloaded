import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import cat.nyaa.nyaacore.utils.HexColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import think.rpgitems.RPGItems;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;

import java.util.ArrayList;
import java.util.List;

public class CommandTest {
    private ServerMock server;
    private RPGItems plugin;
    private RPGItem testItem1;
    private RPGItem testItem2;
    private RPGItem testItem3;
    private PlayerMock rein;

    @Before
    public void before(){
        server = MockBukkit.mock();
        plugin = MockBukkit.load(RPGItems.class);
        Bukkit.getPluginManager().callEvent(new ServerLoadEvent(ServerLoadEvent.LoadType.STARTUP));
        rein = server.addPlayer("Aqua_Rein");

        createItems();
    }

    private void createItems() {
        testItem1 = ItemManager.newItem("test_item1", rein);
        testItem2 = ItemManager.newItem("test_item2", rein);
        testItem3 = ItemManager.newItem("test_item3", rein);
        saveItems();
    }

    private void saveItems() {
        ItemManager.save(testItem1);
        ItemManager.save(testItem2);
        ItemManager.save(testItem3);
    }

    @After
    public void cleanup(){
        List<RPGItem> rpgItems = new ArrayList<>(ItemManager.items());
        rpgItems.forEach(item ->{
            ItemManager.remove(item, true);
        });
        MockBukkit.unmock();
    }

    @Test
    public void testAdminCommands(){
        testMetaCommands();
    }

    private void testMetaCommands() {
        new MetaTests().run();
    }

    class MetaTests{
        public void run() {
            testQuality();
            testType();
        }

        private void testType() {
            //initial value
            Assert.assertEquals(testItem1.getType(), "item");
            Assert.assertEquals(testItem2.getType(), "item");
            Assert.assertEquals(testItem3.getType(), "item");

            rein.performCommand("/rpgitems meta type test_item1 normal");
            rein.performCommand("/rpgitems meta type test_item2 rare");
            rein.performCommand("/rpgitems meta type test_item2 legendary");

            Assert.assertEquals(testItem1.getType(), "normal");
            Assert.assertEquals(testItem2.getType(), "rare");
            Assert.assertEquals(testItem3.getType(), "legendary");

        }

        private void testQuality() {
            testItem2.setDisplayName("testVanilla");
            testItem3.setDisplayName("&6testVanilla");
            saveItems();
            rein.performCommand("/rpgitems meta quality test_item1 normal");
            rein.performCommand("/rpgitems meta quality test_item2 rare");
            rein.performCommand("/rpgitems meta quality test_item2 legendary");

            ItemStack is1 = testItem1.toItemStack();
            ItemStack is2 = testItem2.toItemStack();
            ItemStack is3 = testItem3.toItemStack();


            Assert.assertFalse(is1.getItemMeta().hasDisplayName());
            Assert.assertEquals(is2.getItemMeta().getDisplayName(), HexColorUtils.hexColored("&btestVanilla"));
            Assert.assertEquals(is3.getItemMeta().getDisplayName(), HexColorUtils.hexColored("&e&6testVanilla"));

            //test renaming itemstack
            ItemMeta renameTest = is2.getItemMeta();
            renameTest.setDisplayName("&cDesert Eagle | PrintStream");
            is2.setItemMeta(renameTest);
            testItem2.updateItem(is2);
            Assert.assertEquals(is2.getItemMeta().getDisplayName(), HexColorUtils.hexColored("&b&cDesert Eagle | PrintStream"));
        }
    }
}
