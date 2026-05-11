package tg.eventide.planeDisplayBlock;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Представляет двигатель самолета
 */
public class PlaneEngine {
    private final int id;
    private final Block block;
    private Player assignedPlayer;
    private float power; // Мощность от 0 до 1
    
    public PlaneEngine(int id, Block block) {
        this.id = id;
        this.block = block;
        this.power = 0.0f;
    }
    
    public int getId() { return id; }
    public Block getBlock() { return block; }
    public Player getAssignedPlayer() { return assignedPlayer; }
    public void setAssignedPlayer(Player player) { this.assignedPlayer = player; }
    public float getPower() { return power; }
    public void setPower(float power) { 
        this.power = Math.max(0.0f, Math.min(1.0f, power));
    }
    
    public Location getLocation() {
        return block.getLocation().add(0.5, 0.5, 0.5);
    }
    
    /**
     * Проверка, может ли игрок управлять этим двигателем
     */
    public boolean canControl(Player player) {
        return assignedPlayer == null || assignedPlayer.equals(player);
    }
}
