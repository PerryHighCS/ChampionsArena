import java.util.concurrent.CompletableFuture;
import java.util.Arrays;
import java.util.Random;

public class BattleEngine {
    private final Champion champA;
    private final Champion champB;
    private final BattleLog log;
    private final ModifierVault vault;
    private final ChampionController controller;

    private int round = 1;
    private final Random random = new Random();

    public BattleEngine(Champion champA, Champion champB, BattleLog log, ModifierVault vault, ChampionController controller) {
        this.champA = champA;
        this.champB = champB;
        this.log = log;
        this.vault = vault;
        this.controller = controller;
    }

    public void runMatch() {
        log.addEntry(null, null, "Battle Start",
            champA.getName() + " vs. " + champB.getName(),
            round, BattleLog.EntryType.INFO);

        while (champA.isAlive() && champB.isAlive()) {
            log.addEntry(null, null, "Round " + round, "-----⚔️  Round " + round + " begins! ⚔️-----", round, BattleLog.EntryType.INFO);

            // Get TurnSubmissions from both players
            final CompletableFuture<TurnSubmission> submissionA = getTurnSubmission(champA, champB);
            final CompletableFuture<TurnSubmission> submissionB = getTurnSubmission(champB, champA);

            // Get the player's actual turn submissions once they are ready
            final TurnSubmission turnA = submissionA.join();
            final TurnSubmission turnB = submissionB.join();

            // Apply Loadout Swaps
            applyLoadoutChanges(champA, turnA);
            applyLoadoutChanges(champB, turnB);

            // Randomize execution order
            final boolean aFirst = random.nextBoolean(); // is champ A first?

            final Champion first = aFirst ? champA : champB;
            final Champion second = aFirst ? champB : champA;

            // Execute both actions
            final BattleContext context1 = new BattleContext(first, second, round, log);
            final BattleContext context2 = context1.reverse();

            // Charge any charging actions
            final Action actionA = first.advanceCharge();
            final Action actionB = second.advanceCharge();

            // Execute the first champion's action
            if (actionA != null) {
                actionA.execute(context1);
            }

            // Execute the second champion's action if the first champion didn't kill the second
            if (second.isAlive() && actionB != null) {
                actionB.execute(context2);
            }

            first.getLoadout().endTurn(context1);
            second.getLoadout().endTurn(context2);

            // Print out the round log
            Arrays.stream(log.getLog())
                  .filter(e -> e.round == round)
                  .forEach(System.out::println);

            round++;
        }

        final String winner = champA.isAlive() ? champA.getName() : champB.getName();
        log.addInfoEntry("Victory", winner + " wins the match!", round);
        System.out.println("\n🏆 " + winner + " is victorious! 🏆");
    }

    private CompletableFuture<TurnSubmission> getTurnSubmission(Champion self, Champion opponent) {
        return controller.planTurn(self, opponent, vault);
    }

    private void applyLoadoutChanges(Champion champ, TurnSubmission turn) {
        final Loadout loadout = champ.getLoadout();

        if (turn.newTactic != null) {
            loadout.swapTactic(turn.newTactic);
        }

        if (turn.newRelic != null) {
            loadout.swapRelic(turn.newRelic);

        }

        if (turn.newGambit != null) {
            loadout.swapPocketedGambit(turn.newGambit);

        }

        if (turn.discardSlot != null) {
            champ.getArsenal().discard(turn.discardSlot);
            champ.getArsenal().draw();
        }

        if (turn.selectedAction != null) {
            champ.lockInAction(turn.selectedAction);
            champ.startCharging();
        }
    }
}
