package levelGenerators.groupS;

import agents.MarioAgent;
import engine.core.MarioGame;
import engine.core.MarioLevelModel;
import engine.core.MarioResult;
import engine.helper.MarioStats;
import engine.helper.MarioTimer;
import engine.helper.RunUtils;
import levelGenerators.ParamMarioLevelGenerator;

import java.util.ArrayList;
import java.util.Random;

import static engine.helper.RunUtils.generateLevel;

public class LevelGeneratorSearch implements ParamMarioLevelGenerator{

    private Random random = new Random();
    private MarioGame game = new MarioGame();
    private MarioAgent agent = new agents.robinBaumgarten.Agent();
    private int[] currentBest = new int[7]; // The current or the best combination
    private int[][] sum_currentBest = new int[7][5]; // Sum the winning combinations
    private int win_Frequency; // Count the times of winning for AI
    private int GROUND_Y_LENGTH; // The maximum length of the basic floor
    private float GROUND_PROB; // The probability to create or expand a ground block
    private float OBSTACLES_PROB; // The probability to create or expand a brick block
    private float COIN_PROB; // The probability to create a coin
    private float ENEMY_PROB; // The probability to create an enemy
    private int PIPE_LENGTH; // The maximum length of a (pipe, pipe flower or bullet_bill)
    private float PIPE_PROB; // The probability to create a (pipe, pipe flower or bullet_bill)

    private float[][] searchSpaceNumber = {
            /**
             *  The groundtruth figures for each parameters
            */
            {11, 12, 13, 14, 15}, // GROUND_Y_LENGTH
            {0.3f, 0.35f, 0.4f, 0.45f, 0.5f}, // GROUND_PROB
            {0.2f, 0.25f, 0.3f, 0.35f, 0.4f}, // OBSTACLES_PROB
            {0.1f, 0.15f, 0.2f, 0.25f, 0.3f}, // COIN_PROB
            {0.1f, 0.125f, 0.15f, 0.175f, 0.2f}, // ENEMY_PROB
            {1, 2, 3, 4, 5}, // PIPE_LENGTH
            {0.1f, 0.15f, 0.2f, 0.25f, 0.3f} // PIPE_PROB
    };

    /**
     * Constructor
     * @param evolveTimes is the times to evolve
     * @param mutateTimes is the times to mutate
     */
    public LevelGeneratorSearch(int evolveTimes, int mutateTimes){
        String level;
        for(int i = 0; i < evolveTimes; i++){ // evolve
            this.evolve(mutateTimes);
            this.setParameters(this.currentBest);
            level = generateLevel(this);
            MarioResult result = this.game.runGame(this.agent, level, 20, 0, false);

            // Sum the winning combinations
            if(result.getGameStatus().toString().equals("WIN")){
                this.win_Frequency++;
                for(int k = 0; k < this.sum_currentBest.length; k++){
                    this.sum_currentBest[k][this.currentBest[k]]++;
                }
            }
        }
        if(this.win_Frequency != 0){
            for(int i = 0; i < this.sum_currentBest.length; i++){ // use the best combination
                int max = 0;
                int index = 0;
                for(int j = 0; j < this.sum_currentBest[i].length; j++){ // Pick the highest frequency as the best one
                    if(max < this.sum_currentBest[i][j]){
                        max = this.sum_currentBest[i][j];
                        index = j;
                    }
                    else if(max == this.sum_currentBest[i][j]){
                        if(this.random.nextDouble() < 0.5){
                            index = j;
                        }
                    }
                }
                this.currentBest[i] = index;
            }
        }
        this.setParameters(this.currentBest);
    }

    private int randomBumpableTiles(){
        /**
         *  If the dice is:
         *  < 0.8   =>  0 NORMAL_BRICK,
         *  < 0.95  =>  1 COIN_BRICK,
         *  < 0.95  =>  2 LIFE_BRICK,
         *  < 0.95  =>  3 SPECIAL_BRICK,
         *  >= 0.95 =>  4 SPECIAL_QUESTION_BLOCK,
         *  >= 0.95 =>  5 COIN_QUESTION_BLOCK
         */
        Double dice = this.random.nextDouble();
        Double checkDice = this.random.nextDouble();
        if(dice < 0.8){
            return 0; // => NORMAL_BRICK
        }
        else if(dice < 0.95){
            if(checkDice < 0.7){
                return 1; // => COIN_BRICK
            }
            else if(checkDice < 0.9){
                return 3; // => SPECIAL_BRICK
            }
            else{
                return 2; // => LIFE_BRICK
            }
        }
        else{
            if(checkDice < 0.7){
                return 5; // => COIN_QUESTION_BLOCK
            }
            else{
                return 4; // => SPECIAL_QUESTION_BLOCK
            }
        }
    }

