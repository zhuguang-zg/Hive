package Fox.Hive.Blocks;

import Fox.Hive.HiveNet.HiveNet;
import arc.Core;
import mindustry.content.Blocks;
import mindustry.core.UI;
import mindustry.ctype.UnlockableContent;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Teamc;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.ui.Bar;
import mindustry.world.Block;
import arc.util.Time;
import mindustry.logic.LAccess;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.meta.Stat;
import mindustry.world.modules.ItemModule;

import static mindustry.Vars.*;


public class HiveCore extends Block {
    public HiveCore(String name) {
        super(name);
    }
    //进度条
    @Override
    public void setBars(){
        super.setBars();
        removeBar("items");
        addBar("hive-capacity", (HiveCoreBuild e) -> new Bar(
                () -> Core.bundle.format("bar.capacity", UI.formatAmount(HiveNet.get(e.team).capacity())),
                () -> Pal.items,
                () -> e.items.total() / ((float)Math.max(1, HiveNet.get(e.team).capacity())
                        * Math.max(1, content.items().count(UnlockableContent::unlockedNow)))
        ));
    }    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.itemCapacity);
        stats.add(Stat.itemCapacity, table -> table.add("[white]15000 ~ 300000\n每个蜂巢 +15000，每队上限 20 个"));
    }

    //本队已占用的蜂巢数 = 已建成 + 建造中
    public int countHives(Team team){
        int finished = state.teams.get(team).getCount(this);

        if(finished >= HiveNet.MAX_HIVES) return finished;

        int building = 0;
        for(Building b : Groups.build){
            if(b.team == team && b instanceof ConstructBlock.ConstructBuild cb && cb.current == this){
                building++;
                if(finished + building >= HiveNet.MAX_HIVES) break;
            }
        }
        return finished + building;
    }
    //放置限制
    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        if(tile == null) return false;
        return countHives(team) < HiveNet.MAX_HIVES;
    }
    @Override
    public void blockChanged(Tile tile){
        if(world.isGenerating() || tile == null || !(tile.build instanceof HiveCoreBuild)) return;

        if(countHives(tile.team()) > HiveNet.MAX_HIVES){
            tile.setBlock(Blocks.air);
        }
    }
    public static class HiveCoreBuild extends Building {
        public int netCapacity(){
            return HiveNet.get(team).capacity();
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return items.get(item) < netCapacity();
        }
        @Override
        public int getMaximumAccepted(Item item){
            return netCapacity();
        }
        //多个送入
        @Override
        public void handleStack(Item item, int amount, Teamc source){
            int accepted = Math.min(amount, Math.max(0, netCapacity() - items.get(item)));
            if(accepted > 0){
                noSleep();
                items.add(item, accepted);
            }
        }
        //单个送入
        @Override
        public void handleItem(Building source, Item item){
            if(items.get(item) < netCapacity()){
                items.add(item, 1);
            }
        }

        //逻辑支持
        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.itemCapacity){
                return netCapacity();
            }
            return super.sense(sensor);
        }
        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            if(team == Team.derelict) return;//灰蜂巢各自携带，不并入网络

            HiveNet net = HiveNet.get(team);
            if(net.items == null){
                net.items = items;
            }else{
                items = net.items;
            }
        }

        //销毁
        @Override
        public void onRemoved(){
            if(team == Team.derelict) return;//灰蜂巢被炸/被拆，那份随它消失

            HiveNet net = HiveNet.get(team);
            if(net.items != null && !net.clampQueued){
                net.clampQueued = true;
                Time.run(10f, () -> {
                    net.clampQueued = false;
                    net.clamp();
                });
            }
        }

        //易主
        @Override
        public void changeTeam(Team next){
            changeTeam(next, true);
        }

        @Override
        public void changeTeam(Team next, boolean updatePower){
            if(team == next) return;

            Team last = team;

            //离开旧网络,把自己应得的一份拿出来随身携带
            if(last != Team.derelict && items != null){
                HiveNet old = HiveNet.get(last);

                //确认"我"确实在旧网络的池子里（items 就是池子本身）
                if(old.items != null && old.items == items){
                    //此刻索引器还没换队，所以这个数包含自己
                    int hives = Math.max(1, blocks.hiveCore.countHives(last));

                    ItemModule own = new ItemModule();
                    for(Item item : content.items()){
                        int share = old.items.get(item) / hives;//向下取整，余数留在池子里
                        if(share > 0){
                            old.items.remove(item, share);
                            own.set(item, share);
                        }
                    }
                    items = own;//从现在起自己携带

                    //旧队伍容量降1,排队一次延迟销毁
                    if(!old.clampQueued){
                        old.clampQueued = true;
                        Time.run(10f, () -> {
                            old.clampQueued = false;
                            old.clamp();
                        });
                    }
                }
            }

            //交给引擎换队
            super.changeTeam(next, updatePower);

            //灰队：不组网，自己带着份额留在原地
            if(next == Team.derelict) return;

            //加入新网络：并入（或者我这个模块直接成为新池子）
            HiveNet net = HiveNet.get(next);
            if(net.items == null){
                net.items = items;//新网络还没有池子，我这个直接当池子
            }else{
                net.items.add(items);//把我的份额并进去
                items = net.items;
                net.clamp();
            }
        }
    }
}
