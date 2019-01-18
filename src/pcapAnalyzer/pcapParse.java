package pcapAnalyzer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.johnnie.pcapanalyzer.utils.DataUtils;
import com.johnnie.pcapanalyzer.utils.FileUtils;
import com.johnnie.pcapanalyzer.utils.LogUtils;

public class pcapParse {
	
	private File pcap;
	private File savePath;

	private PcapStruct struct;
	private ProtocolData protocolData;
	private IPHeader ipHeader;
	private Port portHeader;
	
	private double limit=0.03;
	private Map<String,Integer> protocalmap = new HashMap<String,Integer>();
	
	private byte[] file_header = new byte[24];
	private byte[] data_header = new byte[16];
	private byte[] content;
	public ArrayList<String> simplified_packages = new ArrayList<String>();
	public ArrayList<String> limit_packages = new ArrayList<String>(); 
	
	public pcapParse (File pcap, File outpath) {
		this.pcap = pcap;
		this.savePath = outpath;
	}
	
	public boolean parse () {
		boolean rs = true;
		struct = new PcapStruct();
		//List<PcapDataHeader> dataHeaders = new ArrayList<PcapDataHeader>();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(pcap);
			int m = fis.read(file_header);
			if (m > 0) {

				PcapFileHeader fileHeader = parseFileHeader(file_header);
				
				if (fileHeader == null) {
					LogUtils.printObj("fileHeader", "null");
				}
				struct.setFileHeader(fileHeader);

				while (m > 0) {
					m = fis.read(data_header);
					PcapDataHeader dataHeader = parseDataHeader(data_header);
					//dataHeaders.add(dataHeader); 

					content = new byte[dataHeader.getCaplen()];
//					LogUtils.printObj("content.length", content.length);
					m = fis.read(content);

					protocolData = new ProtocolData();
					boolean isDone = parseContent();
					if (isDone) {
						break;
					}
//					LogUtils.printObjInfo(protocolData);
//					LogUtils.printObj("--------------------------------------");
				}

				rs = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			rs = false;
		} finally {
			FileUtils.closeStream(fis, null);
		}
		if(rs) writetofile();
		return rs;
	}
	
