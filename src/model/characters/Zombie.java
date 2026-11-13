package model.characters;

import java.awt.Point;
import java.util.ArrayList;

import engine.Game;
import model.world.Cell;
import model.world.CharacterCell;

public class Zombie extends Character {
    private static int ZOMBIES_COUNT = 0;

    public Zombie() {
        super("Zombie " + ++ZOMBIES_COUNT, 40, 10);
    }

    public void onCharacterDeath() {
        super.onCharacterDeath();
        Game.zombies.remove(this);
        int oldX = this.getLocation().x;
        int oldY = this.getLocation().y;
        Point p = Game.getEmptyCell();
        while (true) {
            while (p.x == oldX && p.y == oldY) {
                p = Game.getEmptyCell();
            }

            boolean hasAdjacentZombie = false;
            ArrayList<Cell> adjacents = Game.getAdjacentCells(p.x, p.y);
            for (Cell cell : adjacents) {
                if (cell instanceof CharacterCell) {
                    CharacterCell c = (CharacterCell) cell;
                    if (c.getCharacter() != null && c.getCharacter() instanceof Zombie) {
                        p = Game.getEmptyCell();
                        hasAdjacentZombie = true;
                        break;
                    }
                }
            }
            if (!hasAdjacentZombie)
                break;
        }

        Zombie z = new Zombie();
        Game.zombies.add(z);
        z.setLocation(p);
        CharacterCell c = (CharacterCell) Game.map[p.x][p.y];
        c.setCharacter(z);
    }

}