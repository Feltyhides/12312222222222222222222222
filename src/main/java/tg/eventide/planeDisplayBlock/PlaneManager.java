package tg.eventide.planeDisplayBlock;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер для управления всеми самолетами
 */
public class PlaneManager {
    private final PlaneDisplayBlock plugin;
    private final Map<UUID, Plane> planes = new ConcurrentHashMap<>();
    private final Map<Player, Plane> playerInPlane = new ConcurrentHashMap<>();
    private int nextPlaneId = 1;
    
    public PlaneManager(PlaneDisplayBlock plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Создать новый самолет
     */
    public Plane createPlane(String name, UUID owner, org.bukkit.Location referenceLocation) {
        UUID id = UUID.randomUUID();
        Plane plane = new Plane(id, name, owner, referenceLocation);
        planes.put(id, plane);
        plugin.getLogger().info("Создан самолет " + name + " владельцем " + owner);
        return plane;
    }
    
    /**
     * Получить самолет по ID
     */
    public Plane getPlane(UUID id) {
        return planes.get(id);
    }
    
    /**
     * Получить самолет по имени
     */
    public Plane getPlaneByName(String name) {
        for (Plane plane : planes.values()) {
            if (plane.getName().equalsIgnoreCase(name)) {
                return plane;
            }
        }
        return null;
    }
    
    /**
     * Получить все самолеты игрока
     */
    public List<Plane> getPlayerPlanes(UUID playerUuid) {
        List<Plane> result = new ArrayList<>();
        for (Plane plane : planes.values()) {
            if (plane.getOwner().equals(playerUuid)) {
                result.add(plane);
            }
        }
        return result;
    }
    
    /**
     * Удалить самолет
     */
    public void removePlane(UUID id) {
        Plane plane = planes.remove(id);
        if (plane != null) {
            plane.removeDisplays();
            plane.ejectAll();
            plugin.getLogger().info("Удален самолет " + plane.getName());
        }
    }
    
    /**
     * Посадить игрока в самолет на указанное сиденье
     */
    public boolean enterPlane(Player player, Plane plane, int seatId) {
        if (!plane.hasAccess(player)) {
            player.sendMessage("§cУ вас нет доступа к этому самолету!");
            return false;
        }
        
        if (plane.seatPlayer(player, seatId)) {
            playerInPlane.put(player, plane);
            player.sendMessage("§aВы сели в самолет §e" + plane.getName() + "§a!");
            
            PlaneSeat seat = plane.getSeats().get(seatId);
            if (seat != null) {
                player.sendMessage("§7Роль: §e" + seat.getRole().getDisplayName());
            }
            return true;
        }
        return false;
    }
    
    /**
     * Высадить игрока из самолета
     */
    public void exitPlane(Player player) {
        Plane plane = playerInPlane.remove(player);
        if (plane != null) {
            PlaneSeat seat = plane.getSeatByPassenger(player);
            if (seat != null) {
                seat.setPassenger(null);
            }
            player.sendMessage("§eВы покинули самолет.");
        }
    }
    
    /**
     * Проверить, находится ли игрок в самолете
     */
    public boolean isInPlane(Player player) {
        return playerInPlane.containsKey(player);
    }
    
    /**
     * Получить самолет, в котором находится игрок
     */
    public Plane getPlayerPlane(Player player) {
        return playerInPlane.get(player);
    }
    
    /**
     * Запустить цикл обновления для всех самолетов
     */
    public void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Plane plane : planes.values()) {
                    // Обновляем визуализацию
                    // В полной версии здесь будет физика полета
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Каждую тик (20 раз в секунду)
    }
    
    /**
     * Сохранить все самолеты (для будущей реализации персистентности)
     */
    public void saveAll() {
        // TODO: Реализация сохранения в файл/базу данных
    }
    
    /**
     * Загрузить все самолеты (для будущей реализации персистентности)
     */
    public void loadAll() {
        // TODO: Реализация загрузки из файла/базы данных
    }
}
