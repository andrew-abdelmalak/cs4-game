package engine;

import java.awt.Point;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Scanner;

import exceptions.GameActionException;
import model.characters.Character;
import model.characters.Direction;
import model.characters.Hero;
import model.characters.Medic;
import model.characters.Zombie;
import model.collectibles.Supply;
import model.collectibles.Vaccine;
import model.world.Cell;
import model.world.CharacterCell;
import model.world.CollectibleCell;

/**
 * Console entry point for The Last of Us: Legacy game engine.
 * Supports a deterministic {@code --demo} walkthrough and an interactive terminal mode.
 */
public class ConsoleMain {
    private static final String DEFAULT_HEROES_FILE = "heroes.csv";
    private static final int DEMO_MAX_TURNS = 6;
    private static final int[] DX = { 1, -1, 0, 0 };
    private static final int[] DY = { 0, 0, -1, 1 };
    private static final Direction[] DIRS = { Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT };

    public static void main(String[] args) {
        if (args != null && args.length > 0 && "--demo".equals(args[0])) {
            int exitCode = runDemo(System.out);
            if (exitCode != 0) {
                System.exit(exitCode);
            }
            return;
        }
        runInteractive(System.in, System.out);
    }

    /**
     * Resets all static {@link Game} collections and map grid to a clean initial state.
     */
    public static void resetGameState() {
        Game.availableHeroes = new ArrayList<Hero>();
        Game.heroes = new ArrayList<Hero>();
        Game.zombies = new ArrayList<Zombie>(10);
        Game.map = new Cell[Game.MAP_SIZE][Game.MAP_SIZE];
    }

    /**
     * Runs an automated multi-turn demonstration using the real engine and {@code heroes.csv}.
     *
     * @param out output stream for board renders and turn logs
     * @return {@code 0} on success, {@code 1} if hero data fails to load
     */
    public static int runDemo(PrintStream out) {
        resetGameState();
        try {
            Game.loadHeroes(DEFAULT_HEROES_FILE);
        } catch (Exception e) {
            out.println("Failed to load " + DEFAULT_HEROES_FILE + ": " + e.getMessage());
            return 1;
        }
        if (Game.availableHeroes.isEmpty()) {
            out.println("No heroes found in " + DEFAULT_HEROES_FILE);
            return 1;
        }

        Hero starter = Game.availableHeroes.get(0);
        Game.startGame(starter);

        out.println("=== The Last of Us: Legacy: Automated Engine Demo ===");
        out.println("Loaded " + (Game.availableHeroes.size() + 1) + " heroes from " + DEFAULT_HEROES_FILE
                + ". Starting hero: " + starter.getName() + " (" + starter.getClass().getSimpleName()
                + ", HP " + starter.getCurrentHp() + "/" + starter.getMaxHp()
                + ", Actions " + starter.getMaxActions()
                + ", ATK " + starter.getAttackDmg() + ")");
        out.println("Initial map: 10 zombies, " + Game.getVaccinesMapCount()
                + " vaccines, 5 supplies, 5 hidden traps.");
        renderBoard(out, true);

        for (int turn = 1; turn <= DEMO_MAX_TURNS && !Game.checkWin() && !Game.checkGameOver(); turn++) {
            out.println("--- Turn " + turn + " ---");
            for (int hIdx = 0; hIdx < Game.heroes.size() && !Game.checkWin() && !Game.checkGameOver(); hIdx++) {
                Hero hero = Game.heroes.get(hIdx);
                playDemoHeroTurn(hero, out);
            }
            if (Game.checkWin() || Game.checkGameOver()) {
                break;
            }
            try {
                Game.endTurn();
                out.println("End of turn " + turn + ": zombies=" + Game.zombies.size()
                        + ", heroes=" + Game.heroes.size()
                        + ", vaccines(map/inv)=" + Game.getVaccinesMapCount()
                        + "/" + Game.getVaccineInventoryCount());
            } catch (GameActionException e) {
                out.println("End turn error: " + e.getMessage());
                break;
            }
        }

        out.println("=== Final Demo Board (Fog-of-War View) ===");
        renderBoard(out, false);
        printStatus(out);
        if (Game.checkWin()) {
            out.println("Outcome: VICTORY");
        } else if (Game.checkGameOver()) {
            out.println("Outcome: GAME OVER");
        } else {
            out.println("Outcome: Demo complete after " + DEMO_MAX_TURNS + " turns (engine active).");
        }
        return 0;
    }

