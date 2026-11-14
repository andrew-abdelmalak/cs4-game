package model.characters;

import java.awt.Point;
import java.util.ArrayList;

import engine.Game;
import exceptions.InvalidTargetException;
import exceptions.MovementException;
import exceptions.NoAvailableResourcesException;
import exceptions.NotEnoughActionsException;
import model.collectibles.Supply;
import model.collectibles.Vaccine;
import model.world.Cell;
import model.world.CharacterCell;
import model.world.CollectibleCell;
import model.world.TrapCell;

public abstract class Hero extends Character {
    private int actionsAvailable;
    private int maxActions;
    private boolean specialAction;
    private ArrayList<Vaccine> vaccineInventory;
    private ArrayList<Supply> supplyInventory;

    public int getActionsAvailable() {
        return actionsAvailable;
    }

    public void setActionsAvailable(int actionsAvailable) {
        this.actionsAvailable = actionsAvailable;
    }

    public int getMaxActions() {
        return maxActions;
    }

    public boolean isSpecialAction() {
        return specialAction;
    }

    public void setSpecialAction(boolean specialAction) {
        this.specialAction = specialAction;
    }

    public ArrayList<Vaccine> getVaccineInventory() {
        return vaccineInventory;
    }

    public ArrayList<Supply> getSupplyInventory() {
        return supplyInventory;
    }

    public Hero(String name, int maxHp, int attackDmg, int maxActions) {
        super(name, maxHp, attackDmg);
        this.maxActions = maxActions;
        this.actionsAvailable = maxActions;
        this.supplyInventory = new ArrayList<Supply>();
        this.vaccineInventory = new ArrayList<Vaccine>();
    }

    public void attack() throws InvalidTargetException, NotEnoughActionsException {
        if (this instanceof Fighter && this.isSpecialAction()) {
            super.attack();
        } else {
            if (this.getActionsAvailable() == 0)
                throw new NotEnoughActionsException("You don't have enough actions available");
            super.attack();
            this.setActionsAvailable(this.getActionsAvailable() - 1);
        }
    }

    public void move(Direction d) throws MovementException, NotEnoughActionsException {
        // Determine target cell from direction, handle collectibles/traps/occupancy,
        // reveal the destination and its 8 neighbors, and spend 1 action point.

        if (this.getActionsAvailable() == 0)
            throw new NotEnoughActionsException("You don't have enough action points");

        int startingX = (int) this.getLocation().getX();
        int startingY = (int) this.getLocation().getY();

        if (this.getCurrentHp() == 0) {
            CharacterCell a = (CharacterCell) Game.map[startingX][startingY];
            a.setCharacter(null);
            return;
        }
        int newX, newY;

        switch (d) {
            case UP:
                newX = startingX + 1;
                newY = startingY;
                break;
            case DOWN:
                newX = startingX - 1;
                newY = startingY;
                break;
            case LEFT:
                newX = startingX;
                newY = startingY - 1;
                break;
            case RIGHT:
                newX = startingX;
                newY = startingY + 1;
                break;
            default:
                throw new MovementException("Invalid Direction");
        }
        if (newX < 0 || newX > 14 || newY < 0 || newY > 14)
            throw new MovementException("Movement out of bounds");

        if (Game.map[newX][newY] instanceof CollectibleCell) {
            CollectibleCell c = (CollectibleCell) Game.map[newX][newY];
            c.getCollectible().pickUp(this);
        }

        if (Game.map[newX][newY] instanceof TrapCell) {
            TrapCell t = (TrapCell) Game.map[newX][newY];
            this.setCurrentHp(this.getCurrentHp() - t.getTrapDamage());
            if (this.getCurrentHp() == 0) {
                this.onCharacterDeath();
                Game.map[newX][newY] = new CharacterCell(null);
                Game.map[newX][newY].setVisible(true);
                return;
            }
        }

        if (Game.map[newX][newY] instanceof CharacterCell) {
            CharacterCell c = (CharacterCell) Game.map[newX][newY];
            if (c.getCharacter() != null)
                throw new MovementException("Cannot move to an occupied character cell");
        }

        CharacterCell oldCell = (CharacterCell) Game.map[startingX][startingY];
        oldCell.setCharacter(null);
        CharacterCell newCell = new CharacterCell(this);
        newCell.setVisible(true);
        Game.map[newX][newY] = newCell;
        this.setLocation(new Point(newX, newY));

        ArrayList<Cell> adjacents = Game.getAdjacentCells(newX, newY);
        for (int i = 0; i < adjacents.size(); i++) {
            adjacents.get(i).setVisible(true);
        }

        this.setActionsAvailable(this.getActionsAvailable() - 1);
    }

    public void useSpecial() throws NoAvailableResourcesException, NotEnoughActionsException, InvalidTargetException {
        // Explorer: Allows the player to be able to see the entirety of the map for 1
        // turn whenever a supply is used.
        // • Medic: Can heal and restore health to other heroes or themselves, each
        // process of healing uses 1 supply.
        // • Fighter: Can attack as many times in a turn without costing action points,
        // for 1 turn whenever a supply is used.
        // when supply is used
        if (this.getSupplyInventory().size() == 0)
            throw new NoAvailableResourcesException("You don't have supplies");
    }

    public void cure() throws NoAvailableResourcesException, NotEnoughActionsException, InvalidTargetException {
        if (this.getActionsAvailable() == 0)
            throw new NotEnoughActionsException("You don't have enough action points");
        if (this.getTarget() == null) {
            throw new InvalidTargetException("You dont have a target");
        }
        // uses a vaccine
        // checks adjacent cells for zombie
        int x = (int) this.getLocation().getX();
        int y = (int) this.getLocation().getY();

        ArrayList<Cell> adjacents = Game.getAdjacentCells(x, y);
        for (int i = 0; i < adjacents.size(); i++) {
            if (adjacents.get(i) instanceof CharacterCell) {
                CharacterCell c = (CharacterCell) adjacents.get(i);
                if ((c.getCharacter() != null)
                        && (c.getCharacter() instanceof Zombie && c.getCharacter() == this.getTarget())) {
                    if (this.getVaccineInventory().size() == 0)
                        throw new NoAvailableResourcesException("You don't have any vaccines");
                    this.getVaccineInventory().get(0).use(this);
                    this.setActionsAvailable(this.getActionsAvailable() - 1);
                    return;
                }
            }
        }

        if (Game.isAdjacent(this, this.getTarget()) && this.getTarget() instanceof Zombie) {
            if (this.getVaccineInventory().size() == 0)
                throw new NoAvailableResourcesException("You don't have any vaccines");
            this.getVaccineInventory().get(0).use(this);
            this.setActionsAvailable(this.getActionsAvailable() - 1);
            return;
        }
        throw new InvalidTargetException("Target not in adjacent cells");
    }

    public void onCharacterDeath() {
        super.onCharacterDeath();
        Game.heroes.remove(this);
        Point p = this.getLocation();
        CharacterCell c = (CharacterCell) Game.map[p.x][p.y];
        c.setCharacter(null);
        c.setVisible(true);
    }
}
