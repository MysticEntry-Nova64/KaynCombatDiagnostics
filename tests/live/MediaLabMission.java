package kayn.diagnostics;

import com.fs.starfarer.api.mission.*;
import com.fs.starfarer.api.fleet.*;

/** Test-only standalone mission. Never packaged in the runtime mod. */
public class MediaLabMission implements MissionDefinitionPlugin {
    public void defineMission(MissionDefinitionAPI api) {
        api.initFleet(FleetSide.PLAYER,"LAB",FleetGoal.ATTACK,false);
        api.initFleet(FleetSide.ENEMY,"SIM",FleetGoal.ATTACK,true);
        api.setFleetTagline(FleetSide.PLAYER,"Diagnostics verification: ordinary ballistic, beam and energy ships");
        api.setFleetTagline(FleetSide.ENEMY,"Standard game AI and loadout");
        api.addToFleet(FleetSide.PLAYER,"hammerhead_Balanced",FleetMemberType.SHIP,"LAB Hammerhead",true);
        api.addToFleet(FleetSide.PLAYER,"sunder_Assault",FleetMemberType.SHIP,"LAB Sunder",false);
        api.addToFleet(FleetSide.PLAYER,"wolf_Assault",FleetMemberType.SHIP,"LAB Wolf",false);
        api.addToFleet(FleetSide.ENEMY,"dominator_Assault",FleetMemberType.SHIP,"SIM Dominator",false);
        api.addBriefingItem("Test-only lab. Ctrl+F8 enables diagnostics; normal game controls switch ships.");
        api.addBriefingItem("Separate, explicitly labelled display examples illustrate states not reached naturally.");
        api.initMap(-4500,4500,-4500,4500);
        api.addPlugin(new MediaLab());
    }
}
