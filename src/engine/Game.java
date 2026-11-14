package engine;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import exceptions.InvalidTargetException;
import exceptions.NotEnoughActionsException;

import model.characters.Explorer;
import model.characters.Fighter;
import model.characters.Hero;
import model.characters.Medic;
import model.characters.Zombie;
import model.characters.Character;
import model.collectibles.Supply;
import model.collectibles.Vaccine;
import model.world.Cell;
import model.world.CharacterCell;
import model.world.CollectibleCell;
import model.world.TrapCell;

public class Game {
    public static final int MAP_SIZE = 15;

    public static ArrayList<Hero> availableHeroes = new ArrayList<Hero>();
    public static ArrayList<Hero> heroes = new ArrayList<Hero>();
    public static ArrayList<Zombie> zombies = new ArrayList<Zombie>(10);
    public static Cell[][] map = new Cell[MAP_SIZE][MAP_SIZE];

    public static void loadHeroes(String filePath) throws Exception {
        try (Scanner sc = new Scanner(new File(filePath))) {
            while (sc.hasNext()) {
                String[] hero = sc.nextLine().split(",");
                String heroName = hero[0];
                String heroType = hero[1];
                int heroMaxHp = Integer.parseInt(hero[2]);
                int heroMaxActions = Integer.parseInt(hero[3]);
                int heroAttackDmg = Integer.parseInt(hero[4]);

                switch (heroType) {
                    case "MED":
                        availableHeroes.add(new Medic(
                                heroName,
                                heroMaxHp,
                                heroAttackDmg,
                                heroMaxActions));
                        break;
                    case "FIGH":
                        availableHeroes.add(new Fighter(
                                heroName,
                                heroMaxHp,
                                heroAttackDmg,
                                heroMaxActions));
                        break;
                    case "EXP":
                        availableHeroes.add(new Explorer(
                                heroName,
                                heroMaxHp,
                                heroAttackDmg,
                                heroMaxActions));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public static int getVaccinesMapCount() {
        int count = 0;
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map.length; j++) {
                if (map[i][j] instanceof CollectibleCell) {
                    CollectibleCell c = (CollectibleCell) map[i][j];
                    if (c.getCollectible() instanceof Vaccine)
                        count++;
                }
            }
        }
        return count;
    }

    public static int getVaccineInventoryCount() {
        int count = 0;
        for (Hero hero : heroes) {
            count += hero.getVaccineInventory().size();
        }
        return count;
    }

    public static int generateRandomNumber(int number) {
        return (int) (Math.random() * number);
    }

    public static Point getEmptyCell() {
        int x, y;
        while (true) {
            x = generateRandomNumber(MAP_SIZE);
            y = generateRandomNumber(MAP_SIZE);
            if (Game.map[x][y] instanceof CharacterCell) {
                CharacterCell c = (CharacterCell) Game.map[x][y];
                if (c.getCharacter() == null)
                    break;
            }
        }
        return new Point(x, y);
    }

    /**
     * Finds an empty {@link CharacterCell} with no adjacent cell matching
     * {@code avoidedCellType}, optionally avoiding adjacent zombies instead.
     */
    private static Point getSpreadOutEmptyCell(Class<? extends Cell> avoidedCellType, boolean avoidZombies) {
        Point p = getEmptyCell();
        while (true) {
            boolean hasAdjacentConflict = false;
            ArrayList<Cell> adjacents = getAdjacentCells(p.x, p.y);
            for (Cell cell : adjacents) {
                if (avoidZombies) {
                    if (cell instanceof CharacterCell) {
                        CharacterCell c = (CharacterCell) cell;
                        if (c.getCharacter() != null && c.getCharacter() instanceof Zombie) {
                            p = getEmptyCell();
                            hasAdjacentConflict = true;
                            break;
                        }
                    }
                } else if (avoidedCellType != null && avoidedCellType.isInstance(cell)) {
                    p = getEmptyCell();
                    hasAdjacentConflict = true;
                    break;
                }
            }
            if (!hasAdjacentConflict) {
                return p;
            }
        }
    }

    public static void startGame(Hero h) {
        availableHeroes.remove(h);
        heroes.add(h);
        h.setLocation(new Point(0, 0));
        Game.map[0][0] = new CharacterCell(h);
        Game.map[0][0].setVisible(true);

        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map.length; j++) {
                if (Game.map[i][j] == null)
                    Game.map[i][j] = new CharacterCell(null);
            }
        }
        // Spawn 10 zombies without placing two zombies adjacent to each other.
        for (int i = 0; i < 10; i++) {
            Zombie z = new Zombie();
            zombies.add(z);
            Point p = getSpreadOutEmptyCell(null, true);
            z.setLocation(p);
            CharacterCell c = (CharacterCell) Game.map[p.x][p.y];
            c.setCharacter(z);
        }

        // Spawn 5 vaccines, 5 supplies, and 5 traps spread out across empty cells.
        for (int i = 0; i < 5; i++) {
            Point p1 = getSpreadOutEmptyCell(CollectibleCell.class, false);
            Game.map[p1.x][p1.y] = new CollectibleCell(new Vaccine());

            Point p2 = getSpreadOutEmptyCell(CollectibleCell.class, false);
            Game.map[p2.x][p2.y] = new CollectibleCell(new Supply());

            Point p3 = getSpreadOutEmptyCell(TrapCell.class, false);
            Game.map[p3.x][p3.y] = new TrapCell();
        }

        Game.map[0][1].setVisible(true);
        Game.map[1][1].setVisible(true);
        Game.map[1][0].setVisible(true);
    }

