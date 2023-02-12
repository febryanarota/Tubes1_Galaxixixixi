package Models;

import Enums.*;
import java.util.*;

public class GameObject {
  public UUID id;
  public Integer size;
  public Integer speed;
  public Integer currentHeading;
  public Position position;
  public ObjectTypes gameObjectType;
  // public Integer TorpedoSalvoCount;
  // public Integer SupernovaAvailable;
  // public Integer TeleportCount;
  // public Integer ShieldCount;
  // public Integer Effects;

  public GameObject(UUID id, Integer size, Integer speed, Integer currentHeading, Position position,
      ObjectTypes gameObjectType) {
    this.id = id;
    this.size = size;
    this.speed = speed;
    this.currentHeading = currentHeading;
    this.position = position;
    this.gameObjectType = gameObjectType;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public int getSpeed() {
    return speed;
  }

  public void setSpeed(int speed) {
    this.speed = speed;
  }

  public Position getPosition() {
    return position;
  }

  public void setPosition(Position position) {
    this.position = position;
  }

  public ObjectTypes getGameObjectType() {
    return gameObjectType;
  }

  // public int getSupernovaCount() {
  // return SupernovaAvailable;
  // }

  // public void addSupernova(int n) {
  // this.SupernovaAvailable += n;
  // }

  // public int getTorpedoCount() {
  // return TorpedoSalvoCount;
  // }

  // public void addTorpedo(int n) {
  // this.TorpedoSalvoCount += n;
  // }

  // public void setGameObjectType(ObjectTypes gameObjectType) {
  // this.gameObjectType = gameObjectType;
  // }

  // public int getTeleporterCount() {
  // return TeleportCount;
  // }

  // public void addTeleporterCount(int n) {
  // this.TeleportCount += n;
  // }

  // public void addEfects(int x) {
  // this.Effects |= x;
  // }

  // public boolean underEffect(int x) {
  // return ((this.Effects & x) != 0);
  // }

  public static GameObject FromStateList(UUID id, List<Integer> stateList) {
    Position position = new Position(stateList.get(4), stateList.get(5));
    return new GameObject(id, stateList.get(0), stateList.get(1), stateList.get(2), position,
        ObjectTypes.valueOf(stateList.get(3)));
  }
}