#### Resumen
`MasteryService` importa `net.minecraft.client.Minecraft` pero nunca lo referencia. Es un landmine para quien refactorice los límites sided-proxy.

#### Problema
**Archivo:** `src/main/java/org/xam/progression/MasteryService.java:4`

```java
import net.minecraft.client.Minecraft;          // line 4 — SIN USAR
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;
import org.xam.config.Requirement;
import org.xam.data.PlayerData;
import org.xam.data.PlayerDataProvider;
import org.xam.client.ClientPacketHandler;      // line 21 — usado, pero acoplamiento sided
```

Grep confirma cero llamadas a `Minecraft.` en el archivo.

#### Fix propuesto
Borrar la línea 4:
```java
- import net.minecraft.client.Minecraft;
```

#### Nota adicional
El import de `ClientPacketHandler` en línea 21 es funcional pero rompe el patrón sided-proxy (clase compartida importa paquete de cliente). El fix correcto es mover la llamada detrás de `DistExecutor.unsafeCallWhenOn`, pero eso es un issue separado (#19 - refactor grande).

#### Esfuerzo
1 minuto.

#### Dependencias
Ninguna.

---