package com.codingame.code.royale.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

class Player {

  private static final Scanner in = new Scanner(System.in);

  private static Map<Integer, Site> sites = new HashMap<>();
  private static List<Structure> emptyStruc;
  private static List<Structure> friendlyStruc;
  private static List<Structure> enemyStruc;
  private static List<Unit> units;
  private static Queen ME;
  private static int gold;

  public static void main(String args[]) {
    createSites();

    // game loop
    while (true) {
      gold = in.nextInt();
      int touchedSite = in.nextInt(); // -1 if none
      System.err.println("touchedSite: " + touchedSite);
      createStructures();
      createUnits();

      String action = actionCommand();
      String train = ME.trainCommand();

      System.out.println(action);
      System.out.println(train);
    }
  }

  static String actionCommand() {
    String action = ME.buildCommand();
    return action.isEmpty() ? "WAIT" : action;
  }

  static void createSites() {
    int numSites = in.nextInt();
    for (int i = 0; i < numSites; i++) {
      int siteId = in.nextInt();
      int x = in.nextInt();
      int y = in.nextInt();
      int radius = in.nextInt();

      sites.put(siteId, new Site(siteId, x, y, radius));
    }
  }

  static void createStructures() {
    emptyStruc = new ArrayList<>();
    friendlyStruc = new ArrayList<>();
    enemyStruc = new ArrayList<>();
    for (int i = 0; i < sites.size(); i++) {
      int siteId = in.nextInt();
      int gold = in.nextInt(); // used in future leagues
      int maxMineSize = in.nextInt(); // used in future leagues
      int structureType = in.nextInt(); // -1 = No structure, 2 = Barracks
      int owner = in.nextInt(); // -1 = No structure, 0 = Friendly, 1 = Enemy
      int param1 = in.nextInt();
      int param2 = in.nextInt();

      Structure s = new Structure(sites.get(siteId), gold, maxMineSize,
          structureType, owner, param1, param2);

      switch (owner) {
        case -1:
          emptyStruc.add(s);
          break;
        case 0:
          friendlyStruc.add(s);
          break;
        case 1:
          enemyStruc.add(s);
          break;
      }
    }
  }

  static void createUnits() {
    units = new ArrayList<>();
    int numUnits = in.nextInt();
    for (int i = 0; i < numUnits; i++) {
      int x = in.nextInt();
      int y = in.nextInt();
      int owner = in.nextInt();
      int unitType = in.nextInt(); // -1 = QUEEN, 0 = KNIGHT, 1 = ARCHER
      int health = in.nextInt();

      Unit u = createUnit(new Coordinate(x, y), owner, unitType, health);
      units.add(u);

      if (owner == 0) {
        if (unitType == -1)
          ME = (Queen) u;
        else
          ME.add(u);
      }
    }

    ME.addAllStructures();
  }

  static class Coordinate {
    int x;
    int y;

    Coordinate(int x, int y) {
      this.x = x;
      this.y = y;
    }

    double distance(Coordinate that) {
      return Math.sqrt(Math.pow(this.x - that.x, 2) + Math.pow(this.y - that.y, 2));
    }
  }

  static class Site {
    int id;
    Coordinate coordinate;
    int radius;

    Site(int id, int x, int y, int radius) {
      this.id = id;
      coordinate = new Coordinate(x, y);
      this.radius = radius;
    }
  }

  enum StructureType {
    NONE,
    GOLDMINE,
    TOWER,
    KNIGHT_BARRACKS {
      @Override
      int trainCost() {
        return 80;
      }
    },
    ARCHER_BARRACKS {
      @Override
      int trainCost() {
        return 100;
      }
    },
    GIANT_BARRACKS {
      @Override
      int trainCost() {
        return 140;
      }
    };

    static StructureType valueOf(int type, int unitType) {
      switch (type) {
        case -1:
          return NONE;
        case 0:
          return GOLDMINE;
        case 1:
          return TOWER;
        case 2:
          switch (unitType) {
            case 0:
              return KNIGHT_BARRACKS;
            case 1:
              return ARCHER_BARRACKS;
            case 2:
              return GIANT_BARRACKS;
          }
        default:
          throw new RuntimeException("Unsupported structure type: " + type + " unitType: " + unitType);
      }
    }

    int trainCost() {
      return 0;
    }
  }

  static class Structure {
    Site site;
    int gold;
    int maxMineSize;
    StructureType type;
    int owner;
    int param1;
    int param2;

    Structure(Site site, int gold, int maxMineSize,
        int type, int owner, int param1, int param2) {
      this.site = site;
      this.gold = gold;
      this.maxMineSize = maxMineSize;
      this.type = StructureType.valueOf(type, param2);
      this.owner = owner;
      this.param1 = param1;
      this.param2 = param2;
    }

    int trainCost() {
      return type.trainCost();
    }

    int getX() {
      return site.coordinate.x;
    }

    int getY() {
      return site.coordinate.y;
    }

    boolean isMaxSize() {
      return maxMineSize == param1;
    }
  }

  enum UnitType {
    QUEEN,
    KNIGHT,
    ARCHER,
    GIANT;

    static UnitType valueOf(int value) {
      switch (value) {
        case -1:
          return QUEEN;
        case 0:
          return KNIGHT;
        case 1:
          return ARCHER;
        case 2:
          return GIANT;
        default:
          throw new RuntimeException("Unsupported unit type: " + value);
      }
    }
  }

  static class Unit {
    Coordinate coordinate;
    int owner;
    UnitType type;
    int health;

    Unit(Coordinate coordinate, int owner, UnitType type, int health) {
      this.coordinate = coordinate;
      this.owner = owner;
      this.type = type;
      this.health = health;
    }
  }

