package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;
    private Integer SalvoCount;

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
        GameObject Target = null;
        if (!gameState.getGameObjects().isEmpty()) {

            List<GameObject> superFoodList = nearestObjectList(7);
            List<GameObject> foodList = nearestObjectList(2);
            List<GameObject> enemyList = nearestEnemyFromObject(bot);

            for (int i = 0; i < superFoodList.size(); i++) {
                GameObject candidate = superFoodList.get(i);
                GameObject nearestEnemyFromTarget = nearestEnemyFromObject(candidate).get(0);
                double enemyToTargetDistance = getDistanceBetween(candidate, nearestEnemyFromTarget);

                if (enemyToTargetDistance > getDistanceBetween(candidate)) {
                    Target = candidate;
                    playerAction.heading = getHeadingBetween(Target);
                    break;
                } else {
                    if (nearestEnemyFromTarget.size < bot.size) {
                        Target = candidate;
                        playerAction.heading = getHeadingBetween(Target);
                        break;
                    }
                }
            }

            /* CASE JIKA SUPERFOOD TIDAK ADA YANG MEMUNGKINKAN (MASIH ERROR) */
            // if (Target == null) {
            // for (int i = 0; i < foodList.size(); i++) {
            // GameObject candidate = foodList.get(i);
            // GameObject nearestEnemyFromTarget = nearestEnemyFromObject(candidate).get(0);
            // double enemyToTargetDistance = getDistanceBetween(candidate,
            // nearestEnemyFromTarget);

            // if (enemyToTargetDistance > getDistanceBetween(candidate)) {
            // Target = candidate;
            // playerAction.heading = getHeadingBetween(Target);
            // break;
            // } else {
            // if (nearestEnemyFromTarget.size < bot.size) {
            // Target = candidate;
            // playerAction.heading = getHeadingBetween(Target);
            // break;
            // }
            // }
            // }
            // }

            // MEMASTIKAN BOT TIDAK KELUAR ARENA
            // still error in some cases (gtau kenapa ya?????)
            double distanceFromWorldCenter = getDistanceWorld();
            if (distanceFromWorldCenter + (1.5 * bot.size) > gameState.world.getRadius()) {
                playerAction.heading = getHeadingBetween(gameState.world.getCenterPoint());
            }

            // if (getDistanceBetween(Target) > getDistanceBetween(enemyList.get(0))) {
            // Target = enemyList.get(0);
            // if (bot.size < Target.size) {
            // if (bot.size >= 20 && bot.TorpedoSalvoCount > 0) {
            // playerAction.action = PlayerActions.FIRETORPEDOES;
            // } else {
            // playerAction.action = PlayerActions.FORWARD ;
            // playerAction.heading = (getHeadingBetween(Target) + 180) % 360;
            // }
            // }
            // }

            this.playerAction = playerAction;
        }
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

    private double getDistanceBetween(GameObject object1, GameObject object2) {
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY) - object1.size - object2.size + 1;
    }

    private double getDistanceBetween(GameObject object) {
        return getDistanceBetween(bot, object);
    }

    private double getDistanceWorld() {
        var triangleX = Math.abs(bot.getPosition().x - gameState.world.getCenterPoint().getX());
        var triangleY = Math.abs(bot.getPosition().y - gameState.world.getCenterPoint().getY());
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY);
    }

    private int getHeadingBetween(GameObject otherObject) {
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
                otherObject.getPosition().x - bot.getPosition().x));
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
        boolean intersect = false;
        int r1 = object1.getSize();
        int r2 = object2.getSize();
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

    private void escapeInGass(GameObject gasAsteroid, PlayerAction playerAction, int code) {
        // hitung jarak terdekat bot keluar dari gasscloud (lingkaran)

        var triangleX = Math.abs(gasAsteroid.getPosition().x - bot.getPosition().x);
        var triangleY = Math.abs(gasAsteroid.getPosition().y - gasAsteroid.getPosition().y);
        var o1o2 = Math.sqrt(triangleX * triangleX + triangleY * triangleY);

        double jarakTerdekat = gasAsteroid.getSize() - o1o2 + bot.getSize();
        playerAction.heading = 180 - (toDegrees(Math.atan2(gasAsteroid.getPosition().y - bot.getPosition().y,
                gasAsteroid.getPosition().x - bot.getPosition().x) + 360) % 360);
        GameObject temp = bot;

        if (jarakTerdekat > bot.speed) {
            // ini klo mempertimbangkan klo dia jln kena gass yg lain ga
            // int xtemp = (int)
            // (Math.round((Math.cos(Math.toRadians(playerAction.heading))) *
            // temp.getSpeed()))
            // + temp.getPosition().getX();
            // int ytemp = (int) Math.round(Math.sqrt(temp.getSpeed() * temp.getSpeed()
            // - (xtemp - temp.getPosition().getX()) * (xtemp -
            // temp.getPosition().getX())));
            // Position tempPosition = new Position(xtemp, ytemp);
            // temp.setPosition(tempPosition);
            // List<GameObject> listObst = nearestObjectList(temp, code);
            // boolean lolos = false;
            // for (GameObject obst : listObst) {
            // double d = c1c2(temp, obst);
            // if (isIntersect(temp, obst, d)) {
            // // hitung sudut antara nanti dpt sudut teta -- bentar ya sumpeh skt kepala
            // aing
            // int teta = 90; // temp
            // playerAction.heading += teta;
            // // hitung jarak juga
            // xtemp = (int) (Math.round((Math.cos(Math.toRadians(playerAction.heading))) *
            // temp.getSpeed()))
            // + bot.getPosition().getX();
            // ytemp = (int) Math.round(Math.sqrt(temp.getSpeed() * temp.getSpeed()
            // - (xtemp - bot.getPosition().getX()) * (xtemp - bot.getPosition().getX())));
            // tempPosition.setX(xtemp);
            // tempPosition.setY(ytemp);
            // temp.setPosition(tempPosition);

            // }

            // }
            playerAction.action = PlayerActions.START_AFTERBURNER;

        } else {
            playerAction.action = PlayerActions.FORWARD;
        }
    }

    // private void runLargeEnemies(GameObject enemy, PlayerAction playerAction) {

    // }

}
