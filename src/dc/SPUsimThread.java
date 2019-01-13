package dc;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.OWLEntityRemover;


public class SPUsimThread extends Thread{

	public OWLOntologyManager  OOM = OWLManager.createOWLOntologyManager();
	public OWLDataFactory      DF  = OOM.getOWLDataFactory();
	public OWLOntology  myOntology;
	public OWLClass classTimer;
	public OWLNamedIndividual tBCF,tBCR,tMWR,tLidar,tUR,VCI,BCF,BCR,MWR,Lidar,UR,XDis;
	public String base = "http://www.semanticweb.org/octopus/ontologies/2018/dc";
	public OWLDataProperty     	refTime ;
	public File InFile;
	public OWLEntityRemover 	remover;
	public int count=0;
	public double velocity=0.0;
	public static int RUNNING_RATE_TIME = 400;
	
	
	public void run() {
		//存在初始化太慢而报错问题，影响不大。
		Timer timer = new Timer();
		
		setClassTimerAndIndividuals();
		
		timer.schedule(new TimerTask() {
			
		        public void run() {		        	
		        	InFile = new File("C:\\Workspace\\eclipse\\DC\\ontology\\dc.owl");		        				        			        	
		        		
		        	try {
		        		myOntology = OOM.loadOntologyFromOntologyDocument(InFile);
					} catch (OWLOntologyCreationException e) {
						System.out.println("\n =====SPU Error=====");
						
					}		        	
		        	remover = new OWLEntityRemover(OOM, Collections.singleton(myOntology));		        	
		        	setTimerAcordingToTime(count);		        		     
		        	//TODO set distance Value
		        	
		        	
		        	if(count<=4) {count++;}else {count=0;}
		            OOM.removeOntology(myOntology);
		        	System.out.println("\n *****SPU running*****");	
		            
		        }
				
		        public void setTimerAcordingToTime(int c) {
					switch (c) {
		        	case 0:	        			        			      
			        	setClassTimer(tBCF);	        			        			      
			        	setClassTimer(tBCR);		        			        			      
			        	setClassTimer(tLidar);
		        		break;
		        	case 2:		        			        			      
			        	setClassTimer(tLidar);
			        	break;
		        	case 4:		        			        			      
			        	setClassTimer(tLidar);
			        	break;
			        default:
			        		break;
		        	}
		       		        			        			      
		        	setClassTimer(tUR);		        			        			      
		        	setClassTimer(tMWR);		        	
				};
				
				public void setClassTimer(OWLNamedIndividual t) {
					remover.visit(t);
					OOM.applyChanges(remover.getChanges());
					
					OWLDataPropertyAssertionAxiom setTimer = DF.getOWLDataPropertyAssertionAxiom(refTime, t,(double)System.nanoTime());
		        	AddAxiom    addAxiom    = new AddAxiom(myOntology, setTimer);
		        	OOM.applyChange(addAxiom);
		        	
		        	OWLClassAssertionAxiom classAssertionTimer = DF.getOWLClassAssertionAxiom(classTimer, t);
		        	OOM.addAxiom(myOntology,classAssertionTimer);
		        	
		        	try {OOM.saveOntology(myOntology);
		        	} catch (OWLOntologyStorageException e) {}
				}
				
		}, 5 , RUNNING_RATE_TIME);//5ms后启动，200ms执行一次		
	}
	
	public void setClassTimerAndIndividuals() {
		tBCF 	= DF.getOWLNamedIndividual(IRI.create(base + "#TimerBCF"));
		tBCR 	= DF.getOWLNamedIndividual(IRI.create(base + "#TimerBCR"));
		tMWR 	= DF.getOWLNamedIndividual(IRI.create(base + "#TimerMWR"));
		tLidar 	= DF.getOWLNamedIndividual(IRI.create(base + "#TimerLidar"));
		tUR 	= DF.getOWLNamedIndividual(IRI.create(base + "#TimerUR"));
		BCF 	= DF.getOWLNamedIndividual(IRI.create(base + "#BinocularCameraFront"));
		BCR 	= DF.getOWLNamedIndividual(IRI.create(base + "#BinocularCameraRight"));
		MWR 	= DF.getOWLNamedIndividual(IRI.create(base + "#MillimeterWaveRadar"));
		Lidar 	= DF.getOWLNamedIndividual(IRI.create(base + "#Lidar"));
		UR 		= DF.getOWLNamedIndividual(IRI.create(base + "#UltrasonicRadar"));
		XDis 	= DF.getOWLNamedIndividual(IRI.create(base + "#XDistance"));
		refTime = DF.getOWLDataProperty(IRI.create(base+"#refTime"));
        classTimer = DF.getOWLClass(IRI.create(base+"#Timer"));

	}

	
}
