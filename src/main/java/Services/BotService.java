package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;

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
        GameObject Target;
        if (!gameState.getGameObjects().isEmpty()) {
            
            List <GameObject> superFoodList = gameState.getGameObjects()
                    .stream().filter(item -> item.gameObjectType.value == 7)
                    .sorted(Comparator
                            .comparing(item -> getDistanceBetween(bot, item)))
                    .collect(Collectors.toList());

            Target = superFoodList.get(0);
            playerAction.heading = getHeadingBetween(Target);

            for (int i = 0; i < superFoodList.size(); i++) { 
                Target = superFoodList.get(i);
                GameObject nearestEnemyFromTarget = nearestEnemyFromObject(Target);
                double enemyToTargetDistance = getDistanceBetween(Target, nearestEnemyFromTarget);
    
                if (enemyToTargetDistance > getDistanceBetween(bot, Target)) { //ini jaraknya blm tau
                    playerAction.heading = getHeadingBetween(Target);
                    break;
                } else {
                    if (nearestEnemyFromTarget.size < bot.size) {
                        playerAction.heading = getHeadingBetween(Target);
                        break;
                    }
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
        Optional<GameObject> optionalBot = gameState.getPlayerGameObjects().stream().filter(gameObject -> gameObject.id.equals(bot.id)).findAny();
        optionalBot.ifPresent(bot -> this.bot = bot);
    }

    private double getDistanceBetween(GameObject object1, GameObject object2) {
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY) - object1.size - object2.size + 1;
    }

    private int getHeadingBetween(GameObject otherObject) {
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
                otherObject.getPosition().x - bot.getPosition().x));
        return (direction + 360) % 360;
    }

    private int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }

    private GameObject nearestEnemyFromObject(GameObject x) {    
        List <GameObject> object = gameState.getPlayerGameObjects().stream()
            .filter(item-> item.id != x.id)
            .sorted(Comparator.comparing(item -> getDistanceBetween(x, item)))
            .collect(Collectors.toList());
        return object.get(0);
    }

    private double nearestObjectDistance (int n, GameObject target) {
        List <GameObject> object = new ArrayList<>();
        object = gameState.getGameObjects().stream().filter(item-> item.gameObjectType.value == n).sorted(Comparator.comparing(thing -> getDistanceBetween(target, thing))).collect(Collectors.toList());
        return getDistanceBetween(target, object.get(0));
    }
    // private GameObject detectObjectNearby(int n){
    //     List <GameObject> object = new ArrayList<>();
    //     if(!gameState.getGameObjects().isEmpty()){
    //         object = gameState.getGameObjects().stream().filter(thing -> (!thing.id.equals(bot.id) && thing.gameObjectType.value == n)).sorted(Comparator.comparing(thing -> getDistanceBetween(bot, thing))).collect(Collectors.toList());
    //     }
    //     return object;
    // }
    
    // private double getDistanceEnemy(GameObject object1, GameObject object2) {
    //     return getDistanceBetween(object1, object2) - object1.getRadius() - object2.getRadius();
    // }
    



    








}
