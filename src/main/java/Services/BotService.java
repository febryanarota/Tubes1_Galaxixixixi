package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;
    private int salvoCount;

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
            List <GameObject> enemyList = nearestEnemyFromObject(bot);

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

            /*CASE JIKA SUPERFOOD TIDAK ADA YANG MEMUNGKINKAN (MASIH ERROR) */
            // if (Target == null) {
            //     for (int i = 0; i < foodList.size(); i++) { 
            //         GameObject candidate = foodList.get(i);
            //         GameObject nearestEnemyFromTarget = nearestEnemyFromObject(candidate).get(0);
            //         double enemyToTargetDistance = getDistanceBetween(candidate, nearestEnemyFromTarget);
        
            //         if (enemyToTargetDistance > getDistanceBetween(candidate)) { 
            //             Target = candidate;
            //             playerAction.heading = getHeadingBetween(Target);
            //             break;
            //         } else {
            //             if (nearestEnemyFromTarget.size < bot.size) {
            //                 Target = candidate;
            //                 playerAction.heading = getHeadingBetween(Target);
            //                 break;
            //             }
            //         }
            //     }
            // }
            
            //MEMASTIKAN BOT TIDAK KELUAR ARENA
            //still error in some cases (gtau kenapa ya?????)
            double distanceFromWorldCenter = getDistanceWorld();
            if (distanceFromWorldCenter + (1.5 * bot.size) >  gameState.world.getRadius()) {
                playerAction.heading = getHeadingBetween(gameState.world.getCenterPoint());      
            }

             /*
              * if (getDistanceBetween(Target) > getDistanceBetween(enemyList.get(0))) {
                 Target = enemyList.get(0);
                 if (bot.size < Target.size) {
                     if (bot.size >= 20 && bot.TorpedoSalvoCount > 0) {
                         playerAction.action = PlayerActions.FIRETORPEDOES;
                    } else {
                         playerAction.action = PlayerActions.FORWARD ;
                         playerAction.heading = (getHeadingBetween(Target) + 180) % 360;
                     }
                 }
             }
              */

             //speed = 60, 
             /*Detect ada kapal lawan di sekitar jarak +- 40
              * Case1: Jarak kurang dari 40 -> langsung tembak
                check size kapal lawan(?) kayaknya gaperlu tembak aja kalo kena dapet poin ini
                pastiin jarak/speed nya kurang dari <= 5, biar bisa kena akurasiya sebelum kapal lawan
                teleport/afterburner
              Case2: Jarak lebih jauh -> deketin dulu sampe 40 baru tembak
              */
            
             int salvoCount = bot.TorpedoSalvoCount;
             if(getDistanceBetween(bot, enemyList.get(0)) <= 40){
                if(bot.size >= 20 && salvoCount > 3){
                    playerAction.heading = getHeadingBetween((enemyList.get(0)));
                    playerAction.action = PlayerActions.FIRETORPEDOES;
                    
                }
                
             } else {
                playerAction.action = PlayerActions.FORWARD;
             }
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
        Optional<GameObject> optionalBot = gameState.getPlayerGameObjects().stream().filter(gameObject -> gameObject.id.equals(bot.id)).findAny();
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

    private List <GameObject> nearestEnemyFromObject(GameObject x) {    
        List <GameObject> object = gameState.getPlayerGameObjects().stream()
            .filter(item-> item.id != x.id)
            .sorted(Comparator.comparing(item -> getDistanceBetween(x, item)))
            .collect(Collectors.toList());
        return object;
    }

    private List<GameObject> nearestObjectList (int n) {
        List <GameObject> object = gameState.getGameObjects().stream()
            .filter(item-> item.gameObjectType.value == n)
            .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item)))
            .collect(Collectors.toList());
        return object;
    }




    








}