    private int randomEnemy(){
        /**
         *  If the dice is:
         *  < 0.8   =>  0 GOOMBA,
         *  < 0.95  =>  1 GOOMBA_WINGED,
         *  < 0.95  =>  2 RED_KOOPA,
         *  < 0.95  =>  3 RED_KOOPA_WINGED,
         *  >= 0.95 =>  4 GREEN_KOOPA,
         *  >= 0.95 =>  5 GREEN_KOOPA_WINGED
         *  >= 0.95 =>  6 SPIKY
         *  >= 0.95 =>  7 SPIKY_WINGED
         */
        Double dice = this.random.nextDouble();
        Double checkDice = this.random.nextDouble();
        if(dice < 0.8){ // non-winged enemy
            if(checkDice < 0.5){
                return 0; // => GOOMBA
            }
            else if(checkDice < 0.7){
                return 2; // => RED_KOOPA
            }
            else if(checkDice < 0.9){
                return 4; // => GREEN_KOOPA
            }
            else{
                return 6; // => SPIKY
            }
        }
        else{ // winged enemy
            if(checkDice < 0.5){
                return 1; // => GOOMBA_WINGED
            }
            else if(checkDice < 0.7){
                return 3; // => RED_KOOPA_WINGED
            }
            else if(checkDice < 0.9){
                return 5; // => GREEN_KOOPA_WINGED
            }
            else{
                return 7; // => SPIKY_WINGED
            }
        }
    }

    private void getBasicFloor(MarioLevelModel model, int windowWidth, int windowNumber){
        /**
         *  Generate a random basic brick or ground, and then expand from it to the whole window
         */
        int basicFloor = random.nextInt(windowWidth - 4);
        basicFloor += windowNumber * windowWidth;
        if(basicFloor == 0){ // Save the place for the Mario
            basicFloor += 1;
        }

        char block;
        double probability = this.random.nextDouble();
        int length = random.nextInt(model.getHeight() - this.GROUND_Y_LENGTH) + this.GROUND_Y_LENGTH;
        if(probability < this.GROUND_PROB){ // Plateau or ground
            model.setRectangle(basicFloor, length, 4 ,model.getHeight() - length, model.GROUND);
        }
        else{ // Float floor or ground
            for(int j = basicFloor; j < basicFloor + 4; j++){
                if(length < model.getHeight() - 1){
                    block = model.getBumpableTiles()[this.randomBumpableTiles()];
                }
                else{
                    block = model.GROUND;
                }
                model.setBlock(j, length, block);
            }
        }
    }

    private int[] getFloorEdge(MarioLevelModel model, int windowWidth, int windowNumber){
        /**
         *  Obtain the left and right side of coordinates for the basic floor
         */
        int[] edge = new int[2]; // 0: leftX, 1: rightX
        for(int j = 0; j < windowWidth; j++){
            if(model.getBlock(j + windowNumber * windowWidth, model.getHeight() - 1) == model.GROUND){
                if(j == 0){
                    edge[0] = j;
                }
                else if(model.getBlock(j + windowNumber * windowWidth - 1, model.getHeight() - 1) == model.EMPTY){
                    edge[0] = j;
                }
                else if(j == windowWidth - 1){
                    edge[1] = j;
                }
                else if(model.getBlock(j + windowNumber * windowWidth + 1, model.getHeight() - 1) == model.EMPTY){
                    edge[1] = j;
                }
            }
        }
        return edge;
    }

