package info.beraki;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Awesome Pojo Generator
 * */
public class SlackMessage{
  @SerializedName("attachments")
  @Expose
  private List<Attachments> attachments;
  void setAttachments(List<Attachments> attachments){
   this.attachments=attachments;
  }
  public List<Attachments> getAttachments(){
   return attachments;
  }
}