package pepisha.taches.explorer;

import java.util.ArrayList;

import pepisha.Constants;
import pepisha.WarExplorerBrainController;
import pepisha.taches.TacheAgent;
import edu.turtlekit3.warbot.agents.MovableWarAgent;
import edu.turtlekit3.warbot.agents.enums.WarAgentType;
import edu.turtlekit3.warbot.agents.percepts.WarPercept;
import edu.turtlekit3.warbot.brains.WarBrainController;

public class ChercherEnnemis extends TacheAgent {

	public ChercherEnnemis(WarBrainController b){
		super(b);
	}

	@Override
	public void exec() {		
		WarExplorerBrainController explorer = (WarExplorerBrainController) typeAgent;
		
		ArrayList<WarPercept> ennemis = explorer.getBrain().getPerceptsEnemies();
		
		if (ennemis != null && ennemis.size() > 0) {
			// On envoie le message aux rocket launcher
			explorer.getBrain().broadcastMessageToAgentType(WarAgentType.WarRocketLauncher, Constants.ennemyHere, (String[]) null);
		}
		else {
			explorer.getBrain().setRandomHeading(40);
		}
		
		explorer.setToReturn(MovableWarAgent.ACTION_MOVE);
	}

	@Override
	public String toString() {
		return "Tache Chercher Ennemis";
	}
	
	
}