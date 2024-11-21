package dependencies;
import java.io.Serializable;

public class RequestEntry implements Serializable{
    String entryName;
    String entryContent;
    public RequestEntry(String entryName,String entryContent){
        this.entryName=entryName;
        this.entryContent=entryContent;
    }
    public String getEntryName(){
        return entryName;
    }
    public String getEntryContent(){
        return entryContent;
    }
}
