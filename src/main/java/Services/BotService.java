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
        GameObject Target = null;
        if (!gameState.getGameObjects().isEmpty()) {
            
            List <GameObject> superFoodList = nearestObjectList(7);
            List <GameObject> foodList = nearestObjectList(2);

            for (int i = 0; i < superFoodList.size(); i++) { 
                GameObject candidate = superFoodList.get(i);
                GameObject nearestEnemyFromTarget = nearestEnemyFromObject(candidate);
                double enemyToTargetDistance = getDistanceBetween(candidate, nearestEnemyFromTarget);
    
                if (enemyToTargetDistance > getDistanceBetween(bot, candidate)) { 
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

            if (Target == null) {

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

    private List<GameObject> nearestObjectList (int n) {
        List <GameObject> object = gameState.getGameObjects().stream()
            .filter(item-> item.gameObjectType.value == n)
            .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item)))
            .collect(Collectors.toList());
        return object;
    }




    








}
