package tg.eventide.planeDisplayBlock;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Менеджер GUI для меню выхода и выбора сидений
 */
public class GuiManager implements Listener {
    private final PlaneDisplayBlock plugin;
    
    // Игроки, ожидающие выбора действия
    private final Map<UUID, PendingAction> pendingActions = new HashMap<>();
    
    public enum PendingAction {
        EXIT_MENU,
        SEAT_SELECT
    }
    
    public GuiManager(PlaneDisplayBlock plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Показать меню выхода из самолета
     */
    public void showExitMenu(Player player, Plane plane) {
        pendingActions.put(player.getUniqueId(), PendingAction.EXIT_MENU);
        
        player.sendMessage("§6=== Самолет: " + plane.getName() + " ===");
        player.sendMessage("");
        player.sendMessage("§c✖ §fНажмите §e[ЛКМ] §fс пустой рукой чтобы покинуть самолет");
        player.sendMessage("");
        player.sendMessage("§a● §fНажмите §e[ПКМ] §fна блок сиденья чтобы пересесть");
        player.sendMessage("");
        player.sendMessage("§7Доступные сиденья:");
        
        for (PlaneSeat seat : plane.getSeats().values()) {
            if (seat.getPassenger() == null) {
                player.sendMessage("  §e- Сиденье #" + seat.getId() + 
                                 " (§f" + seat.getRole().getDisplayName() + "§e)");
            }
        }
    }
    
    /**
     * Обработка кликов в меню
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PendingAction action = pendingActions.get(player.getUniqueId());
        
        if (action == null) return;
        
        Plane plane = plugin.getPlaneManager().getPlayerPlane(player);
        if (plane == null) {
            pendingActions.remove(player.getUniqueId());
            return;
        }
        
        if (event.getAction() == Action.LEFT_CLICK_AIR || 
            event.getAction() == Action.LEFT_CLICK_BLOCK) {
            
            ItemStack item = event.getItem();
            // ЛКМ с пустой рукой - выход
            if (item == null || item.getType() == Material.AIR) {
                event.setCancelled(true);
                plugin.getPlaneManager().exitPlane(player);
                pendingActions.remove(player.getUniqueId());
                player.sendMessage("§eВы покинули самолет.");
            }
        }
        
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // ПКМ по блоку - проверка на сиденье
            event.setCancelled(true);
            
            var clickedBlock = event.getClickedBlock();
            if (clickedBlock != null) {
                // Ищем сиденье с этим блоком
                for (PlaneSeat seat : plane.getSeats().values()) {
                    if (seat.getBlock().equals(clickedBlock)) {
                        if (seat.getPassenger() == null) {
                            // Пересаживаемся
                            plane.seatPlayer(player, seat.getId());
                            pendingActions.remove(player.getUniqueId());
                            player.sendMessage("§aВы пересели на сиденье #" + seat.getId());
                            player.sendMessage("§7Роль: §e" + seat.getRole().getDisplayName());
                        } else {
                            player.sendMessage("§cЭто место занято!");
                        }
                        return;
                    }
                }
                player.sendMessage("§cЭто не сиденье самолета!");
            }
        }
    }
    
    /**
     * Очистить ожидающее действие для игрока
     */
    public void clearPendingAction(Player player) {
        pendingActions.remove(player.getUniqueId());
    }
}
