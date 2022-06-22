package com.company;

/*
    Simulate a room using a 16x16 grid, randomly placing occupants in the room and iterate over the room
    using an executor service in tandem with a genetic algorithm executor service to find the best configurations
    for keeping occupants 6 feet (units) apart.

    Zach Freeman - Fall 2021
 */

import java.util.concurrent.*;

public class Main {
    private static final int THREAD_COUNT = 32, GRID_HEIGHT = 16, GRID_WIDTH = 16, POOL_SIZE = 100, MAX_GENERATIONS = 100;
    private static final double MUTATION_RATE = 0.01, MUTATION_CHANCE = 0.05;
    private static final Pool[] pools = new Pool[THREAD_COUNT];
    private static final GUI gui = new GUI();

    private final static class Pool {
        Room[] rooms = new Room[POOL_SIZE];
        double poolScore = 0;
    }
    private final static class Room {
        int[][] grid = new int[GRID_WIDTH][GRID_HEIGHT];
        int population = 0, isolateds = 0;
        double score = 0;
    }
    private final static class Coordinate {
        int x, y;
        public Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int x = 0, consecutiveConvergences = 0;
        ExecutorService executorService = Executors.newWorkStealingPool();
        Future[] fl = new Future[THREAD_COUNT];
        do {
            for (int i = 0; i < fl.length; i++) {
                fl[i] = executorService.submit(newRunnable(i));
            }
            for (Future f : fl) {
                try {
                    f.get();    // synchronization point ?
                } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                }
            }
            cremeDeLaCreme();   // crossover pools with the champ room of the champ pool
            Thread.sleep(500);  // delay each after each GUI update
            if(checkConvergence()) { consecutiveConvergences++; }
            else { consecutiveConvergences = 0; }
            x++;
        } while(x < MAX_GENERATIONS && consecutiveConvergences < 5);

