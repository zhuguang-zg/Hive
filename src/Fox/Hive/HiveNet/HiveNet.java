package Fox.Hive.HiveNet;

import arc.struct.IntMap;
import Fox.Hive.Blocks.blocks;
import mindustry.game.Team;
import mindustry.type.Item;
import mindustry.world.modules.ItemModule;

import static mindustry.Vars.*;

public class HiveNet {
    public static final int MAX_HIVES = 20;
    public static final int CAPACITY_PER_HIVE = 15000;



    public static final IntMap<HiveNet> nets = new IntMap<>();

    public Team team;
    public ItemModule items;
    public boolean clampQueued;

    public int capacity(){
        if(team == Team.derelict) return 0;//灰蜂巢不组网,不收料，只携带自己的份额
        int hives = state.teams.get(team).getCount(blocks.hiveCore);
        return Math.min(hives, MAX_HIVES) * CAPACITY_PER_HIVE;
    }

    //销毁超出容量上限的物品
    public void clamp(){
        if(items == null) return;

        int cap = capacity();
        if(cap <= 0) return;//没有蜂巢时不销毁

        for(Item item : content.items()){
            if(items.get(item) > cap){
                items.set(item, cap);
            }
        }
    }

    public static HiveNet get(Team team){
        HiveNet net = nets.get(team.id);
        if(net == null){
            net = new HiveNet();
            net.team = team;
            nets.put(team.id, net);
        }
        return net;
    }

    public static void reset(){
        nets.clear();
    }
}