    private static void playDemoHeroTurn(Hero hero, PrintStream out) {
        int guard = 0;
        while (hero.getActionsAvailable() > 0 && Game.heroes.contains(hero)
                && !Game.checkWin() && !Game.checkGameOver() && guard++ < 16) {
            if (!hero.getSupplyInventory().isEmpty() && !hero.isSpecialAction()) {
                if (hero instanceof Medic) {
                    hero.setTarget(hero);
                }
                try {
                    hero.useSpecial();
                    out.println("  " + hero.getName() + " used special action (Supply).");
                } catch (GameActionException ignored) {
                    // Continue with movement or combat if special action preconditions are unmet.
                }
            }

            Zombie adjacentZombie = findAdjacentZombie(hero);
            if (adjacentZombie != null && !hero.getVaccineInventory().isEmpty()) {
                hero.setTarget(adjacentZombie);
                try {
                    int beforeHeroes = Game.heroes.size();
                    hero.cure();
                    out.println("  " + hero.getName() + " cured " + adjacentZombie.getName()
                            + " (heroes=" + beforeHeroes + "->" + Game.heroes.size() + ").");
                    continue;
                } catch (GameActionException ignored) {
                    // Fall through to combat or movement.
                }
            }

            Direction nextStep = findNextStepToGoal(hero);
            if (nextStep != null) {
                try {
                    Point from = new Point(hero.getLocation());
                    hero.move(nextStep);
                    out.println("  " + hero.getName() + " moved " + nextStep
                            + " (" + from.x + "," + from.y + ")->("
                            + hero.getLocation().x + "," + hero.getLocation().y + ")"
                            + " HP=" + hero.getCurrentHp());
                    continue;
                } catch (GameActionException ignored) {
                    // Fall through to attack if blocked.
                }
            }

            if (adjacentZombie != null) {
                hero.setTarget(adjacentZombie);
                try {
                    hero.attack();
                    out.println("  " + hero.getName() + " attacked " + adjacentZombie.getName()
                            + " (hero HP=" + hero.getCurrentHp() + ").");
                    continue;
                } catch (GameActionException ignored) {
                    break;
                }
            }
            break;
        }
    }

    private static Zombie findAdjacentZombie(Hero hero) {
        Point loc = hero.getLocation();
        for (Cell cell : Game.getAdjacentCells(loc.x, loc.y)) {
            if (cell instanceof CharacterCell) {
                Character ch = ((CharacterCell) cell).getCharacter();
                if (ch instanceof Zombie) {
                    return (Zombie) ch;
                }
            }
        }
        return null;
    }

    private static Direction findNextStepToGoal(Hero hero) {
        boolean seekZombie = !hero.getVaccineInventory().isEmpty();
        Point start = hero.getLocation();
        boolean[][] visited = new boolean[Game.MAP_SIZE][Game.MAP_SIZE];
        Direction[][] firstDir = new Direction[Game.MAP_SIZE][Game.MAP_SIZE];
        Queue<Point> queue = new ArrayDeque<Point>();
        visited[start.x][start.y] = true;
        queue.add(start);

        while (!queue.isEmpty()) {
            Point cur = queue.poll();
            for (int d = 0; d < 4; d++) {
                int nx = cur.x + DX[d];
                int ny = cur.y + DY[d];
                if (nx < 0 || nx >= Game.MAP_SIZE || ny < 0 || ny >= Game.MAP_SIZE || visited[nx][ny]) {
                    continue;
                }
                visited[nx][ny] = true;
                Direction stepDir = (cur.x == start.x && cur.y == start.y) ? DIRS[d] : firstDir[cur.x][cur.y];
                firstDir[nx][ny] = stepDir;

                Cell cell = Game.map[nx][ny];
                if (isGoalCell(cell, seekZombie)) {
                    return cur.equals(start) && cell instanceof CharacterCell ? null : stepDir;
                }
                if (isWalkable(cell)) {
                    queue.add(new Point(nx, ny));
                }
            }
        }
        return null;
    }

