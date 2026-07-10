package org.xam.client.gui;

import org.xam.config.PathInfo;
import org.xam.config.Requirement;
import net.minecraft.network.chat.Component;
import java.util.List;

/**
 * Validates PathInfo and Requirement objects before saving config.
 * Returns ValidationResult with either success or a descriptive error message.
 */
public final class MasteryEditorValidator {

    public static final class ValidationResult {
        public final boolean ok;
        public final String errorMessage; // null if ok

        private ValidationResult(boolean ok, String errorMessage) {
            this.ok = ok;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult ok() { return new ValidationResult(true, null); }
        public static ValidationResult error(String msg) { return new ValidationResult(false, msg); }
    }

    public ValidationResult validateAll(List<PathInfo> paths) {
        // ponytail: validate all fields with descriptive errors in footer
        for (int i = 0; i < paths.size(); i++) {
            PathInfo p = paths.get(i);
            ValidationResult r = validate(p, i + 1);
            if (!r.ok) return r;
        }
        return ValidationResult.ok();
    }

    public ValidationResult validate(PathInfo p, int branchNumber) {
        if (p.name.trim().isEmpty()) {
            return ValidationResult.error(Component.translatable("xam.screen.mastery_editor.err_branch_no_name", branchNumber).getString());
        }
        if (p.mod_id.trim().isEmpty() || p.mod_id.equals("modid")) {
            return ValidationResult.error(Component.translatable("xam.screen.mastery_editor.err_need_mod_id", p.name).getString());
        }
        for (int j = 0; j < p.requirements.size(); j++) {
            Requirement req = p.requirements.get(j);
            ValidationResult r = validate(req, j + 1, p.name);
            if (!r.ok) return r;
        }
        return ValidationResult.ok();
    }

    public ValidationResult validate(Requirement req, int reqNumber, String branchName) {
        if (req.id.trim().isEmpty()) {
            return ValidationResult.error(Component.translatable("xam.screen.mastery_editor.err_task_no_id", reqNumber, branchName).getString());
        }
        if (req.name.trim().isEmpty()) {
            return ValidationResult.error(Component.translatable("xam.screen.mastery_editor.err_task_no_name", reqNumber, branchName).getString());
        }
        if (req.description.trim().isEmpty()) {
            return ValidationResult.error(Component.translatable("xam.screen.mastery_editor.err_task_no_desc", reqNumber, branchName).getString());
        }
        return ValidationResult.ok();
    }
}
