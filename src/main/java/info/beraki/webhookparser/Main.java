package info.beraki.webhookparser;

import com.google.gson.JsonObject;
import info.beraki.webhookparser.SlackMessageModel.SlackMessageBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import spark.Spark;

import java.io.IOException;
import java.util.logging.Logger;

import static spark.Spark.before;

import static spark.Spark.options;
import static spark.Spark.threadPool;

public class Main {

    private static Logger LOGGER = Logger.getLogger("Logging");
    private static int NOOFINCIDENTS=4;
    String s=null;
    static HttpResponse response=null;
    static String slackMessage = null;
    final static String SLACK_URL = "SLACK_HERE";
    final static String DEV_SLACK_URL = "SLACK_HERE";

    public static void main(String[] args){

        if(args.length != 0){
            int port= Util.tryParse(args[0],8888);
            Spark.port(port); //4567
            if(port == 8888)
                System.out.println("Default port used");
        }


        Spark.post("/webhook/testing", (req, res) -> {
            String serviceName="Papertrail";

            String toReturn=null;
            try{
                String rawRequestData = req.body();
                JSONObject rawRequestJSON = new JSONObject(rawRequestData);
                MessageType messageType = getMessageType(rawRequestJSON);

                if (messageType.equals(MessageType.COMPONENT)) {
                    String componentMessage = prepareComponentMessage(rawRequestJSON, serviceName);
                    toReturn=sendWebhookToSlack(SLACK_URL, componentMessage);
                } else if (messageType.equals(MessageType.INCIDENT)){
                    String incidentMessage=prepareIncidentMessage(rawRequestJSON, serviceName);
                    toReturn=sendWebhookToSlack(SLACK_URL, incidentMessage);
                }else if(messageType.equals(MessageType.UNKNOWN)){
                    sendBerakiAnAngryMessage(DEV_SLACK_URL,rawRequestJSON);
                }
            }catch(Exception e){
                e.printStackTrace();
                toReturn = "Issue parsing data";
            }

            return toReturn;
        });

        Spark.post("/webhook/bitbucket", (req, res) -> {
            String serviceName="BitBucket";

            String toReturn=null;
            try{
                String rawRequestData = req.body();
                JSONObject rawRequestJSON = new JSONObject(rawRequestData);
                MessageType messageType = getMessageType(rawRequestJSON);

                if (messageType.equals(MessageType.COMPONENT)) {
                    String componentMessage = prepareComponentMessage(rawRequestJSON, serviceName);
                    toReturn=sendWebhookToSlack(SLACK_URL, componentMessage);
                } else if (messageType.equals(MessageType.INCIDENT)){
                    String incidentMessage=prepareIncidentMessage(rawRequestJSON, serviceName);
                    toReturn=sendWebhookToSlack(SLACK_URL, incidentMessage);
                }else if(messageType.equals(MessageType.UNKNOWN)){
                    sendBerakiAnAngryMessage(DEV_SLACK_URL,rawRequestJSON);
                }
            }catch(Exception e){
                e.printStackTrace();
                toReturn = "Issue parsing data";
            }

            return toReturn;
        });

        Spark.post("/webhook/talkdesk", (req, res) -> {
            String serviceName="Talkdesk";

            String toReturn=null;
            try{
                String rawRequestData = req.body();
                JSONObject rawRequestJSON = new JSONObject(rawRequestData);
                MessageType messageType = getMessageType(rawRequestJSON);

                if (messageType.equals(MessageType.COMPONENT)) {
                    String componentMessage = prepareComponentMessage(rawRequestJSON, serviceName);
                    toReturn=sendWebhookToSlack(SLACK_URL, componentMessage);
                } else if (messageType.equals(MessageType.INCIDENT)){
                    String incidentMessage=prepareIncidentMessage(rawRequestJSON, serviceName);
                    toReturn=sendWebhookToSlack(SLACK_URL, incidentMessage);
                }else if(messageType.equals(MessageType.UNKNOWN)){
                    sendBerakiAnAngryMessage(DEV_SLACK_URL,rawRequestJSON);
                }
            }catch(Exception e){
                e.printStackTrace();
                toReturn = "Issue parsing data";
            }

            return toReturn;
        });

        options("/*", (request, response) -> {

            String accessControlRequestHeaders = request
                    .headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers",
                        accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request
                    .headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods",
                        accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) ->
                response.header("Access-Control-Allow-Origin", "*"));
    }


