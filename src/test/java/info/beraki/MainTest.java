package info.beraki;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MainTest {

    @Test
    public void main() {

    }

    @Test
    public void getCrutialCode(){
        String actual=Main.getCrutialCode("major");
        assertEquals("danger",actual);
        String actual1=Main.getCrutialCode("minor");
        assertEquals("warning",actual1);
    }

    @Test
    public void sendBerakiAnAngryMessage() {

    }

    @Test
    public void prepareIncidentMessage() {

    }

    @Test
    public void prepareComponentMessage() {

    }

    @Test
    public void sendWebhookToSlack() {

    }

    @Test
    public void getMessageType() {
        JSONObject jsonObject= new JSONObject();
        
    }

    @Test
    public void parserMyRequest() {

    }
}