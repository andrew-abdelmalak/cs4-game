package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;

import engine.ConsoleMain;
import engine.Game;
import exceptions.InvalidTargetException;
import exceptions.MovementException;
import exceptions.NotEnoughActionsException;
import model.characters.Direction;
import model.characters.Explorer;
import model.characters.Fighter;
import model.characters.Hero;
import model.characters.Medic;
import model.characters.Zombie;
import model.collectibles.Supply;
import model.collectibles.Vaccine;
import model.world.CharacterCell;

public class EngineGameplayTest {

    @Before
    public void resetState() throws Exception {
        ConsoleMain.resetGameState();
        Field zombiesCountField = Zombie.class.getDeclaredField("ZOMBIES_COUNT");
        zombiesCountField.setAccessible(true);
        zombiesCountField.setInt(null, 0);
    }

    @Test
    public void testLoadHeroesCsvLoadsAllNineRows() throws Exception {
        Game.loadHeroes("heroes.csv");
        assertEquals(9, Game.availableHeroes.size());
        assertEquals("Joel Miller", Game.availableHeroes.get(0).getName());
        assertTrue(Game.availableHeroes.get(0) instanceof Fighter);
        assertEquals("Andrew Beda", Game.availableHeroes.get(8).getName());
        assertTrue(Game.availableHeroes.get(8) instanceof Explorer);
    }

    @Test(expected = MovementException.class)
    public void testMoveOutOfBoundsThrowsMovementException() throws Exception {
        Game.loadHeroes("heroes.csv");
        Hero hero = Game.availableHeroes.get(0);
        Game.startGame(hero);
        hero.move(Direction.DOWN);
    }

    @Test
    public void testMedicSpecialHealsDamagedAndFullHpAllies() throws Exception {
        Medic medic = new Medic("Ellie Williams", 110, 15, 6);
        Fighter fighter = new Fighter("Joel Miller", 140, 30, 5);
        for (int i = 0; i < Game.MAP_SIZE; i++) {
            for (int j = 0; j < Game.MAP_SIZE; j++) {
                Game.map[i][j] = new CharacterCell(null);
            }
        }
        medic.setLocation(new Point(0, 0));
        fighter.setLocation(new Point(0, 1));
        Game.map[0][0] = new CharacterCell(medic);
        Game.map[0][1] = new CharacterCell(fighter);
        Game.heroes.add(medic);
        Game.heroes.add(fighter);

        fighter.setCurrentHp(80);
        medic.getSupplyInventory().add(new Supply());
        medic.setTarget(fighter);
        medic.useSpecial();
        assertEquals(140, fighter.getCurrentHp());
        assertTrue(medic.getSupplyInventory().isEmpty());

        medic.getSupplyInventory().add(new Supply());
        medic.useSpecial();
        assertEquals(140, fighter.getCurrentHp());
        assertTrue(medic.getSupplyInventory().isEmpty());
    }

    @Test(expected = InvalidTargetException.class)
    public void testMedicSpecialRejectsZombieTarget() throws Exception {
        Medic medic = new Medic("Ellie Williams", 110, 15, 6);
        Zombie zombie = new Zombie();
        medic.getSupplyInventory().add(new Supply());
        medic.setTarget(zombie);
        medic.useSpecial();
    }

    @Test
    public void testFighterSpecialAllowsFreeAttacks() throws Exception {
        Fighter fighter = new Fighter("Joel Miller", 140, 30, 2);
        Zombie z1 = new Zombie();
        Zombie z2 = new Zombie();
        for (int i = 0; i < Game.MAP_SIZE; i++) {
            for (int j = 0; j < Game.MAP_SIZE; j++) {
                Game.map[i][j] = new CharacterCell(null);
            }
        }
        fighter.setLocation(new Point(1, 1));
        z1.setLocation(new Point(1, 2));
        z2.setLocation(new Point(2, 1));
        Game.map[1][1] = new CharacterCell(fighter);
        Game.map[1][2] = new CharacterCell(z1);
        Game.map[2][1] = new CharacterCell(z2);
        Game.heroes.add(fighter);
        Game.zombies.add(z1);
        Game.zombies.add(z2);

        fighter.getSupplyInventory().add(new Supply());
        fighter.useSpecial();
        assertTrue(fighter.isSpecialAction());

        fighter.setTarget(z1);
        fighter.attack();
        fighter.setTarget(z2);
        fighter.attack();
        assertEquals(2, fighter.getActionsAvailable());
        assertEquals(10, z1.getCurrentHp());
        assertEquals(10, z2.getCurrentHp());
    }

