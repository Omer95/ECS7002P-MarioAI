/**
 * @author Omer Farooq Ahmed
 */
package levelGenerators.groupS;

import levelGenerators.MarioLevelGenerator;
import engine.core.MarioLevelModel;
import engine.helper.MarioTimer;

import java.util.Random;

public class LevelGeneratorCellAutomata implements MarioLevelGenerator {

    Random random;

    public LevelGeneratorCellAutomata() {
        this.random = new Random();
    }

    @Override
    public String getGeneratedLevel(MarioLevelModel model, MarioTimer timer) {

        char[] allSprites = MarioLevelModel.getAllTiles();
        char[] enemyCharacters = {model.GOOMBA, model.RED_KOOPA, model.GREEN_KOOPA, model.SPIKY, model.PIPE};

        // initialize the tiles with random values based on probability except floor
        for (int x=0; x<model.getWidth(); x++) {
            for (int y=0; y<model.getHeight() - 1; y++) {
                if (Math.abs(random.nextGaussian()) < 0.5) {
                    model.setBlock(x, y, allSprites[random.nextInt(allSprites.length)]);
                } else {
                    model.setBlock(x, y, MarioLevelModel.EMPTY);
                }

            }
        }
        // manually fill the floor based on probabilities but first two tiles are floor to place mario
        model.setBlock(0, model.getHeight()-1, MarioLevelModel.GROUND);
        model.setBlock(1, model.getHeight()-1, MarioLevelModel.GROUND);

        for (int i=2; i<model.getWidth(); i++) {
            // place a ground if the random number is within 1 standard deviation of the mean
            double rnd = random.nextGaussian();
            if ((rnd) < 0.75) {
                model.setBlock(i, model.getHeight()-1, MarioLevelModel.GROUND);
            } else {
                model.setBlock(i, model.getHeight()-1, MarioLevelModel.EMPTY);
            }
        }
        // fill all one empty spaces with ground
        for (int i=2; i<model.getWidth()-2; i++) {
            if (model.getBlock(i, model.getHeight()-1) == MarioLevelModel.EMPTY &&
                    model.getBlock(i-1, model.getHeight()-1) == MarioLevelModel.GROUND &&
                    model.getBlock(i+1, model.getHeight()-1) == MarioLevelModel.GROUND) {
                model.setBlock(i, model.getHeight()-1, MarioLevelModel.GROUND);
            }
        }
        // fill final two spaces with ground
        model.setBlock(model.getWidth()-1, model.getHeight()-1, MarioLevelModel.GROUND);
        model.setBlock(model.getWidth()-2, model.getHeight()-1, MarioLevelModel.GROUND);
        // make sure there aren't four consecutive empty spots on floor
        int height = model.getHeight()-1;
        for (int i=0; i<model.getWidth()-3; i++) {
            if (model.getBlock(i, height)==MarioLevelModel.EMPTY && model.getBlock(i+1, height)==MarioLevelModel.EMPTY &&
                    model.getBlock(i+2, height)==MarioLevelModel.EMPTY && model.getBlock(i+3, height)==MarioLevelModel.EMPTY) {
                model.setBlock(i, height, MarioLevelModel.GROUND);
            }
        }
        // cellular automata algorithm
        for (int x=0; x<model.getWidth(); x++) {
            for (int y=0; y<model.getHeight(); y++) {
                /**
                 * rule 1: if there is floor, most tiles above should be empty
                 */
                if (model.getBlock(x, y) == MarioLevelModel.GROUND) {
                    double rnd;
                    // set neighborhood of 2x3 tiles above the current tile
                    int x0 = x-1;
                    if (x0 < 0) {
                        x0 = 0;
                    }
                    int x1 = x+1;
                    if (x1 >= model.getWidth()) {
                        x1 = model.getWidth()-1;
                    }
                    int y0 = y-3;
                    if (y0 < 0) {
                        y0 = 0;
                    }
                    int y1 = y-1;
                    if (y1 < 0) {
                        y1 = 0;
                    }
                    for (int i=x0; i<=x1; i++) {
                        for (int j=y0; j<=y1; j++) {
                            rnd = random.nextGaussian();
                            if (rnd < 0.55) {
                                model.setBlock(i, j, MarioLevelModel.EMPTY);
                            }
                        }
                    }
                }
            }
        }
        /**
         * rule 2: after rule 1, if there is floor, there should be enemies or pipes just above based
         * on a probability
         */
        for (int x=0; x<model.getWidth(); x++) {
            for (int y=0; y<model.getHeight(); y++) {
                if (model.getBlock(x, y) == MarioLevelModel.GROUND) {
                    if (y-1 >= 0 && random.nextGaussian() > 0.70) {
                        model.setBlock(x, y-1, enemyCharacters[random.nextInt(enemyCharacters.length)]);
                    }
                }
            }
        }
        /**
         * rule 3: remove all pipes that aren't above a ground, brick or block
         */
        for (int x=0; x<model.getWidth(); x++) {
            for (int y=0; y<model.getHeight(); y++) {
                if (model.getBlock(x, y) == MarioLevelModel.PIPE && y+1 < model.getHeight()) {
                    if (model.getBlock(x, y+1) != MarioLevelModel.GROUND &&
                            model.getBlock(x, y+1) != MarioLevelModel.PYRAMID_BLOCK &&
                            model.getBlock(x, y+1) != MarioLevelModel.NORMAL_BRICK &&
                            model.getBlock(x, y+1) != MarioLevelModel.COIN_BRICK &&
                            model.getBlock(x, y+1) != MarioLevelModel.LIFE_BRICK &&
                            model.getBlock(x, y+1) != MarioLevelModel.SPECIAL_BRICK &&
                            model.getBlock(x, y+1) != MarioLevelModel.PLATFORM &&
                            model.getBlock(x, y+1) != MarioLevelModel.SPECIAL_QUESTION_BLOCK &&
                            model.getBlock(x, y+1) != MarioLevelModel.COIN_QUESTION_BLOCK) {
                        model.setBlock(x, y, MarioLevelModel.EMPTY);
                    }
                }
            }
        }
        /**
         * rule 4: after rule 2, for all pipes apply a pipe flower based on a probability
         * and remove all pipe flowers that aren't directly above a pipe
         */
        for (int x=0; x<model.getWidth(); x++) {
            for (int y=0; y<model.getHeight(); y++) {

                if (model.getBlock(x, y) == MarioLevelModel.PIPE_FLOWER) {
                    if (y+1 < model.getHeight() && model.getBlock(x, y+1) != MarioLevelModel.PIPE) {
                        model.setBlock(x, y, MarioLevelModel.EMPTY);
                    }
                }
                if (model.getBlock(x, y) == MarioLevelModel.PIPE && y-1 >= 0 && random.nextGaussian() < 0.5) {
                    model.setBlock(x, y-1, MarioLevelModel.PIPE_FLOWER);
                }
            }
        }
        /**
         * rule 5: place question blocks and bricks three blocks above current
         * block if ground based on a probability
         */
        char[] qAndBrick = {model.NORMAL_BRICK, model.COIN_BRICK, model.LIFE_BRICK, model.SPECIAL_BRICK,
                model.SPECIAL_QUESTION_BLOCK, model.COIN_QUESTION_BLOCK, model.PLATFORM};

        for (int x=0; x<model.getWidth(); x++) {
            for (int y=0; y<model.getHeight(); y++) {
                if (model.getBlock(x, y) == MarioLevelModel.GROUND && y-3 >= 0) {
                    if (Math.abs(random.nextGaussian()) > 1) {
                        model.setBlock(x, y-3, qAndBrick[random.nextInt(qAndBrick.length)]);
                    }
                }
            }
        }
        /**
         * rule 6: if there is no surface beneath an enemy, either make it empty or
         * change to a flying enemy. First get rid of all flying enemies
         */
        for (int x=0; x<model.getWidth(); x++) {
            for (int y=0; y<model.getHeight()-1; y++) {
                if (model.getBlock(x, y) == MarioLevelModel.GOOMBA_WINGED ||
                        model.getBlock(x, y) == MarioLevelModel.RED_KOOPA_WINGED ||
                        model.getBlock(x, y) == MarioLevelModel.GREEN_KOOPA_WINGED ||
                        model.getBlock(x, y) == MarioLevelModel.SPIKY_WINGED) {
                    model.setBlock(x, y, MarioLevelModel.EMPTY);
                }
            }
        }
        char[] wingedEnemies = MarioLevelModel.getEnemyCharacters(true);
        for (int x=0; x<model.getWidth(); x++) {
            for (int y=0; y<model.getHeight()-1; y++) {
                if (model.getBlock(x, y) == MarioLevelModel.SPIKY ||
                        model.getBlock(x, y) == MarioLevelModel.GREEN_KOOPA ||
                        model.getBlock(x, y) == MarioLevelModel.RED_KOOPA ||
                        model.getBlock(x, y) == MarioLevelModel.GOOMBA) {
                    if (model.getBlock(x, y+1) == MarioLevelModel.EMPTY) {
                        if (Math.abs(random.nextGaussian()) < 1) {
                            model.setBlock(x, y, MarioLevelModel.EMPTY);
                        } else {
                            model.setBlock(x, y, wingedEnemies[random.nextInt(wingedEnemies.length)]);
                        }
                    }
                }
            }
        }
        // filter through and reduce objects to give more room for movement
        for (int x=0; x<model.getWidth(); x++) {
            for (int y=0; y<model.getHeight()-4; y++) {
                if (model.getBlock(x, y) != MarioLevelModel.EMPTY && Math.abs(random.nextGaussian()) > 0.6) {
                    model.setBlock(x, y, MarioLevelModel.EMPTY);
                }
            }
        }
        /**
         * rule 7: if platforms are less than 3 blocks long, extend them by 1, 2 or 3 blocks
         */
        for (int x=0; x<model.getWidth()-2; x++) {
            for (int y=0; y<model.getHeight(); y++) {
                if (model.getBlock(x, y) == MarioLevelModel.NORMAL_BRICK ||
                        model.getBlock(x, y) == MarioLevelModel.PLATFORM ||
                        model.getBlock(x, y) == MarioLevelModel.COIN_BRICK ||
                        model.getBlock(x, y) == MarioLevelModel.LIFE_BRICK ||
                        model.getBlock(x, y) == MarioLevelModel.SPECIAL_BRICK ||
                        model.getBlock(x, y) == MarioLevelModel.SPECIAL_QUESTION_BLOCK ||
                        model.getBlock(x, y) == MarioLevelModel.COIN_QUESTION_BLOCK) {
                    if (model.getBlock(x+1, y) == MarioLevelModel.EMPTY) { // if only one surface
                        if (Math.abs(random.nextGaussian()) < 0.5) {
                            model.setBlock(x+1, y, qAndBrick[random.nextInt(qAndBrick.length)]);
                        }
                        if (Math.abs(random.nextGaussian()) > 1.5) {
                            model.setBlock(x+2, y, qAndBrick[random.nextInt(qAndBrick.length)]);
                        }
                        if (Math.abs(random.nextGaussian()) > 2) {
                            model.setBlock(x+3, y, qAndBrick[random.nextInt(qAndBrick.length)]);
                        }
                    }
                }
            }
        }
        /**
         * get rid of extra enemies except those on the ground
         */
        for (int x=0; x<model.getWidth(); x++) {
            for (int y=0; y<model.getHeight()-2; y++) {
                if (model.getBlock(x, y) == MarioLevelModel.GOOMBA ||
                        model.getBlock(x, y) == MarioLevelModel.RED_KOOPA ||
                        model.getBlock(x, y) == MarioLevelModel.GREEN_KOOPA ||
                        model.getBlock(x, y) == MarioLevelModel.SPIKY ||
                        model.getBlock(x, y) == MarioLevelModel.GREEN_KOOPA_WINGED ||
                        model.getBlock(x, y) == MarioLevelModel.RED_KOOPA_WINGED ||
                        model.getBlock(x, y) == MarioLevelModel.GOOMBA_WINGED) {
                    if (Math.abs(random.nextGaussian()) > 0.5) {
                        model.setBlock(x, y, MarioLevelModel.EMPTY);
                    }
                }
            }
        }
        /**
         * Housekeeping Rules
         */
        // Make sure the first two and last two columns are empty
        for (int i=0; i<model.getHeight()-1; i++) {
            model.setBlock(0, i, MarioLevelModel.EMPTY);
            model.setBlock(1, i, MarioLevelModel.EMPTY);
            model.setBlock(2, i, MarioLevelModel.EMPTY);
            model.setBlock(3, i, MarioLevelModel.EMPTY);
            model.setBlock(model.getWidth()-1, i, MarioLevelModel.EMPTY);
            model.setBlock(model.getWidth()-2, i, MarioLevelModel.EMPTY);
        }

        // Playability 1: Make sure that Mario doesn't get stuck by not being able to jump over a pit
        for (int i=1; i<model.getWidth()-1; i++) {
            if (model.getBlock(i, model.getHeight()-1) == MarioLevelModel.EMPTY) {
                model.setBlock(i, model.getHeight()-3, MarioLevelModel.EMPTY);
                model.setBlock(i-1, model.getHeight()-3, MarioLevelModel.EMPTY);
                model.setBlock(i+1, model.getHeight()-3, MarioLevelModel.EMPTY);
                model.setBlock(i, model.getHeight()-4, MarioLevelModel.EMPTY);
                model.setBlock(i-1, model.getHeight()-4, MarioLevelModel.EMPTY);
                model.setBlock(i+1, model.getHeight()-4, MarioLevelModel.EMPTY);
            }
        }
        // Playability 2: Reduce enemies on the ground
        for (int i=0; i<model.getWidth(); i++) {
            if (model.getBlock(i, model.getHeight()-2) != MarioLevelModel.EMPTY && Math.abs(random.nextGaussian()) > 0.7) {
                model.setBlock(i, model.getHeight()-2, MarioLevelModel.EMPTY);
            }
        }
        // Playability 3: big mario needs at least 2 spaces between structures to move
        for (int x=0; x<model.getWidth(); x++) {
            for (int y=2; y<model.getHeight(); y++) {
                if (model.getBlock(x, y) == MarioLevelModel.NORMAL_BRICK ||
                        model.getBlock(x, y) == MarioLevelModel.PLATFORM ||
                        model.getBlock(x, y) == MarioLevelModel.COIN_BRICK ||
                        model.getBlock(x, y) == MarioLevelModel.LIFE_BRICK ||
                        model.getBlock(x, y) == MarioLevelModel.SPECIAL_BRICK ||
                        model.getBlock(x, y) == MarioLevelModel.SPECIAL_QUESTION_BLOCK ||
                        model.getBlock(x, y) == MarioLevelModel.COIN_QUESTION_BLOCK ||
                        model.getBlock(x, y) == MarioLevelModel.COIN_HIDDEN_BLOCK ||
                        model.getBlock(x, y) == MarioLevelModel.LIFE_HIDDEN_BLOCK ||
                        model.getBlock(x, y) == MarioLevelModel.USED_BLOCK) {
                    if (Math.abs(random.nextGaussian()) < 0.5) {
                        model.setBlock(x, y-1, MarioLevelModel.EMPTY);
                        model.setBlock(x, y-2, MarioLevelModel.EMPTY);
                    }
                }
            }
        }

        return model.getMap();
    }

    @Override
    public String getGeneratorName() {
        return "CellularAutomataGenerator";
    }
}
