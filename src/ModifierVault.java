import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * A vault of battle modifiers that can be drawn randomly throughout the match.
 * This class is responsible for loading and managing the modifiers, it will dynamically
 * load classes from a specified path and allow for random selection of modifiers, or
 * random selection of a specific type of modifier.
 */

public class ModifierVault {
    private final List<Class<? extends BattleModifier>> registry = new ArrayList<>();
    private final Random random = new Random();

    private static ModifierVault instance;

    /**
     * Get the singleton instance of the ModifierVault.
     * @return The instance of ModifierVault.
     * @throws IllegalStateException if the vault has not been initialized.
     */
    public static ModifierVault getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ModifierVault has not been initialized yet.");
        }
        return instance;
    }

    /**
     * Initialize the vault loading the modifiers from a specified path. If no path is provided,
     * it will initialize with only default modifiers.
     * @param folderPath The path to the directory containing the modifier classes. 
     *                   If null or empty, only default modifiers will be loaded.
     * @throws IllegalStateException if the vault has already been initialized.
     * @throws IllegalArgumentException if the folderPath is invalid.
     * @return The instance of ModifierVault.
     */
    public static ModifierVault initialize(String folderPath) {
        if (instance != null) {
            throw new IllegalStateException("ModifierVault has already been initialized.");
        }
        if (folderPath == null || folderPath.isEmpty()) {
            instance = new ModifierVault();
        }
        else {
            instance = new ModifierVault(folderPath);
        }
        return instance;
    }

    /**
     * Initialize the vault with default modifiers.
     */
    private ModifierVault() {
        // Initialize the vault with default modifiers
        registry.add(EmberCrystal.class);
        registry.add(StoneAmulet.class);
        registry.add(Chargebreaker.class);
        registry.add(LastLight.class);
        registry.add(AdrenalSurge.class);
        registry.add(RecklessBurst.class);

    }

    /**
     * Initialize the vault with default modifiers and modifiers from a specified path.
     * @param modifierPath The path to the directory containing the modifier classes.
     * @throws IllegalArgumentException if the path is invalid.
     */
    private ModifierVault(String modifierPath) {

        // Initialize the pool with default modifiers
        this();

        if (modifierPath != null && !modifierPath.isEmpty()) {
            try {
                DynamicClassLoader classLoader = new DynamicClassLoader(modifierPath);
                List<Class<? extends BattleModifier>> classes = classLoader.getSubtypesOf(BattleModifier.class);
                registry.addAll(classes);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid path: " + modifierPath, e);
            }
        }
    }

    /**
     * Register a new battle modifier class to the vault.
     * @param clazz The class of the battle modifier to register.
     */
    public void registerBattleModifier(Class<? extends BattleModifier> clazz) {
        if (clazz != null && !registry.contains(clazz)) {
            registry.add(clazz);
        }
    }
    
    /**
     * Draw a random battle modifier from the vault.
     * @return A random instance of a battle modifier, or null if the vault is empty.
     */
    public BattleModifier drawRandom() {
        if (registry.isEmpty()) return null;
        return instantiate(registry.get(random.nextInt(registry.size())));
    }

    /**
     * Draw a random battle modifier of a specific type from the vault.
     * @param type The class of the battle modifier type to draw (ie Tactic.class).
     * @return A random instance of the specified battle modifier type, or null if none are found.
     */
    public BattleModifier drawRandomOfType(Class<? extends BattleModifier> type) {
        List<Class<? extends BattleModifier>> matches = registry.stream()
            .filter(c -> type.isAssignableFrom(c))
            .collect(Collectors.toList());

        if (matches.isEmpty()) return null;
        return instantiate(matches.get(random.nextInt(matches.size())));
    }

    /**
     * Safely instantiate a battle modifier class.
     * @param clazz The class of the battle modifier to instantiate.
     * @return A new instance of the specified battle modifier class, or null if instantiation fails.
     */
    private BattleModifier instantiate(Class<? extends BattleModifier> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Could not instantiate " + clazz.getName() + ": " + e.getMessage());
            return null;
        }
    }
}
