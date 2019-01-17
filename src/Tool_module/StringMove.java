package Tool_module;

public class StringMove {
	//��������nλ�ַ�������	
	public static String moveToLeft(String str,int position) {		
		String str1=str.substring(position);		
		String str2=str.substring(0, position);		
		return str1+str2;	
	}	
	
	//��������nλ�ַ�������		
	public static String moveToRight(String str,int position) {
		String str1=str.substring(str.length()-position);			
		String str2=str.substring(0, str.length()-position);			
		return str1+str2;		
	}
	
	public static String hash_xor(String x,String y) {
		
		String t = "";
		for(int i=0;i<20;i++)
			t += xor(x.substring(i, i+1), y);
		return t+x.substring(x.length()-10);
	}

	public static String xor(String strHex_X,String strHex_Y){ 
			//��x��yת�ɶ�������ʽ 
			char A[] = strHex_X.toCharArray();
			char B[] = strHex_Y.toCharArray();
			String anotherBinary=Integer.toBinaryString(A[0]); 
			String thisBinary=Integer.toBinaryString(B[0]); 
			String result = ""; 

			//�ж��Ƿ�Ϊ8λ�����ƣ��������� 			
			if(anotherBinary.length() != 8){ 			
				for (int i = anotherBinary.length(); i <8; i++) { 					
					anotherBinary = "0"+anotherBinary; 				
				} 			
			} 			
			if(thisBinary.length() != 8){
				for (int i = thisBinary.length(); i <8; i++) { 	
					thisBinary = "0"+thisBinary; 				
					} 			
			} 
			
			//������� 
			for(int i=0;i<anotherBinary.length();i++){ 
			//�����ͬλ������ͬ����0������1 
				if(thisBinary.charAt(i)==anotherBinary.charAt(i)) 
					result+="0"; 
				else{ 
					result+="1"; 
				} 
			}
			return Integer.toHexString(Integer.parseInt(result, 2)); 
		}
}
