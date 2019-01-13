package dc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class core extends Thread {

	public static void main(String[] args) {	
		//启动SPUsim线程
		//new dc.SPUsimThread().start();
		//启动core线程
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
		
	//计时循环进行
		while(true) 
		{
		if(costTime(SPUbt,endtime)/1000000 >=RUNNING_RATE_TIME) {
			OP.callSPUsim();
			SPUbt = System.nanoTime();
		}
		begintime = System.nanoTime();//System.nanoTime()从开机到现在经过的纳秒；System.currentTimeMillis()UTC毫秒
		
		//传感融合，目标判断
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
		
		//写入本体,决策(速度，障碍物距离),读取决策结果，返回加速度值
		decisionResult = OP.getDecision(decisionResult);
		//目标判断
		decisionResult = checkSum(judge,decisionResult,OP);
		
		//计算本次运行用时
		lastendTime = endtime;
		endtime = System.nanoTime();
				
		//读取决策结果，计算当前速度、位置
		OP.refreshDistance_simulation(thisTime,decisionResult);
		thisTime = costTime(lastendTime,endtime);
		System.out.print("\n" + " cost time:   "+thisTime/1000000.0 + "ms");
		//System.out.print("\n" + " acc:  "+ decisionResult);

		}//while疯狂循环
	}
	
	public void coreInit() {
		SPUbt = System.nanoTime();
		endtime = System.nanoTime();
	}
	//计算耗时
	public long costTime(long begintime,long endtime) {
		return (endtime-begintime);// ns
	}
	
	//WADD,高计算量,考虑历史信息  考虑队列实现
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
		
	//EW,低计算量，少数服从多数
	public boolean EW(sensor[] a,long begintime,dc.ontology_process OP) {
		double score=0;
		for(int i=0;i<a.length;i++) {
			score=score+a[i].detected;
		}
		System.out.print("\n" + " EW score:  "+ score);
		if(score>=0.5*a.length) {return true;}
		else return false;
	}
		
	//TTB,低计算量，用当前weight最高的
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
	
	//判断传感器数据异常：数据突变+横向对比，横向用推理解决，突变不考虑了：突变则用不确定性，否则不用
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
	
	//TODO  ,结果用一个个体和一个对象属性来做约束
	public void targetJudge(sensor[] a) {
		
	}
}