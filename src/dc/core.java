package dc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class core extends Thread {

	public static void main(String[] args) {	
		//����SPUsim�߳�
		//new dc.SPUsimThread().start();
		//����core�߳�
		new core().start();
	}
	
	public long begintime, endtime ,lastendTime,lastTime=0,thisTime=0,SPUbt=0;
	
	public void run() {
		dc.ontology_process OP = new dc.ontology_process();
		sensor[] sensors = new sensor[5];
		boolean judge = false;
		String runMode;		
		double decisionResult=0.0;
		double RUNNING_RATE_TIME = 400;//ms
		
		File f=new File("C:\\Workspace\\eclipse\\DC\\ontology\\log.txt");

        FileOutputStream fileOutputStream;
		try {
			fileOutputStream = new FileOutputStream(f);
	        PrintStream printStream = new PrintStream(fileOutputStream);
	        System.setOut(printStream);
		} catch (FileNotFoundException e) {}
        
		coreInit();
		OP.callSPUsim();
		
	//��ʱѭ������
		while(true) 
		{
		if(costTime(SPUbt,endtime)/1000000 >=RUNNING_RATE_TIME) {
			OP.callSPUsim();
			SPUbt = System.nanoTime();
		}
		begintime = System.nanoTime();//System.nanoTime()�ӿ��������ھ��������룻System.currentTimeMillis()UTC����
		
		//�����ںϣ�Ŀ���ж�
		runMode=OP.RunMode();
		//runMode="TTB";
		switch (runMode) {
		case "WADD":
			sensors=OP.WADDreadOntology();
			judge = WADD(sensors,begintime,OP);
			
			break;
		case "TTB":
			sensors=OP.TTBreadOntology();
			judge = TTB(sensors,begintime,OP);
			break;
		case "EW":
			sensors=OP.WADDreadOntology();
			judge = EW(sensors,begintime,OP);
			break;
		case "WADDtoTTB":
			
			break;
		default:
			sensors=OP.WADDreadOntology();
			judge = WADD(sensors,begintime,OP);
			break;	
		}
		
		//д�뱾��,����(�ٶȣ��ϰ������),��ȡ���߽�������ؼ��ٶ�ֵ
		decisionResult = OP.getDecision(decisionResult);
		//Ŀ���ж�
		decisionResult = checkSum(judge,decisionResult,OP);
		
		//���㱾��������ʱ
		lastendTime = endtime;
		endtime = System.nanoTime();
				
		//��ȡ���߽�������㵱ǰ�ٶȡ�λ��
		OP.refreshDistance_simulation(thisTime,decisionResult);
		thisTime = costTime(lastendTime,endtime);
		System.out.print("\n" + " cost time:   "+thisTime/1000000.0 + "ms");
		//System.out.print("\n" + " acc:  "+ decisionResult);

		}//while���ѭ��
	}
	
	public void coreInit() {
		SPUbt = System.nanoTime();
		endtime = System.nanoTime();
	}
	//�����ʱ
	public long costTime(long begintime,long endtime) {
		return (endtime-begintime);// ns
	}
	
	//WADD,�߼�����,������ʷ��Ϣ  ���Ƕ���ʵ��
	public boolean WADD(sensor[] a,long begintime,dc.ontology_process OP) {
		double score=0;
		for(int i=0;i<a.length;i++) {
			score=score+(OP.calcWeight(a[i],begintime,"WADD")) * a[i].detected;
		}
		score = OP.WADDhistory(score);
		//System.out.print("\n" + " WADD Threshold:  "+50*a.length);
		//System.out.print("\n" + " WADD score:  "+ score);
		if(score>=50*a.length) return true;
		else return false;
	}
		
	//EW,�ͼ��������������Ӷ���
	public boolean EW(sensor[] a,long begintime,dc.ontology_process OP) {
		double score=0;
		for(int i=0;i<a.length;i++) {
			score=score+a[i].detected;
		}
		System.out.print("\n" + " EW score:  "+ score);
		if(score>=0.5*a.length) {return true;}
		else return false;
	}
		
	//TTB,�ͼ��������õ�ǰweight��ߵ�
	public boolean TTB(sensor[] a,long begintime,dc.ontology_process OP) {
		double score=0;double temp =0;int j=0;
		for(int i=0;i<a.length;i++) {
			temp=OP.calcWeight(a[i],begintime,"TTB");
			if(score<=temp) {score=temp;j=i;}
		}
		if(a[j].distance!=0)
		{return true;}
		else return false;
	}
	
	//�жϴ����������쳣������ͻ��+����Աȣ���������������ͻ�䲻�����ˣ�ͻ�����ò�ȷ���ԣ�������
	public double checkSum(boolean j ,double d, dc.ontology_process OP) {
		if (d==2019001) {
			if(j)
				return -18.0;
			else return 5.0;
		}
		if (d==2019002) {
			if(j)
				return -8.0;
			else return 5.0;
		}
		if (d==2019003) {
			if(j)
				return -18.0;
			else return 0.0;
		}
		if (d==2019003) {
			if(j)
				return -8.0;
			else return 0.0;
		}
		return d;		
	}
	
	//TODO  ,�����һ�������һ��������������Լ��
	public void targetJudge(sensor[] a) {
		
	}
}