    private void expandFloor(MarioLevelModel model, int leftX, int rightX, int windowWidth, int windowNumber){
        /**
         *  Expand the basic floor from the left and right edges of itself
         */
        for(int j = leftX; j > 0; j--){ // To the left side
            if(this.random.nextDouble() < this.OBSTACLES_PROB){
                if(model.getBlock(j + windowNumber * windowWidth, model.getHeight() - 1) != model.EMPTY){
                    j--;
                }
                else if(model.getBlock(j + windowNumber * windowWidth + 3, model.getHeight() - 1) == model.EMPTY){
                    model.setBlock(j + windowNumber * windowWidth, model.getHeight() - 1, model.GROUND);
                }
            }
            else{
                model.setBlock(j + windowNumber * windowWidth, model.getHeight() - 1, model.GROUND);
            }
        }

        for(int j = rightX; j < windowWidth; j++){ // To the right side
            if(this.random.nextDouble() < this.OBSTACLES_PROB){
                if(model.getBlock(j + windowNumber * windowWidth - 3, model.getHeight() - 1) == model.EMPTY){
                    model.setBlock(j + windowNumber * windowWidth, model.getHeight() - 1, model.GROUND);
                }
                else{
                    j++;
                }
            }
            else{
                model.setBlock(j + windowNumber * windowWidth, model.getHeight() - 1, model.GROUND);
            }
        }
    }

    private void getPipe(MarioLevelModel model, int windowWidth, int windowNumber){
        /**
         *  Place a (pipe, pipe flower or bullet bill)
         */
        Double randomType = this.random.nextDouble();
        char pipeType;
        int pipeWidth = 2;
        if(randomType < this.ENEMY_PROB){
            if(this.random.nextDouble() < 0.6){
                pipeType = model.PIPE; // Pipe flower
            }
            else{
                pipeType = model.BULLET_BILL; // Bullet bill
                pipeWidth = 1;
            }
        }
        else{
            pipeType = model.PIPE_FLOWER; // Pipe with flower
        }
        int disparity = model.getHeight() - this.GROUND_Y_LENGTH;
        int pipeLength = this.random.nextInt(Math.abs(this.PIPE_LENGTH - disparity) + disparity);
        int startX = windowNumber * windowWidth + this.random.nextInt(windowWidth - 3);
        if(startX < 2){ // Save the place for the Mario
            startX = 2;
        }
        model.setRectangle(startX, model.getHeight() - pipeLength, pipeWidth, pipeLength, pipeType);
        model.setRectangle(startX - 1, model.getHeight() - pipeLength, 1, pipeLength, model.EMPTY);
        model.setRectangle(startX + pipeWidth, model.getHeight() - pipeLength, 1, pipeLength, model.EMPTY);
    }

    private boolean isGroundOrBrick(MarioLevelModel model, int x, int y){
        /**
         *  Check if the chosen block is a ground or brick
         */
        if((model.getBlock(x, y) == model.GROUND) || //is a ground block
                new String(model.getBumpableTiles()).contains(String.valueOf(model.getBlock(x, y)))){ // or a brick block
            return true;
        }
        else{
            return false;
        }
    }

    private boolean isEnemy(MarioLevelModel model, int x, int y){
        /**
         *  Check if the chosen block is an enemy
         */
        if(new String(model.getEnemyCharacters()).contains(String.valueOf(model.getBlock(x, y)))){
            return true;
        }
        else{
            return false;
        }
    }

    private boolean isPipe(MarioLevelModel model, int x, int y){
        /**
         *  Check if the chosen block is a (pipe, pipe flower or bullet bill)
         */
        boolean result = false;
        if(model.getBlock(x, y) == model.PIPE || model.getBlock(x, y) == model.PIPE_FLOWER || model.getBlock(x, y) == model.BULLET_BILL){
            result = true;
        }
        return result;
    }