    private static boolean isGoalCell(Cell cell, boolean seekZombie) {
        if (cell instanceof CollectibleCell) {
            CollectibleCell cc = (CollectibleCell) cell;
            if (!seekZombie && cc.getCollectible() instanceof Vaccine) {
                return true;
            }
            if (cc.getCollectible() instanceof Supply) {
                return true;
            }
        }
        if (seekZombie && cell instanceof CharacterCell) {
            return ((CharacterCell) cell).getCharacter() instanceof Zombie;
        }
        return false;
    }

    private static boolean isWalkable(Cell cell) {
        if (cell instanceof CharacterCell) {
            return ((CharacterCell) cell).getCharacter() == null;
        }
        return true;
    }

    /**
     * Runs an interactive terminal session for playing the game.
     */
    public static void runInteractive(InputStream in, PrintStream out) {
        resetGameState();
        try {
            Game.loadHeroes(DEFAULT_HEROES_FILE);
        } catch (Exception e) {
            out.println("Failed to load " + DEFAULT_HEROES_FILE + ": " + e.getMessage());
            return;
        }

        try (Scanner scanner = new Scanner(in)) {
            out.println("=== The Last of Us: Legacy ===");
            out.println("Choose a starting hero:");
            for (int i = 0; i < Game.availableHeroes.size(); i++) {
                Hero h = Game.availableHeroes.get(i);
                out.printf("  [%d] %-14s %-8s HP:%3d  ATK:%2d  AP:%d%n",
                        i, h.getName(), h.getClass().getSimpleName(),
                        h.getMaxHp(), h.getAttackDmg(), h.getMaxActions());
            }

            Hero chosen = null;
            while (chosen == null) {
                out.print("Hero index (0-" + (Game.availableHeroes.size() - 1) + "): ");
                if (!scanner.hasNextLine()) {
                    return;
                }
                String line = scanner.nextLine().trim();
                try {
                    int idx = Integer.parseInt(line);
                    if (idx >= 0 && idx < Game.availableHeroes.size()) {
                        chosen = Game.availableHeroes.get(idx);
                    }
                } catch (NumberFormatException ignored) {
                    // Re-prompt on non-numeric input.
                }
                if (chosen == null) {
                    out.println("Enter a valid hero index.");
                }
            }

            Game.startGame(chosen);
            int activeHeroIdx = 0;
            printHelp(out);

            while (!Game.checkWin() && !Game.checkGameOver()) {
                if (Game.heroes.isEmpty()) {
                    break;
                }
                if (activeHeroIdx >= Game.heroes.size()) {
                    activeHeroIdx = 0;
                }
                Hero active = Game.heroes.get(activeHeroIdx);
                renderBoard(out, false);
                printStatus(out);
                out.print("[" + active.getName() + " AP=" + active.getActionsAvailable() + "] command> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    continue;
                }
                String[] parts = input.split("\\s+");
                String cmd = parts[0].toLowerCase();

                try {
                    if ("q".equals(cmd) || "quit".equals(cmd)) {
                        out.println("Exiting game.");
                        return;
                    } else if ("h".equals(cmd) || "help".equals(cmd)) {
                        printHelp(out);
                    } else if ("w".equals(cmd) || "up".equals(cmd)) {
                        active.move(Direction.UP);
                    } else if ("s".equals(cmd) || "down".equals(cmd)) {
                        active.move(Direction.DOWN);
                    } else if ("a".equals(cmd) || "left".equals(cmd)) {
                        active.move(Direction.LEFT);
                    } else if ("d".equals(cmd) || "right".equals(cmd)) {
                        active.move(Direction.RIGHT);
                    } else if (("t".equals(cmd) || "target".equals(cmd)) && parts.length >= 3) {
                        int row = Integer.parseInt(parts[1]);
                        int col = Integer.parseInt(parts[2]);
                        if (row < 0 || row >= Game.MAP_SIZE || col < 0 || col >= Game.MAP_SIZE) {
                            out.println("Coordinates out of bounds (0..14).");
                        } else if (Game.map[row][col] instanceof CharacterCell
                                && ((CharacterCell) Game.map[row][col]).getCharacter() != null) {
                            active.setTarget(((CharacterCell) Game.map[row][col]).getCharacter());
                            out.println("Target set to " + active.getTarget().getName());
                        } else {
                            out.println("No character at (" + row + "," + col + ").");
                        }
                    } else if ("x".equals(cmd) || "attack".equals(cmd)) {
                        active.attack();
                    } else if ("u".equals(cmd) || "special".equals(cmd)) {
                        active.useSpecial();
                    } else if ("c".equals(cmd) || "cure".equals(cmd)) {
                        active.cure();
                    } else if ("hero".equals(cmd) && parts.length >= 2) {
                        int idx = Integer.parseInt(parts[1]);
                        if (idx >= 0 && idx < Game.heroes.size()) {
                            activeHeroIdx = idx;
                        } else {
                            out.println("Hero index out of range.");
                        }
                    } else if ("e".equals(cmd) || "end".equals(cmd)) {
                        Game.endTurn();
                    } else {
                        out.println("Unknown command. Type 'help' for commands.");
                    }
                } catch (GameActionException | NumberFormatException e) {
                    out.println("Action rejected: " + e.getMessage());
                }
            }

            if (Game.checkWin()) {
                out.println("You won: 5+ heroes recruited and all vaccines used.");
            } else if (Game.checkGameOver()) {
                out.println("Game over.");
            }
        }
    }

