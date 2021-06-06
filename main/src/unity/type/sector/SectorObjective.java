package unity.type.sector;

import arc.*;
import arc.func.*;
import arc.graphics.Color;
import arc.math.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

/** @author GlennFolker */
@SuppressWarnings("unchecked")
public abstract class SectorObjective{
    private final Cons<SectorObjective> executor;
    public final ScriptedSector sector;

    public final int executions;
    int execution;

    public Seq<SectorObjective> dependencies = new Seq<>();

    private boolean initialized;
    private boolean finalized;

    private Cons<SectorObjective> init = objective -> {};
    private Cons<SectorObjective> update = objective -> {};
    private Cons<SectorObjective> draw = objective -> {};

    public <T extends SectorObjective> SectorObjective(ScriptedSector sector, int executions, Cons<T> executor){
        this.sector = sector;
        this.executor = (Cons<SectorObjective>)executor;
        this.executions = executions;
    }

    public void init(){
        init.get(this);
        initialized = true;
    }

    public boolean isInitialized(){
        return initialized;
    }

    //using 'do' because method name clash
    public void doFinalize(){
        finalized = true;
    }

    public boolean isFinalized(){
        return finalized;
    }

    public void update(){
        update.get(this);
    }

    public void draw(){
        draw.get(this);
    }

    public boolean shouldUpdate(){
        return !isExecuted() && !completed() && dependencyCompleted();
    }

    public boolean shouldDraw(){
        return shouldUpdate();
    }

    public <T extends SectorObjective> SectorObjective init(Cons<T> init){
        this.init = (Cons<SectorObjective>)init;
        return this;
    }

    public <T extends SectorObjective> SectorObjective update(Cons<T> update){
        this.update = (Cons<SectorObjective>)update;
        return this;
    }

    public <T extends SectorObjective> SectorObjective draw(Cons<T> draw){
        this.draw = (Cons<SectorObjective>)draw;
        return this;
    }

    public SectorObjective dependencies(SectorObjective... dependencies){
        this.dependencies.addAll(dependencies);
        return this;
    }

    public void reset(){
        initialized = false;
        finalized = false;
    }

    public void execute(){
        executor.get(this);
    }

    public abstract boolean completed();

    public boolean isExecuted(){
        return execution >= executions;
    }

    public void stop(){
        execution = executions;
    }

    public boolean qualified(){
        return !isExecuted() && completed() && dependencyCompleted();
    }

    public boolean dependencyCompleted(){
        return dependencies.find(obj -> !obj.isExecuted()) == null;
    }

    public int getExecution(){
        return execution;
    }

    /** Triggers when a {@linkplain #counts specific amount} of units with a certain type spawns. */
    public static class UnitSpawnObjective extends SectorObjective{
        public final UnitType type;
        public final Team team;

        public final int counts;
        protected IntSet ids = new IntSet();

        public <T extends SectorObjective> UnitSpawnObjective(Team team, UnitType type, int counts, ScriptedSector sector, int executions, Cons<T> executor){
            super(sector, executions, executor);

            this.team = team;
            this.type = type;
            this.counts = counts;

            Events.on(UnitDestroyEvent.class, e -> {
                Unit unit = e.unit;
                if(!isExecuted() && sector.valid() && ids.contains(unit.id) && unit.team == this.team && unit.type.id == this.type.id){
                    ids.remove(unit.id);
                }
            });
        }

        @Override
        public void reset(){
            super.reset();
            ids.clear();
        }

        @Override
        public void update(){
            super.update();
            Groups.unit.each(
                unit ->
                    !isExecuted() && sector.valid() &&
                    !ids.contains(unit.id) && unit.team == team && unit.type.id == type.id,
                unit -> ids.add(unit.id)
            );
        }

        @Override
        public boolean completed(){
            return ids.size >= counts;
        }

        @Override
        public void execute(){
            super.execute();
            ids.clear();
        }
    }

    /** Triggers when a {@linkplain #counts specific amount} of units with a certain type dies. */
    public static class UnitDeathObjective extends SectorObjective{
        public final UnitType type;
        public final Team team;

        public final int counts;
        protected IntSet ids = new IntSet();

        public <T extends SectorObjective> UnitDeathObjective(Team team, UnitType type, int counts, ScriptedSector sector, int executions, Cons<T> listener){
            super(sector, executions, listener);

            this.team = team;
            this.type = type;
            this.counts = counts;

            Events.on(UnitDestroyEvent.class, e -> {
                Unit unit = e.unit;
                if(!isExecuted() && sector.valid() && !ids.contains(unit.id) && unit.team == this.team && unit.type.id == this.type.id){
                    ids.add(unit.id);
                }
            });
        }

        @Override
        public void reset(){
            super.reset();
            ids.clear();
        }

        @Override
        public boolean completed(){
            return ids.size >= counts;
        }

        @Override
        public void execute(){
            super.execute();
            ids.clear();
        }
    }

    /** Triggers when a provided unit groups's size is equal or larger to the {@linkplain #counts threshold}. */
    public static class UnitGroupObjective extends SectorObjective{
        public final Prov<Seq<Unit>> provider;
        public final boolean continuous;

        public final int counts;
        protected int count;
        private final IntSet ids = new IntSet();

        public <T extends SectorObjective> UnitGroupObjective(Prov<Seq<Unit>> provider, boolean continuous, int counts, ScriptedSector sector, int executions, Cons<T> executor){
            super(sector, executions, executor);

            this.continuous = continuous;
            this.counts = counts;
            this.provider = provider;
        }

        @Override
        public void reset(){
            super.reset();
            count = 0;
            ids.clear();
        }

