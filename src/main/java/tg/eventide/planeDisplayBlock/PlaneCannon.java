package tg.eventide.planeDisplayBlock;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Представляет пушку самолета с направлением
 */
public class PlaneCannon {
    private final int id;
    private final Block block;
    private final float yaw;
    private final float pitch;
    private BlockDisplay display;
    
    public PlaneCannon(int id, Block block, float yaw, float pitch) {
        this.id = id;
        this.block = block;
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    public int getId() { return id; }
    public Block getBlock() { return block; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    
    public Location getLocation() {
        return block.getLocation().add(0.5, 0.5, 0.5);
    }
    
    /**
     * Создает визуализацию пушки через BlockDisplay
     */
    public void spawnDisplay() {
        if (display != null && !display.isDead()) {
            display.remove();
        }
        
        Location loc = getLocation();
        display = (BlockDisplay) loc.getWorld().spawnEntity(loc, EntityType.BLOCK_DISPLAY);
        display.setBlock(block.getBlockData());
        
        // Поворот дисплея в соответствии с направлением пушки
        Transformation transformation = new Transformation(
            new Vector3f(0, 0, 0),
            new AxisAngle4f((float) Math.toRadians(pitch), 0, 1, 0),
            new Vector3f(1, 1, 1),
            new AxisAngle4f((float) Math.toRadians(yaw), 0, 1, 0)
        );
        display.setTransformation(transformation);
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
