#### Resumen
Una llamada a `e.printStackTrace()` bypassa el framework de logging del mod (SLF4J via `XamConstants.LOGGER`), escribiendo a stderr sin nivel ni contexto.

#### Problema
**Archivo:** `src/main/java/org/xam/client/event/ClientForgeEvents.java:86-88`

```java
} catch (Exception e) {
    e.printStackTrace();
}
```

Todo el resto del código usa `XamConstants.LOGGER` (SLF4J). Esta única llamada se pierde en logs de producción.

#### Fix propuesto
```java
// ClientForgeEvents.java:86-88
} catch (Exception e) {
-   e.printStackTrace();
+   XamConstants.LOGGER.error("Failed to add widget to {} via reflection", screen, e);
}
```

#### Esfuerzo
1 minuto.

#### Dependencias
Ninguna. (Una vez hecho el issue #9, este catch desaparece por completo, pero igual conviene arreglarlo ahora.)

---