        /*for (Pool pool : pools) {
            //for (int j = 0; j < pool.rooms.length; j++) {
                //printGrid(pool.rooms[j]);
                //System.out.println(pool.rooms[j].score);
            //}
            System.out.println(pool.poolScore);
        }*/
        executorService.shutdown();
    }

    // thread
    private static Runnable newRunnable(int index) {
        //final Exchanger<Room> exchanger = new Exchanger<>();
        return new Runnable() {
            public void run() {
                if(pools[index] == null)
                    pools[index] = initPool();
                // mutate pool
                for(int x = 0; x < pools[index].rooms.length; x++) {
                    if (ThreadLocalRandom.current().nextDouble() < MUTATION_CHANCE)
                        mutate(pools[index].rooms[x]);
                }
                // internal crossover
                pools[index] = newGeneration(pools[index]);
                /*scorePool(pools[index]);
                Room champ = wheresTheChamp(pools[index]);
                try {
                    champ = exchanger.exchange(champ, 5, TimeUnit.SECONDS);
                    theChampIsHere(champ, pools[index]);
                    scorePool(pools[index]);
                } catch(InterruptedException | TimeoutException e){
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }*/
            }
        };
    }

    private static void cremeDeLaCreme() {
        Pool besto = pools[0];
        // find best pool
        for(Pool p : pools) {
            if(p.poolScore > besto.poolScore)
                besto = p;
        }
        // find champ of best pool
        Room champ = wheresTheChamp(besto);
        // introduce the creme de la creme to the other pools
        for(int i = 0; i < pools.length; i++) {
            if(!pools[i].equals(besto))
                theChampIsHere(champ, pools[i]);
        }
        // display champ to GUI
        gui.updateLabel(champ.grid, champ.population, champ.isolateds, champ.score);
    }

    // find highest pool score and then return whether all pools are within 5% of that score or not
    private static Boolean checkConvergence() {
        double peak = pools[0].poolScore;
        for(int i = 1; i < pools.length; i++) {
            if(pools[i].poolScore > peak)
                peak = pools[i].poolScore;
        }
        for (Pool pool : pools)
            if (percentDiff(pool.poolScore, peak) > 0.05)
                return false;

        return true;
    }

    private static double percentDiff(double a, double b) {
        return Math.abs((a - b) / b);
    }

    // create new generation of candidates with some elitism
    private static Pool newGeneration(Pool oldPool) {
        Room child, champ;
        Pool newPool = new Pool();

        for(int i = 0; i < POOL_SIZE; i++) {
            child = reproduce(oldPool);
            if(ThreadLocalRandom.current().nextDouble() < MUTATION_CHANCE) { mutate(child); }
            newPool.rooms[i] = child;
        }
        champ = wheresTheChamp(oldPool);     // find champ of old pool
        theChampIsHere(champ, newPool); // insert proportionally to new pool
        scorePool(newPool);

        return newPool;
    }

    // make new child from "random" parents
    private static Room reproduce(Pool oldPool) {
        double totalFitness = calcTotalFitness(oldPool);
        Room parent1 = selectParent(oldPool, totalFitness);
        Room parent2 = selectParent(oldPool, totalFitness);

        return crossover(parent1, parent2);
    }

    // pick a parent randomly but in such a way that favors more fit candidates.
    // (fitter candidates have a larger range to encapsulate the lucky number)
    private static Room selectParent(Pool pool, double totalFitness) {
        double luckyNumber = ThreadLocalRandom.current().nextDouble() * totalFitness;
        double runningSum = 0;

        for(Room grid : pool.rooms) {
            runningSum += grid.score;
            if(runningSum >= luckyNumber)
                return grid;
        }
        // will never reach
        return null;
    }

    // make a new child by combining the first half parent1 with the latter half of parent2
    private static Room crossover(Room p1, Room p2) {
        int halfHeight = GRID_HEIGHT / 2;
        Room child = new Room();

        for(int i = 0; i < GRID_HEIGHT; i++) {
            for(int j = 0; j < GRID_WIDTH; j++)
                if(i < halfHeight)
                    splice(child, p1, i, j);
                else
                    splice(child, p2, i, j);
        }
        return child;
    }

    // splice target single element of 'b' onto 'a' at same index
    private static void splice(Room a, Room b, int i, int j) {
        if(b != null) {
            if(a.grid[i][j] == 0 && b.grid[i][j] == 1) {
                a.population++;
                a.grid[i][j] = 1;
            } else if(a.grid[i][j] == 1 && b.grid[i][j] == 0) {
                a.population--;
                a.grid[i][j] = 0;
            }
        }
    }

    // turn x% of the grids into the previous generation's champion
    private static void theChampIsHere(Room champ, Pool pool) {
        double amtOfChamps = Math.ceil(POOL_SIZE * 0.03);
        if(amtOfChamps < 1) {
            double rand = ThreadLocalRandom.current().nextDouble() * POOL_SIZE;
            int unlucky = (int) Math.ceil(rand);
            pool.rooms[unlucky] = champ;
        } else {
            for (int i = 0; i < amtOfChamps; i++) {
                double rand = ThreadLocalRandom.current().nextDouble() * POOL_SIZE;
                int unlucky = (int) Math.ceil(rand);
                if (unlucky == POOL_SIZE) { unlucky--; }
                pool.rooms[unlucky] = champ;
            }
        }
    }

    // locate the champ
    private static Room wheresTheChamp(Pool pool) {
        Room champ = new Room();
        double contender;

        for(int i = 0; i < POOL_SIZE; i++) {
            contender = pool.rooms[i].score;
            if(contender > champ.score) { champ = pool.rooms[i]; }
        }
        return champ;
    }

    private static void scorePool(Pool pool) {
        for(Room room : pool.rooms)
            calcFitness(room);
        pool.poolScore = calcTotalFitness(pool);
    }

    private static double calcTotalFitness(Pool pool) {
        double total = 0;
        for(Room candidate : pool.rooms)
            total += candidate.score;
        return total;
    }

    // calculate the fitness of the grid, scoring slightly for total occupants and largely for isolated occupants
    private static void calcFitness(Room room) {
        Coordinate[] coords = findPositions(room);
        double distance;
        boolean isolated;

        room.isolateds = 0; // reset so subsequent calc's don't just keep incrementing this up
        room.score = room.population;
        room.score = room.score * 0.3;

        for(int i = 0; i < coords.length-1; i++) {
            isolated = true;
            for(int j = 0; j < coords.length-1; j++) {
                if (i != j) {   // don't compare point to itself - will guaranteed fail isolation check by calculating that it's too close
                    distance = calcDistance(coords[i], coords[j]);
                    if (distance < 6) {
                        j = coords.length;
                        isolated = false;
                    }
                }
            }
            if (isolated) {
                room.score = room.score + 10;
                room.isolateds++;
            }
        }
    }

    // find the coordinate (x,y) positions of each occupied point on the grid
    // and store in an array.
    private static Coordinate[] findPositions(Room room) {
        Coordinate[] coords = new Coordinate[room.population];
        int pos = 0;

        for(int i = 0; i < GRID_HEIGHT; i++) {
            for(int j = 0; j < GRID_WIDTH; j++) {
                if(room.grid[i][j] == 1 && pos < coords.length) {
                    coords[pos] = new Coordinate(i, j);
                    pos++;
                }
            }
        }

        return coords;
    }

    private static double calcDistance(Coordinate a, Coordinate b) {
        double x1 = a.x, x2 = b.x, y1 = a.y, y2 = b.y;
        return Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));  // distance formula babyyy
    }

    private static void mutate(Room grid) {
        for(int i = 0; i < GRID_HEIGHT; i++) {
            for (int j = 0; j < GRID_WIDTH; j++) {
                if (ThreadLocalRandom.current().nextDouble() < MUTATION_RATE) {
                    if(grid.grid[i][j] == 1) {
                        grid.grid[i][j] = 0;
                        grid.population--;
                    }
                    else {
                        grid.grid[i][j] = 1;
                        grid.population++;
                    }
                }
            }
        }
    }

    public static void printGrid(Room g) {
        for(int i = 0; i < 16; i++) {
            for(int j = 0; j < 16; j++) {
                System.out.print(g.grid[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println("Population: " + g.population);
        System.out.println("# Isolated: " + g.isolateds);
        System.out.println("Fitness Score: " + g.score);
    }

    // create first generation pool of rooms
    private static Pool initPool() {
        Pool pool = new Pool();

        for(int i = 0; i < pool.rooms.length; i++)
            pool.rooms[i] = newRoom();

        return pool;
    }

    // create fresh room for the pool
    public static Room newRoom() {
        Room room = new Room();
        for(int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                if (ThreadLocalRandom.current().nextDouble() < .05) {
                    room.grid[i][j] = 1;
                    room.population++;
                }
            }
        }
        return room;
    }
}
