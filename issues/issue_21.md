> **ESTADO:** ✅ Resuelto en commit `235b451`.

#### Resumen
`PlayerData.getStartedPaths/getMasteredPaths/getCompletedRequirements` devuelven el `ArrayList` interno directamente. Callers hacen `data.getMasteredPaths().remove(pathId)` (mutación externa de estado interno). Encapsulación rota.

#### Problema
**Archivo:** `src/main/java/org/xam/data/PlayerData.java:53,63,73`

```java
public List<String> getStartedPaths() {
    return startedPaths;  // ← ArrayList mutable expuesto
}
public List<String> getMasteredPaths() {
    return masteredPaths;  // ← ArrayList mutable expuesto
}
public List<String> getCompletedRequirements() {
    return completedRequirements;  // ← ArrayList mutable expuesto
}
```

**Callers que mutan externamente:**
- `XamCommand.java:299` — `data.getMasteredPaths().remove(pathId)`
- `MasteryService.java:106` — `data.getCompletedRequirements().removeIf(...)`
- `XamCommand.java:316` — `data.getCompletedRequirements().removeIf(...)`
- `XamCommand.java:74-83` — `data.getMasteredPaths().clear()` (issue #2)

#### Fix propuesto
1. Devolver vistas inmutables:

```java
public List<String> getStartedPaths() {
    return Collections.unmodifiableList(startedPaths);
}
public List<String> getMasteredPaths() {
    return Collections.unmodifiableList(masteredPaths);
}
public List<String> getCompletedRequirements() {
    return Collections.unmodifiableList(completedRequirements);
}
```

2. Exponer mutators explícitos:

```java
public boolean startPath(String pathId) {
    return startedPaths.add(pathId);
}
public boolean masterPath(String pathId) {
    return masteredPaths.add(pathId);
}
public boolean unmasterPath(String pathId) {
    return masteredPaths.remove(pathId);
}
public void completeRequirement(String reqId) {
    completedRequirements.add(reqId);
}
public boolean uncompleteRequirement(String reqId) {
    return completedRequirements.remove(reqId);
}
public void clearStartedPaths() { startedPaths.clear(); }
public void clearMasteredPaths() { masteredPaths.clear(); }
public void clearCompletedRequirements() { completedRequirements.clear(); }
public void clearAll() {
    startedPaths.clear();
    masteredPaths.clear();
    completedRequirements.clear();
    currentPath = null;
    devMode = false;
}
```

3. Refactorizar callers:
```java
// XamCommand.java:299
- data.getMasteredPaths().remove(pathId);
+ data.unmasterPath(pathId);

// XamCommand.java:74-83 (reset, issue #2)
- data.getMasteredPaths().clear();
- data.clearCompletedRequirements();
+ data.clearAll();
```

#### Esfuerzo
2 días.

#### Dependencias
- Issue #2 (fix /xam reset) — se beneficia de `clearAll()`.

---