package Fox.Hive.Blocks;

import mindustry.content.Items;
import mindustry.type.ItemStack;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.BuildVisibility;

public class blocks {
    public static HiveCore hiveCore;
    public static void load(){
        hiveCore = new HiveCore("HiveCore"){{
            size=3;
            health=1700;
            update=false;
            hasItems = true;
            itemCapacity = 100;
            unloadable = true;
            hasPower = false;
            hasLiquids = false;
            destructible = true;
            sync = true;
            allowResupply = true;
            canPickup = false;
            solid = true;
            buildVisibility = BuildVisibility.shown;
            group = BlockGroup.transportation;
            requirements = ItemStack.with(Items.copper, 100, Items.silicon, 100);
        }};
    }
}
