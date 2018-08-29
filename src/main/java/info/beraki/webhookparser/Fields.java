package info.beraki.webhookparser;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
/**
 * Awesome Pojo Generator
 * */
public class Fields{
  @SerializedName("short")
  @Expose
  private Boolean attachmentHeight;
  @SerializedName("title")
  @Expose
  private String title;
  @SerializedName("value")
  @Expose
  private String value;
  public void setShort(Boolean attachmentHeight){
   this.attachmentHeight=attachmentHeight;
  }
  public Boolean getAttachmentHeight(){
   return attachmentHeight;
  }
  void setTitle(String title){
   this.title=title;
  }
  public String getTitle(){
   return title;
  }
  void setValue(String value){
   this.value=value;
  }
  public String getValue(){
   return value;
  }
}