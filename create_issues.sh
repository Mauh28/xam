#!/usr/bin/env bash
# Auto-generated batch issue creator for XAM repo
# Usage: cd into your local clone of github.com/Mauh28/xam, then run:
#   bash create_issues.sh

set -euo pipefail

# Safety check: must be inside the xam repo
if [ ! -f "build.gradle" ] || [ ! -d "src/main/java/org/xam" ]; then
  echo "ERROR: Run this script from the root of the xam repo (where build.gradle lives)"
  exit 1
fi

# Safety check: gh must be installed and authenticated
if ! command -v gh >/dev/null 2>&1; then
  echo "ERROR: gh CLI not installed. Install: https://cli.github.com/"
  exit 1
fi
if ! gh auth status >/dev/null 2>&1; then
  echo "ERROR: gh not authenticated. Run: gh auth login"
  exit 1
fi

ISSUES_DIR="$(dirname "$0")/issues"
if [ ! -d "$ISSUES_DIR" ]; then
  echo "ERROR: issues/ directory not found next to this script at: $ISSUES_DIR"
  exit 1
fi

echo "Repo: $(gh repo view --json nameWithOwner -q .nameWithOwner)"
echo "Will create ${1:-22} issues. Press Ctrl+C to cancel..."
sleep 2
echo ""

# Issue #1
echo "[1/22] Creating issue #1..."
gh issue create --title 'fix: typo `sr` → `sb` en `drawFlatPanel` (shadow color rojizo)' --body-file "$ISSUES_DIR/issue_01.md" --label 'quick-win,P1,bug,GUI'

# Issue #2
echo "[2/22] Creating issue #2..."
gh issue create --title 'fix: `/xam reset` no limpia `startedPaths`' --body-file "$ISSUES_DIR/issue_02.md" --label 'quick-win,P0,bug,network'

# Issue #3
echo "[3/22] Creating issue #3..."
gh issue create --title 'chore: borrar import muerto `Minecraft` en `MasteryService`' --body-file "$ISSUES_DIR/issue_03.md" --label 'quick-win,P3,architecture'

# Issue #4
echo "[4/22] Creating issue #4..."
gh issue create --title 'fix: reemplazar `printStackTrace` por SLF4J en `ClientForgeEvents`' --body-file "$ISSUES_DIR/issue_04.md" --label 'quick-win,P3,bug'

# Issue #5
echo "[5/22] Creating issue #5..."
gh issue create --title 'refactor: eliminar reflexión sobre `Screen.addRenderableWidget`' --body-file "$ISSUES_DIR/issue_05.md" --label 'quick-win,P1,GUI'

# Issue #6
echo "[6/22] Creating issue #6..."
gh issue create --title 'perf: reemplazar linear scans sobre `PATHS` por `PATHS_MAP.get`' --body-file "$ISSUES_DIR/issue_06.md" --label 'quick-win,P2,performance'

# Issue #7
echo "[7/22] Creating issue #7..."
gh issue create --title 'chore: bump `compatibilityLevel` JAVA_8 → JAVA_17 en `xam.mixins.json`' --body-file "$ISSUES_DIR/issue_07.md" --label 'quick-win,P3,architecture'

# Issue #8
echo "[8/22] Creating issue #8..."
gh issue create --title 'fix: ID generator collision-prone en `MasteryEditorScreen`' --body-file "$ISSUES_DIR/issue_08.md" --label 'quick-win,P2,bug,GUI'

# Issue #9
echo "[9/22] Creating issue #9..."
gh issue create --title 'refactor: extraer `MasteryGuard.enforceItemRestriction` (eliminar duplicación 5×)' --body-file "$ISSUES_DIR/issue_09.md" --label 'medium,P1,architecture,event'

# Issue #10
echo "[10/22] Creating issue #10..."
gh issue create --title 'refactor: centralizar `PathIcons.getDefaultIcon` (4 lugares duplicados)' --body-file "$ISSUES_DIR/issue_10.md" --label 'medium,P2,architecture,GUI'

# Issue #11
echo "[11/22] Creating issue #11..."
gh issue create --title 'perf: cachear `MobEffect` en `PathInfo` en parse time' --body-file "$ISSUES_DIR/issue_11.md" --label 'medium,P2,performance'

# Issue #12
echo "[12/22] Creating issue #12..."
gh issue create --title 'perf: construir `namespaceToPath` map en `ConfigManager`' --body-file "$ISSUES_DIR/issue_12.md" --label 'medium,P1,performance'

# Issue #13
echo "[13/22] Creating issue #13..."
gh issue create --title 'security: rate limiting en `SelectPathPacket` y `RequestConfigPacket`' --body-file "$ISSUES_DIR/issue_13.md" --label 'medium,P1,security,network'

# Issue #14
echo "[14/22] Creating issue #14..."
gh issue create --title 'security: whitelist de teclas en `isInteractionKey` (vs blacklist)' --body-file "$ISSUES_DIR/issue_14.md" --label 'medium,P1,security,bug'

# Issue #15
echo "[15/22] Creating issue #15..."
gh issue create --title 'i18n: mover strings hardcoded en español a lang files' --body-file "$ISSUES_DIR/issue_15.md" --label 'medium,P1,i18n'

# Issue #16
echo "[16/22] Creating issue #16..."
gh issue create --title 'security: validación profunda de `UpdateConfigPacket`' --body-file "$ISSUES_DIR/issue_16.md" --label 'medium,P2,security,network'

# Issue #17
echo "[17/22] Creating issue #17..."
gh issue create --title 'refactor: split `MasteryEditorScreen` (1,518 líneas) en Layout + Model + Validator + ConfirmDeleteScreen' --body-file "$ISSUES_DIR/issue_17.md" --label 'refactor,P1,architecture,GUI'

# Issue #18
echo "[18/22] Creating issue #18..."
gh issue create --title 'refactor: split `MasteryService` (6 responsabilidades) en 3 servicios' --body-file "$ISSUES_DIR/issue_18.md" --label 'refactor,P2,architecture'

# Issue #19
echo "[19/22] Creating issue #19..."
gh issue create --title 'refactor: borrar mixin boilerplate completo (si se elige Opción A del issue #7)' --body-file "$ISSUES_DIR/issue_19.md" --label 'refactor,P3,architecture'

# Issue #20
echo "[20/22] Creating issue #20..."
gh issue create --title 'refactor: convertir `PathInfo` y `Requirement` a Java 17 records' --body-file "$ISSUES_DIR/issue_20.md" --label 'refactor,P2,architecture,config'

# Issue #21
echo "[21/22] Creating issue #21..."
gh issue create --title 'refactor: encapsular `PlayerData` (getters inmutables + mutators explícitos)' --body-file "$ISSUES_DIR/issue_21.md" --label 'refactor,P2,architecture,config'

# Issue #22
echo "[22/22] Creating issue #22..."
gh issue create --title 'refactor: observer pattern para `ConfigManager` (`ConfigReloadedEvent`)' --body-file "$ISSUES_DIR/issue_22.md" --label 'refactor,P3,architecture,config'
