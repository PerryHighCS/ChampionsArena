import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;


public class ChampionsArena {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ChampionsArena <controller_type>");
            System.err.println("Available controller types: console, gui, web");
            return;
        }

        ChampionController controller = switch(args[0]) {
            case "console" -> new ConsoleChampionController();
            case "gui" -> new GuiChampionController();
            case "web" -> new WebChampionController();
            default -> throw new IllegalArgumentException("Unknown controller type: " + args[0]);
        };

        List<Class<? extends Champion>> championClasses = loadChampionClasses("arena/");
        championClasses.add(TrainingDummy.class);
        championClasses.add(AdvancedTrainingDummy.class);

        if (championClasses.size() < 2) {
            System.err.println("Not enough Champions found to run a battle.");
            return;
        }

        ModifierVault vault = ModifierVault.initialize("arena/");

        CompletableFuture<Champion> playerOneFuture = controller.chooseChampion("Player 1", championClasses);
        CompletableFuture<Champion> playerTwoFuture = controller.chooseChampion("Player 2", championClasses);

        Champion playerOne = playerOneFuture.join();
        Champion playerTwo = playerTwoFuture.join();

        BattleLog log = new BattleLog();
        BattleEngine engine = new BattleEngine(playerOne, playerTwo, log, vault, controller);
        engine.runMatch();
    }

    private static List<Class<? extends Champion>> loadChampionClasses(String folderPath) {
        try {
            DynamicClassLoader loader = new DynamicClassLoader(folderPath);
            return loader.getSubtypesOf(Champion.class);
        } catch (Exception e) {
            System.err.println("Error loading champions: " + e.getMessage());
            return List.of();
        }
    }
}
