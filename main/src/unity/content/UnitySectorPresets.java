package unity.content;

import mindustry.type.*;
import mindustry.ctype.*;
import unity.annotations.Annotations.*;
import unity.type.sector.*;
import unity.type.sector.SectorObjective.*;

import static mindustry.Vars.*;
import static unity.content.UnityPlanets.*;

public class UnitySectorPresets implements ContentList{
    public static SectorPreset imberlands;
    public static @FactionDef("monolith") SectorPreset accretion;

    @Override
    public void load(){
        /*imberlands = new SectorPreset("imberlands", electrode, 30){{
            alwaysUnlocked = true;
            captureWave = 15;
        }};*/
        /*accretion = new SectorPreset("accretion", megalith, 30){{
            alwaysUnlocked = true;
            captureWave = 15;
        }};*/

        //testing scripted sectors
        accretion = new ScriptedSector("accretion", megalith, 30){{
            alwaysUnlocked = true;
            captureWave = 15;

            objectives.addAll(
                new UnitDeathObjective(UnityUnitTypes.kami, 1, this, 1, (sector, objective) -> {
                    ui.announce("Kami has been defeated!", 2f);
                }
            ));
        }};
    }
}
