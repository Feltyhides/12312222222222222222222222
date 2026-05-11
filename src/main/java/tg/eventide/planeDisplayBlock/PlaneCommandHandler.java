package tg.eventide.planeDisplayBlock;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.util.BlockIterator;

import java.util.*;

/**
 * Обработчик команд плагина
 */
public class PlaneCommandHandler implements CommandExecutor, TabCompleter {
    private final PlaneDisplayBlock plugin;
    private final Map<Player, Set<Block>> playerSelections = new HashMap<>();
    
    // Временное хранение направления для пушек
    private final Map<Player, Float[]> pendingCannonDirections = new HashMap<>();
    
    public PlaneCommandHandler(PlaneDisplayBlock plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cЭту команду можно использовать только из игры!");
            return true;
        }
        
        if (!player.hasPermission("plane.use")) {
            player.sendMessage("§cУ вас нет разрешения на использование этой команды!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create" -> handleCreate(player, args);
            case "addseat" -> handleAddSeat(player);
            case "addcannon" -> handleAddCannon(player, args);
            case "addengine" -> handleAddEngine(player);
            case "setcontrol" -> handleSetControl(player, args);
            case "list" -> handleList(player);
            case "enter" -> handleEnter(player, args);
            case "exit" -> handleExit(player);
            case "sync" -> handleSync(player);
            default -> sendHelp(player);
        }
        
        return true;
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§6=== Управление самолетами ===");
        player.sendMessage("§e/plane create <name>§7 - Создать самолет из выбранных блоков");
        player.sendMessage("§e/plane addseat§7 - Добавить сиденье (смотрите на блок)");
        player.sendMessage("§e/plane addcannon [yaw] [pitch]§7 - Добавить пушку");
        player.sendMessage("§e/plane addengine§7 - Добавить двигатель");
        player.sendMessage("§e/plane setcontrol <pilot|gunner|engineer>§7 - Установить роль");
        player.sendMessage("§e/plane list§7 - Список ваших самолетов");
        player.sendMessage("§e/plane enter <id|name> [seat]§7 - Войти в самолет");
        player.sendMessage("§e/plane exit§7 - Покинуть самолет (или Shift)");
        player.sendMessage("§e/plane sync§7 - Синхронизировать визуализацию");
    }
    
    private void handleCreate(Player player, String[] args) {
        Set<Block> selection = playerSelections.get(player);
        if (selection == null || selection.isEmpty()) {
            player.sendMessage("§cСначала выберите блоки! Используйте ПКМ с палкой.");
            player.sendMessage("§7Подойдите к блоку и нажмите §eПКМ §7с палкой в руке.");
            return;
        }
        
        if (selection.size() < 2) {
            player.sendMessage("§cВыбрано слишком мало блоков! Минимум 2 блока.");
            return;
        }
        
        String name = args.length > 1 ? args[1] : "Самолет-" + System.currentTimeMillis();
        Location refLoc = player.getLocation();
        
        Plane plane = plugin.getPlaneManager().createPlane(name, player.getUniqueId(), refLoc);
        
        for (Block block : selection) {
            plane.addBlock(block);
        }
        
        // Создаем визуализацию
        plane.spawnDisplays();
        
        player.sendMessage("§a✈ Самолет §e" + name + " §aуспешно создан!");
        player.sendMessage("§7Блоков: §e" + selection.size());
        player.sendMessage("");
        player.sendMessage("§6Следующие шаги:");
        player.sendMessage("§e1. /plane addseat §7- Добавить сиденье (смотрите на блок)");
        player.sendMessage("§e2. /plane addcannon §7- Добавить пушку");
        player.sendMessage("§e3. /plane addengine §7- Добавить двигатель");
        player.sendMessage("§e4. /plane setcontrol <role> §7- Установить роль сиденья");
        
        // Очищаем выделение
        playerSelections.remove(player);
    }
    
