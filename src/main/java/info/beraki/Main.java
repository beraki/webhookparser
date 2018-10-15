package info.beraki;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import spark.Request;
import spark.Spark;
import org.slf4j.*;
import java.io.IOException;

import static spark.Spark.before;
import static spark.Spark.options;

public class Main {

    final static Logger LOGGER = LoggerFactory.getLogger("loggly");
    private static int NOOFINCIDENTS=4;
    String s=null;
    static HttpResponse response=null;
    static String slackMessage = null;
    final static String SLACK_URL = "SLACK URL HERE";
    final static String DEV_SLACK_URL = "SLACK URL HERE";

    public static void main(String[] args){
        LOGGER.info("Hello World from Logback!");
        LOGGER.info("{\"message\" : \"Hello World from Logback!\"}");
        if(args.length != 0){
            int port= Util.tryParse(args[0],8888);
            Spark.port(port); //4567
            if(port == 8888)
                System.out.println("Default port used");
        }


        Spark.post("/webhook/papertrail", (req, res) -> {
            String serviceName="Papertrail";

            return parserMyRequest(req, serviceName);
        });

        Spark.post("/webhook/bitbucket", (req, res) -> {
            String serviceName="BitBucket";

            return parserMyRequest(req, serviceName);
        });

        Spark.post("/webhook/atlassian", (req, res) -> {
            String serviceName="Atlassian Suite";

            return parserMyRequest(req, serviceName);
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


    public static String getCrutialCode(String statusIndicator) {
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


    public static void sendBerakiAnAngryMessage(String DEV_SLACK_URL,JSONObject jsonObject) throws IOException {
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

    public static String prepareIncidentMessage(JSONObject reqRawJSONObject, String serviceName) throws JSONException {
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


    public static String prepareComponentMessage(JSONObject reqRawJSONObject, String serviceName) throws JSONException {
        String statusIndicator = reqRawJSONObject.getJSONObject("page").getString("status_indicator");
        String statusDesc = reqRawJSONObject.getJSONObject("page").getString("status_description");

        String componentName= reqRawJSONObject.getJSONObject("component").getString("name");
        String componentStatus= reqRawJSONObject.getJSONObject("component_update").getString("old_status");
        String componentNewStatus= reqRawJSONObject.getJSONObject("component_update").getString("new_status");

        return SlackMessageBuilder.build("*"+serviceName+" Status Page Update* \n*"
                        + statusDesc +"* "+
                        "`"+ componentStatus +" ---> "+ componentNewStatus +"`",
                getCrutialCode(statusIndicator),
                componentName,
                statusDesc);
    }
    public static Integer sendWebhookToSlack(String SLACK_URL, String slackMessage) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(SLACK_URL);
        StringEntity postingString;

        postingString = new StringEntity(slackMessage);//gson.tojson() converts your pojo to json

        post.setEntity(postingString);
        post.setHeader("Content-type", "application/json");
        response = httpClient.execute(post);


        return response.getStatusLine().getStatusCode();
    }
    public static MessageType getMessageType(JSONObject reqRawJSONObject) {
        MessageType toReturn=MessageType.UNKNOWN;
        if(reqRawJSONObject.has("page") &&
                reqRawJSONObject.has("component") &&
                reqRawJSONObject.has("component_update")) {
            toReturn = MessageType.COMPONENT;
        }else if(reqRawJSONObject.has("page") &&
                reqRawJSONObject.has("incident")) {
            toReturn = MessageType.INCIDENT;
        }
        return toReturn;
    }

    public static String parserMyRequest(Request req, String serviceName){
        String toReturn=null;
        Integer statusCode;
        try{
            String rawRequestData = req.body();
            JSONObject rawRequestJSON = new JSONObject(rawRequestData);
            MessageType messageType = getMessageType(rawRequestJSON);

            if (messageType.equals(MessageType.COMPONENT)) {
                String componentMessage = prepareComponentMessage(rawRequestJSON, serviceName);
                statusCode=sendWebhookToSlack(SLACK_URL, componentMessage);
                toReturn = "Webhook returned with status code " + statusCode;
            } else if (messageType.equals(MessageType.INCIDENT)){
                String incidentMessage=prepareIncidentMessage(rawRequestJSON, serviceName);
                statusCode=sendWebhookToSlack(SLACK_URL, incidentMessage);
                toReturn = "Webhook returned with status code" + statusCode;
            }else if(messageType.equals(MessageType.UNKNOWN)){
                sendBerakiAnAngryMessage(DEV_SLACK_URL,rawRequestJSON);
            }
        }catch(Exception e){
            e.printStackTrace();
            toReturn = "Issue parsing data";
        }
        return toReturn;
    }
}
