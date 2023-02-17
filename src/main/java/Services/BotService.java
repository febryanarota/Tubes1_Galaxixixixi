package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;
import java.io.*;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;
    private GameObject Target;
    private GameObject targetSupernova;
    private boolean ablePickup = true;
    private boolean fireSupernova = false;
    private boolean fireTeleport = false;

    public BotService() {
        this.playerAction = new PlayerAction();
        this.gameState = new GameState();
    }

    public GameObject getBot() {
        return this.bot;
    }

    public void setBot(GameObject bot) {
        this.bot = bot;
    }

    public PlayerAction getPlayerAction() {
        return this.playerAction;
    }

    public void setPlayerAction(PlayerAction playerAction) {
        this.playerAction = playerAction;
    }

    public void computeNextPlayerAction(PlayerAction playerAction) {
        playerAction.action = PlayerActions.FORWARD;
        List<GameObject> gases = nearestObjectList(4);
        GameObject Target = null;
        // cek apakah dlm pengaruh gas cloud
        List<GameObject> asteroid = nearestObjectList(5);
        List<GameObject> superFoodList = nearestObjectList(7);
        List<GameObject> foodList = nearestObjectList(2);
        List<GameObject> enemyList = nearestEnemyFromObject(bot);
        List<GameObject> listTorpedo = nearestObjectList(6);

        if (bot.underEffect(4)) {
            for (GameObject gas : gases) {
                double d = c1c2(bot, gas);
                if (isIntersect(gas, bot, d)) {
                    playerAction = escapeInGass(gas, playerAction, 4);
                    break;
                }
            }

            // SHIELD MSH PROTO
        } else if (bot.getSize() > 20 && !listTorpedo.isEmpty() &&
                getDistanceBetween(bot, listTorpedo.get(0)) <= 60
                && listTorpedo.get(0).currentHeading >= ((getHeadingBetween(
                        listTorpedo.get(0), bot)) - calculateHeadingRange(listTorpedo.get(0), bot))
                && listTorpedo.get(0).currentHeading <= ((getHeadingBetween(
                        listTorpedo.get(0), bot)) + calculateHeadingRange(listTorpedo.get(0), bot))) {
            playerAction.action = PlayerActions.ACTIVATESHIELD;

        } else {
            if (!gameState.getGameObjects().isEmpty()) {
                List<GameObject> gasCloud = nearestObjectList(4);
                GameObject enemy = enemyList.get(0);

                findFood();
                if (ablePickup) {
                    findSuperNova();
                }

                // Bot kecil tapi ada musuh mendekat

                if (bot.size >= 40 && getDistanceBetween(enemy) - bot.size - enemy.size <= 500) {
                    if (bot.TorpedoSalvoCount > 0) { // tembak kalo ada salvo
                        playerAction.action = PlayerActions.FIRETORPEDOES;
                        playerAction.heading = getHeadingBetween(enemy);
                    } else if (enemy.size > bot.size && getDistanceBetween(enemy) - bot.size - enemy.size <= 100) {
                        playerAction.action = PlayerActions.FORWARD;
                        playerAction.heading = (getHeadingBetween(enemy) + 180) % 360;

                    } else { // Putar balik kalo gaada salvo
                        playerAction.action = PlayerActions.FORWARD;
                        playerAction.heading = (getHeadingBetween(enemy) + 180) % 360;
                    }
                } else if (bot.size < 40 && getDistanceBetween(enemy) <= 100) {
                    playerAction.action = PlayerActions.FORWARD;
                    playerAction.heading = (getHeadingBetween(enemy) + 180) % 360;
                    // if (isThereGas()) {
                    // playerAction.action = PlayerActions.FORWARD;
                    // playerAction.heading = (playerAction.heading + 120) % 360;
                    // }
                }

                // Bot besar, ada musuh mendekat
                if (bot.size > 1.5 * enemy.size && getDistanceBetween(enemy) <= 100) {
                    playerAction.action = PlayerActions.FORWARD;
                    playerAction.heading = getHeadingBetween(enemy);
                    // if (isThereGas()) {
                    // playerAction.action = PlayerActions.FORWARD;
                    // playerAction.heading = (playerAction.heading + 120) % 360;
                    // }
                } else if (bot.size > 200 && bot.TorpedoSalvoCount > 0) { // Bot udah besar, tembak musuh paling dekat
                    playerAction.action = PlayerActions.FIRETORPEDOES;
                    playerAction.heading = getHeadingBetween(enemyList.get(0));
                }
                if (bot.size > 1.5 * enemy.size && getDistanceBetween(enemy) <= 50) { // Bot besar, ada musuh mendekat
                    playerAction.action = PlayerActions.FORWARD;
                    playerAction.heading = getHeadingBetween(enemy);
                }

                // strategi detonate supernova
                if (fireSupernova == true) {
                    GameObject bomb = nearestObjectList(8).get(0);
                    if (nearestObjectList(8).size() > 0) {
                        if (getDistanceBetween(bomb, targetSupernova) <= 50 ||
                                getDistanceBetween(gameState.world.getCenterPoint(), bomb) <= 200) {
                            playerAction.action = PlayerActions.DETONATESUPERNOVA;
                            playerAction.heading = (getHeadingBetween(bomb) + 180) % 360;
                            fireSupernova = false;

                        }
                    }
                }

                // strategi firesupernova
                if (bot.SupernovaAvailable == 1) {
                    List<GameObject> biggestEnemy = gameState.getPlayerGameObjects().stream()
                            .filter(item -> item.id != bot.id)
                            .sorted(Comparator.comparing(item -> item.size))
                            .collect(Collectors.toList());
                    Collections.reverse(biggestEnemy);

                    // mencari lawan terbesar dengan jarak >= 500
                    for (GameObject bigEnemy : biggestEnemy) {
                        if (getDistanceBetween(bigEnemy) >= 500) {
                            playerAction.action = PlayerActions.FIRESUPERNOVA;
                            playerAction.heading = getHeadingBetween(bigEnemy);
                            targetSupernova = bigEnemy;
                            fireSupernova = true;
                            break;
                        }
                    }
                }

                // Memastikan bot tidak keluar arena
                double distanceFromWorldCenter = getDistanceBetween(gameState.world.getCenterPoint(), bot);
                if (distanceFromWorldCenter + 1.5 * bot.size + bot.speed * 10 > gameState.world.getRadius()) {
                    playerAction.heading = getHeadingBetween(gameState.world.getCenterPoint());
                    playerAction.action = PlayerActions.FORWARD;
                }

            }
        }

        this.playerAction = playerAction;

    }

    public GameState getGameState() {
        return this.gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        updateSelfState();
    }

    private void updateSelfState() {
        Optional<GameObject> optionalBot = gameState.getPlayerGameObjects().stream()
                .filter(gameObject -> gameObject.id.equals(bot.id)).findAny();
        optionalBot.ifPresent(bot -> this.bot = bot);
    }

    private void findFood() {
        List<GameObject> superFoodList = nearestObjectList(7);
        List<GameObject> foodList = nearestObjectList(2);
        GameObject candidate = null;

        if (superFoodList.size() > 0) { // ada superfood, cari yang terdekat
            for (int i = 0; i < superFoodList.size(); i++) {
                candidate = superFoodList.get(i);
                GameObject nearestGas = nearestObjectList(candidate, (4)).get(0);

                // pastikan target tidak terlalu pinggir arena dan tidak ada gas yang
                // menghalangi
                if (getDistanceBetween(gameState.world.getCenterPoint(), candidate) >= 180 + bot.size
                        && !isGasBlocking(candidate) && getDistanceBetween(candidate, nearestGas) >= 100) {
                    Target = candidate;
                    playerAction.heading = getHeadingBetween(Target);
                    break;
                }
            }
        }

        if (superFoodList.size() == 0 || Target == null) { // tidak ada superfood, cari food biasa
            for (int i = 0; i < foodList.size(); i++) {
                candidate = foodList.get(i);
                GameObject nearestGas = nearestObjectList(candidate, (4)).get(0);
                if (getDistanceBetween(gameState.world.getCenterPoint(), candidate) >= 180 + bot.size
                        && !isGasBlocking(candidate) && getDistanceBetween(candidate, nearestGas) >= 100) {
                    Target = candidate;
                    playerAction.heading = getHeadingBetween(Target);
                    break;
                }
            }
            // Kalau ada superfood, cek apakah ada food biasa yang searah
        } else {
            if (getDistanceBetween(foodList.get(0)) < getDistanceBetween(Target)) {
                candidate = foodList.get(0);
                GameObject nearestGas = nearestObjectList(candidate, (4)).get(0);
                int normalFoodHeading = getHeadingBetween(foodList.get(0));
                if (Math.abs(normalFoodHeading - playerAction.heading) <= 10 && !isGasBlocking(candidate)
                        && getDistanceBetween(candidate, nearestGas) >= 100) {
                    Target = candidate;
                    playerAction.heading = getHeadingBetween(Target);
                }
            }
        }

        // tidak ada lagi target makanan yang memungkinkan (asumsi sudah late game), bot
        // bergerak ke tengah
        if (Target == null) {
            playerAction.heading = getHeadingBetween(gameState.world.getCenterPoint());
            playerAction.action = PlayerActions.FORWARD;
        }

    }

    // memeriksa apakah ada gas cloud atau asteroids pada jalan menuju target
    private boolean isGasBlocking(GameObject object) {
        List<GameObject> gasCloud = nearestObjectList(4);
        List<GameObject> asteroids = nearestObjectList(5);
        List<GameObject> obstacles = Stream.concat(gasCloud.stream(), asteroids.stream()).toList();

        if (gasCloud.size() > 0) {
            if (getDistanceBetween(gasCloud.get(0), object) <= bot.size + 5 * bot.speed) {
                return true;
            }
        }
        if (obstacles.size() > 0) {
            for (GameObject obs : obstacles) {
                if (isBlocking(obs) && getDistanceBetween(obs) <= getDistanceBetween(object)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isBlocking(GameObject obj) {
        int theta = Math.abs(getHeadingBetween(obj) - bot.currentHeading);
        double a = getDistanceBetween(obj, bot);
        double b = bot.size + obj.size;
        if (a * Math.sin(theta) <= b) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isThereGas() {
        List<GameObject> gasCloud = nearestObjectList(4);
        List<GameObject> asteroids = nearestObjectList(5);
        List<GameObject> obstacles = Stream.concat(gasCloud.stream(), asteroids.stream()).toList();

        for (GameObject obs : obstacles) {
            if (getHeadingBetween(obs) - bot.currentHeading <= 45 && getDistanceBetween(obs) <= 100 + 5 * bot.speed) {
                return true;
            }
        }
        return false;
    }

    private void findSuperNova() {
        List<GameObject> superNovaList = nearestObjectList(8);
        if (superNovaList.size() > 0) {
            Target = superNovaList.get(0);
            GameObject enemyNearSupernova = nearestEnemyFromObject(Target).get(0);
            if (getDistanceBetween(Target) > getDistanceBetween(Target, enemyNearSupernova) && !isThereGas()
                    && !isGasBlocking(Target)) {
                playerAction.heading = getHeadingBetween(Target);
                playerAction.action = PlayerActions.FORWARD;
            } else if (isThereGas()) {
                ablePickup = false;
            }
        }
    }

    // private void fireTeleporter() {
    // List<GameObject> candidateList = nearestEnemyFromObject(bot);
    // if (candidateList.size() > 0) {
    // for (GameObject candidate : candidateList) {
    // if (getDistanceBetween(candidate) <= 500 && candidate.size + 60 <= bot.size)
    // {
    // playerAction.action = PlayerActions.FIRETELEPORT;
    // playerAction.heading = getHeadingBetween(candidate);
    // fireTeleport = true;
    // break;
    // }
    // }
    // }
    // }

    // private void teleport() {
    // GameObject tele = nearestObjectList(10).get(0);
    // List <GameObject> enemies = nearestEnemyFromObject(tele);
    // for (GameObject enemy : enemies) {
    // if (getDistanceBetween(enemy, tele) <= 50 && enemy.size + 60 <= bot.size) {
    // playerAction.action = PlayerActions.TELEPORT;
    // playerAction.heading = getHeadingBetween(enemy);
    // break;
    // }
    // }
    // }

    private double getDistanceBetween(GameObject object1, GameObject object2) {
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY) - object1.size - object2.size + 1;
    }

    private double getDistanceBetween(GameObject object) {
        return getDistanceBetween(bot, object);
    }

    private double getDistanceBetween(Position object1, GameObject object2) {
        var triangleX = Math.abs(object2.getPosition().x - object1.getX());
        var triangleY = Math.abs(object2.getPosition().y - object1.getY());
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY);
    }

    private int getHeadingBetween(GameObject otherObject) {
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
                otherObject.getPosition().x - bot.getPosition().x));
        return (direction + 360) % 360;
    }

    private int getHeadingBetween(GameObject o1, GameObject o2) {
        var direction = toDegrees(Math.atan2(o2.getPosition().y - o1.getPosition().y,
                o2.getPosition().x - o1.getPosition().x));
        return (direction + 360) % 360;
    }

    private int getHeadingBetween(Position otherObject) {
        var direction = toDegrees(Math.atan2(otherObject.y - bot.getPosition().y,
                otherObject.x - bot.getPosition().x));
        return (direction + 360) % 360;
    }

    private int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }

    private List<GameObject> nearestEnemyFromObject(GameObject x) {
        List<GameObject> object = gameState.getPlayerGameObjects().stream()
                .filter(item -> item.id != x.id)
                .sorted(Comparator.comparing(item -> getDistanceBetween(x, item)))
                .collect(Collectors.toList());
        return object;
    }

    private List<GameObject> nearestObjectList(int n) {
        List<GameObject> object = gameState.getGameObjects().stream()
                .filter(item -> item.gameObjectType.value == n)
                .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item)))
                .collect(Collectors.toList());
        return object;
    }

    private List<GameObject> nearestObjectList(GameObject x, int n) {
        List<GameObject> object = gameState.getGameObjects().stream()
                .filter(item -> item.gameObjectType.value == n)
                .sorted(Comparator.comparing(item -> getDistanceBetween(x, item)))
                .collect(Collectors.toList());
        return object;
    }

    private int calculateHeadingRange(GameObject object1, GameObject object2) {
        int sizey = object2.getSize();
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        var o1o2 = Math.sqrt(triangleX * triangleX + triangleY * triangleY); // jarak antara 2 pusat objek
        int deg = toDegrees(Math.asin(sizey / o1o2));

        return deg; // return heading range for object 1 to hit object 2 (range : (heading between -
                    // deg)%360 - (heading between+deg)%360 )

    }

    private double c1c2(GameObject object1, GameObject object2) {
        int x1 = object1.getPosition().getX();
        int y1 = object1.getPosition().getY();
        int x2 = object2.getPosition().getX();
        int y2 = object2.getPosition().getY();
        double d = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
        return d;
    }

    private boolean isIntersect(GameObject object1, GameObject object2, double d) {
        int r1 = object1.getSize();
        int r2 = object2.getSize();
        return (d < (r1 + r2));
    }

    private boolean object1IsIn(GameObject object1, GameObject object2, double d) {
        int r1 = object1.getSize();
        int r2 = object2.getSize();
        return (d <= (r2 - r1));
    }

    private boolean isColliding(GameObject object1, GameObject object2, int speed1) {
        int x1 = object1.getPosition().getX();
        int y1 = object1.getPosition().getY();
        int v1 = speed1;
        int teta1 = object1.currentHeading;
        int r1 = object1.getSize();

        int x2 = object1.getPosition().getX();
        int y2 = object1.getPosition().getY();
        int v2 = object1.getSpeed();
        int teta2 = object1.currentHeading;
        int r2 = object1.getSize();

        double a = (v1 * Math.cos(Math.toRadians(teta1)) - v2 * Math.cos(Math.toRadians(teta2)))
                * (v1 * Math.cos(Math.toRadians(teta1)) - v2 * Math.cos(Math.toRadians(teta2)))
                + (v1 * Math.sin(Math.toRadians(teta1)) - v2 * Math.sin(Math.toRadians(teta2)))
                        * (v1 * Math.sin(Math.toRadians(teta1)) - v2 * Math.sin(Math.toRadians(teta2)));
        double b = 2 * ((x1 - x2) * (v1 * Math.cos(Math.toRadians(teta1)) - v2 * Math.cos(Math.toRadians(teta2)))
                + (y1 - y2) * (v1 * Math.sin(Math.toRadians(teta1)) - v2 * Math.sin(Math.toRadians(teta2))));
        double c = (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) - (r1 + r2) * (r1 + r2);

        double determinan = b * b - 4 * a * c;
        if (determinan < 0 && a < 0) {
            return true;
        } else if (determinan < 0 && a > 0) {
            return false;
        } else {
            double akar1 = ((-1) * b + Math.sqrt(determinan)) / (2 * a);
            double akar2 = ((-1) * b - Math.sqrt(determinan)) / (2 * a);
            double temp;
            if (akar1 > akar2) {
                temp = akar1;
                akar1 = akar2;
                akar2 = temp;
            }
            boolean area1 = ((a * ((akar1 - 1) * (akar1 - 1)) + b * (akar1 - 1) + c) < 0); // area : t < akar 1
            boolean area2 = ((a * ((1 / 2 * akar1 + 1 / 2 * akar2) * (1 / 2 * akar1 + 1 / 2 * akar2))
                    + b * (1 / 2 * akar1 + 1 / 2 * akar2) + c) < 0); // area : akar1 < t < akar2
            boolean area3 = ((a * ((akar2 + 1) * (akar2 + 1)) + b * (akar2 + 1) + c) < 0);
            if (akar1 >= 1) {
                return area1;
            } else if (akar1 <= 0 && akar2 >= 1) {
                return area2;
            } else if (akar2 <= 0) {
                return area3;
            } else {
                return true;
            }

        }
    }

    private PlayerAction escapeInGass(GameObject gasAsteroid, PlayerAction playerAction, int code) {
        // hitung jarak terdekat bot keluar dari gasscloud (lingkaran)

        double closestExitDeg = 180 + (toDegrees(Math.atan2(gasAsteroid.getPosition().y - bot.getPosition().y,
                gasAsteroid.getPosition().x - bot.getPosition().x) + 360) % 360);
        playerAction.heading = (int) closestExitDeg;

        playerAction.action = PlayerActions.FORWARD;
        return playerAction;
    }

}
