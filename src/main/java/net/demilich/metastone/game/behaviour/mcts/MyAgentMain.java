package net.demilich.metastone.game.behaviour.mcts;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.threat.GameStateValueBehaviour;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardCatalogue;
import net.demilich.metastone.game.cards.CardProxy;
import net.demilich.metastone.game.cards.HeroCard;
import net.demilich.metastone.game.decks.Deck;
import net.demilich.metastone.game.entities.heroes.HeroClass;
import net.demilich.metastone.game.logic.GameLogic;
import net.demilich.metastone.gui.deckbuilder.DeckProxy;
import net.demilich.metastone.gui.gameconfig.GameConfig;
import net.demilich.metastone.gui.gameconfig.PlayerConfig;
import net.demilich.metastone.gui.simulationmode.SimulationResult;
import net.demilich.metastone.utils.MathUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@SuppressWarnings("Duplicates")
public class MyAgentMain {

    private static class PlayGameTask implements Callable<Void> {

        private final GameConfig gameConfig;

        public PlayGameTask(GameConfig gameConfig) {
            this.gameConfig = gameConfig;
        }

        @Override
        public Void call() throws Exception {
            PlayerConfig playerConfig1 = gameConfig.getPlayerConfig1();
            PlayerConfig playerConfig2 = gameConfig.getPlayerConfig2();

            Player player1 = new Player(playerConfig1);
            Player player2 = new Player(playerConfig2);

            GameContext newGame = new GameContext(player1, player2, new GameLogic());
            newGame.play();

            onGameComplete(newGame);
            newGame.dispose();

            return null;
        }

    }

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(MyAgentMain.class);
    private static int gamesCompleted = 0;
    private static long lastUpdate;

    private static final GameConfig gameConfig = new GameConfig();
    private static final SimulationResult result = new SimulationResult(gameConfig);
    private static final DeckProxy deckProxy;

    private static void onGameComplete(GameContext state) {
        long timeStamp = System.currentTimeMillis();
        gamesCompleted++;
        if (timeStamp - lastUpdate > 100) {
            lastUpdate = timeStamp;
            System.out.print("\rGames completed: " + gamesCompleted + " of " + gameConfig.getNumberOfGames());
            if (gamesCompleted == gameConfig.getNumberOfGames()) System.out.println();
        }
        synchronized (result) {
            result.getPlayer1Stats().merge(state.getPlayer1().getStatistics());
            result.getPlayer2Stats().merge(state.getPlayer2().getStatistics());
        }
    }


    static {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);

        new CardProxy();
        deckProxy = new DeckProxy();

        try {
            deckProxy.loadDecks();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    protected static HeroCard getHeroCardForClass(HeroClass heroClass) {
        for (Card card : CardCatalogue.getHeroes()) {
            HeroCard heroCard = (HeroCard) card;
            if (heroCard.getHeroClass() == heroClass) {
                return heroCard;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        int numberOfGames = 5;
        int iterations = 400;
        if (args.length >= 1) {
            if (args[0].equals("help") || args[0].equals("-h") || args[0].equals("--help")) {
                usage();
            } else {
                try {
                    numberOfGames = Integer.parseInt(args[0]);
                    numberOfGames = MathUtils.clamp(numberOfGames, 2, 1000);
                    iterations = Integer.parseInt(args[1]);
                    iterations = MathUtils.clamp(iterations, 100, 1000);
                } catch (NumberFormatException e) {
                    usage();
                }
            }
        }

        Deck deck = deckProxy.getDeckByName("Brann Dragon Priest");
        //Deck deck2 = DeckFactory.getRandomDeck(HeroClass.WARRIOR);
        PlayerConfig playerConfig1 = new PlayerConfig(deck, new HearthAgent(iterations));
        playerConfig1.setName("Player 1");
        playerConfig1.setHeroCard(getHeroCardForClass(HeroClass.PRIEST));
        Behaviour behaviour = new GameStateValueBehaviour(FeatureVector.getFittest(), "(untrained)");
        PlayerConfig playerConfig2 = new PlayerConfig(deck, behaviour);
        playerConfig2.setName("Player 2");
        playerConfig2.setHeroCard(getHeroCardForClass(HeroClass.PRIEST));

        gameConfig.setPlayerConfig1(playerConfig1);
        gameConfig.setPlayerConfig2(playerConfig2);
        gameConfig.setNumberOfGames(numberOfGames);

        int cores = Runtime.getRuntime().availableProcessors();
        logger.info("Starting simulation on " + cores + " cores");
        System.out.print("Games completed: 0 of " + gameConfig.getNumberOfGames());
        ExecutorService executor = Executors.newFixedThreadPool(cores);

        List<Future<Void>> futures = new ArrayList<>();

        // queue up all games as tasks
        lastUpdate = System.currentTimeMillis();
        for (int i = 0; i < gameConfig.getNumberOfGames(); i++) {
            PlayGameTask task = new PlayGameTask(gameConfig);
            Future<Void> future = executor.submit(task);
            futures.add(future);
        }

        executor.shutdown();
        boolean completed = false;
        while (!completed) {
            completed = true;
            for (Future<Void> future : futures)
            {
                if (!future.isDone()) {
                    completed = false;
                    continue;
                }
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error(ExceptionUtils.getStackTrace(e));
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
            futures.removeIf(Future::isDone);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        result.calculateMetaStatistics();
        logger.info("Simulation finished");
        logger.info("\n Game statistics: \n --- Player 1 --- \n" + result.getPlayer1Stats().toString()
                + "\n --- Player 2 --- \n" + result.getPlayer2Stats().toString());
    }

    private static void usage() {
        System.out.println("Usage: java -jar hearthagent.jar [numberOfGames] [iterations]");
        System.exit(0);
    }
}