  static class Queen extends Unit {
    Map<UnitType, List<Unit>> myUnits = new HashMap<>();
    Map<StructureType, List<Structure>> myStructures = new HashMap<>();

    Queen(Coordinate coordinate, int owner, UnitType type, int health) {
      super(coordinate, owner, type, health);
      myUnits.put(UnitType.KNIGHT, new ArrayList<>());
      myUnits.put(UnitType.ARCHER, new ArrayList<>());
      myUnits.put(UnitType.GIANT, new ArrayList<>());

      myStructures.put(StructureType.KNIGHT_BARRACKS, new ArrayList<>());
      myStructures.put(StructureType.ARCHER_BARRACKS, new ArrayList<>());
      myStructures.put(StructureType.GIANT_BARRACKS, new ArrayList<>());
      myStructures.put(StructureType.GOLDMINE, new ArrayList<>());
      myStructures.put(StructureType.TOWER, new ArrayList<>());
    }

    Structure nearestEmptyStructure() {
      double nearestDistance = Double.MAX_VALUE;
      Structure nearest = null;
      for (Structure s : emptyStruc) {
        double distance = coordinate.distance(s.site.coordinate);
        if (distance < nearestDistance) {
          nearestDistance = distance;
          nearest = s;
        }
      }
      return nearest;
    }

    void add(Unit u) {
      switch (u.type) {
        case KNIGHT:
          myUnits.get(UnitType.KNIGHT).add(u);
          break;
        case ARCHER:
          myUnits.get(UnitType.ARCHER).add(u);
          break;
        case GIANT:
          myUnits.get(UnitType.GIANT).add(u);
          break;
      }
    }

    void addAllStructures() {
      for (Structure s : friendlyStruc)
        add(s);
    }

    void add(Structure s) {
      switch (s.type) {
        case KNIGHT_BARRACKS:
          myStructures.get(StructureType.KNIGHT_BARRACKS).add(s);
          break;
        case ARCHER_BARRACKS:
          myStructures.get(StructureType.ARCHER_BARRACKS).add(s);
          break;
        case GIANT_BARRACKS:
          myStructures.get(StructureType.GIANT_BARRACKS).add(s);
          break;
        case GOLDMINE:
          myStructures.get(StructureType.GOLDMINE).add(s);
          break;
        case TOWER:
          myStructures.get(StructureType.TOWER).add(s);
          break;
      }
    }

    int unitCount(UnitType type) {
      return myUnits.get(type).size();
    }

    int structureCount(StructureType type) {
      return myStructures.get(type).size();
    }

    public String buildCommand() {
      Structure nearest = ME.nearestEmptyStructure();
      if (nearest == null)
        return "";

      String build = "BUILD " + nearest.site.id;

      if (structureCount(StructureType.GOLDMINE) == 0)
        return build + " MINE";

      if (structureCount(StructureType.ARCHER_BARRACKS) == 0)
        return build + " BARRACKS-ARCHER";

      if (structureCount(StructureType.TOWER) == 0)
        return build + " TOWER";

      if (myStructures.get(StructureType.GOLDMINE).size() > 0) {
        Structure mine = myStructures.get(StructureType.GOLDMINE).get(0);
        if (!mine.isMaxSize())
          return String.format("BUILD %d MINE", mine.site.id);
      }

      if (structureCount(StructureType.TOWER) == 1)
        return build + " TOWER";

      if (structureCount(StructureType.GOLDMINE) == 1)
        return build + " MINE";

      if (myStructures.get(StructureType.GOLDMINE).size() > 1) {
        Structure mine = myStructures.get(StructureType.GOLDMINE).get(1);
        if (!mine.isMaxSize())
          return String.format("BUILD %d MINE", mine.site.id);
      }

      if (structureCount(StructureType.GIANT_BARRACKS) == 0)
        return build + " BARRACKS-GIANT";

      if (structureCount(StructureType.TOWER) == 2)
        return build + " TOWER";

      if (structureCount(StructureType.GOLDMINE) == 2)
        return build + " MINE";

      if (myStructures.get(StructureType.GOLDMINE).size() > 2) {
        Structure mine = myStructures.get(StructureType.GOLDMINE).get(2);
        if (!mine.isMaxSize())
          return String.format("BUILD %d MINE", mine.site.id);
      }

      if (structureCount(StructureType.KNIGHT_BARRACKS) == 0)
        return build + " BARRACKS-KNIGHT";

      return structureCount(StructureType.TOWER) > 4
          ? build + " TOWER" : "";
    }

    String trainCommand() {
      String train = "TRAIN";
      if (gold >= 100 && unitCount(UnitType.ARCHER) == 0 && structureCount(StructureType.ARCHER_BARRACKS) > 0)
        return train + train(StructureType.ARCHER_BARRACKS);

      if (gold >= 140 && unitCount(UnitType.GIANT) == 0 && structureCount(StructureType.GIANT_BARRACKS) > 0 )
        return train + train(StructureType.GIANT_BARRACKS);

      if (structureCount(StructureType.KNIGHT_BARRACKS) > 0)
        return train + train(StructureType.KNIGHT_BARRACKS);

      return train;
    }

    String train(StructureType type) {
      for (Structure s : myStructures.get(type)) {
        if (s.param1 != 0)
          continue;

        gold -= s.type.trainCost();
        return " " + s.site.id;
      }
      return "";
    }
  }

  static Unit createUnit(Coordinate coordinate, int owner, int type, int health) {
    UnitType unitType = UnitType.valueOf(type);
    return type == -1 && owner == 0
        ? new Queen(coordinate, owner, unitType, health)
        : new Unit(coordinate, owner, unitType, health);
  }
}
