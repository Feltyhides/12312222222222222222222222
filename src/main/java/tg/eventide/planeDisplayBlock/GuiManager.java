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
        
        // Сообщение уже показано в PlayerListener, здесь только устанавливаем статус
        player.sendTitle("§eМеню самолета", "§7Выберите действие", 10, 60, 10);
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
            player.resetTitle();
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
                player.resetTitle();
                player.sendMessage("§e✈ Вы успешно покинули самолет!");
                
                // Эффект выхода
                var loc = player.getLocation();
                var world = loc.getWorld();
                if (world != null) {
                    world.spawnParticle(org.bukkit.Particle.CLOUD, loc, 20, 0.5, 0.5, 0.5, 0.1);
                    world.playSound(loc, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                }
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
                            player.resetTitle();
                            
                            String roleIcon = switch (seat.getRole()) {
                                case PILOT -> "§e🎯";
                                case GUNNER -> "§c🔫";
                                case ENGINEER -> "§9⚙️";
                                default -> "§7•";
                            };
                            
                            player.sendMessage("§a✺ Вы пересели на сиденье §e#" + seat.getId());
                            player.sendMessage(roleIcon + " §7Роль: §f" + seat.getRole().getDisplayName());
                            
                            // Эффект посадки
                            var loc = seat.getLocation();
                            var world = loc.getWorld();
                            if (world != null) {
                                world.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, loc, 10, 0.3, 0.3, 0.3, 0.05);
                                world.playSound(loc, org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
                            }
                        } else {
                            player.sendMessage("§c❌ Это место уже занято!");
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        }
                        return;
                    }
                }
                player.sendMessage("§c❌ Это не сиденье вашего самолета!");
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
