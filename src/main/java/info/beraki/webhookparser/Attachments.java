package info.beraki.webhookparser;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.List;
/**
 * Awesome Pojo Generator
 * */
public class Attachments{
  @SerializedName("color")
  @Expose
  private String color;
  @SerializedName("pretext")
  @Expose
  private String pretext;
  @SerializedName("fields")
  @Expose
  private List<Fields> fields;
  @SerializedName("fallback")
  @Expose
  private String fallback;
  void setColor(String color){
   this.color=color;
  }
  public String getColor(){
   return color;
  }
  void setPretext(String pretext){
   this.pretext=pretext;
  }
  public String getPretext(){
   return pretext;
  }
  void setFields(List<Fields> fields){
   this.fields=fields;
  }
  public List<Fields> getFields(){
   return fields;
  }
  void setFallback(String fallback){
   this.fallback=fallback;
  }
  public String getFallback(){
   return fallback;
  }
}