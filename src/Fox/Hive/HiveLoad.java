package Fox.Hive;

import Fox.Hive.Blocks.blocks;
import Fox.Hive.HiveNet.HiveNet;
import arc.Events;
import mindustry.game.EventType;
import mindustry.mod.Mod;

public class HiveLoad extends Mod {
    @Override
    public void loadContent(){
        Events.on(EventType.WorldLoadBeginEvent.class, e -> HiveNet.reset());
        Events.on(EventType.WorldLoadEvent.class, e -> {
            for(HiveNet net : HiveNet.nets.values()){
                net.clamp();
            }
        });
        blocks.load();
    }
}
