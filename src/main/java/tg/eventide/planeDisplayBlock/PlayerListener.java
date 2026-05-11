package tg.eventide.planeDisplayBlock;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Обработчик событий игрока
 */
public class PlayerListener implements Listener {
    private final PlaneDisplayBlock plugin;
    
    public PlayerListener(PlaneDisplayBlock plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Обработка кликов для выделения блоков (палкой)
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        
        // Проверка на использование палки для выделения
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.STICK) {
            return;
        }
        
        if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            
            // Выделяем блок
            plugin.getCommandHandler().selectBlock(player, event.getClickedBlock());
        }
    }
    
    /**
     * Обработка нажатия Shift для выхода из самолета
     */
    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getPlaneManager().isInPlane(player)) {
            return;
        }
        
        // Игрок в самолете и нажал Shift - показываем меню выхода
        Plane plane = plugin.getPlaneManager().getPlayerPlane(player);
        PlaneSeat seat = plane.getSeatByPassenger(player);
        
        if (seat != null) {
            event.setCancelled(true); // Отменяем приседание
            
            // Показываем сообщение с вариантами
            player.sendMessage("§6=== Меню самолета ===");
            player.sendMessage("§eНажмите §c[ЛКМ]§e чтобы покинуть самолет");
            player.sendMessage("");
            player.sendMessage("§7Другие доступные сиденья:");
            
            int seatNum = 1;
            for (PlaneSeat s : plane.getSeats().values()) {
                if (s != seat && s.getPassenger() == null) {
                    player.sendMessage("§e" + seatNum + ". §f" + s.getRole().getDisplayName() + 
                                     " §7(ID: " + s.getId() + ")");
                }
                seatNum++;
            }
            
            // Помечаем игрока как ожидающего выбор
            plugin.getGuiManager().showExitMenu(player, plane);
        }
    }
    
    /**
     * Обработка движения игрока в самолете
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getPlaneManager().isInPlane(player)) {
            return;
        }
        
        // В полной версии здесь будет логика движения самолета
        // в зависимости от роли игрока и состояния двигателей
    }
}
