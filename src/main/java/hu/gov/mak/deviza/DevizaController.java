package hu.gov.mak.deviza;

import java.io.File;
import javafx.fxml.FXML;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/*
 * JavaFX FXML Controller class
 */

public class DevizaController implements Initializable {

    private Stage stage;

    private static final String CURRENCY = "currency";
    private static final String RATE = "rate";
    private static final String ECB_WEB = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";
   
    private static float arfolyamHUF;
    private static float darabHUF;

    // "Letöltés" gomb
    @FXML
    private Button webxmlBtn;
    // "Átvaltás" gomb
    @FXML
    private Button atvaltasBtn;
    // "Pénznem" lenyíló értéklista
    @FXML
    private ComboBox<String> arfolyamCbx;
    // "Devizaárfolyam forrása" szöveg mező (nem szerkeszthető)
    @FXML
    private TextField xmlUrlTxt;
    // "Összeg" szöveg mező
    @FXML
    private TextField dbDevizaTxt;
    // "Átváltott összeg forintban" szöveg mező (nem szerkeszthető)
    @FXML
    private TextField dbHufTxt;
    // "Összeg" mező adatrögzítésnek ellenőrzése, szabályozása.
    @FXML
    private void handleItemDbDeviza(ActionEvent event){             
      dbHufTxt.clear();     
      dbDevizaTxt.setText(getNumber(dbDevizaTxt.getText()));
      if (!dbDevizaTxt.getText().isEmpty()  && Float.valueOf(dbDevizaTxt.getText()) > 0.0){
        dbHufTxt.setDisable(false);
        atvaltasBtn.setDisable(false);  
      }
      else {            
        dbHufTxt.setDisable(true);
        atvaltasBtn.setDisable(true);  
      }
    }
 
    // "Pénznem" lista kiválaztott eleme és az "Összeg" mező értéke alapjan forintban számolja ki
    // az átváltott összeget (figyelembe veszi 5 forintra kerekítés). 
    @FXML
    private void handleButtonAtvaltas(ActionEvent event){  
      String devizaValasztas = arfolyamCbx.getValue();
      float devizaDb = Float.valueOf(dbDevizaTxt.getText());    
      float devizaArfolyam = Float.valueOf(devizaValasztas.substring(devizaValasztas.indexOf("-") + 2));
      int forintDb = (int)(Math.round(((devizaDb / devizaArfolyam) * arfolyamHUF) / 5) * 5f);
      dbHufTxt.setText(String.valueOf(forintDb));
      System.out.println("Választot Deviza: "+devizaValasztas+" Árfolyam:"+devizaArfolyam); 
    }

    //Lenyíló értéklista feltöltése xml-ből(devizaárfolyamok)
    @FXML
    private void handleComboBox(ActionEvent event){
      System.out.println("Árfolyam: "+arfolyamCbx.getValue());
      dbDevizaTxt.clear();
      dbHufTxt.clear();  
      dbDevizaTxt.setDisable(false);
      dbHufTxt.setDisable(true);
      atvaltasBtn.setDisable(true);    
    } 

    //Friss árfolyamadatok leszedése a netről
    @FXML
    private void handleButtonWebXml(ActionEvent event){
      java.util.List<String> arfolyamok = xmlOlvasWeb(xmlUrlTxt.getText());
      for (String a : arfolyamok) {
        arfolyamCbx.getItems().add(a);
      }
      // szükséges az EUR öszegek átváltásához 
      arfolyamCbx.getItems().add("EUR - 1.0000");
      // Set the default value.
      arfolyamCbx.setValue("EUR - 1.0000");
    }
   
    // Xml fájlból(devizaárfolyamok) feltölti az árfolyamokat tartalmazó listát. 
    private java.util.List<String> xmlOlvasWeb(String xmlForras) {
        java.util.List<String> arfolyamok = new ArrayList<>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {            
            URL url = new URL(xmlForras);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(url.openStream()));
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("Cube");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode; 
                  if (eElement.getAttribute(CURRENCY) != null && !eElement.getAttribute(CURRENCY).isEmpty()) {                                     
                    if ("HUF".equalsIgnoreCase(eElement.getAttribute(CURRENCY))) {
                        // Mindent csak forintra váltunk és kizárunk a "forintot forintra" műveletet.
                        arfolyamHUF = Float.valueOf(eElement.getAttribute(RATE)); 
                    }
                    else {
                        arfolyamok.add(eElement.getAttribute(CURRENCY) + " - " + eElement.getAttribute(RATE)); 
                    } 
                  }
                }
            }
            arfolyamCbx.setDisable(false);
        } catch (Exception e) {
            atvaltasBtn.setDisable(true);
            arfolyamCbx.setDisable(true);
            dbDevizaTxt.setDisable(true);
            e.printStackTrace();
        }
        return arfolyamok;
    }
    
    // Egy karakterláncot átalakítja a csak számjegyeket tartalmazó szövegre 
    // vagyis szűri le a nem numerikus karaktereket.
    private String getNumber(String value) {
        String n = "";
        try {
            return String.valueOf(Integer.parseInt(value));
        } catch (Exception e) {
            String[] array = value.split("");
            for (String tab : array) {
                try {
                    n = n.concat(String.valueOf(Integer.parseInt(String.valueOf(tab))));
                } catch (Exception ex) {
                    System.out.println("Hibásan rögzített szám!!!");
                }
            }
            return n;
        }
    }

    // kezdő értékek, állapotok beállítása
    @Override
    public void initialize(URL location, ResourceBundle resources) {
      xmlUrlTxt.setText(ECB_WEB); 
      atvaltasBtn.setDisable(true);
      arfolyamCbx.setDisable(true);
      dbDevizaTxt.setDisable(true);
      dbHufTxt.setDisable(true);
    }       
}  
