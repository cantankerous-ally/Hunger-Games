package me.cantankerousally.hungergames.commands;

import org.bukkit.*;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ChestRefillCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private FileConfiguration arenaConfig = null;
    private File arenaFile = null;
    Map<String, Color> colorMap = new HashMap<>();

    public ChestRefillCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        colorMap.put("AQUA", Color.AQUA);
        colorMap.put("BLACK", Color.BLACK);
        colorMap.put("BLUE", Color.BLUE);
        colorMap.put("FUCHSIA", Color.FUCHSIA);
        colorMap.put("GRAY", Color.GRAY);
        colorMap.put("GREEN", Color.GREEN);
        colorMap.put("LIME", Color.LIME);
        colorMap.put("MAROON", Color.MAROON);
        colorMap.put("NAVY", Color.NAVY);
        colorMap.put("OLIVE", Color.OLIVE);
        colorMap.put("ORANGE", Color.ORANGE);
        colorMap.put("PURPLE", Color.PURPLE);
        colorMap.put("RED", Color.RED);
        colorMap.put("SILVER", Color.SILVER);
        colorMap.put("TEAL", Color.TEAL);
        colorMap.put("WHITE", Color.WHITE);
        colorMap.put("YELLOW", Color.YELLOW);
    }

    public void createArenaConfig() {
        arenaFile = new File(plugin.getDataFolder(), "arena.yml");
        if (!arenaFile.exists()) {
            arenaFile.getParentFile().mkdirs();
            plugin.saveResource("arena.yml", false);
        }

        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
    }

    public FileConfiguration getArenaConfig() {
        if (arenaConfig == null) {
            createArenaConfig();
        }
        return arenaConfig;
    }

    public void saveArenaConfig() {
        try {
            getArenaConfig().save(arenaFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save config to " + arenaFile);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("chestrefill")) {
            FileConfiguration config = getArenaConfig();
            World world = plugin.getServer().getWorld(Objects.requireNonNull(config.getString("region.world")));
            double pos1x = config.getDouble("region.pos1.x");
            double pos1y = config.getDouble("region.pos1.y");
            double pos1z = config.getDouble("region.pos1.z");
            double pos2x = config.getDouble("region.pos2.x");
            double pos2y = config.getDouble("region.pos2.y");
            double pos2z = config.getDouble("region.pos2.z");

            int minX = (int) Math.min(pos1x, pos2x);
            int minY = (int) Math.min(pos1y, pos2y);
            int minZ = (int) Math.min(pos1z, pos2z);
            int maxX = (int) Math.max(pos1x, pos2x);
            int maxY = (int) Math.max(pos1y, pos2y);
            int maxZ = (int) Math.max(pos1z, pos2z);

            FileConfiguration itemsConfig;
            File itemsFile = new File(plugin.getDataFolder(), "items.yml");
            if (itemsFile.exists()) {
                itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
            } else {
                try {
                    itemsConfig = new YamlConfiguration();
                    itemsConfig.set("chest-items", new ArrayList<>());
                    itemsConfig.save(itemsFile);
                    sender.sendMessage(ChatColor.YELLOW + "Created new items.yml file!");
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Could not create items.yml file!");
                    return true;
                }
            }

            List<ItemStack> chestItems = new ArrayList<>();
            List<Integer> chestItemWeights = new ArrayList<>();
            for (Map<?, ?> itemMap : itemsConfig.getMapList("chest-items")) {
                String type = (String) itemMap.get("type");
                int weight = (int) itemMap.get("weight");
                int amount = itemMap.containsKey("amount") ? (int) itemMap.get("amount") : 1;
                ItemStack item = new ItemStack(Material.valueOf(type), amount);
                if (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) {
                    PotionMeta meta = (PotionMeta) item.getItemMeta();
                    String potionType = (String) itemMap.get("potion-type");
                    int level = (int) itemMap.get("level");
                    boolean extended = itemMap.containsKey("extended") && (boolean) itemMap.get("extended");
                    assert meta != null;
                    meta.setBasePotionData(new PotionData(PotionType.valueOf(potionType), extended, level > 1));
                    item.setItemMeta(meta);
                } else if (item.getType() == Material.FIREWORK_ROCKET) {
                    FireworkMeta meta = (FireworkMeta) item.getItemMeta();
                    int power = (int) itemMap.get("power");
                    assert meta != null;
                    meta.setPower(power);
                    Object effectsObj = itemMap.get("effects");
                    if (effectsObj instanceof List<?> effectsList) {
                        for (Object effectObj : effectsList) {
                            if (effectObj instanceof Map<?, ?> effectMap) {
                                String effectType = (String) effectMap.get("type");
                                Object colorsObj = effectMap.get("colors");
                                if (colorsObj instanceof List<?> colorsList) {
                                    List<Color> colors = colorsList.stream()
                                            .filter(String.class::isInstance)
                                            .map(String.class::cast)
                                            .map(colorName -> colorMap.getOrDefault(colorName.toUpperCase(), Color.RED))
                                            .collect(Collectors.toList());
                                    Object fadeColorsObj = effectMap.get("fade-colors");
                                    if (fadeColorsObj instanceof List<?> fadeColorsList) {
                                        List<Color> fadeColors = fadeColorsList.stream()
                                                .filter(String.class::isInstance)
                                                .map(String.class::cast)
                                                .map(colorName -> colorMap.getOrDefault(colorName.toUpperCase(), Color.RED))
                                                .collect(Collectors.toList());
                                        boolean flicker = (boolean) effectMap.get("flicker");
                                        boolean trail = (boolean) effectMap.get("trail");
                                        FireworkEffect effect = FireworkEffect.builder()
                                                .with(FireworkEffect.Type.valueOf(effectType))
                                                .withColor(colors)
                                                .withFade(fadeColors)
                                                .flicker(flicker)
                                                .trail(trail)
                                                .build();
                                        meta.addEffect(effect);
                                    }
                                }
                            }
                        }
                        item.setItemMeta(meta);
                    }
                } else if (itemMap.containsKey("enchantments")) {
                    Object enchantmentsObj = itemMap.get("enchantments");
                    if (enchantmentsObj instanceof List<?> enchantmentsList) {
                        for (Object enchantmentObj : enchantmentsList) {
                            if (enchantmentObj instanceof Map<?, ?> enchantmentMap) {
                                String enchantmentType = (String) enchantmentMap.get("type");
                                int level = (int) enchantmentMap.get("level");
                                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentType.toLowerCase()));
                                if (enchantment != null) {
                                    if (item.getType() == Material.ENCHANTED_BOOK) {
                                        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                                        assert meta != null;
                                        meta.addStoredEnchant(enchantment, level, true);
                                        item.setItemMeta(meta);
                                    } else {
                                        item.addEnchantment(enchantment, level);
                                    }
                                }
                            }
                        }
                    }
                }
                chestItems.add(item);
                chestItemWeights.add(weight);

            }

            List<ItemStack> barrelItems = new ArrayList<>();
            List<Integer> barrelItemWeights = new ArrayList<>();
            for (Map<?, ?> itemMap : itemsConfig.getMapList("barrel-items")) {
                String type = (String) itemMap.get("type");
                int weight = (int) itemMap.get("weight");
                int amount = itemMap.containsKey("amount") ? (int) itemMap.get("amount") : 1;
                ItemStack item = new ItemStack(Material.valueOf(type), amount);
                if (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) {
                    PotionMeta meta = (PotionMeta) item.getItemMeta();
                    String potionType = (String) itemMap.get("potion-type");
                    int level = (int) itemMap.get("level");
                    boolean extended = itemMap.containsKey("extended") && (boolean) itemMap.get("extended");
                    assert meta != null;
                    meta.setBasePotionData(new PotionData(PotionType.valueOf(potionType), extended, level > 1));
                    item.setItemMeta(meta);
                } else if (item.getType() == Material.FIREWORK_ROCKET) {
                    FireworkMeta meta = (FireworkMeta) item.getItemMeta();
                    int power = (int) itemMap.get("power");
                    assert meta != null;
                    meta.setPower(power);
                    Object effectsObj = itemMap.get("effects");
                    if (effectsObj instanceof List<?> effectsList) {
                        for (Object effectObj : effectsList) {
                            if (effectObj instanceof Map<?, ?> effectMap) {
                                String effectType = (String) effectMap.get("type");
                                Object colorsObj = effectMap.get("colors");
                                if (colorsObj instanceof List<?> colorsList) {
                                    List<Color> colors = colorsList.stream()
                                            .filter(String.class::isInstance)
                                            .map(String.class::cast)
                                            .map(colorName -> colorMap.getOrDefault(colorName.toUpperCase(), Color.RED))
                                            .collect(Collectors.toList());
                                    Object fadeColorsObj = effectMap.get("fade-colors");
                                    if (fadeColorsObj instanceof List<?> fadeColorsList) {
                                        List<Color> fadeColors = fadeColorsList.stream()
                                                .filter(String.class::isInstance)
                                                .map(String.class::cast)
                                                .map(colorName -> colorMap.getOrDefault(colorName.toUpperCase(), Color.RED))
                                                .collect(Collectors.toList());
                                        boolean flicker = (boolean) effectMap.get("flicker");
                                        boolean trail = (boolean) effectMap.get("trail");
                                        FireworkEffect effect = FireworkEffect.builder()
                                                .with(FireworkEffect.Type.valueOf(effectType))
                                                .withColor(colors)
                                                .withFade(fadeColors)
                                                .flicker(flicker)
                                                .trail(trail)
                                                .build();
                                        meta.addEffect(effect);
                                    }
                                }
                            }
                        }
                        item.setItemMeta(meta);
                    }
                } else if (itemMap.containsKey("enchantments")) {
                    Object enchantmentsObj = itemMap.get("enchantments");
                    if (enchantmentsObj instanceof List<?> enchantmentsList) {
                        for (Object enchantmentObj : enchantmentsList) {
                            if (enchantmentObj instanceof Map<?, ?> enchantmentMap) {
                                String enchantmentType = (String) enchantmentMap.get("type");
                                int level = (int) enchantmentMap.get("level");
                                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentType.toLowerCase()));
                                if (enchantment != null) {
                                    if (item.getType() == Material.ENCHANTED_BOOK) {
                                        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                                        assert meta != null;
                                        meta.addStoredEnchant(enchantment, level, true);
                                        item.setItemMeta(meta);
                                    } else {
                                        item.addEnchantment(enchantment, level);
                                    }
                                }
                            }
                        }
                    }
                }
                barrelItems.add(item);
                barrelItemWeights.add(weight);
            }

            List<ItemStack> trappedChestItems = new ArrayList<>();
            List<Integer> trappedChestItemWeights = new ArrayList<>();
            for (Map<?, ?> itemMap : itemsConfig.getMapList("trapped-chest-items")) {
                String type = (String) itemMap.get("type");
                int weight = (int) itemMap.get("weight");
                int amount = itemMap.containsKey("amount") ? (int) itemMap.get("amount") : 1;
                ItemStack item = new ItemStack(Material.valueOf(type), amount);
                if (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) {
                    PotionMeta meta = (PotionMeta) item.getItemMeta();
                    String potionType = (String) itemMap.get("potion-type");
                    int level = (int) itemMap.get("level");
                    boolean extended = itemMap.containsKey("extended") && (boolean) itemMap.get("extended");
                    assert meta != null;
                    meta.setBasePotionData(new PotionData(PotionType.valueOf(potionType), extended, level > 1));
                    item.setItemMeta(meta);
                } else if (item.getType() == Material.FIREWORK_ROCKET) {
                    FireworkMeta meta = (FireworkMeta) item.getItemMeta();
                    int power = (int) itemMap.get("power");
                    assert meta != null;
                    meta.setPower(power);
                    Object effectsObj = itemMap.get("effects");
                    if (effectsObj instanceof List<?> effectsList) {
                        for (Object effectObj : effectsList) {
                            if (effectObj instanceof Map<?, ?> effectMap) {
                                String effectType = (String) effectMap.get("type");
                                Object colorsObj = effectMap.get("colors");
                                if (colorsObj instanceof List<?> colorsList) {
                                    List<Color> colors = colorsList.stream()
                                            .filter(String.class::isInstance)
                                            .map(String.class::cast)
                                            .map(colorName -> colorMap.getOrDefault(colorName.toUpperCase(), Color.RED))
                                            .collect(Collectors.toList());
                                    Object fadeColorsObj = effectMap.get("fade-colors");
                                    if (fadeColorsObj instanceof List<?> fadeColorsList) {
                                        List<Color> fadeColors = fadeColorsList.stream()
                                                .filter(String.class::isInstance)
                                                .map(String.class::cast)
                                                .map(colorName -> colorMap.getOrDefault(colorName.toUpperCase(), Color.RED))
                                                .collect(Collectors.toList());
                                        boolean flicker = (boolean) effectMap.get("flicker");
                                        boolean trail = (boolean) effectMap.get("trail");
                                        FireworkEffect effect = FireworkEffect.builder()
                                                .with(FireworkEffect.Type.valueOf(effectType))
                                                .withColor(colors)
                                                .withFade(fadeColors)
                                                .flicker(flicker)
                                                .trail(trail)
                                                .build();
                                        meta.addEffect(effect);
                                    }
                                }
                            }
                        }
                        item.setItemMeta(meta);
                    }
                } else if (itemMap.containsKey("enchantments")) {
                    Object enchantmentsObj = itemMap.get("enchantments");
                    if (enchantmentsObj instanceof List<?> enchantmentsList) {
                        for (Object enchantmentObj : enchantmentsList) {
                            if (enchantmentObj instanceof Map<?, ?> enchantmentMap) {
                                String enchantmentType = (String) enchantmentMap.get("type");
                                int level = (int) enchantmentMap.get("level");
                                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentType.toLowerCase()));
                                if (enchantment != null) {
                                    if (item.getType() == Material.ENCHANTED_BOOK) {
                                        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                                        assert meta != null;
                                        meta.addStoredEnchant(enchantment, level, true);
                                        item.setItemMeta(meta);
                                    } else {
                                        item.addEnchantment(enchantment, level);
                                    }
                                }
                            }
                        }
                    }
                }
                trappedChestItems.add(item);
                trappedChestItemWeights.add(weight);
            }

            File chestLocationsFile = new File(plugin.getDataFolder(), "chest-locations.yml");
            if (!chestLocationsFile.exists()) {
                List<Location> chestLocations = new ArrayList<>();
                List<Location> barrelLocations = new ArrayList<>();
                List<Location> trappedChestLocations = new ArrayList<>();
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            assert world != null;
                            Block block = world.getBlockAt(x, y, z);
                            if (block.getType() == Material.CHEST) {
                                chestLocations.add(block.getLocation());
                            } else if (block.getType() == Material.BARREL) {
                                barrelLocations.add(block.getLocation());
                            } else if (block.getType() == Material.TRAPPED_CHEST) {
                                trappedChestLocations.add(block.getLocation());
                            }
                        }
                    }
                }

                FileConfiguration chestLocationsConfig = new YamlConfiguration();
                chestLocationsConfig.set("locations", chestLocations.stream()
                        .map(Location::serialize)
                        .collect(Collectors.toList()));
                chestLocationsConfig.set("bonus-locations", barrelLocations.stream()
                        .map(Location::serialize)
                        .collect(Collectors.toList()));
                chestLocationsConfig.set("mid-locations", trappedChestLocations.stream()
                        .map(Location::serialize)
                        .collect(Collectors.toList()));
                try {
                    chestLocationsConfig.save(chestLocationsFile);
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Could not save chest locations to file!");
                    return true;
                }
            }

            // load the chest locations from the file
            FileConfiguration chestLocationsConfig = YamlConfiguration.loadConfiguration(chestLocationsFile);
            List<Location> chestLocations = Objects.requireNonNull(chestLocationsConfig.getList("locations")).stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(Location::deserialize)
                    .toList();
            List<Location> barrelLocations = Objects.requireNonNull(chestLocationsConfig.getList("bonus-locations")).stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(Location::deserialize)
                    .toList();
            List<Location> trappedChestLocations = Objects.requireNonNull(chestLocationsConfig.getList("mid-locations")).stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(Location::deserialize)
                    .toList();

            // refill the chests
            for (Location location : chestLocations) {
                Block block = location.getBlock();
                if (block.getType() == Material.CHEST) {
                    Chest chest = (Chest) block.getState();
                    chest.getInventory().clear();


                    int minChestContent = config.getInt("min-chest-content");
                    int maxChestContent = config.getInt("max-chest-content");
                    Random rand = new Random();
                    int numItems = rand.nextInt(maxChestContent - minChestContent + 1) + minChestContent;
                    numItems = Math.min(numItems, chestItems.size());


                    // Shuffle the chestItems list and get the first 5 items
                    List<ItemStack> randomItems = new ArrayList<>();
                    for (int i = 0; i < numItems; i++) {
                        int index = getRandomWeightedIndex(chestItemWeights);
                        randomItems.add(chestItems.get(index));
                    }

                    // Add the random items to random slots in the chest inventory
                    Set<Integer> usedSlots = new HashSet<>();
                    for (ItemStack item : randomItems) {
                        int slot = rand.nextInt(chest.getInventory().getSize());
                        while (usedSlots.contains(slot)) {
                            slot = rand.nextInt(chest.getInventory().getSize());
                        }
                        usedSlots.add(slot);
                        chest.getInventory().setItem(slot, item);
                    }
                }
            }

            // refill the bonus chests
        for (Location location : barrelLocations) {
            Block block = location.getBlock();
            if (block.getType() == Material.BARREL) {
                Inventory barrel = getItemStacks(block);

                // Get the min and max bonus chest content values from the config
                int minBarrelContent = config.getInt("min-barrel-content");
                int maxBarrelContent = config.getInt("max-barrel-content");
                Random rand = new Random();
                int numItems = rand.nextInt(maxBarrelContent - minBarrelContent + 1) + minBarrelContent;
                numItems = Math.min(numItems, barrelItems.size());

                List<ItemStack> randomItems = new ArrayList<>();
                for (int i = 0; i < numItems; i++) {
                    int index = getRandomWeightedIndex(barrelItemWeights);
                    randomItems.add(barrelItems.get(index));
                }

                // Add the random items to random slots in the bonus chest inventory
                Set<Integer> usedSlots = new HashSet<>();
                for (ItemStack item : randomItems) {
                    int slot = rand.nextInt(barrel.getSize());
                    while (usedSlots.contains(slot)) {
                        slot = rand.nextInt(barrel.getSize());
                    }
                    usedSlots.add(slot);
                    barrel.setItem(slot, item);
                }
            }
        }

            // refill the trapped-chests
            for (Location location : trappedChestLocations) {
                Block block = location.getBlock();
                List<String> trappedChestTypes = config.getStringList("trapped-chest-types");
                if (trappedChestTypes.contains(block.getType().name())) {
                    Inventory trappedChest = getItemStacks(block);

                    // Get the min and max trapped-chest content values from the config
                    int mintrappedChestContent = config.getInt("min-trapped-chest-content");
                    int maxtrappedChestContent = config.getInt("max-trapped-chest-content");
                    Random rand = new Random();
                    int numItems = rand.nextInt(maxtrappedChestContent - mintrappedChestContent + 1) + mintrappedChestContent;
                    numItems = Math.min(numItems, trappedChestItems.size());

                    List<ItemStack> randomItems = new ArrayList<>();
                    for (int i = 0; i < numItems; i++) {
                        int index = getRandomWeightedIndex(trappedChestItemWeights);
                        randomItems.add(trappedChestItems.get(index));
                    }

                    // Add the random items to random slots in the trapped-chest inventory
                    Set<Integer> usedSlots = new HashSet<>();
                    for (ItemStack item : randomItems) {
                        int slot = rand.nextInt(trappedChest.getSize());
                        while (usedSlots.contains(slot)) {
                            slot = rand.nextInt(trappedChest.getSize());
                        }
                        usedSlots.add(slot);
                        trappedChest.setItem(slot, item);
                    }
                }
            }

            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Chests have been refilled!");
        }
        return false;
    }

    @NotNull
    private static Inventory getItemStacks(Block block) {
        Inventory bonusChest;
        if (block.getType() == Material.BARREL) {
            Barrel barrel = (Barrel) block.getState();
            bonusChest = barrel.getInventory();
        } else if (block.getType() == Material.TRAPPED_CHEST) {
            Chest chest = (Chest) block.getState();
            bonusChest = chest.getInventory();
        } else {
            ShulkerBox shulkerBox = (ShulkerBox) block.getState();
            bonusChest = shulkerBox.getInventory();
        }
        bonusChest.clear();
        return bonusChest;
    }

    private int getRandomWeightedIndex(List<Integer> weights) {
        int totalWeight = 0;
        for (int weight : weights) {
            totalWeight += weight;
        }
        int randInt = new Random().nextInt(totalWeight);
        for (int i = 0; i < weights.size(); i++) {
            randInt -= weights.get(i);
            if (randInt < 0) {
                return i;
            }
        }
        return -1;
    }
}



