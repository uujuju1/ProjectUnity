package unity.tools;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.Mathf;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import unity.entities.*;
import unity.entities.units.*;
import unity.gen.*;
import unity.tools.SpriteProcessor.*;
import unity.type.*;

import static mindustry.Vars.*;

public class IconGenerator implements Generator{
    @Override
    public void generate(){
        Func<TextureRegion, String> parseName = reg -> ((AtlasRegion)reg).name.replaceFirst("unity-", "");

        content.items().each(item -> {
            if(item.minfo.mod == null) return;

            try{
                item.init();

                if(item instanceof AnimatedItem anim){
                    for(int i = 0; i < anim.frames; i++){
                        String fname = anim.name.replaceFirst("unity-", "") + (i + 1);

                        GenRegion region = SpriteProcessor.getRegion(fname);
                        Sprite sprite = SpriteProcessor.get(region);

                        sprite.antialias();
                        region.path.delete();
                        sprite.save(fname);
                    }
                }else{
                    String fname = item.name.replaceFirst("unity-", "");

                    GenRegion region = SpriteProcessor.getRegion(fname);
                    Sprite sprite = SpriteProcessor.get(region);

                    sprite.antialias();
                    region.path.delete();
                    sprite.save(fname);
                }
            }catch(Throwable e){
                if(e instanceof IllegalArgumentException i){
                    Log.err("Skipping item @: @", item, i.getMessage());
                }else{
                    Log.err(Strings.format("Problematic item @", item), e);
                }
            }
        });

        content.units().each(t -> {
            if(t.minfo.mod == null || !(t instanceof UnityUnitType type)) return;

            ObjectSet<String> outlined = new ObjectSet<>();
            try{
                type.load();
                type.init();

                Func<Sprite, Sprite> outline = i -> i.outline(3, type.outlineColor);
                Seq<String> optional = Seq.with("-joint", "-leg-back", "-leg-base-back", "-foot-back");

                Cons<TextureRegion> outliner = tr -> {
                    if(tr != null){
                        String fname = parseName.get(tr);

                        if(SpriteProcessor.has(fname)){
                            GenRegion region = SpriteProcessor.getRegion(fname);
                            Sprite sprite = SpriteProcessor.get(fname);

                            sprite.draw(outline.get(sprite));

                            sprite.antialias();
                            region.path.delete();
                            sprite.save(fname);
                        }else if(!optional.contains(fname::contains)){
                            Log.warn("@ not found", fname);
                        }
                    }else{
                        Log.warn("A region is null");
                    }
                };
                Cons2<String, TextureRegion> outlSeparate = (suff, tr) -> {
                    if(tr != null){
                        String fname = parseName.get(tr);

                        if(SpriteProcessor.has(fname)){
                            Sprite sprite = SpriteProcessor.get(fname);
                            sprite.draw(outline.get(sprite));
                            sprite.antialias();

                            sprite.save(fname + "-" + suff);
                        }else{
                            Log.warn("@ not found", fname);
                        }
                    }else{
                        Log.warn("A region is null");
                    }
                };

                for(Weapon weapon : type.weapons){
                    String fname = weapon.name.replaceFirst("unity-", "");

                    if(outlined.add(fname) && SpriteProcessor.has(fname)){
                        outlSeparate.get("outline", weapon.region);
                    }
                }

                for(Weapon weapon : type.segWeapSeq){
                    String fname = weapon.name.replaceFirst("unity-", "");

                    if(outlined.add(fname) && SpriteProcessor.has(fname)){
                        outlSeparate.get("outline", weapon.region);
                    }
                }

                Unit unit = type.constructor.get();

                if(unit instanceof Legsc){
                    outliner.get(type.jointRegion);
                    outliner.get(type.footRegion);
                    outliner.get(type.legBaseRegion);
                    outliner.get(type.baseJointRegion);
                    outliner.get(type.legRegion);

                    outliner.get(type.legBackRegion);
                    outliner.get(type.legBaseBackRegion);
                    outliner.get(type.footBackRegion);
                }

                if(unit instanceof Mechc){
                    outliner.get(type.legRegion);
                }

                if(unit instanceof Copterc){
                    for(Rotor rotor : type.rotors){
                        String fname = type.name.replaceFirst("unity-", "") + "-rotor";

                        if(outlined.add(fname + "-blade")){
                            outlSeparate.get("outline", rotor.bladeRegion);
                        }
                        if(outlined.add(fname + "-top")){
                            outliner.get(rotor.topRegion);
                        }
                    }
                }

                for(TextureRegion reg : type.abilityRegions){
                    String fname = parseName.get(reg);
                    if(outlined.add(fname) && SpriteProcessor.has(fname)){
                        outliner.get(reg);
                    }
                }

                for(TentacleType tentacle : type.tentacles){
                    if(outlined.add(tentacle.name)){
                        outliner.get(tentacle.region);
                    }
                    if(outlined.add(tentacle.name + "-tip")){
                        outliner.get(tentacle.tipRegion);
                    }
                }

                outlSeparate.get("outline", type.region);
                if(unit instanceof WormDefaultUnit){
                    outlSeparate.get("outline", type.segmentRegion);
                    outlSeparate.get("outline", type.tailRegion);
                }

                String fname = parseName.get(type.region);
                Sprite outl = outline.get(SpriteProcessor.get(fname));
                Sprite region = SpriteProcessor.get(fname);

                Sprite icon = Sprite.createEmpty(region.width, region.height);
                icon.draw(region);
                icon.draw(outline.get(region));

                if(unit instanceof Mechc){
                    Sprite leg = SpriteProcessor.get(parseName.get(type.legRegion));
                    icon.drawCenter(leg);
                    icon.drawCenter(leg, true, false);

                    icon.draw(region);
                    icon.draw(outl);
                }

                for(Weapon weapon : type.weapons){
                    weapon.load();
                    String wname = weapon.name.replaceFirst("unity-", "");

                    if((!weapon.top || type.bottomWeapons.contains(weapon.name)) && SpriteProcessor.has(wname)){
                        Sprite weapSprite = SpriteProcessor.get(wname);

                        icon.draw(weapSprite,
                            (int)(weapon.x * 4f / Draw.scl + icon.width / 2f - weapSprite.width / 2f),
                            (int)(-weapon.y * 4f / Draw.scl + icon.height / 2f - weapSprite.height / 2f),
                            weapon.flipSprite, false
                        );

                        icon.draw(outline.get(weapSprite),
                            (int)(weapon.x * 4f / Draw.scl + icon.width / 2f - weapSprite.width / 2f),
                            (int)(-weapon.y * 4f / Draw.scl + icon.height / 2f - weapSprite.height / 2f),
                            weapon.flipSprite, false
                        );
                    }
                }

                icon.draw(region);
                icon.draw(outl);

                Sprite baseCell = SpriteProcessor.get(parseName.get(type.cellRegion));
                Sprite cell = new Sprite(baseCell.width, baseCell.height);
                cell.each((x, y) -> cell.draw(x, y, baseCell.getColor(x, y).mul(Color.valueOf("ffa665"))));

                icon.draw(cell, icon.width / 2 - cell.width / 2, icon.height / 2 - cell.height / 2);

                for(Weapon weapon : type.weapons){
                    weapon.load();
                    String wname = weapon.name.replaceFirst("unity-", "");

                    if(SpriteProcessor.has(wname) && !type.bottomWeapons.contains(weapon.name)){
                        Sprite weapSprite = SpriteProcessor.get(wname);

                        icon.draw(weapSprite,
                            (int)(weapon.x * 4f / Draw.scl + icon.width / 2f - weapSprite.width / 2f),
                            (int)(-weapon.y * 4f / Draw.scl + icon.height / 2f - weapSprite.height / 2f),
                            weapon.flipSprite, false
                        );

                        if(weapon.top){
                            icon.draw(outline.get(weapSprite),
                                (int)(weapon.x * 4f / Draw.scl + icon.width / 2f - weapSprite.width / 2f),
                                (int)(-weapon.y * 4f / Draw.scl + icon.height / 2f - weapSprite.height / 2f),
                                weapon.flipSprite, false
                            );
                        }
                    }
                }

                if(unit instanceof Copterc){
                    Sprite propellers = new Sprite(icon.width, icon.height);
                    Sprite tops = new Sprite(icon.width, icon.height);

                    for(Rotor rotorConfig : type.rotors){
                        String fileName = rotorConfig.name.replace("unity-", "");
                        Sprite bladeSprite = SpriteProcessor.get(fileName + "-blade");

                        float bladeSeparation = 360f / rotorConfig.bladeCount;

                        float propXCenter = (rotorConfig.x * 4f / Draw.scl + icon.width / 2f) - 0.5f;
                        float propYCenter = (-rotorConfig.y * 4f / Draw.scl + icon.height / 2f) - 0.5f;

                        float bladeSpriteXCenter = bladeSprite.width  / 2f - 0.5f;
                        float bladeSpriteYCenter = bladeSprite.height / 2f - 0.5f;

                        propellers.each((x, y) -> {
                            for(int blade = 0; blade < rotorConfig.bladeCount; blade++){
                                // Ideally the rotation would be offset, but as per Mindustry's significance of symmetry
                                // unit composites should have rotors in symmetrical configurations. This is at EoD's request
                                float deg = blade * bladeSeparation/* + rotorConfig.rotOffset*/;
                                float cos = Mathf.cosDeg(deg);
                                float sin = Mathf.sinDeg(deg);

                                Tmp.c1.set(bladeSprite.getColor(
                                    ((propXCenter - x) * cos + (propYCenter - y) * sin) / rotorConfig.scale + bladeSpriteXCenter,
                                    ((propXCenter - x) * sin - (propYCenter - y) * cos) / rotorConfig.scale + bladeSpriteYCenter
                                ));

                                propellers.draw(x, y, Tmp.c1);
                            }
                        });

                        Sprite topSprite = SpriteProcessor.get(fileName + "-top");
                        int topXCenter = (int)(rotorConfig.x * 4f / Draw.scl + icon.width / 2f - topSprite.width / 2f);
                        int topYCenter = (int)(-rotorConfig.y * 4f / Draw.scl + icon.height / 2f - topSprite.height / 2f);
                        tops.draw(topSprite, topXCenter, topYCenter);
                    }

                    Sprite propOutlined = propellers.outline(3, type.outlineColor);
                    propOutlined.draw(propellers, 0, 0);

                    icon.draw(propOutlined);
                    icon.draw(tops.outline(3, type.outlineColor));
                    icon.draw(tops);

                    Sprite payloadCell = new Sprite(baseCell.width, baseCell.height);
                    int cellCenterX = payloadCell.width / 2;
                    int cellCenterY = payloadCell.height / 2;
                    int propCenterX = propOutlined.width / 2;
                    int propCenterY = propOutlined.height / 2;

                    payloadCell.each((x, y) -> {
                        int cellX = x - cellCenterX;
                        int cellY = y - cellCenterY;

                        float alpha = propOutlined.getColor(cellX + propCenterX, cellY + propCenterY).a;
                        payloadCell.draw(x, y, baseCell.getColor(x, y).mul(1, 1, 1, 1 - alpha));
                    });

                    payloadCell.save(fname + "-cell-payload");
                }

                icon.antialias().save(fname + "-full");
            }catch(Throwable e){
                if(e instanceof IllegalArgumentException i){
                    Log.err("Skipping unit @: @", type, i.getMessage());
                }else{
                    Log.err(Strings.format("Problematic unit @", type), e);
                }
            }
        });

        content.blocks().each(block -> {
            if(block.minfo.mod == null || block.outlineIcon) return;

            try{
                block.init();
                block.load();

                ObjectSet<String> antialiased = new ObjectSet<>();
                for(TextureRegion reg : block.getGeneratedIcons()){
                    String fname = parseName.get(reg);
                    if(antialiased.add(fname)){
                        GenRegion region = SpriteProcessor.getRegion(fname);
                        Sprite sprite = SpriteProcessor.get(region);

                        sprite.antialias();
                        region.path.delete();
                        sprite.save(fname);
                    }
                }

                for(TextureRegion reg : block.variantRegions()){
                    String fname = parseName.get(reg);
                    if(antialiased.add(fname)){
                        GenRegion region = SpriteProcessor.getRegion(fname);
                        Sprite sprite = SpriteProcessor.get(region);

                        sprite.antialias();
                        region.path.delete();
                        sprite.save(fname);
                    }
                }
            }catch(Throwable e){
                if(e instanceof IllegalArgumentException i){
                    Log.err("Skipping block @: @", block, i.getMessage());
                }else{
                    Log.err(Strings.format("Problematic block @", block), e);
                }
            }
        });
    }
}