    @Test
    public void testCureRecruitsHeroAndSatisfiesWinCondition() throws Exception {
        Game.loadHeroes("heroes.csv");
        for (int i = 0; i < Game.MAP_SIZE; i++) {
            for (int j = 0; j < Game.MAP_SIZE; j++) {
                Game.map[i][j] = new CharacterCell(null);
            }
        }
        Hero h1 = Game.availableHeroes.remove(0);
        Hero h2 = Game.availableHeroes.remove(0);
        Hero h3 = Game.availableHeroes.remove(0);
        Hero h4 = Game.availableHeroes.remove(0);
        h1.setLocation(new Point(0, 0));
        h2.setLocation(new Point(0, 2));
        h3.setLocation(new Point(0, 3));
        h4.setLocation(new Point(0, 4));
        Game.heroes.add(h1);
        Game.heroes.add(h2);
        Game.heroes.add(h3);
        Game.heroes.add(h4);
        Game.map[0][0] = new CharacterCell(h1);
        Game.map[0][2] = new CharacterCell(h2);
        Game.map[0][3] = new CharacterCell(h3);
        Game.map[0][4] = new CharacterCell(h4);

        Zombie z = new Zombie();
        z.setLocation(new Point(0, 1));
        Game.zombies.add(z);
        Game.map[0][1] = new CharacterCell(z);

        h1.getVaccineInventory().add(new Vaccine());
        assertFalse(Game.checkWin());
        h1.setTarget(z);
        h1.cure();

        assertEquals(5, Game.heroes.size());
        assertEquals(0, Game.zombies.size());
        assertTrue(Game.checkWin());
    }

    @Test
    public void testExplorerSpecialRevealsEntireMapUntilEndTurn() throws Exception {
        Game.loadHeroes("heroes.csv");
        Explorer explorer = new Explorer("Tess", 80, 20, 6);
        Game.startGame(explorer);
        assertFalse(Game.map[14][14].isVisible());

        explorer.getSupplyInventory().add(new Supply());
        explorer.useSpecial();
        assertTrue(Game.map[14][14].isVisible());

        Game.endTurn();
        assertFalse(explorer.isSpecialAction());
        assertFalse(Game.map[14][14].isVisible());
        assertTrue(Game.map[0][0].isVisible());
    }

    @Test(expected = NotEnoughActionsException.class)
    public void testExplorerCannotUseSpecialTwiceInOneTurn() throws Exception {
        Explorer explorer = new Explorer("Tess", 80, 20, 6);
        for (int i = 0; i < Game.MAP_SIZE; i++) {
            for (int j = 0; j < Game.MAP_SIZE; j++) {
                Game.map[i][j] = new CharacterCell(null);
            }
        }
        explorer.getSupplyInventory().add(new Supply());
        explorer.getSupplyInventory().add(new Supply());
        explorer.useSpecial();
        explorer.useSpecial();
    }

    @Test
    public void testConsoleDemoRunsAndRendersBoard() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        int exitCode = ConsoleMain.runDemo(out);
        String output = buffer.toString(StandardCharsets.UTF_8);

        assertEquals(0, exitCode);
        assertTrue(output.contains("Automated Engine Demo"));
        assertTrue(output.contains("Loaded 9 heroes from heroes.csv"));
        assertTrue(output.contains("Final Demo Board"));
        assertTrue(output.contains("Outcome:"));
    }

    @Test
    public void testConsoleInteractiveSessionStartsMovesAndQuits() {
        String scriptedInput = "0\nw\ne\nq\n";
        ByteArrayInputStream in = new ByteArrayInputStream(scriptedInput.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        ConsoleMain.runInteractive(in, out);
        String output = buffer.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("Choose a starting hero:"));
        assertTrue(output.contains("Joel Miller"));
        assertTrue(output.contains("Exiting game."));
        assertEquals(11, Game.zombies.size());
    }
}
