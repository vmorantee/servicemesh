package dependencies;
import java.io.Serializable;
import java.util.ArrayList;

public class Request implements Serializable {
    ArrayList<RequestEntry> content = new ArrayList<>();
    int requestID;
    String requestType;
    String messageCode;
    public Request(String requestType, int requestID){
        this.requestType=requestType;
        this.requestID=requestID;
    }
    public void setMessageCode(String messageCode){
        this.messageCode = messageCode;
    }
    public void addEntry(String entryName,String entryContent){
        RequestEntry re = new RequestEntry(entryName,entryContent);
        content.add(re);
    }
    public RequestEntry getContent(String entryName){
        for(RequestEntry entry: content){
            if(entry.getEntryName().equals(entryName)) return entry;
        }
        return null;
    }
    public String toString(){
        String request="";
        for(RequestEntry entry:content){
            request+=entry.getEntryName()+":";  
            request+=entry.getEntryContent()+"\n";
        }
        return request;
    }
    public String getMessageCode(){
        return this.messageCode;
    }
    public String getRequestType(){
        return this.requestType;
    }
    public int getRequestID(){
        return requestID;
    }
    public void changeEntry(String entryName, String entryContent){
        for(RequestEntry entry: content){
            if(entry.getEntryName().equals(entryName)) {
                entry.entryContent=entryContent;
                entry.entryName=entryName;
            }
        }
    }
}
