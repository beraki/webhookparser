package info.beraki.webhookparser;

import info.beraki.webhookparser.SlackMessageModel.SlackMessage;
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
import java.io.UnsupportedEncodingException;
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
    String postUrl = "https://hooks.slack.com/services/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";// put in your url

    public static void main(String[] args){

        if(args.length != 0){
            int port= Util.tryParse(args[0],8888);
            Spark.port(port); //4567
            if(port == 8888)
                System.out.println("Default port used");
        }


        //http://ec2-34-238-50-223.compute-1.amazonaws.com:8000/webhook/papertrail
        Spark.post("/webhook/papertrail", (req, res) -> { // post/LIMIT

            String rawRequest=req.body();

            try {

                JSONObject jsonObject=new JSONObject(rawRequest);


                if(jsonObject.has("page") && jsonObject.has("components") && jsonObject.has("component_update")) {
                    String statusIndicator = jsonObject.getJSONObject("page").getString("status_indicator");
                    String statusDesc = jsonObject.getJSONObject("page").getString("status_description");

                    String componentName= jsonObject.getJSONObject("components").getString("name");
                    String componentStatus= jsonObject.getJSONObject("component_update").getString("old_status");
                    String componentNewStatus= jsonObject.getJSONObject("component_update").getString("new_status");

                    slackMessage= SlackMessageBuilder.build("*Papertail Status Page Update* \n*"
                                    + statusDesc +"* "+
                                    "`"+ componentStatus +" ---> "+ componentNewStatus +"`",
                            getCrutialCode(statusIndicator),
                            componentName,
                            statusDesc);
                }

                if(jsonObject.has("page") && jsonObject.has("incident")) {
                    String statusIndicator = jsonObject.getJSONObject("page").getString("status_indicator");
                    String statusDesc = jsonObject.getJSONObject("page").getString("status_description");

                    String incidentName= jsonObject.getJSONObject("incident").getString("name");
                    String incidentStatus= jsonObject.getJSONObject("incident").getString("status");
                    JSONArray incidentUpdates=jsonObject.getJSONObject("incident").getJSONArray("incident_updates");
                    String incidentUpdateFormattedText="";
                    if(incidentUpdates.length() != 0){
                        for(int i=0; i < incidentUpdates.length() && i < NOOFINCIDENTS; i++){
                            JSONObject incidentUpdateObject= incidentUpdates.getJSONObject(i);
                            String incidentStatusStage= incidentUpdateObject.getString("status");
                            String incidentStatusBody= incidentUpdateObject.getString("body");
                            incidentUpdateFormattedText += "*"+incidentStatusStage+"-* " + incidentStatusBody +"\n";
                        }
                    }


                    slackMessage= SlackMessageBuilder.build("Papertail Status Page Update",
                            getCrutialCode(statusIndicator),
                            "*"+ incidentName +"* `"+statusDesc+"`",
                                        incidentUpdateFormattedText);
                }


                String postUrlZinaWorkspace = "https://hooks.slack.com/services/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";// put in your url
                String postUrl= "SLACK HOOK HERE";


                CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                HttpPost post = new HttpPost(postUrlZinaWorkspace);
                StringEntity postingString;
                if(slackMessage != null){
                    postingString = new StringEntity(slackMessage);//gson.tojson() converts your pojo to json
                }else{
                   postingString = new StringEntity(SlackMessageBuilder.build("Papertail Status Page Update *Unknown*",
                           "warning",
                           "Unknown",
                           "```@Beraki messed up somewhere and he is notified. \nPlease check "+
                           "http://www.papertrailstatus.com```"));

                   sendBerakiAnAngryMessage(jsonObject);
                }
                    post.setEntity(postingString);
                    post.setHeader("Content-type", "application/json");
                    response = httpClient.execute(post);


                    LOGGER.warning(response.getStatusLine().getStatusCode()+"");
            }catch(Exception e){
                e.printStackTrace();
            }
            res.status(200);
            return "Thanks for the webhook";
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


    private static void sendBerakiAnAngryMessage(JSONObject jsonObject) throws IOException {
        String postUrlZinaWorkspace = "SLACK HOOK HERE";// put in your url

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(postUrlZinaWorkspace);
        StringEntity postingString;

        postingString = new StringEntity(SlackMessageBuilder.build("Papertail Status Page Update *Error in json Formatting*",
                "warning",
                "Unknown",
                "```"+jsonObject+"```"));

        post.setEntity(postingString);
        post.setHeader("Content-type", "application/json");
        HttpResponse response = httpClient.execute(post);
    }


    public static String sendSlackWebhook(String SLACK_URL, String rawRequest){
        try {

            JSONObject reqRawJSONObject=new JSONObject(rawRequest);

            MessageType messasageType=checkOutMessageType(reqRawJSONObject);


            if(messasageType.equals(MessageType.COMPONENT)){
                slackMessage=prepareComponentMessage(reqRawJSONObject);
            }else if(messasageType.equals(MessageType.INCIDENT)){
                slackMessage=prepareIncidentMessage(reqRawJSONObject);
            }


            if(slackMessage != null)
                sendWebhookToSlack(SLACK_URL, slackMessage);
            else{
                slackMessage = SlackMessageBuilder.build("Papertail Status Page Update *Unknown*",
                        "warning",
                        "Unknown",
                        "```@Beraki messed up somewhere and he is notified. \nPlease check "+
                                "http://www.papertrailstatus.com```");
                sendWebhookToSlack(SLACK_URL,slackMessage);
                sendBerakiAnAngryMessage(reqRawJSONObject);
            }


            LOGGER.warning(response.getStatusLine().getStatusCode()+"");
        }catch(Exception e){
            e.printStackTrace();
        }
        //res.status(200);
        return "Thanks for the webhook";
    }

    private static String prepareIncidentMessage(JSONObject reqRawJSONObject) throws JSONException {
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


        String slackMessage= SlackMessageBuilder.build("Papertail Status Page Update",
                getCrutialCode(statusIndicator),
                "*"+ incidentName +"* `"+statusDesc+"`",
                incidentUpdateFormattedText);

        return slackMessage;
    }


    private static String prepareComponentMessage(JSONObject reqRawJSONObject) throws JSONException {
        String statusIndicator = reqRawJSONObject.getJSONObject("page").getString("status_indicator");
        String statusDesc = reqRawJSONObject.getJSONObject("page").getString("status_description");

        String componentName= reqRawJSONObject.getJSONObject("component").getString("name");
        String componentStatus= reqRawJSONObject.getJSONObject("component_update").getString("old_status");
        String componentNewStatus= reqRawJSONObject.getJSONObject("component_update").getString("new_status");

        String slackMessage= SlackMessageBuilder.build("*Papertail Status Page Update* \n*"
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
    private static MessageType checkOutMessageType(JSONObject reqRawJSONObject) {
        MessageType toReturn=null;
        if(reqRawJSONObject.has("page") &&
                reqRawJSONObject.has("component") && reqRawJSONObject.has("component_update")) {
            toReturn = MessageType.COMPONENT;
        }else if(reqRawJSONObject.has("page") && reqRawJSONObject.has("incident")) {
            toReturn = MessageType.INCIDENT;
        }
        return toReturn;
    }
}