        @Override
        public void update(){
            if(!continuous){
                for(Unit unit : provider.get()){
                    ids.add(unit.id);
                }
            }
        }

        @Override
        public boolean completed(){
            if(continuous){
                return provider.get().size >= count;
            }else{
                return ids.size >= counts;
            }
        }
    }

    /** Extends {@link UnitGroupObjective}; provides units in a certain area. */
    public static class UnitPositionObjective extends UnitGroupObjective{
        public <T extends SectorObjective> UnitPositionObjective(Team team, float x, float y, float width, float height, boolean continuous, int count, ScriptedSector sector, int executions, Cons<T> executor){
            super(() ->
                Groups.unit
                    .intersect(x, y, width, height)
                    .select(unit -> unit.team == team),
                continuous, count, sector, executions, executor
            );
        }

        public <T extends SectorObjective> UnitPositionObjective(Team team, float x, float y, float radius, boolean continuous, int count, ScriptedSector sector, int executions, Cons<T> executor){
            super(() ->
                Groups.unit
                    .intersect(x - radius, y - radius, radius * 2f, radius * 2f)
                    .select(unit -> unit.dst(x, y) <= radius && unit.team == team),
                continuous, count, sector, executions, executor
            );
        }
    }

    /** Extends {@link UnitGroupObjective}; provides players in a certain area. */
    public static class PlayerPositionObjective extends UnitGroupObjective{
        public <T extends SectorObjective> PlayerPositionObjective(Team team, float x, float y, float width, float height, boolean continuous, int count, ScriptedSector sector, int executions, Cons<T> executor){
            super(() -> 
                Groups.player
                    .intersect(x, y, width, height)
                    .select(player -> player.team() == team)
                    .map(Player::unit),
                continuous, count, sector, executions, executor
            );
        }

        public <T extends SectorObjective> PlayerPositionObjective(Team team, float x, float y, float radius, boolean continuous, int count, ScriptedSector sector, int executions, Cons<T> executor){
            super(() -> 
                Groups.player
                    .intersect(x - radius, y - radius, radius * 2f, radius * 2f)
                    .select(player -> player.team() == team)
                    .map(Player::unit),
                continuous, count, sector, executions, executor
            );
        }
    }

    public static class ResourceAmountObjective extends SectorObjective{
        protected Table container;
        public final ItemStack[] items;

        public final Color from;
        public final Color to;

        public <T extends SectorObjective> ResourceAmountObjective(ItemStack[] items, ScriptedSector sector, Cons<T> executor){
            this(items, Color.lightGray, Color.green, sector, executor);
        }

        public <T extends SectorObjective> ResourceAmountObjective(ItemStack[] items, Color from, Color to, ScriptedSector sector, Cons<T> executor){
            super(sector, 1, executor);
            this.items = items;
            this.from = from;
            this.to = to;
        }

        @Override
        public void init(){
            super.init();

            if(!headless){
                ui.hudGroup.fill(table -> {
                    table.name = "unity-resource-amount-objective";

                    table.actions(
                        Actions.scaleTo(0f, 1f),
                        Actions.visible(true),
                        Actions.scaleTo(1f, 1f, 0.07f, Interp.pow3Out)
                    );

                    table.center().left();

                    var cell = table.table(Styles.black6, t -> {
                        container = t;

                        ScrollPane pane = t.pane(Styles.defaultPane, cont -> {
                            cont.defaults().pad(4f);

                            for(int i = 0; i < items.length; i++){
                                if(i > 0) cont.row();

                                ItemStack item = items[i];
                                cont.table(Styles.black3, hold -> {
                                    hold.defaults().pad(4f);

                                    hold.left();
                                    hold.image(() -> item.item.uiIcon)
                                        .scaling(Scaling.bounded);

                                    hold.right();
                                    hold.labelWrap(() -> {
                                        int amount = Math.min(state.teams.playerCores().sum(b -> b.items.get(item.item)), item.amount);
                                        return
                                            "[#" + Tmp.c1.set(from).lerp(to, (float)amount / (float)item.amount).toString() + "]" + amount +
                                            " []/ [accent]" + item.amount + "[]";
                                    })
                                        .grow()
                                        .get()
                                        .setAlignment(Align.right);
                                })
                                    .height(40f)
                                    .growX()
                                    .left();
                            }
                        })
                            .update(p -> {
                                if(p.hasScroll()){
                                    Element result = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                                    if(result == null || !result.isDescendantOf(p)){
                                        Core.scene.setScrollFocus(null);
                                    }
                                }
                            })
                            .grow()
                            .pad(4f, 0f, 4f, 4f)
                            .get();

                        pane.setScrollingDisabled(true, false);
                        pane.setOverscroll(false, false);
                    })
                        .minSize(300f, 48f)
                        .maxSize(300f, 156f);

                    cell.visible(() -> ui.hudfrag.shown && sector.valid() && container == cell.get());
                });
            }
        }

        @Override
        public boolean completed(){
            for(ItemStack item : items){
                if(state.teams.playerCores().sum(b -> b.items.get(item.item)) < item.amount){
                    return false;
                }
            }

            return true;
        }

        @Override
        public void doFinalize(){
            super.doFinalize();
            if(container != null){
                container.actions(
                    Actions.moveBy(-container.getWidth(), 0f, 2f, Interp.pow3In),
                    Actions.visible(false),
                    new Action(){
                        @Override
                        public boolean act(float delta){
                            container.parent.removeChild(container);
                            container = null;

                            return true;
                        }
                    }
                );
            }
        }
    }
}
