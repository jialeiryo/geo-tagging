package com.jdsu.ranadvisor.geotagging;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CellsiteXmlParser extends DefaultHandler{
	
	private Cellsite currentReading = null;
	private String currentTech = "";
	private List<Cellsite> cellsites;
	
	public List<Cellsite> parse(File file) {
		cellsites = new ArrayList<Cellsite>();
		try {
	        SAXParserFactory factory = SAXParserFactory.newInstance();
	        SAXParser saxParser = factory.newSAXParser();
		    saxParser.parse(file, this);
		}catch (Exception e) {
			e.printStackTrace();
	    }
		
		return cellsites;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
		Attributes attributes) throws SAXException {
		
		if(qName.equals("LTE")) {
			currentTech = "LTE";
		}
		else if(qName.equals("UMTS")) {
			currentTech = "UMTS";
		}
		   
		if(qName.startsWith("Cell")) {
			currentReading = new Cellsite();
			currentReading.tech = currentTech;
			currentReading.channelValue = attributes.getValue("ARFCN");
			if(currentTech.endsWith("LTE"))
				currentReading.channelCode = attributes.getValue("PhyCellID");
			else if(currentTech.endsWith("UMTS"))
				currentReading.channelCode = attributes.getValue("SC");
			currentReading.lon = Double.valueOf(attributes.getValue("Longitude"));
			currentReading.lat = Double.valueOf(attributes.getValue("Latitude"));
		}
    }	
	
    @Override
    public void endElement(String uri, String localName, String qName)
          throws SAXException {

    	if(qName.startsWith("Cell") && currentReading!=null) {
    		cellsites.add(currentReading);
    	}
    }

}
