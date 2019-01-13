package dc;
//读取SPU传入的本体数据，设定/更新权值，读取推理结果并返回

import dc.sensor; 
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.util.*;


public class ontology_process {
	
	public double wBCF,wBCR,wMWR,wLidar,wUR,XDistance=0.0,XVelocity=0.0,XAcc=0.0,RUNNING_RATE_TIME = 400; //  m/s
	public double[] history = new double[5];
	public OWLOntologyManager  OOM  = OWLManager.createOWLOntologyManager();
	public OWLDataFactory      DF  = OOM.getOWLDataFactory();
	public OWLOntology  myOntology ;
	public OWLEntityRemover remover;
	public sensor[] sensors = new sensor[5];
	public String base ;
	public OWLNamedIndividual Control,BCF,BCR,MWR,Lidar,UR,tBCF,tBCR,tMWR,tLidar,tUR,VCI;
	public OWLDataProperty   Distance,Velocity,Acceleration,refTime; //  m , m/s , m/s^2
	public OWLReasonerFactory RF = new Reasoner.ReasonerFactory();//调用HermiT 推理机
	public int count = 0;
	
	public ontology_process()  {
		sensors[0]= new sensor("BCF",0.0);	
		sensors[0].setSensor(85, 65, RUNNING_RATE_TIME*6000000);//6ms*RUNNING_RATE_TIME
		sensors[1]= new sensor("BCR",0.0);
		sensors[1].setSensor(85, 65, RUNNING_RATE_TIME*6000000);//6ms*
		sensors[2]= new sensor("MWR",0.0);
		sensors[2].setSensor(80, 70, RUNNING_RATE_TIME*1000000);//1ms*
		sensors[3]= new sensor("Lidar",0.0);
		sensors[3].setSensor(75, 60, RUNNING_RATE_TIME*2000000);//2ms*
		sensors[4]= new sensor("UR",0.0);
		sensors[4].setSensor(70, 70, RUNNING_RATE_TIME*1000000);//1ms*
		setIndividuals();
		System.out.print("\n ****OP init****");
	}
	
	public double calcWeight(sensor a,long begintime,String mod ) {		
		//double x=(double)(System.nanoTime()-begintime)/a.refreshCycle;
		double x=(double)(begintime-(long)a.refTime)/a.refreshCycle;
		if(x>=1.3 && x<=3.0) {
			//System.out.print("\n " +a.name+ "   x: " +x);
			callWarning(a,mod);
			return a.worstweight;
		}
		else if(x>3.0) {
			//System.out.print("\n " +a.name+ "   x: " +x);
			callSevereWarning(a,mod);
			return 50.0;
		}
		
		else return (a.worstweight-a.weight)*x+a.weight;
	}
	
	public void callWarning(sensor s,String mod) {
		System.out.print("\n" + s.name+" Waring: Engine calc too slow, running at mod:  " + mod);
	}
	
	public void callSevereWarning(sensor s,String mod) {
		System.out.print("\n" + s.name+" Severe Waring: Missed Sensors' input, running at mod:  " + mod);
	}
	
	public void callSPUsim() {		        	
    	File InFile = new File("C:\\Workspace\\eclipse\\DC\\ontology\\dc.owl");	 		
    	try {
    		myOntology = OOM.loadOntologyFromOntologyDocument(InFile);
		} catch (OWLOntologyCreationException e) {
			System.out.println("\n =====SPU Error=====");
			
		}		        	
    	remover = new OWLEntityRemover(OOM, Collections.singleton(myOntology));		        	
    	setTimerAcordingToTime(count);		        		     
    	//set distance Value
    	
    	
    	//
    	if(count<=4) {count++;}else {count=0;}
        OOM.removeOntology(myOntology);
    	System.out.println("\n ****SPU running****");	      
    }

