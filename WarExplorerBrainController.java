package pepisha;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

import pepisha.taches.TacheAgent;
import pepisha.taches.explorer.ChercherEnnemis;
import pepisha.taches.explorer.ChercherNourriture;
import pepisha.taches.explorer.LocaliserBase;
import edu.turtlekit3.warbot.agents.ControllableWarAgent;
import edu.turtlekit3.warbot.agents.MovableWarAgent;
import edu.turtlekit3.warbot.agents.agents.WarBase;
import edu.turtlekit3.warbot.agents.agents.WarExplorer;
import edu.turtlekit3.warbot.agents.enums.WarAgentType;
import edu.turtlekit3.warbot.agents.percepts.WarPercept;
import edu.turtlekit3.warbot.brains.braincontrollers.WarExplorerAbstractBrainController;
import edu.turtlekit3.warbot.communications.WarMessage;
import edu.turtlekit3.warbot.tools.CoordPolar;


public class WarExplorerBrainController extends WarExplorerAbstractBrainController
{	
	/** Attributs **/
	
	// Action de l'explorer à retourner
	private String toReturn;
	
	// Tache courante
	private TacheAgent tacheCourante;
	
	// Distance de la nourriture indiquée par d'autres agents
	private double distance = 0.0;
	
	// Mode de l'explorer
	private boolean cueilleur;
	
	// Pour revenir vers la nourriture (chemin)
	private double distanceLastFood;
	
	// Liste de messages
	private ArrayList<WarMessage> messages;
	
	
	/**
	 * Constructeur
	 */
	public WarExplorerBrainController() {
		super();
		tacheCourante = new ChercherNourriture(this);
		cueilleur = true;
	}
		
	
	public ArrayList<WarMessage> getListeMessages() {
		return this.messages;
	}
	
	public boolean estCueilleur() {
		return this.cueilleur;
	}
	
	/**
	 * @action change le toReturn
	 * */
	public void setToReturn(String nvReturn){
		toReturn=nvReturn;
	}
	
	public void setTacheCourante(TacheAgent nvTache){
		tacheCourante=nvTache;
	}
	
	public double getDistance(){
		return distance;
	}
	
	public void setDistance(double nvDistance){
		distance=nvDistance;
	}
	
	public double getDistanceLastFood(){
		return distanceLastFood;
	}
	
	public void setDistanceLastFood(double distance){
		distanceLastFood=distance;
	}
	
	
	/**
	 * @action Définit le comportement de l'explorer
	 * @return Action à effectuer (move,take etc...)
	 */
	public String action() 
	{	
		toReturn = null;
		
		this.messages = getBrain().getMessages();
		
		if (getBrain().isBlocked())
			getBrain().setRandomHeading();
		
		doReflex();
		
		getBrain().setDebugStringColor(Color.black);
		getBrain().setDebugString(tacheCourante.toString());
		
		if(toReturn == null)
			tacheCourante.exec();

		if(toReturn == null) {
			return WarExplorer.ACTION_MOVE;
		}
		
		return toReturn;
	}
	
	
	/**
	 * @action Définit l'ensemble des réflèxes de l'agent
	 */
	private void doReflex()
	{	
		changeComportement();
		
		perceptEnemyBase();
		
		groupEnemyHere();
		
		perceptFood();
		
		imAlive();
	}
	
	
	/**
	 * @action Vérifie les messages de la base et change de comportement
	 * 			si demandé.
	 */
	private void changeComportement()
	{
		for (WarMessage m : messages)
		{
			if (m.getMessage().equals("cueille")) {
				if (!cueilleur) {
					cueilleur = true;
					tacheCourante = new ChercherNourriture(this);
				}
			}
			else if (m.getMessage().equals(Constants.noEspion)) {
				if (cueilleur) {
					cueilleur = false;
					tacheCourante = new ChercherEnnemis(this);
				}
			}
		}
	}
	
	
	/**
	 * @action Prévient la base que l'agent est encore vivant
	 */
	private void imAlive()
	{
		if (estCueilleur()) {
			getBrain().broadcastMessageToAgentType(WarAgentType.WarBase, Constants.imAlive, "c");
		} else {
			getBrain().broadcastMessageToAgentType(WarAgentType.WarBase, Constants.imAlive, "e");
		}
	}
	
	
	/**
	 * @action Envoie un message aux autres agents s'il perçoit la base ennemie
	 */
	private void perceptEnemyBase()
	{
		ArrayList<WarPercept> basesEnnemies = getBrain().getPerceptsEnemiesByType(WarAgentType.WarBase);
		
		if (basesEnnemies != null && basesEnnemies.size() > 0)
		{	
			WarPercept base = basesEnnemies.get(0);
			
			// On envoie la position de la base ennemie
			getBrain().broadcastMessageToAll(Constants.enemyBaseHere, String.valueOf(base.getDistance()), String.valueOf(base.getAngle()));
		
			// Si c'est un espion, on tourne
			if (!estCueilleur()) {
				getBrain().setHeading(base.getAngle() + 45);
				setDistance(base.getDistance());
				setTacheCourante(new LocaliserBase(this));
			}
		}
	}
	
	
	/**
	 * @action S'il y a un groupe d'ennemis dans le percept, prévient le kamikaze
	 */
	private void groupEnemyHere()
	{
		ArrayList<WarPercept> ennemisTourelles = getBrain().getPerceptsEnemiesByType(WarAgentType.WarTurret);
		ArrayList<WarPercept> ennemisRockets = getBrain().getPerceptsEnemiesByType(WarAgentType.WarRocketLauncher);
		
		if (ennemisRockets != null && ennemisRockets.size() >= Constants.NB_MIN_ROCKETS_TO_KILL){
			getBrain().broadcastMessageToAgentType(WarAgentType.WarKamikaze, Constants.groupEnemyHere);
		}
		else if (ennemisTourelles != null && ennemisTourelles.size() >= Constants.NB_MIN_TURRET_TO_KILL){
			getBrain().broadcastMessageToAgentType(WarAgentType.WarKamikaze, Constants.groupEnemyHere);
		}
	}
	
	
	/**
	 * @action Envoie un message aux autres explorers pour avertir qu'il y a de la nourriutre
	 */
	private void perceptFood()
	{
		ArrayList<WarPercept> nourriture = getBrain().getPerceptsResources();
		
		if (nourriture != null && nourriture.size() > 0)
		{
			distanceLastFood = 0;
			WarPercept food = nourriture.get(0);
			
			// On envoie un message aux autres explorers et ingénieurs pour dire qu'il y a de la nourriture
			getBrain().broadcastMessageToAgentType(WarAgentType.WarExplorer, Constants.foodHere,
					String.valueOf(food.getDistance()), String.valueOf(food.getAngle()));
			getBrain().broadcastMessageToAgentType(WarAgentType.WarEngineer, Constants.foodHere,
					String.valueOf(food.getDistance()), String.valueOf(food.getAngle()));
		}
	}
}
