import agents.MarioAgent;
import engine.core.MarioGame;
import engine.core.MarioResult;
import levelGenerators.MarioLevelGenerator;
import levelGenerators.groupS.LevelGeneratorCellAutomata;
import levelGenerators.groupS.LevelGeneratorSearch;

import static engine.helper.RunUtils.*;

public class Testing {
    public static void main(String[] args) {
        boolean visuals = true;
        boolean generateDifferentLevels = false;

        MarioLevelGenerator gen = new LevelGeneratorCellAutomata();
        // MarioLevelGenerator gen = new LevelGeneratorSearch(10, 1);
        String currLevel = getLevel(null, gen);
        MarioGame game = new MarioGame();
        game.buildWorld(currLevel, 1);
        MarioAgent agent = new agents.robinBaumgarten.Agent();

        int playAgain = 0;
        while (playAgain == 0) {  // 0 - play again! 1 - end execution.

            // Play the level, either as a human ...
            // MarioResult result = game.playGame(level, 200, 0);

            // ... Or with an AI agent
            //MarioResult result = game.playGame(currLevel, 400, 0);
            MarioResult result = game.runGame(agent, currLevel, 20, 0, visuals);

            // Print the results of the game
            System.out.println(result.getGameStatus().toString());
//            System.out.println(resultToStats(result).toString());

            if (generateDifferentLevels) {
                currLevel = generateLevel(gen);
            }

            // Check if we should play again.
            playAgain = (game.playAgain == 0 && visuals) ? 0 : 1;  // If visuals are not on, only play 1 time
        }

    }
}