    public static boolean checkWin() {
        return heroes.size() >= 5 && getVaccinesMapCount() == 0 && getVaccineInventoryCount() == 0;
    }

    public static boolean checkGameOver() {
        return (getVaccinesMapCount() == 0 && getVaccineInventoryCount() == 0) || heroes.isEmpty();
    }

    public static void endTurn() throws InvalidTargetException, NotEnoughActionsException {
        // Each zombie attempts one attack against a random adjacent hero.
        // Then all heroes are refreshed and visibility is recalculated.
        for (int i = 0; i < zombies.size(); i++) {
            Zombie zombie = zombies.get(i);

            int x = zombie.getLocation().x;
            int y = zombie.getLocation().y;

            ArrayList<Cell> adjacents = getAdjacentCells(x, y);
            while (adjacents.size() != 0) {
                Cell c = adjacents.remove(generateRandomNumber(adjacents.size()));
                if (c instanceof CharacterCell) {
                    CharacterCell c1 = (CharacterCell) c;
                    if (c1.getCharacter() != null && c1.getCharacter() instanceof Hero) {
                        zombie.setTarget(c1.getCharacter());
                        zombie.attack();
                        break;
                    }
                }
            }
        }
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map.length; j++) {
                map[i][j].setVisible(false);
            }
        }

        for (int i = 0; i < heroes.size(); i++) {
            Hero hero = heroes.get(i);
            hero.setActionsAvailable(hero.getMaxActions());
            hero.setSpecialAction(false);
            hero.setTarget(null);
            Point p = hero.getLocation();
            ArrayList<Cell> adjacents = getAdjacentCells(p.x, p.y);
            Game.map[p.x][p.y].setVisible(true);
            for (Cell cell : adjacents) {
                cell.setVisible(true);
            }
        }
        Point p = getSpreadOutEmptyCell(null, true);
        Zombie z = new Zombie();
        zombies.add(z);
        z.setLocation(p);
        CharacterCell c = (CharacterCell) Game.map[p.x][p.y];
        c.setCharacter(z);

        for (Zombie zombie : zombies) {
            zombie.setTarget(null);
        }
    }

    public static ArrayList<Cell> getAdjacentCells(int x, int y) {
        ArrayList<Cell> adjacentCells = new ArrayList<Cell>();
        // right
        if (x + 1 < MAP_SIZE)
            adjacentCells.add(Game.map[x + 1][y]);
        // left
        if (x - 1 > -1)
            adjacentCells.add(Game.map[x - 1][y]);
        // up
        if (y + 1 < MAP_SIZE)
            adjacentCells.add(Game.map[x][y + 1]);
        // down
        if (y - 1 > -1)
            adjacentCells.add(Game.map[x][y - 1]);
        // right up
        if (x + 1 < MAP_SIZE && y + 1 < MAP_SIZE)
            adjacentCells.add(Game.map[x + 1][y + 1]);
        // left up
        if (x - 1 > -1 && y + 1 < MAP_SIZE)
            adjacentCells.add(Game.map[x - 1][y + 1]);
        // right down
        if (y - 1 > -1 && x + 1 < MAP_SIZE)
            adjacentCells.add(Game.map[x + 1][y - 1]);
        // left down
        if (y - 1 > -1 && x - 1 > -1)
            adjacentCells.add(Game.map[x - 1][y - 1]);

        return adjacentCells;
    }

    public static boolean isAdjacent(Character a, Character b) {
        return (int) a.getLocation().distance(b.getLocation().getX(), b.getLocation().getY()) <= 1;
    }
}
