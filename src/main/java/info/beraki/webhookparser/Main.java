package info.beraki.webhookparser;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import spark.Spark;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import static spark.Spark.before;

import static spark.Spark.options;

public class Main {

    private static Logger LOGGER = Logger.getLogger("Logging");


    public static void main(String[] args){

        if(args.length != 0){
            int port= Util.tryParse(args[0],8888);
            Spark.port(port); //4567
            if(port == 8888)
                System.out.println("Default port used");
        }


        //http://ec2-34-238-50-223.compute-1.amazonaws.com:8000/webhook/papertrail
        Spark.post("/webhook/papertrail", (req, res) -> { // post/LIMIT
            String s=null;
            HttpResponse response=null;
            String slackMessage = null;
            try {

                JSONObject jsonObject=new JSONObject(req.body());


                if(jsonObject.has("page") && jsonObject.has("component") && jsonObject.has("component_update")) {
                    String statusIdicator = jsonObject.getJSONObject("page").getString("status_indicator");
                    String statusDesc = jsonObject.getJSONObject("page").getString("status_description");

                    String componentName= jsonObject.getJSONObject("component").getString("name");
                    String componentStatus= jsonObject.getJSONObject("component_update").getString("old_status");

                    String componentNewStatus= jsonObject.getJSONObject("component_update").getString("new_status");
                    String newStatusUpdatedAt = jsonObject.getJSONObject("component_update").getString("created_at");

                    slackMessage= SlackMessageBuilder.build("Papertail Status Page Update \n*"
                                    + statusDesc +"* \n"+
                                    "`"+ componentStatus +" ---> "+ componentNewStatus +"`",
                            "warning",
                            componentName,
                            statusDesc +
                            "\n Please check "+
                            "http://www.papertrailstatus.com");
                }


                //String postUrlZinaWorkspace = "SLACK HOOK HERE";// put in your url
                String postUrl= "SLACK HOOK HERE";

                CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                HttpPost post = new HttpPost(postUrl);
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
}