    private static String getCrutialCode(String statusIndicator) {
        String toReturn="warning";

        if(statusIndicator.equals("none")){
            toReturn="good";
        }
        if(statusIndicator.equals("minor")){
            toReturn="warning";
        }
        if(statusIndicator.equals("major") || statusIndicator.equals("severe")){
            toReturn="danger";
        }

        return toReturn;
    }


    private static void sendBerakiAnAngryMessage(String DEV_SLACK_URL,JSONObject jsonObject) throws IOException {


        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(DEV_SLACK_URL);
        StringEntity postingString;

        postingString = new StringEntity(SlackMessageBuilder.build("*Error in json Formatting*",
                "warning",
                "Unknown",
                "```"+jsonObject+"```"));

        post.setEntity(postingString);
        post.setHeader("Content-type", "application/json");
        HttpResponse response = httpClient.execute(post);
    }

    private static String prepareIncidentMessage(JSONObject reqRawJSONObject, String serviceName) throws JSONException {
        String statusIndicator = reqRawJSONObject.getJSONObject("page").getString("status_indicator");
        String statusDesc = reqRawJSONObject.getJSONObject("page").getString("status_description");

        String incidentName= reqRawJSONObject.getJSONObject("incident").getString("name");
        JSONArray incidentUpdates=reqRawJSONObject.getJSONObject("incident").getJSONArray("incident_updates");
        String incidentUpdateFormattedText="";
        if(incidentUpdates.length() != 0){
            for(int i=0; i < incidentUpdates.length() && i < NOOFINCIDENTS; i++){
                JSONObject incidentUpdateObject= incidentUpdates.getJSONObject(i);
                String incidentStatusStage= incidentUpdateObject.getString("status");
                String incidentStatusBody= incidentUpdateObject.getString("body");
                incidentUpdateFormattedText += "*"+incidentStatusStage+"-* " + incidentStatusBody +"\n";
            }
        }


        String slackMessage= SlackMessageBuilder.build("*"+serviceName+" Status Page Update*",
                getCrutialCode(statusIndicator),
                "*"+ incidentName +"* `"+statusDesc+"`",
                incidentUpdateFormattedText);

        return slackMessage;
    }


    private static String prepareComponentMessage(JSONObject reqRawJSONObject, String serviceName) throws JSONException {
        String statusIndicator = reqRawJSONObject.getJSONObject("page").getString("status_indicator");
        String statusDesc = reqRawJSONObject.getJSONObject("page").getString("status_description");

        String componentName= reqRawJSONObject.getJSONObject("component").getString("name");
        String componentStatus= reqRawJSONObject.getJSONObject("component_update").getString("old_status");
        String componentNewStatus= reqRawJSONObject.getJSONObject("component_update").getString("new_status");

        String slackMessage= SlackMessageBuilder.build("*"+serviceName+" Status Page Update* \n*"
                        + statusDesc +"* "+
                        "`"+ componentStatus +" ---> "+ componentNewStatus +"`",
                getCrutialCode(statusIndicator),
                componentName,
                statusDesc);

        return slackMessage;
    }
    public static String sendWebhookToSlack(String SLACK_URL, String slackMessage) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(SLACK_URL);
        StringEntity postingString;

        postingString = new StringEntity(slackMessage);//gson.tojson() converts your pojo to json

        post.setEntity(postingString);
        post.setHeader("Content-type", "application/json");
        response = httpClient.execute(post);


        return response.getStatusLine().getStatusCode()+"";
    }
    private static MessageType getMessageType(JSONObject reqRawJSONObject) {
        MessageType toReturn=MessageType.UNKNOWN;
        if(reqRawJSONObject.has("page") &&
                reqRawJSONObject.has("component") &&
                reqRawJSONObject.has("component_update")) {
            toReturn = MessageType.COMPONENT;
        }else if(reqRawJSONObject.has("page") && reqRawJSONObject.has("incident")) {
            toReturn = MessageType.INCIDENT;
        }
        return toReturn;
    }
}
