package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;
    private GameObject Target;

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
        Target = null;
        
        if (!gameState.getGameObjects().isEmpty()) {
                List <GameObject> superFoodList = nearestObjectList(7);
                List <GameObject> foodList = nearestObjectList(2);
                List <GameObject> enemyList = nearestEnemyFromObject(bot);

                for (int i = 0; i < superFoodList.size(); i++) {
                    if (getDistanceBetween(gameState.world.getCenterPoint(), superFoodList.get(i)) >= 180) { //Pastiin foodnya gak terlalu deket sama pinggir arena
                        Target = superFoodList.get(i);
                        playerAction.heading = getHeadingBetween(Target);
                        break;
                    }
                }

                if (superFoodList.size() == 0) { //Kalau gak ada superfood, cari food biasa
                    for (int i = 0; i < foodList.size(); i++) {
                        if (getDistanceBetween(gameState.world.getCenterPoint(), foodList.get(i)) >= 180) {
                            Target = foodList.get(i);
                            playerAction.heading = getHeadingBetween(Target);
                            break;
                        }
                    }

                //Kalau ada superfood, cek apakah ada food biasa yang searah
                } else if (getDistanceBetween(foodList.get(0)) < getDistanceBetween(Target)) { 
                    int normalFoodHeading = getHeadingBetween(foodList.get(0));
                    if (Math.abs(normalFoodHeading - playerAction.heading) <= 10) {
                        Target = foodList.get(0);
                        playerAction.heading = getHeadingBetween(Target);
                    }
                }

                if (bot.size > 100 && bot.TorpedoSalvoCount > 0) { //Bot udah besar, tembak musuh paling dekat
                    playerAction.action = PlayerActions.FIRETORPEDOES;
                    playerAction.heading = getHeadingBetween(enemyList.get(0));
                }
                
                GameObject enemy = enemyList.get(0);

                if (bot.size >= 20 && getDistanceBetween(enemy) - bot.size - enemy.size <= 500) { //Bot kecil tapi ada musuh mendekat (range jarak "dekat" blm fix)
                    if (bot.TorpedoSalvoCount > 0) {                        //tembak kalo ada salvo
                        playerAction.action = PlayerActions.FIRETORPEDOES;
                        playerAction.heading = getHeadingBetween(enemy);
                    } else {                                                //Putar balik kalo gaada salvo
                        playerAction.action = PlayerActions.FORWARD;
                        playerAction.heading = (getHeadingBetween(enemy) + 180) % 360;
                    }
                }

                if (bot.size > enemy.size && getDistanceBetween(enemy) - bot.size - enemy.size <= 200) { //Bot besar, ada musuh mendekat
                    playerAction.action = PlayerActions.FORWARD;
                    playerAction.heading = getHeadingBetween(enemy);
                    //if () {
                    // mau implementasiin afterburner buat ngejar (masih rencana)
                    //}
                }

                //Memastikan bot tidak keluar arena
                double distanceFromWorldCenter = getDistanceBetween(gameState.world.getCenterPoint(), bot);
                if (distanceFromWorldCenter + 1.5 * bot.size >  gameState.world.getRadius()) { //masih bingung nentuin batas amannya
                    playerAction.heading = getHeadingBetween(gameState.world.getCenterPoint());
                    playerAction.action = PlayerActions.FORWARD;
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

    // private void updateSalvo() {
    //     if (SalvoCount <= 10 && gameState.world.currentTick % 10 == 0) {
    //         SalvoCount++;
    //     } 
    // }

    private double getDistanceBetween(GameObject object1, GameObject object2) {
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY);
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



    