	public void callReasoner(File f,OWLReasoner r,ArrayList<InferredAxiomGenerator<? extends OWLAxiom>> iag) {
		OWLOntology infOnt;
		try {
			infOnt = OOM.createOntology();
			InferredOntologyGenerator IOG = new InferredOntologyGenerator(r, iag);
	     	IOG.fillOntology(OOM, infOnt);
	     	OOM.saveOntology(infOnt,IRI.create(f));
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}

	public double callGetresult(File f,double lastDecisionResult) {
		try {
			OWLOntology Inquire = OOM.loadOntologyFromOntologyDocument(f);
			Map<OWLObjectPropertyExpression, Set<OWLIndividual>> infOP=Control.getObjectPropertyValues(Inquire);
			//Decision = infOP.toString().replaceAll(base+"#", "");	
			
			//同时有break和up说明有传感器故障
			if (infOP.toString().indexOf("#OPSpeedBreak>=[<"+base+"#Control>]")!=-1){
				
				if (infOP.toString().indexOf("#OPSpeedUP>=[<"+base+"#Control>]")!=-1) {
					OOM.removeOntology(Inquire);
		        	f.delete();
		        	return 2019001;//to use WADD or ...
				}
				else {
					if(infOP.toString().indexOf("#OPSpeedKeep>=[<"+base+"#Control>]")!=-1) {
						OOM.removeOntology(Inquire);
						f.delete();
						return 2019003;
					}
					//only break
					OOM.removeOntology(Inquire);
					f.delete();
					return -18.0;}
				}
			
			//no break,同时有speedup和down说明有传感器故障
			if (infOP.toString().indexOf("#OPSpeedDown>=[<"+base+"#Control>]")!=-1){
				
				if (infOP.toString().indexOf("#OPSpeedUP>=[<"+base+"#Control>]")!=-1) {
					OOM.removeOntology(Inquire);
		        	f.delete();
		        	return 2019002;//to use WADD or ...
				}
				else {
					if (infOP.toString().indexOf("#OPSpeedKeep>=[<"+base+"#Control>]")!=-1) {
						OOM.removeOntology(Inquire);
			        	f.delete();
			        	return 2019004;//to use WADD or ...
					}
					//only down
					OOM.removeOntology(Inquire);
					f.delete();
					return -8.0;}
				}

			//no break or down , only keep
			if (infOP.toString().indexOf("#OPSpeedKeep>=[<"+base+"#Control>]")!=-1){
	        	OOM.removeOntology(Inquire);f.delete();
	        	return 0.0;}
			//only speedup
			if (infOP.toString().indexOf("#OPSpeedUP>=[<"+base+"#Control>]")!=-1){
	        	OOM.removeOntology(Inquire);f.delete();
	        	return 5.0;}	        
		} catch (OWLOntologyCreationException e) {
			System.out.print("\n !!callGetresult: Ontology Creation Error!!");
			return -8.0;
		}
		return lastDecisionResult;
	}
	
	public String RunMode() {
		
		if(XVelocity>=60.0) return "WADD";//"WADDtoTTB";
		
		else return "WADD";
	}

	public void setIndividuals() {
		File InFile = new File("C:\\Workspace\\eclipse\\DC\\ontology\\dc.owl");
		try {
			myOntology  = OOM.loadOntologyFromOntologyDocument(InFile);
		} catch (OWLOntologyCreationException e) {
			System.out.print("\n !!OP.init Read Ontology Error!!");
		}
		base = getOntologyIRI(myOntology.toString());
		remover = new OWLEntityRemover(OOM, Collections.singleton(myOntology));
		Distance= DF.getOWLDataProperty(IRI.create(base+"#Distance"));
		Velocity= DF.getOWLDataProperty(IRI.create(base+"#Velocity"));
		Acceleration= DF.getOWLDataProperty(IRI.create(base+"#Acceleration"));
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
		VCI		= DF.getOWLNamedIndividual(IRI.create(base + "#VCI"));
		refTime = DF.getOWLDataProperty(IRI.create(base+"#refTime"));
		Control	= DF.getOWLNamedIndividual(IRI.create(base + "#Control"));
		
		OOM.removeOntology(myOntology);
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
	OWLDataPropertyAssertionAxiom setTimer = DF.getOWLDataPropertyAssertionAxiom(refTime, t,(double)System.nanoTime());
 	AddAxiom    addAxiom    = new AddAxiom(myOntology, setTimer);
 	OOM.applyChange(addAxiom);
 	OOM.applyChanges(remover.getChanges());
 	try {
 		OOM.saveOntology(myOntology);
 		} catch (OWLOntologyStorageException e) {
 			System.out.print("\n !! failed to set Timer" + t);
 		}
	}
	
	public void setVelocity(OWLEntityRemover r,OWLNamedIndividual t,double v) {
	r.visit(t);
	OWLDataPropertyAssertionAxiom setTimer = DF.getOWLDataPropertyAssertionAxiom(Velocity,t,v);
 	AddAxiom    addAxiom    = new AddAxiom(myOntology, setTimer);
 	OOM.applyChange(addAxiom);
 	OOM.applyChanges(r.getChanges());
 	try {
 		OOM.saveOntology(myOntology);
 		} catch (OWLOntologyStorageException e) {
 			System.out.print("\n !! failed to set VCI");
 		}
	}

	public sensor[] WADDreadOntology()   {		
		File InFile = new File("C:\\Workspace\\eclipse\\DC\\ontology\\dc.owl");
		try {
			myOntology  = OOM.loadOntologyFromOntologyDocument(InFile);
		} catch (OWLOntologyCreationException e) {
			System.out.print("\n !!WADD read: Read Ontology Error!!");
		}	
		
		if((sensors[0].distance=getDataValue(BCF,myOntology))!=0) {sensors[0].detected=1;}								
		if((sensors[1].distance=getDataValue(BCR,myOntology))!=0) {sensors[1].detected=1;}	
		if((sensors[2].distance=getDataValue(MWR,myOntology))!=0) {sensors[2].detected=1;}
		if((sensors[3].distance=getDataValue(Lidar,myOntology))!=0) {sensors[3].detected=1;}
		if((sensors[4].distance=getDataValue(UR,myOntology))!=0) {sensors[4].detected=1;}
		sensors[0].refTime=getDataValueT(tBCF,myOntology);
		sensors[1].refTime=getDataValueT(tBCR,myOntology);
		sensors[2].refTime=getDataValueT(tMWR,myOntology);
		sensors[3].refTime=getDataValueT(tLidar,myOntology);
		sensors[4].refTime=getDataValueT(tUR,myOntology);

		OOM.removeOntology(myOntology);
		return sensors;
	}
	
	public double WADDhistory(double score) {
		for(int i=0;i<4;i++) {
			history[i]=history[i+1];
		}
		history[4]=score;
		return (score+history[0]*0.05+history[1]*0.1+history[2]*0.15+history[3]*0.2)/1.5;
	}
	
	//unfinished
	public sensor[] TTBreadOntology()   {
		File InFile = new File("C:\\Workspace\\eclipse\\DC\\ontology\\dc.owl");
		try {
			myOntology  = OOM.loadOntologyFromOntologyDocument(InFile);
		} catch (OWLOntologyCreationException e) {}	
		
		sensors[0].distance=getDataValue(BCF,myOntology);								
		sensors[1].distance=getDataValue(BCR,myOntology);	
		sensors[2].distance=getDataValue(MWR,myOntology);
		sensors[3].distance=getDataValue(Lidar,myOntology);
		sensors[4].distance=getDataValue(UR,myOntology);

		sensors[0].refTime=getDataValueT(tBCF,myOntology);
		sensors[1].refTime=getDataValueT(tBCR,myOntology);
		sensors[2].refTime=getDataValueT(tMWR,myOntology);
		sensors[3].refTime=getDataValueT(tLidar,myOntology);
		sensors[4].refTime=getDataValueT(tUR,myOntology);
		
		OOM.removeOntology(myOntology);
		return sensors;
	}
	
	public String getOntologyIRI(String basetemp) {
		//注意本体是否开启了versionIRI，开启了则需要处理掉versionIRI
		String tempStr="";
		Boolean tempBool = false;
		if(basetemp != null && !"".equals(basetemp))
		{
			for(int i=0;i<basetemp.length();i++)
			{
				if(basetemp.charAt(i)==60) {tempBool = true;i=i+1;}
				if(basetemp.charAt(i)==62) {tempBool = false;}
				if(tempBool==true)
					{
					tempStr+=basetemp.charAt(i);
					}	
			}
		}
		return tempStr;
	}
	
	public double getDataValue(OWLNamedIndividual Indi ,OWLOntology  myOntology) {
		Map<OWLDataPropertyExpression, Set<OWLLiteral>> ODPV=Indi.getDataPropertyValues(myOntology);
		String StringODPV = ODPV.toString();
		String tempStr="";
		Boolean tempBool = false;
		//System.out.print("\n "+Indi+"\n  :"+StringODPV);
		if(StringODPV != null && !"".equals(StringODPV))
		{
			for(int i=0;i<StringODPV.length();i++)
			{
				if(StringODPV.charAt(i)==34 ) {
					if(tempBool==false) {tempBool = true;}
					else break;
					}
				if(StringODPV.charAt(i)>=48 && StringODPV.charAt(i)<=57 && tempBool==true)
					{
						tempStr+=StringODPV.charAt(i);
					}
				if(StringODPV.charAt(i)==46  && tempBool==true)
					{
						tempStr+=StringODPV.charAt(i);
					}
			}				
		}
		if(tempStr.isEmpty()) {return 0.0;}
		else {
			return Double.valueOf(tempStr);}
	}
	
	public double getDataValueT(OWLNamedIndividual Indi,OWLOntology myOntology ) {
		Map<OWLDataPropertyExpression, Set<OWLLiteral>> ODPV=Indi.getDataPropertyValues(myOntology);
		String StringODPV = ODPV.toString();
		String tempStr="",tempStrE="";
		Boolean tempBool = false;
		if(StringODPV != null && !"".equals(StringODPV))
		{
			for(int i=0;i<StringODPV.length();i++)
			{
				if(StringODPV.charAt(i)==34 ) {
					if(tempBool==false) {tempBool = true;}
					else break;
					}
				if(StringODPV.charAt(i)>=48 && StringODPV.charAt(i)<=57 && tempBool==true)
					{
						tempStr+=StringODPV.charAt(i);
					}
				if(StringODPV.charAt(i)==46 && tempBool==true)
				{
					tempStr+=StringODPV.charAt(i);
				}
				if(StringODPV.charAt(i)==69 && tempBool==true) {
					tempStrE = tempStrE + StringODPV.charAt(i+1) + StringODPV.charAt(i+2);
					break;
					}
			}				
		}
		if(tempStr.isEmpty()) {return 0.0;}
		else {
			return Double.valueOf(tempStr) * Math.pow(10, Double.valueOf(tempStrE));
			}
	}
		
	public double getDecision(double lastDecisionResult) {
		File InFile = new File("C:\\Workspace\\eclipse\\DC\\ontology\\dc.owl");
		File tempFile = new File("C:\\Workspace\\eclipse\\DC\\ontology\\temp.owl");
		try {
			myOntology  = OOM.loadOntologyFromOntologyDocument(InFile);
		} catch (OWLOntologyCreationException e) {
			System.out.print("\n !!getDecision: Read Ontology Error!!");
			return callGetresult(tempFile,lastDecisionResult);
		}
		
		OWLReasoner reasoner = RF.createNonBufferingReasoner(myOntology);
		reasoner.precomputeInferences();//Infer All 
		ArrayList<InferredAxiomGenerator<? extends OWLAxiom>> IAG = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
		//推理结果导出
	    IAG.add(new InferredPropertyAssertionGenerator());
	    //推理结果导出
	    callReasoner(tempFile,reasoner,IAG);
	    //读取推理结果
	    OOM.removeOntology(myOntology);
	    return callGetresult(tempFile,lastDecisionResult);

	}
	
	//remover 的初始化一定要在读取文件之后
	public void refreshDistance_simulation(long lastTime, double decisionResult) {
		//lastTimes ns
		//XDistance m  XVelocity  !<km/h>!  XAcc m/s^2
		File InFile = new File("C:\\Workspace\\eclipse\\DC\\ontology\\dc.owl");
		try {
			myOntology  = OOM.loadOntologyFromOntologyDocument(InFile);
		} catch (OWLOntologyCreationException e) {
			System.out.print("\n !!refreshSim: Read Ontology Error!!");
		}
		
		OWLEntityRemover removerRef = new OWLEntityRemover(OOM, Collections.singleton(myOntology));
		
		double t = (double)lastTime/1000000000;
		XDistance = XDistance + XVelocity/3.6 * t+0.5*XAcc*t*t; 
		XVelocity = XVelocity + XAcc * t  * 3.6;
		
		XAcc = decisionResult;
		
		System.out.print("\n speed : "+ XVelocity+"km/h");
		System.out.print("\n position : "+ XDistance+"m");
		
		setVelocity(removerRef,VCI,XVelocity);

		OOM.removeOntology(myOntology);
	}
		
}