    private void handleAddSeat(Player player) {
        Plane plane = getTargetPlane(player);
        if (plane == null) {
            player.sendMessage("§cСначала создайте самолет или подойдите к готовому самолету.");
            player.sendMessage("§7Используйте §e/plane list §7для просмотра ваших самолетов.");
            return;
        }
        
        Block targetBlock = getTargetBlock(player);
        if (targetBlock == null) {
            player.sendMessage("§cПосмотрите на блок, куда хотите поставить сиденье.");
            player.sendMessage("§7Расстояние до блока: §eдо 10 блоков");
            return;
        }
        
        // Проверяем, что блок принадлежит самолету
        if (!plane.getBlocks().contains(targetBlock)) {
            player.sendMessage("§cЭтот блок не принадлежит вашему самолету!");
            return;
        }
        
        int seatId = plane.getSeats().size() + 1;
        PlaneSeat seat = new PlaneSeat(seatId, targetBlock);
        plane.addSeat(seat);
        seat.spawnDisplay();
        
        player.sendMessage("§a✺ Добавлено сиденье §e#" + seatId);
        player.sendMessage("§7Роль: §f" + seat.getRole().getDisplayName());
        player.sendMessage("§7Чтобы изменить роль, используйте: §e/plane setcontrol <pilot|gunner|engineer>");
    }
    
    private void handleAddCannon(Player player, String[] args) {
        Plane plane = getTargetPlane(player);
        if (plane == null) {
            player.sendMessage("§cСначала создайте самолет или подойдите к готовому самолету.");
            player.sendMessage("§7Используйте §e/plane list §7для просмотра ваших самолетов.");
            return;
        }
        
        Block targetBlock = getTargetBlock(player);
        if (targetBlock == null) {
            player.sendMessage("§cПосмотрите на блок, куда хотите установить пушку.");
            player.sendMessage("§7Направление пушки будет таким же, как направление вашего взгляда.");
            return;
        }
        
        // Проверяем, что блок принадлежит самолету
        if (!plane.getBlocks().contains(targetBlock)) {
            player.sendMessage("§cЭтот блок не принадлежит вашему самолету!");
            return;
        }
        
        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();
        
        // Если указаны аргументы, используем их
        if (args.length >= 3) {
            try {
                yaw = Float.parseFloat(args[1]);
                pitch = Float.parseFloat(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cНеверный формат углов. Используйте числа.");
                player.sendMessage("§7Пример: §e/plane addcannon 45 -10");
                return;
            }
        }
        
        int cannonId = plane.getCannons().size() + 1;
        PlaneCannon cannon = new PlaneCannon(cannonId, targetBlock, yaw, pitch);
        plane.addCannon(cannon);
        cannon.spawnDisplay();
        
        player.sendMessage("§a✺ Добавлена пушка §e#" + cannonId);
        player.sendMessage("§7Направление: Yaw §f" + String.format("%.1f", yaw) + "§7, Pitch §f" + String.format("%.1f", pitch));
        player.sendMessage("§7Чтобы изменить направление, сломайте и поставьте пушку заново.");
    }
    
    private void handleAddEngine(Player player) {
        Plane plane = getTargetPlane(player);
        if (plane == null) {
            player.sendMessage("§cСначала создайте самолет или подойдите к готовому самолету.");
            player.sendMessage("§7Используйте §e/plane list §7для просмотра ваших самолетов.");
            return;
        }
        
        Block targetBlock = getTargetBlock(player);
        if (targetBlock == null) {
            player.sendMessage("§cПосмотрите на блок, куда хотите установить двигатель.");
            return;
        }
        
        // Проверяем, что блок принадлежит самолету
        if (!plane.getBlocks().contains(targetBlock)) {
            player.sendMessage("§cЭтот блок не принадлежит вашему самолету!");
            return;
        }
        
        int engineId = plane.getEngines().size() + 1;
        PlaneEngine engine = new PlaneEngine(engineId, targetBlock);
        engine.setAssignedPlayer(player); // Назначаем текущего игрока
        plane.addEngine(engine);
        
        player.sendMessage("§a✺ Добавлен двигатель §e#" + engineId);
        player.sendMessage("§7Управление двигателем закреплено за вами.");
        player.sendMessage("§7Когда вы сядете в самолет, вы сможете управлять тягой.");
    }
    
    private void handleSetControl(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cИспользование: /plane setcontrol <pilot|gunner|engineer>");
            return;
        }
        
        Plane plane = getTargetPlane(player);
        if (plane == null) {
            player.sendMessage("§cВы должны находиться рядом с самолетом.");
            return;
        }
        
        // Находим ближайшее сиденье
        PlaneSeat nearestSeat = null;
        double minDist = Double.MAX_VALUE;
        
        for (PlaneSeat seat : plane.getSeats().values()) {
            double dist = seat.getLocation().distance(player.getLocation());
            if (dist < minDist && dist < 5) {
                minDist = dist;
                nearestSeat = seat;
            }
        }
        
        if (nearestSeat == null) {
            player.sendMessage("§cПодойдите ближе к сиденью.");
            return;
        }
        
        String roleStr = args[1].toLowerCase();
        PlaneSeat.ControlRole role;
        
        try {
            role = PlaneSeat.ControlRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cНеверная роль! Доступно: pilot, gunner, engineer");
            return;
        }
        
        nearestSeat.setRole(role);
        player.sendMessage("§aРоль сиденья изменена на §e" + role.getDisplayName());
    }
    
