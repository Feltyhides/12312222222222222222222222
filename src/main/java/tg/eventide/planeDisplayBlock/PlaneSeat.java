package tg.eventide.planeDisplayBlock;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Представляет сиденье самолета с назначенной ролью
 */
public class PlaneSeat {
    private final int id;
    private final Block block;
    private ControlRole role;
    private Player passenger;
    private BlockDisplay display;
    
    public enum ControlRole {
        PILOT("Пилот", true, false, false),
        GUNNER("Стрелок", false, true, false),
        ENGINEER("Инженер", false, false, true);
        
        private final String displayName;
        private final boolean canFly;
        private final boolean canShoot;
        private final boolean canControlEngine;
        
        ControlRole(String displayName, boolean canFly, boolean canShoot, boolean canControlEngine) {
            this.displayName = displayName;
            this.canFly = canFly;
            this.canShoot = canShoot;
            this.canControlEngine = canControlEngine;
        }
        
        public String getDisplayName() { return displayName; }
        public boolean isCanFly() { return canFly; }
        public boolean isCanShoot() { return canShoot; }
        public boolean isCanControlEngine() { return canControlEngine; }
    }
    
    public PlaneSeat(int id, Block block) {
        this.id = id;
        this.block = block;
        this.role = ControlRole.PILOT;
    }
    
    public int getId() { return id; }
    public Block getBlock() { return block; }
    public ControlRole getRole() { return role; }
    public void setRole(ControlRole role) { this.role = role; }
    public Player getPassenger() { return passenger; }
    public void setPassenger(Player passenger) { this.passenger = passenger; }
    
    public Location getLocation() {
        return block.getLocation().add(0.5, 1, 0.5);
    }
    
    public void spawnDisplay() {
        if (display != null && !display.isDead()) {
            display.remove();
        }
        Location loc = getLocation();
        display = (BlockDisplay) loc.getWorld().spawnEntity(loc, EntityType.BLOCK_DISPLAY);
        display.setBlock(block.getBlockData());
        display.setTeleportDuration(0);
        display.setInterpolationDuration(1);
    }
    
    public void updateDisplay() {
        if (display != null && !display.isDead()) {
            display.setBlock(block.getBlockData());
            display.teleport(getLocation());
        }
    }
    
    public void removeDisplay() {
        if (display != null && !display.isDead()) {
            display.remove();
            display = null;
        }
    }
}
