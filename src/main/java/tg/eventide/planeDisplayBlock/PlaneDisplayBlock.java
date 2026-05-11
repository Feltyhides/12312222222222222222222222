package tg.eventide.planeDisplayBlock;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;

public final class PlaneDisplayBlock extends JavaPlugin {

    private PlaneManager planeManager;
    private PlaneCommandHandler commandHandler;
    private PlayerListener playerListener;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        // Инициализация менеджеров
        planeManager = new PlaneManager(this);
        commandHandler = new PlaneCommandHandler(this);
        guiManager = new GuiManager(this);
        playerListener = new PlayerListener(this);
        
        // Регистрация команд
        var command = getCommand("plane");
        if (command != null) {
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
        }
        
        // Регистрация слушателей событий
        Bukkit.getPluginManager().registerEvents(playerListener, this);
        Bukkit.getPluginManager().registerEvents(guiManager, this);
        
        // Запуск цикла обновления самолетов
        planeManager.startUpdateTask();
        
        getLogger().info("PlaneDisplayBlock включен!");
        getLogger().info("Используйте /plane help для списка команд");
    }

    @Override
    public void onDisable() {
        // Очистка всех дисплеев перед выключением
        if (planeManager != null) {
            for (var plane : planeManager.getPlayerPlanes(java.util.UUID.randomUUID())) {
                plane.removeDisplays();
            }
        }
        
        getLogger().info("PlaneDisplayBlock выключен!");
    }
    
    public PlaneManager getPlaneManager() {
        return planeManager;
    }
    
    public PlaneCommandHandler getCommandHandler() {
        return commandHandler;
    }
    
    public GuiManager getGuiManager() {
        return guiManager;
    }
}
