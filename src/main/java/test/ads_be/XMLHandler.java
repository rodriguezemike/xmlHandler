package test.ads_be;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.json.JSONArray;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLHandler {

    private final String filePath;
    private final String elementName;
    private DocumentBuilderFactory documentFactory;
    private Document inXMLDocument;
    private List<Map<String,String>> data;
    private JSONObject dataJSON;
    private Document outXMLDocument;
    private boolean converted = false;
    
    
    public XMLHandler(String filePath, String elementName) {
        this.filePath = filePath;
        this.elementName = elementName;
        this.data = new ArrayList<>();
    }
    
    public void convert(){
        this.openDocument();
        this.loadDataFromDocument();
        this.createJSONObject();
        this.createXMLObject();
        this.converted = true;
    }
    
    public void saveJSON(String outFilePath){
        if(!this.converted){
            this.convert();
        }
        try {
            FileWriter f = new FileWriter(outFilePath);
            f.write(this.dataJSON.toString(4));
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void validateXML(String schemaFilePath, boolean validateOutgoingXML){
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaFile = new StreamSource(new File(schemaFilePath));
        try {
            Schema schema = factory.newSchema(schemaFile);
            Validator val = schema.newValidator();
            if(validateOutgoingXML == true){
                if(!this.converted){
                    this.convert();
                }
                val.validate(new DOMSource(this.outXMLDocument));
                System.out.println("Outgoing XML file Validated.");
            } else {
                this.loadDataFromDocument();
                val.validate(new DOMSource(this.inXMLDocument));
                System.out.println("Incoming XML file Validated");
            }
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }     
    }
    
    public void saveXML(String outFilePath){
        if(!this.converted){
            this.convert();
        }
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
            transformer.transform(new DOMSource(this.outXMLDocument), new StreamResult(new FileOutputStream(outFilePath)));    
        } catch (TransformerConfigurationException  e) {
           e.printStackTrace();
        } catch (FileNotFoundException e) {
           e.printStackTrace();
        } catch (TransformerException e) {
           e.printStackTrace();
        }
    }
    
    private void createJSONObject(){
        this.dataJSON = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for(int i = 0; i < this.data.size(); ++i){
            JSONObject childObject = new JSONObject(this.data.get(i));
            jsonArray.put(childObject);
        }
        this.dataJSON.put(this.elementName,jsonArray);
        System.out.println("Done Creating JSON Object");
        System.out.println(this.dataJSON.toString(4));
    }
    
    private void createXMLObject(){
        DocumentBuilder builder;
        try {
            builder = this.documentFactory.newDocumentBuilder();
            this.outXMLDocument = builder.newDocument();
            Element rootElement = outXMLDocument.createElement("Root");
            for (int i = 0; i < this.data.size(); ++i){
                Element e = this.outXMLDocument.createElement(this.elementName);
                Iterator iter = this.data.get(i).entrySet().iterator();
                while(iter.hasNext()){
                    Map.Entry entry = (Map.Entry) iter.next();
                    Element child = this.outXMLDocument.createElement((String) entry.getKey());
                    child.appendChild(this.outXMLDocument.createTextNode((String) entry.getValue()));
                    e.appendChild(child);
                }
                rootElement.appendChild(e);
            }
            this.outXMLDocument.appendChild(rootElement);
            System.out.println("Done creating XML Dom.");
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }
    
    private void loadDataFromDocument(){
        NodeList contacts = this.inXMLDocument.getElementsByTagName(elementName);
        for(int i =0; i < contacts.getLength(); ++i){
            NodeList children = contacts.item(i).getChildNodes();
            Map<String, String> map = new HashMap<>();
            for(int j = 0; j < children.getLength(); ++j){
                if(children.item(j).getNodeType() == Node.ELEMENT_NODE){
                    map.put(children.item(j).getNodeName(),children.item(j).getTextContent());
                }
            }
            this.data.add(map);
        }
    }
    
    private void openDocument(){
        this.documentFactory = DocumentBuilderFactory.newInstance();
        try{
            this.documentFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder builder = this.documentFactory.newDocumentBuilder();
            this.inXMLDocument = builder.parse(new File(this.filePath));
            this.inXMLDocument.normalize();
            System.out.println("File Open");
        } catch (ParserConfigurationException | SAXException | IOException e){
            e.printStackTrace();
        }
    }
    
    
    public JSONObject getJSON(){
        if(!this.converted){
            this.convert();
        }
        return dataJSON;
    }
    
    public Document getOutXMLDocument(){
        if(!this.converted){
            this.convert();
        }
        return outXMLDocument;
        
    }
    
    public static void main(String[] args){
    
        if(args.length == 2)
        {
            String filePath = null;
            String elementName = null;
            for (int i = 0; i < args.length; ++i){
                if(new File(args[i]).isFile() & !(new File(args[i]).isDirectory())){
                    filePath = args[i];
                }
                else{
                    elementName = args[i];
                }
            }
            if(filePath != null & elementName != null){
                String dir = new File(filePath).getParent();
                String jsonPath = new File(dir, "out.json").getPath();
                String xmlPath = new File(dir, "out.xml").getPath();
                XMLHandler handler = new XMLHandler(filePath, elementName);
                handler.convert();
                handler.saveJSON(jsonPath);
                handler.saveXML(xmlPath);
            } else {
                if (filePath == null){
                    throw new IllegalArgumentException("Need a valid file path.");
                }
                else{
                    throw new IllegalArgumentException("Need an element name.");
                }
            }
        }else{
            throw new IllegalArgumentException("Need to pass in a file path and a unique element name iterate through.");
        }
    }
}
