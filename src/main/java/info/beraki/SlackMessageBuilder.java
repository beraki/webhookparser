package info.beraki;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class SlackMessageBuilder {


    public static String build(String customMessage, String severity, String incidentTitle, String incidentDescription){

        List<Fields> fieldsList= new ArrayList<>();
        Fields fields= new Fields();
        fields.setTitle(incidentTitle);
        fields.setValue(incidentDescription);

        fieldsList.add(fields);

        List<Attachments> attachmentsList= new ArrayList<>();
        Attachments attachments= new Attachments();
        attachments.setFallback(customMessage);
        attachments.setPretext(customMessage);
        attachments.setFields(fieldsList);
        attachments.setColor(severity);

        attachmentsList.add(attachments);


        SlackMessage slackMessage= new SlackMessage();
        slackMessage.setAttachments(attachmentsList);


        return new Gson().toJson(slackMessage);
    }

}