	private void writetofile() {
		
		//����
		//���ｫmap.entrySet()ת����list
        List<Map.Entry<String,Integer>> list = new ArrayList<Map.Entry<String,Integer>>(protocalmap.entrySet());
        //Ȼ��ͨ���Ƚ�����ʵ������
        Collections.sort(list,new Comparator<Map.Entry<String,Integer>>() {
            //��������
            public int compare(Entry<String,Integer> o1,
                    Entry<String, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
		
		
		try {
			FileOutputStream outStream = new FileOutputStream(savePath);
			String str = "";
	        for(Map.Entry<String,Integer> mapping:list){
	        	str += mapping.getKey() + ": " + mapping.getValue() + "\r\n";
	        	if(mapping.getValue() >= simplified_packages.size()*limit) {
	        		limit_packages.add(mapping.getKey());
	        	}
	        }
			byte re[] = str.getBytes();
			outStream.write(re);
			outStream.close();
			System.out.println("successful");
		}
		catch(Exception e){
			e.printStackTrace();
		}

	}
	
	private void addProtocalData(ProtocolData protocolData) {
		String filename =  "[" + protocolData.getSrcIP() + "]"
			     + "[" + protocolData.getDesIP() + "]"
			     + "[" + protocolData.getProtocolType() + "]";
		filename = protocolData.getDesIP() + " " + protocolData.getProtocolType();  //�˿�ɨ��
		String reverseFilename =  "[" + protocolData.getProtocolType() + "]"
								+ "[" + protocolData.getDesIP() + "]"
								+ "[" + protocolData.getSrcIP() + "]";
		// �ж��Ƿ��Ѿ���������Ԫ��
		if ( protocalmap.containsKey(filename)) {
			int value = protocalmap.get(filename);
			protocalmap.put(filename,value+1);
		//	LogUtils.printObj(filename + "�Ѵ���...");
		} else {
		//	LogUtils.printObj(filename + "������...");
			
			// ��ԴIP��ԴPort��Ŀ��IP��Ŀ��Port ����˳�򣬲鿴���ļ��Ƿ���ڣ������ڣ���׷��
			
			protocalmap.put(filename,1);
			}
	}
	
	private Port readPort(byte[] content) {
		int offset = 14+20;
		
		Port ip = new Port();
		protocolData.setSrcPort((short)0);
		protocolData.setDesPort((short)0);
		protocolData.setDateLength((short)(ipHeader.getTotalLen() - 20));
		protocolData.setSyn(false);
		
		byte[] buff_src = new byte[4];
		byte[] buff_dst = new byte[4];
		if(ipHeader.getProtocol() == 17 || ipHeader.getProtocol() == 6) {
			buff_src[0] = 0;buff_src[1] = 0;buff_dst[0] = 0;buff_dst[1] = 0;
			for(int i=0;i<2;i++) {
				buff_src[i+2] = content[offset+i];
			}
			for(int i=0;i<2;i++) {
				buff_dst[i+2] = content[offset+i+2];
			}
			//ip.setsrcport(DataUtils.byteArrayToInt(buff_src));
			//ip.setdstport(DataUtils.byteArrayToInt(buff_dst));
			protocolData.setSrcPort(DataUtils.byteArrayToInt(buff_src));
			protocolData.setDesPort(DataUtils.byteArrayToInt(buff_dst));
			
			if(ipHeader.getProtocol() == 17) {
				protocolData.setDateLength((short)(ipHeader.getTotalLen() -20-8));
			}
			else {
				byte syn = content[offset+13];
				byte[] headlength = new byte[2];headlength[0] = 0;headlength[1] = content[offset+12];
				if(syn == 2 || syn == 17) protocolData.setSyn(true);
				short len = DataUtils.byteArrayToShort(headlength);
				protocolData.setDateLength((short)(ipHeader.getTotalLen() -20- len/4));
			}
		}
		return ip;
	}
	
	private IPHeader readIPHeader(byte[] content) {
		int offset = 14;
		IPHeader ip = new IPHeader();

		byte[] buff_2 = new byte[2];
		byte[] buff_4 = new byte[4];

		byte varHLen = content[offset ++];				// offset = 15
//		LogUtils.printByteToBinaryStr("varHLen", varHLen);
		if (varHLen == 0) {
			return null;
		}
		
		ip.setVarHLen(varHLen);

		byte tos = content[offset ++];					// offset = 16
		ip.setTos(tos);

		for (int i = 0; i < 2; i ++) {		
			buff_2[i] = content[i + offset];
		}
		offset += 2;									// offset = 18
		short totalLen = DataUtils.byteArrayToShort(buff_2);
		ip.setTotalLen(totalLen);

		for (int i = 0; i < 2; i ++) {			
			buff_2[i] = content[i + offset];
		}
		offset += 2;									// offset = 20
		short id = DataUtils.byteArrayToShort(buff_2);
		ip.setId(id);

		for (int i = 0; i < 2; i ++) {					
			buff_2[i] = content[i + offset];
		}
		offset += 2;									// offset = 22
		short flagSegment = DataUtils.byteArrayToShort(buff_2);
		ip.setFlagSegment(flagSegment);

		byte ttl = content[offset ++];					// offset = 23
		ip.setTtl(ttl);

		byte protocol = content[offset ++];				// offset = 24
		ip.setProtocol(protocol);
		protocolData.setProtocolType(protocol);

		for (int i = 0; i < 2; i ++) {					
			buff_2[i] = content[i + offset];
		}
		offset += 2;									// offset = 26
		short checkSum = DataUtils.byteArrayToShort(buff_2);
		ip.setCheckSum(checkSum);

		for (int i = 0; i < 4; i ++) {					
			buff_4[i] = content[i + offset];
		}
		offset += 4;									// offset = 30
		int srcIP = DataUtils.byteArrayToInt(buff_4);
		ip.setSrcIP(srcIP);

		// ƴ�ӳ� SourceIP
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 4; i++) {
			builder.append((int) (buff_4[i] & 0xff));
			builder.append(".");
		}
		builder.deleteCharAt(builder.length() - 1);
		String sourceIP = builder.toString();
		protocolData.setSrcIP(sourceIP);

		for (int i = 0; i < 4; i ++) {		
			buff_4[i] = content[i + offset];
		}
		offset += 4;									// offset = 34
		int dstIP = DataUtils.byteArrayToInt(buff_4);
		ip.setDstIP(dstIP);

		// ƴ�ӳ� DestinationIP
		builder = new StringBuilder();
		for (int i = 0; i < 4; i++) {
			builder.append((int) (buff_4[i] & 0xff));
			builder.append(".");
		}
		builder.deleteCharAt(builder.length() - 1);
		String destinationIP = builder.toString();
		protocolData.setDesIP(destinationIP);

//		LogUtils.printObjInfo(ip);

		return ip;
	}
	
	/**
	 * ��ȡ Pcap ����֡
	 * @param fis
	 */
	public short readEthernetFrame(byte[] content) {
		PcapDataFrame dataFrame = new PcapDataFrame();
		int offset = 12;
		byte[] buff_2 = new byte[2];
		for (int i = 0; i < 2; i ++) {
			buff_2[i] = content[i + offset];
		}
		short frameType = DataUtils.byteArrayToShort(buff_2);
		dataFrame.setFrameType(frameType);
		return frameType;
		
//		LogUtils.printObjInfo(dataFrame);
	}
	
	/**
	 * ��������
	 */
	private boolean parseContent() {
		// 1. ��ȡ��̫������֡
		short type = readEthernetFrame(content);
		if(type == 0x0800 ) {
			// 2. ��ȡ IP
			ipHeader = readIPHeader(content);
			if (ipHeader == null) {							// �� ip Ϊ null ʱ�������
				return true;
			}
			portHeader = readPort(content);
			
			addProtocalData(protocolData);     // ����׼ȷ����
			
			String t = protocolData.toString();
			simplified_packages.add(t);    // �����ݰ�����ֻ�����Լ���Ҫ�����ݶΣ���󴫵ݸ�����ģ��
		}

		return false;
	}
	
	/**
	 * ��ȡ pcap �ļ�ͷ
	 */
	public PcapFileHeader parseFileHeader(byte[] file_header) throws IOException {
		PcapFileHeader fileHeader = new PcapFileHeader();
		byte[] buff_4 = new byte[4];	// 4 �ֽڵ�����
		byte[] buff_2 = new byte[2];	// 2 �ֽڵ�����

		int offset = 0;
		for (int i = 0; i < 4; i ++) {
			buff_4[i] = file_header[i + offset];
		}
		offset += 4;
		int magic = DataUtils.byteArrayToInt(buff_4);
		fileHeader.setMagic(magic);

		for (int i = 0; i < 2; i ++) {
			buff_2[i] = file_header[i + offset];
		}
		offset += 2;
		short magorVersion = DataUtils.byteArrayToShort(buff_2);
		fileHeader.setMagorVersion(magorVersion);

		for (int i = 0; i < 2; i ++) {
			buff_2[i] = file_header[i + offset];
		}
		offset += 2;
		short minorVersion = DataUtils.byteArrayToShort(buff_2);
		fileHeader.setMinorVersion(minorVersion);

		for (int i = 0; i < 4; i ++) {
			buff_4[i] = file_header[i + offset];
		}
		offset += 4;
		int timezone = DataUtils.byteArrayToInt(buff_4);
		fileHeader.setTimezone(timezone);

		for (int i = 0; i < 4; i ++) {
			buff_4[i] = file_header[i + offset];
		}
		offset += 4;
		int sigflags = DataUtils.byteArrayToInt(buff_4);
		fileHeader.setSigflags(sigflags);

		for (int i = 0; i < 4; i ++) {
			buff_4[i] = file_header[i + offset];
		}
		offset += 4;
		int snaplen = DataUtils.byteArrayToInt(buff_4);
		fileHeader.setSnaplen(snaplen);

		for (int i = 0; i < 4; i ++) {
			buff_4[i] = file_header[i + offset];
		}
		offset += 4;
		int linktype = DataUtils.byteArrayToInt(buff_4);
		fileHeader.setLinktype(linktype);

//		LogUtils.printObjInfo(fileHeader);

		return fileHeader;
	}

	/**
	 * ��ȡ���ݰ�ͷ
	 */
	public PcapDataHeader parseDataHeader(byte[] data_header){
		byte[] buff_4 = new byte[4];
		PcapDataHeader dataHeader = new PcapDataHeader();
		int offset = 0;
		for (int i = 0; i < 4; i ++) {
			buff_4[i] = data_header[i + offset];
		}
		offset += 4;
		int timeS = DataUtils.byteArrayToInt(buff_4);
		dataHeader.setTimeS(timeS);

		for (int i = 0; i < 4; i ++) {
			buff_4[i] = data_header[i + offset];
		}
		offset += 4;
		int timeMs = DataUtils.byteArrayToInt(buff_4);
		dataHeader.setTimeMs(timeMs);

		for (int i = 0; i < 4; i ++) {
			buff_4[i] = data_header[i + offset];
		}
		offset += 4;
		// ����������תΪ int
		DataUtils.reverseByteArray(buff_4);
		int caplen = DataUtils.byteArrayToInt(buff_4);
		dataHeader.setCaplen(caplen);
//		LogUtils.printObj("���ݰ�ʵ�ʳ���", dataHeader.getCaplen());

		for (int i = 0; i < 4; i ++) {
			buff_4[i] = data_header[i + offset];
		}
		offset += 4;
		//		int len = DataUtils.byteArrayToInt(buff_4);
		DataUtils.reverseByteArray(buff_4);
		int len = DataUtils.byteArrayToInt(buff_4);
		dataHeader.setLen(len);

//		LogUtils.printObjInfo(dataHeader);

		return dataHeader;
	}

	
}
