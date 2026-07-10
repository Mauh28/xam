package org.xam.progression;

import net.minecraft.world.entity.player.Player;
import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;
import org.xam.config.Requirement;
import org.xam.data.PlayerData;

public final class DependencyResolver {

    public static boolean isDependencyMet(Player player, PlayerData data, String depStr) {
        if (depStr == null || depStr.isEmpty()) return true;
        DependencySpec spec = DependencySpec.parse(depStr);
        if (spec == null) return true;

        if (!ConfigManager.PATHS_MAP.containsKey(spec.pathId)) {
            return true; // ignore deleted dependencies to prevent deadlocks
        }

        if (data.getMasteredPaths().contains(spec.pathId)) {
            return true;
        }

        if (spec.kind == DependencySpec.Kind.MASTERED) {
            return false;
        }

        PathInfo depPath = ConfigManager.PATHS_MAP.get(spec.pathId);
        if (depPath == null) return true;

        int requiredCount = 0;
        if (spec.kind == DependencySpec.Kind.PERCENT) {
            if (!depPath.requirements.isEmpty()) {
                requiredCount = (int) Math.ceil((spec.value / 100.0) * depPath.requirements.size());
            }
        } else {
            requiredCount = spec.value;
        }

        int completed = MasteryService.getCompletedRequirementsCount(player, data, depPath);
        return completed >= requiredCount;
    }

    public static boolean areDependenciesMastered(Player player, PlayerData data, PathInfo path) {
        if (path.dependencies == null || path.dependencies.isEmpty()) return true;
        for (String dep : path.dependencies) {
            if (!isDependencyMet(player, data, dep)) {
                return false;
            }
        }
        return true;
    }

    public static boolean areRequirementDependenciesMet(Player player, PlayerData data, Requirement req) {
        if (req.dependencies == null || req.dependencies.isEmpty()) return true;
        String currentPath = data.getCurrentPath();
        if (currentPath == null) return false;
        PathInfo path = ConfigManager.PATHS_MAP.get(currentPath);
        if (path == null) return false;

        for (String depId : req.dependencies) {
            Requirement depReq = null;
            for (Requirement r : path.requirements) {
                if (r.id.equals(depId)) {
                    depReq = r;
                    break;
                }
            }
            if (depReq != null) {
                if (!MasteryService.isRequirementCompleted(player, data, path.id, depReq)) {
                    return false;
                }
            }
        }
        return true;
    }
}
