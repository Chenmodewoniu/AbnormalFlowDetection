package Process_module;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;

import Tool_module.HashUtil;

public class Filter {
	
	public double SamplingProbability;
	private boolean[] BloomFilter = new boolean[(int)Math.pow(2,16)];
	private String[] Hash = new String[]{"MD5","SHA1"};
	public ArrayList<String> Statistics = new ArrayList<String>(); //ͳ�Ƶı���
	public Filter(double p) {
		SamplingProbability = p;
		for(int i=0;i<BloomFilter.length;i++)
			BloomFilter[i] = false;
	}
	
	public boolean BloomJudge(String a) {
		int hashresult[] = new int[Hash.length];
		for(int i=0;i<Hash.length;i++) {
			String t = HashUtil.hash(a, Hash[i]);
			hashresult[i] = Integer.parseInt(t.substring(0, 4),16);
		}
		boolean flag = true;
		for(int i=0;i<hashresult.length;i++) 
			if(!BloomFilter[hashresult[i]]) {flag = false;break;}
		if(flag) { //�Ѵ���
			//�������ģ��
			return true;
		}
		else { //��δ����
			if(Math.random() < SamplingProbability) {//������
				Statistics.add(a);
				for(int i=0;i<hashresult.length;i++) {
					BloomFilter[hashresult[i]] = true;
				}
				return true;
			}
			else {
				return false;
			}
		}
	}
}