    private void handleList(Player player) {
        List<Plane> planes = plugin.getPlaneManager().getPlayerPlanes(player.getUniqueId());
        
        if (planes.isEmpty()) {
            player.sendMessage("§cУ вас нет самолетов.");
            return;
        }
        
        player.sendMessage("§6=== Ваши самолеты ===");
        for (Plane plane : planes) {
            player.sendMessage("§eID: §f" + plane.getId() + 
                             " §7| §eИмя: §f" + plane.getName() +
                             " §7| §eБлоков: §f" + plane.getBlocks().size() +
                             " §7| §eСидений: §f" + plane.getSeats().size() +
                             " §7| §eПушек: §f" + plane.getCannons().size() +
                             " §7| §eДвигателей: §f" + plane.getEngines().size());
        }
    }
    
    private void handleEnter(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cИспользование: /plane enter <id|name> [seat]");
            return;
        }
        
        Plane plane = plugin.getPlaneManager().getPlaneByName(args[1]);
        if (plane == null) {
            try {
                UUID id = UUID.fromString(args[1]);
                plane = plugin.getPlaneManager().getPlane(id);
            } catch (IllegalArgumentException e) {
                // Не UUID
            }
        }
        
        if (plane == null) {
            player.sendMessage("§cСамолет не найден!");
            return;
        }
        
        int seatId = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        
        plugin.getPlaneManager().enterPlane(player, plane, seatId);
    }
    
    private void handleExit(Player player) {
        plugin.getPlaneManager().exitPlane(player);
    }
    
    private void handleSync(Player player) {
        Plane plane = plugin.getPlaneManager().getPlayerPlane(player);
        if (plane == null) {
            player.sendMessage("§cВы не находитесь в самолете!");
            return;
        }
        
        plane.syncDisplays();
        player.sendMessage("§aВизуализация синхронизирована!");
    }
    
    // Вспомогательные методы
    
    private Plane getTargetPlane(Player player) {
        // Сначала проверяем, в самолете ли игрок
        if (plugin.getPlaneManager().isInPlane(player)) {
            return plugin.getPlaneManager().getPlayerPlane(player);
        }
        
        // Ищем ближайший самолет по блокам
        Block targetBlock = getTargetBlock(player);
        if (targetBlock == null) return null;
        
        for (Plane plane : plugin.getPlaneManager().getPlayerPlanes(player.getUniqueId())) {
            if (plane.getBlocks().contains(targetBlock)) {
                return plane;
            }
        }
        
        return null;
    }
    
    private Block getTargetBlock(Player player) {
        BlockIterator iterator = new BlockIterator(player, 10);
        Block lastBlock = null;
        while (iterator.hasNext()) {
            lastBlock = iterator.next();
            if (lastBlock.getType().isAir()) continue;
        }
        return lastBlock;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "addseat", "addcannon", "addengine", 
                               "setcontrol", "list", "enter", "exit", "sync");
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("setcontrol")) {
            return Arrays.asList("pilot", "gunner", "engineer");
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("enter")) {
            if (sender instanceof Player player) {
                List<String> names = new ArrayList<>();
                for (Plane plane : plugin.getPlaneManager().getPlayerPlanes(player.getUniqueId())) {
                    names.add(plane.getName());
                }
                return names;
            }
        }
        
        return new ArrayList<>();
    }
    
    // Метод для выделения блоков (вызывается из listener)
    public void selectBlock(Player player, Block block) {
        playerSelections.computeIfAbsent(player, k -> new HashSet<>()).add(block);
        player.sendMessage("§aБлок выбран! Всего выбрано: §e" + playerSelections.get(player).size());
    }
    
    public Set<Block> getSelection(Player player) {
        return playerSelections.getOrDefault(player, new HashSet<>());
    }
}
