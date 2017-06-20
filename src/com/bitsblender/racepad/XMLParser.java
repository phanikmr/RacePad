package com.bitsblender.racepad;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLParser {
	
	Element element;
	public XMLParser(File XMLFile) {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = null;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(XMLFile);
			document.getDocumentElement().normalize();
			NodeList nodeList = document.getElementsByTagName(document.getDocumentElement().getNodeName());
			Node node = nodeList.item(0);
			element = (Element) node;
		} catch (ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	public String getAttributeFromTagName(String tagName){
		try{
		return element.getElementsByTagName(tagName).item(0).getTextContent();
		}
		catch(Exception err){
			return "";
		}
	}
}
