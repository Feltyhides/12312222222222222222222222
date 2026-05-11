package tg.eventide.planeDisplayBlock;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;

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
            
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null) {
                // Выделяем блок
                plugin.getCommandHandler().selectBlock(player, clickedBlock);
                
                // Красивый эффект выделения
                spawnHighlightEffect(clickedBlock);
            }
        }
    }
    
    /**
     * Создает красивый эффект выделения блока
     */
    private void spawnHighlightEffect(Block block) {
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        var world = loc.getWorld();
        if (world == null) return;
        
        // Кольцо частиц вокруг блока
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI * i) / 20;
            double x = Math.cos(angle) * 0.6;
            double z = Math.sin(angle) * 0.6;
            
            world.spawnParticle(
                Particle.END_ROD,
                loc.clone().add(x, 0, z),
                1,
                0, 0, 0,
                0
            );
        }
        
        // Вертикальные линии частиц
        for (int y = 0; y <= 1; y++) {
            world.spawnParticle(
                Particle.HAPPY_VILLAGER,
                loc.clone().add(0, y, 0),
                5,
                0.3, 0.3, 0.3,
                0.01
            );
        }
        
        // Звук
        world.playSound(loc, org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
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
            player.sendMessage("");
            player.sendMessage("§6══════════════════════════════");
            player.sendMessage("§e✈ §fСамолет: §b" + plane.getName());
            player.sendMessage("§6══════════════════════════════");
            player.sendMessage("");
            player.sendMessage("§c🚪 §fНажмите §e[ЛКМ] §fс §7пустой рукой §fчтобы покинуть самолет");
            player.sendMessage("");
            player.sendMessage("§a🪑 §fНажмите §e[ПКМ] §fна блок сиденья чтобы пересесть");
            player.sendMessage("");
            player.sendMessage("§7Доступные сиденья:");
            
            int availableCount = 0;
            for (PlaneSeat s : plane.getSeats().values()) {
                if (s != seat && s.getPassenger() == null) {
                    String roleIcon = switch (s.getRole()) {
                        case PILOT -> "§e🎯";
                        case GUNNER -> "§c🔫";
                        case ENGINEER -> "§9⚙️";
                        default -> "§7•";
                    };
                    player.sendMessage("  " + roleIcon + " §fСиденье §e#" + s.getId() + 
                                     " §7(" + s.getRole().getDisplayName() + "§7)");
                    availableCount++;
                }
            }
            
            if (availableCount == 0) {
                player.sendMessage("  §7Нет свободных мест");
            }
            
            player.sendMessage("");
            player.sendMessage("§6══════════════════════════════");
            
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
