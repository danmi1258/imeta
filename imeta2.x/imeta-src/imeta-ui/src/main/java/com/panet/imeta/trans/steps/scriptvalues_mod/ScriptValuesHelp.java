package com.panet.imeta.trans.steps.scriptvalues_mod;

import java.io.InputStream;
import java.util.Hashtable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.panet.imeta.core.exception.KettleXMLException;
import com.panet.imeta.core.xml.XMLHandler;

public class ScriptValuesHelp {

	private static Document dom;
	private static Hashtable<String, String> hatFunctionsList;
	
	
	public ScriptValuesHelp(String strFileName) throws KettleXMLException {
		super();
		xparseXmlFile(strFileName);
		buildFunctionList();
		
	}
	public Hashtable<String, String> getFunctionList(){
		return hatFunctionsList;
	}
	
	private static void buildFunctionList(){
		hatFunctionsList = new Hashtable<String, String>();
		NodeList nlFunctions = dom.getElementsByTagName("jsFunction");
		for(int i=0;i<nlFunctions.getLength();i++){
			String strFunctionName = nlFunctions.item(i).getAttributes().getNamedItem("name").getNodeValue();
			Node elType = ((Element)nlFunctions.item(i)).getElementsByTagName("type").item(0);
			String strType = "";
			if(elType.hasChildNodes()) strType = elType.getFirstChild().getNodeValue();
			NodeList nlFunctionArgs=((Element)nlFunctions.item(i)).getElementsByTagName("argument");
			for(int j=0;j<nlFunctionArgs.getLength();j++){
				String strFunctionArgs=nlFunctionArgs.item(j).getFirstChild().getNodeValue();
				hatFunctionsList.put(strFunctionName +"("+strFunctionArgs+")", strType);
			}
			if(nlFunctionArgs.getLength()==0) hatFunctionsList.put(strFunctionName +"()", strType);
		}
	}
	
	public String getSample(String strFunctionName, String strFunctionNameWithArgs){
		String sRC="// Sorry, no Script available for "+ strFunctionNameWithArgs;
		
		NodeList nl = dom.getElementsByTagName("jsFunction");
		for(int i=0;i<nl.getLength();i++){
			if(nl.item(i).getAttributes().getNamedItem("name").getNodeValue().equals(strFunctionName)){
				Node elSample = ((Element)nl.item(i)).getElementsByTagName("sample").item(0);
				if(elSample.hasChildNodes()) return(elSample.getFirstChild().getNodeValue());
			}
		}
		return sRC;
	}
	
	private static void xparseXmlFile(String strFileName) throws KettleXMLException{
        try
        {
            InputStream is = ScriptValuesHelp.class.getResourceAsStream(strFileName);
            int c;
            StringBuffer buffer = new StringBuffer();
            while ( (c=is.read())!=-1 ) buffer.append((char)c);
            is.close();
            dom = XMLHandler.loadXMLString(buffer.toString());
        }
        catch(Exception e)
        {
            throw new KettleXMLException("Unable to read script values help file from file ["+strFileName+"]", e);
        }
	}	
}