    private boolean isFloorFront(MarioLevelModel model, int x, int y){
        /**
         *  Check if there is a floor in front of the wall
         */
        boolean result = false;
        if(x > 0){
            for(int j = 0; j < 5; j++){
                if(this.isGroundOrBrick(model, x - 1, y + j)){ // if the block is a ground or brick
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    private boolean isNeighbourFree(MarioLevelModel model, int x, int y){
        /**
         *  Check the specific neighbour blocks in order to insert a new floor
         */
        if(
            // The two block on the left and below side of this chosen block are not ground or brick blocks
                !this.isGroundOrBrick(model, x - 1, y + 1) &&
                        !this.isGroundOrBrick(model, x - 1, y + 2) &&
                        !this.isGroundOrBrick(model, x, y + 2) &&
                        !this.isGroundOrBrick(model, x, y + 1) &&
                        //---------------------------------------------------------------------------------------------
                        // The block below this chosen block should not be an enemy
                        !this.isEnemy(model, x, y + 1) &&
                        //---------------------------------------------------------------------------------------------
                        // The neighbour blocks beside and below this chosen block are not any kind of pipes
                        !this.isPipe(model, x, y + 1) &&
                        !this.isPipe(model, x, y + 2) &&
                        !this.isPipe(model, x - 1, y) &&
                        !this.isPipe(model, x - 1, y + 1) &&
                        !this.isPipe(model, x + 1, y) &&
                        !this.isPipe(model, x + 1, y + 1)
            //---------------------------------------------------------------------------------------------
        ){
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public String getGeneratedLevel(MarioLevelModel model, MarioTimer timer){
        /**
         *  The main generator function
         */
        model.clearMap();
        int windowNumber = 10; // Separate the level into 10 windows
        int windowWidth = model.getWidth() / windowNumber; // The length of each equally sized window
        for(int i = 0; i < windowNumber; i++){ // Separate the whole level into 15 windows
            for(int y = model.getHeight() - 1; y >= 1; y--) { // From the bottom to the top
                if(y != model.getHeight() - 1){
                    for(int x = 0 + i * windowWidth; x < windowWidth + i * windowWidth; x++){ // From the leftmost to the rightmost in each window
                        // The block is able to place something new on
                        if(model.getBlock(x, y) == model.EMPTY && !this.isPipe(model, x, y + 1)){
                            // The block below is not empty, coin or enemy
                            if((model.getBlock(x, y + 1) != model.EMPTY && model.getBlock(x, y + 1) != model.COIN) &&
                                    !this.isEnemy(model, x, y + 1)){
                                // Place a new random enemy
                                if(this.random.nextDouble() < this.ENEMY_PROB){
                                    model.setBlock(x, y, model.getEnemyCharacters()[this.randomEnemy()]);
                                }
                                // Or expand a ground or brick from the block below it
                                else if(this.random.nextDouble() < this.OBSTACLES_PROB && // if there is a floor in front of the wall
                                        model.getBlock(x, y + 1) == model.GROUND &&
                                        !this.isEnemy(model, x - 1, y) &&
                                        this.isFloorFront(model, x, y)){
                                    model.setBlock(x, y, model.GROUND);
                                }
                                // Or place a coin over this block
                                else if(this.random.nextDouble() < this.COIN_PROB){
                                    model.setBlock(x, y - 1, model.COIN);
                                }
                            }

                            // place some new floors if the two blocks below this are empty
                            if(model.getBlock(x, y) == model.EMPTY &&
                                    this.random.nextDouble() < this.OBSTACLES_PROB &&
                                    y < this.GROUND_Y_LENGTH){
                                Double randomFloor = random.nextDouble();
                                for(int j = 0; j <= 2; j++){
                                    if(this.isNeighbourFree(model, x + j, y) &&
                                            this.isFloorFront(model, x + j, y) &&
                                            !this.isEnemy(model, x + j - 1, y)){
                                        if(randomFloor < this.GROUND_PROB){
                                            model.setBlock(x + j, y, model.GROUND);
                                        }
                                        else if(y > 1){ // Keep an empty space in order to gain the items from the bricks
                                            model.setBlock(x + j, y, model.getBumpableTiles()[this.randomBumpableTiles()]);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else{
                    // Initialise a random basic floor for each window
                    this.getBasicFloor(model, windowWidth, i);
                    int[] edge = this.getFloorEdge(model, windowWidth, i);
                    this.expandFloor(model, edge[0], edge[1], windowWidth, i);
                    if(this.random.nextDouble() < this.PIPE_PROB) { // Place a pipe type set
                        this.getPipe(model, windowWidth, i);
                    }
                }
            }
            if(i == 0){ // Place the Mario on the leftmost ground or brick
                boolean isMarioExist = false;
                for(int y = model.getHeight() - 1; y > 1; y--){ // From the bottom to the top
                    // If there is a ground or brick
                    if(this.isGroundOrBrick(model, 0, y)){ // if the block is a ground or brick
                        model.setBlock(0, y - 1, model.MARIO_START);
                        isMarioExist = true;
                        break;
                    }
                }
                if(!isMarioExist){ // If there is no ground or brick exist in the first y-axis
                    model.setBlock(0, model.getHeight() - 1, model.GROUND); // Place a ground on the left-bottom corner
                    model.setBlock(0, model.getHeight() - 2, model.MARIO_START); // Place the Mario on that ground
                }
            }
            if(i == windowNumber - 1){ // Place the Exit on the right-bottom corner
                model.setBlock(model.getWidth() - 1, model.getHeight() - 1, model.GROUND); // Place a ground on the right-bottom corner
                model.setBlock(model.getWidth() - 1, model.getHeight() - 2, model.PYRAMID_BLOCK); // Place a pyramid block on that ground
                model.setBlock(model.getWidth() - 1, model.getHeight() - 3, model.MARIO_EXIT); // Place the exit on that pyramid block
            }
        }
        return model.getMap();
    }

    @Override
    public String getGeneratorName(){
        return "SearchBasedLevelGenerator";
    }

    @Override
    public ArrayList<float[]> getParameterSearchSpace(){
        ArrayList < float [] > searchSpace = new ArrayList ();
        for(int i = 0; i < this.searchSpaceNumber.length; i++){
            searchSpace.add( this.searchSpaceNumber[i]);
        }
        return searchSpace;
    }

    @Override
    public void setParameters(int[] paramIndex){
        /**
         *  Update the parameters
         */
        this.GROUND_Y_LENGTH = (int) this.searchSpaceNumber[0][paramIndex[0]];
        this.GROUND_PROB = this.searchSpaceNumber[1][paramIndex[1]];
        this.OBSTACLES_PROB = this.searchSpaceNumber[2][paramIndex[2]];
        this.COIN_PROB = this.searchSpaceNumber[3][paramIndex[3]];
        this.ENEMY_PROB = this.searchSpaceNumber[4][paramIndex[4]];
        this.PIPE_LENGTH = (int) this.searchSpaceNumber[5][paramIndex[5]];
        this.PIPE_PROB = this.searchSpaceNumber[6][paramIndex[6]];
    }

    /**
     * The rest of functions are referenced from the given sample on QMplus
     */
    public void evolve(int nIterations) {
        ArrayList<float[]> searchSpace = this.getParameterSearchSpace();

        // Random initialization
        this.currentBest = this.getRandomPoint(searchSpace);
        float bestFitness = this.evaluate(this.currentBest);

        // Repeat for nIterations
        for (int i = 0; i < nIterations; i++) {
            // Mutate current best
            int[] candidate = this.mutate(this.currentBest, searchSpace);
            float candidateFitness = this.evaluate(candidate);

            // Keep the better solution
            if (candidateFitness > bestFitness) {
                this.currentBest = candidate;
                bestFitness = candidateFitness;
            }
        }
    }

    private int[] getRandomPoint(ArrayList<float[]> searchSpace) {
        int nParams = searchSpace.size();
        int[] solution = new int[nParams];
        for (int i = 0; i < nParams; i++) {
            int nValues = searchSpace.get(i).length;
            solution[i] = this.random.nextInt(nValues);
        }
        return solution;
    }

    private int[] mutate(int[] solution, ArrayList<float[]> searchSpace) {
        int[] mutated = solution.clone();
        // Mutate with probability 1/n
        float mutateProb = 1f/solution.length;
        for (int i = 0; i < solution.length; i++) {
            if (this.random.nextFloat() < mutateProb) {
                mutated[i] = this.random.nextInt(searchSpace.get(i).length);
            }
        }
        return mutated;
    }

    private float evaluate(int[] solution) {
        int noRepsPlay = 1;
        int noLevelsGen = 1;

        this.setParameters(solution);

        MarioStats stats = new MarioStats();
        for (int i = 0; i < noLevelsGen; i++) {
            String level = RunUtils.generateLevel(this);

            for (int j = 0; j < noRepsPlay; j++) {
                MarioResult result = this.game.runGame(this.agent, level, 20, 0, false);
                stats = stats.merge(RunUtils.resultToStats(result));
            }
        }
        return stats.winRate;
    }
}