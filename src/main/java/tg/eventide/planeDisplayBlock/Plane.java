package tg.eventide.planeDisplayBlock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * Представляет самолет со всеми его компонентами
 */
public class Plane {
    private final UUID id;
    private final String name;
    private final UUID owner;
    private final Set<Block> blocks;
    private final Map<Integer, PlaneSeat> seats;
    private final Map<Integer, PlaneCannon> cannons;
    private final Map<Integer, PlaneEngine> engines;
    private Location referenceLocation;
    
    // Визуальные дисплеи для блоков
    private final List<BlockDisplay> blockDisplays = new ArrayList<>();
    
    public Plane(UUID id, String name, UUID owner, Location referenceLocation) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.referenceLocation = referenceLocation;
        this.blocks = new HashSet<>();
        this.seats = new LinkedHashMap<>();
        this.cannons = new LinkedHashMap<>();
        this.engines = new LinkedHashMap<>();
    }
    
    public UUID getId() { return id; }
    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public Location getReferenceLocation() { return referenceLocation; }
    public void setReferenceLocation(Location location) { this.referenceLocation = location; }
    
    public Set<Block> getBlocks() { return Collections.unmodifiableSet(blocks); }
    public Map<Integer, PlaneSeat> getSeats() { return Collections.unmodifiableMap(seats); }
    public Map<Integer, PlaneCannon> getCannons() { return Collections.unmodifiableMap(cannons); }
    public Map<Integer, PlaneEngine> getEngines() { return Collections.unmodifiableMap(engines); }
    
    public void addBlock(Block block) {
        blocks.add(block);
    }
    
    public void removeBlock(Block block) {
        blocks.remove(block);
    }
    
    public void addSeat(PlaneSeat seat) {
        seats.put(seat.getId(), seat);
        addBlock(seat.getBlock());
    }
    
    public void addCannon(PlaneCannon cannon) {
        cannons.put(cannon.getId(), cannon);
        addBlock(cannon.getBlock());
    }
    
    public void addEngine(PlaneEngine engine) {
        engines.put(engine.getId(), engine);
        addBlock(engine.getBlock());
    }
    
    /**
     * Получить общее количество тяги от всех двигателей
     */
    public float getTotalThrust() {
        float total = 0;
        for (PlaneEngine engine : engines.values()) {
            total += engine.getPower();
        }
        return total / Math.max(1, engines.size());
    }
    
    /**
     * Получить направление самолета (yaw)
     */
    public float getYaw() {
        return referenceLocation.getYaw();
    }
    
    /**
     * Получить наклон самолета (pitch)
     */
    public float getPitch() {
        return referenceLocation.getPitch();
    }
    
    /**
     * Создать визуализацию всего самолета через BlockDisplay
     */
    public void spawnDisplays() {
        removeDisplays();
        
        if (referenceLocation.getWorld() == null) return;
        
        // Создаем дисплеи для каждого блока
        for (Block block : blocks) {
            if (block.getType() != Material.AIR) {
                Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                BlockDisplay display = (BlockDisplay) loc.getWorld().spawnEntity(loc, EntityType.BLOCK_DISPLAY);
                display.setBlock(block.getBlockData());
                display.setTeleportDuration(0);
                display.setInterpolationDuration(1);
                // Маленький размер для лучшей производительности
                display.setTransformation(new Transformation(new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(0.9f, 0.9f, 0.9f), new Quaternionf()));
                blockDisplays.add(display);
            }
        }
    }
    
    /**
     * Синхронизировать визуализацию с текущим состоянием блоков
     */
    public void syncDisplays() {
        removeDisplays();
        spawnDisplays();
    }
    
    /**
     * Удалить все дисплеи
     */
    public void removeDisplays() {
        for (BlockDisplay display : blockDisplays) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
        blockDisplays.clear();
    }
    
    /**
     * Обновить позицию всех дисплеев при движении
     */
    public void updateDisplays(Vector movement) {
        // В полной версии здесь будет логика перемещения дисплеев
        // Для простоты пока просто синхронизируем
        syncDisplays();
    }
    
    /**
     * Проверка, является ли игрок владельцем или имеет доступ
     */
    public boolean hasAccess(Player player) {
        return owner.equals(player.getUniqueId()) || 
               player.hasPermission("plane.admin");
    }
    
    /**
     * Найти сиденье, в котором находится игрок
     */
    public PlaneSeat getSeatByPassenger(Player player) {
        for (PlaneSeat seat : seats.values()) {
            if (player.equals(seat.getPassenger())) {
                return seat;
            }
        }
        return null;
    }
    
    /**
     * Посадить игрока на указанное сиденье
     */
    public boolean seatPlayer(Player player, int seatId) {
        PlaneSeat seat = seats.get(seatId);
        if (seat == null) return false;
        
        // Освободить предыдущее сиденье игрока
        PlaneSeat oldSeat = getSeatByPassenger(player);
        if (oldSeat != null) {
            oldSeat.setPassenger(null);
        }
        
        // Если место занято, освободить его
        if (seat.getPassenger() != null) {
            seat.getPassenger().sendMessage("§cВас выгнали из сиденья!");
            seat.setPassenger(null);
        }
        
        seat.setPassenger(player);
        player.teleport(seat.getLocation());
        return true;
    }
    
    /**
     * Высадить всех пассажиров
     */
    public void ejectAll() {
        for (PlaneSeat seat : seats.values()) {
            if (seat.getPassenger() != null) {
                Player p = seat.getPassenger();
                p.sendMessage("§eСамолет остановлен. Вы высажены.");
                seat.setPassenger(null);
            }
        }
    }
}