    private static void printHelp(PrintStream out) {
        out.println("Commands: w/up, s/down, a/left, d/right, t <row> <col> (target), "
                + "x (attack), u (special), c (cure), hero <idx>, e (end turn), q (quit)");
    }

    private static void printStatus(PrintStream out) {
        out.println("Heroes (" + Game.heroes.size() + ") | Zombies (" + Game.zombies.size()
                + ") | Map vaccines: " + Game.getVaccinesMapCount()
                + " | Inventory vaccines: " + Game.getVaccineInventoryCount());
        for (int i = 0; i < Game.heroes.size(); i++) {
            Hero h = Game.heroes.get(i);
            out.printf("  [%d] %s (%s) pos=(%d,%d) HP=%d/%d AP=%d/%d V=%d S=%d%n",
                    i, h.getName(), h.getClass().getSimpleName(),
                    h.getLocation().x, h.getLocation().y,
                    h.getCurrentHp(), h.getMaxHp(),
                    h.getActionsAvailable(), h.getMaxActions(),
                    h.getVaccineInventory().size(), h.getSupplyInventory().size());
        }
    }

    /**
     * Renders the 15x15 grid with row 14 at the top so {@link Direction#UP} moves upward.
     */
    public static void renderBoard(PrintStream out, boolean revealAll) {
        out.println("     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4");
        for (int row = Game.MAP_SIZE - 1; row >= 0; row--) {
            out.printf("%2d | ", row);
            for (int col = 0; col < Game.MAP_SIZE; col++) {
                Cell cell = Game.map[row][col];
                out.print(symbolFor(cell, revealAll));
                out.print(' ');
            }
            out.println();
        }
        out.println("Legend: H=Hero  Z=Zombie  V=Vaccine  S=Supply  .=Empty  #=Fog");
    }

    private static char symbolFor(Cell cell, boolean revealAll) {
        if (cell == null) {
            return '?';
        }
        if (!revealAll && !cell.isVisible()) {
            return '#';
        }
        if (cell instanceof CharacterCell) {
            Character ch = ((CharacterCell) cell).getCharacter();
            if (ch instanceof Hero) {
                return 'H';
            }
            if (ch instanceof Zombie) {
                return 'Z';
            }
            return '.';
        }
        if (cell instanceof CollectibleCell) {
            if (((CollectibleCell) cell).getCollectible() instanceof Vaccine) {
                return 'V';
            }
            return 'S';
        }
        return '.';
    }
}
