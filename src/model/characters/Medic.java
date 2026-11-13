package model.characters;

import java.util.ArrayList;

import engine.Game;
import exceptions.InvalidTargetException;
import exceptions.NoAvailableResourcesException;
import exceptions.NotEnoughActionsException;
import model.world.Cell;
import model.world.CharacterCell;

public class Medic extends Hero {
    public Medic(String name, int maxHp, int attackDmg, int maxActions) {
        super(name, maxHp, attackDmg, maxActions);
    }

    public void useSpecial() throws NoAvailableResourcesException, NotEnoughActionsException, InvalidTargetException {
        if (this.getTarget() instanceof Zombie) {
            throw new InvalidTargetException("Cannot heal a zombie");
        }
        if (this.getTarget() == null) {
            throw new InvalidTargetException("Cannot heal nothing");
        }

        super.useSpecial();
        int x = getLocation().x;
        int y = getLocation().y;

        ArrayList<Cell> adjacents = Game.getAdjacentCells(x, y);
        adjacents.add(Game.map[x][y]);

        for (Cell cell : adjacents) {
            if (cell instanceof CharacterCell) {
                CharacterCell c1 = (CharacterCell) cell;
                if (c1.getCharacter() == this.getTarget()) {
                    this.getTarget().setCurrentHp(this.getTarget().getMaxHp());
                    this.getSupplyInventory().get(0).use(this);
                    return;
                }
            }
        }

        if (Game.isAdjacent(this, this.getTarget())) {
            this.getTarget().setCurrentHp(this.getTarget().getMaxHp());
            this.getSupplyInventory().get(0).use(this);
            return;
        }
        throw new InvalidTargetException("Hero not close enough");
    }
}
