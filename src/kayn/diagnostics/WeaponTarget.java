package kayn.diagnostics;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

/** Provenance of one observed weapon target; never substitutes a nearby enemy. */
record WeaponTarget(WeaponGroupAPI group, AutofireAIPlugin ai, CombatEntityAPI entity,
                    Vector2f point, String source, boolean individualAiTarget,
                    boolean beamContact, boolean wantsFire) {
    static WeaponTarget observe(WeaponAPI weapon, ShipAPI ship, CombatEngineAPI engine) {
        WeaponGroupAPI group = ship.getWeaponGroupFor(weapon);
        boolean activeAutofire = group != null && group.isAutofiring() && group != ship.getSelectedGroupAPI();
        AutofireAIPlugin ai = activeAutofire ? group.getAutofirePlugin(weapon) : null;
        CombatEntityAPI entity = ai == null ? null : ai.getTargetShip();
        if (entity == null && ai != null) entity = ai.getTargetMissile();
        Vector2f point = ai == null ? null : ai.getTarget();
        String source = point == null ? (ai == null ? "UNOBSERVED" : "NONE") : "AUTOFIRE_AI";
        if (point == null && entity != null) {
            point = entity.getLocation();
            source = "AI_ENTITY_CENTRE";
        }
        // Preserve the distinction between individual autofire readiness and other target evidence.
        boolean individualAiTarget = point != null;
        if (point == null && group != null && group == ship.getSelectedGroupAPI() &&
                (ship.getShipAI() != null || ship == engine.getPlayerShip() && engine.isUIAutopilotOn()) &&
                ship.getShipTarget() != null) {
            entity = ship.getShipTarget();
            point = ship.getMouseTarget();
            if (point == null) point = entity.getLocation();
            source = "SHIP_AI_SELECTED_TARGET";
        }
        boolean contact = false;
        if (weapon.isBeam()) {
            for (BeamAPI beam : engine.getBeams()) {
                if (beam.getWeapon() == weapon && beam.didDamageThisFrame() && beam.getDamageTarget() != null) {
                    entity = beam.getDamageTarget();
                    point = beam.getTo();
                    source = "BEAM_CONTACT";
                    contact = true;
                    break;
                }
            }
        }
        return new WeaponTarget(group, ai, entity, point, source, individualAiTarget,
            contact, ai != null && ai.shouldFire());
    }

    boolean observed() { return point != null; }

    String label() {
        if (entity instanceof MissileAPI) return "MISSILE";
        if (entity instanceof DamagingProjectileAPI) return "PROJECTILE";
        if (entity instanceof ShipAPI) return "SHIP";
        if (observed()) return "POINT";
        return ai == null ? "UNKNOWN" : "NONE";
    }

    String id() {
        if (entity == null) return "NA";
        if (entity instanceof ShipAPI ship) return ship.getId();
        if (entity instanceof MissileAPI) return "MISSILE";
        if (entity instanceof DamagingProjectileAPI) return "PROJECTILE";
        return "ENTITY";
    }
}
