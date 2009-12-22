package com.where.gae.data;

import com.google.appengine.api.datastore.Text;

import javax.jdo.annotations.*;
import java.util.Date;
import java.io.Serializable;

/**
 * @author Charles Kubicek
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
@Version(
    strategy = VersionStrategy.VERSION_NUMBER,
    extensions = {@Extension(vendorName="datanucleus", key="field-name", value="versionField")})
public class LineIterationResult implements Serializable {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    @Extension(vendorName="datanucleus", key="gae.encoded-pk", value="true")
    private String id;

    @Persistent
    private String line; // TODO change to line id from database

    @Persistent
    private Text result;

    @Persistent
    private Date validityTime;

    @Persistent
    private long casValue;

    private long versionField;

    public static LineIterationResult copy(LineIterationResult other) {
        return new LineIterationResult(
                other.line,
                other.result,
                other.validityTime
        );
    }

    public void copyDataInto(LineIterationResult other) {
        if(other.result == null){
            throw new NullPointerException("cannot create instance with null result");
        }
        this.line = other.line;
        this.result = other.result;
        this.validityTime = other.validityTime;
    }

     private LineIterationResult(String line, Text result, Date validityTime) {
        if(result == null){
            throw new NullPointerException("cannot create instance with null result");
        }
        this.line = line;
        this.result = result;
        this.validityTime = validityTime;
        this.casValue = 0;

    }

    public LineIterationResult(String line, String result, Date validityTime) {
        this(line, new Text(result), validityTime);
    }
        
    public void setResult(String result) {
        if(result == null){
            throw new NullPointerException("cannot set result to null");
        }
        this.result = new Text(result);
    }

    public long getVersionField() {
        return versionField;
    }

    public long getCasValue() {
        return casValue;
    }

    public void setCasValue(long casValue) {
        this.casValue = casValue;
    }

    public String getId() {
        return id;
    }

    public String getLine() {
        return line;
    }

    public String getResult() {
        return result.getValue();
    }

    public Date getValidityTime() {
        return validityTime;
    }

    @Override
    public String toString() {
        return "LineIterationResult{" +
                "id=" + id +
                ", line='" + line + '\'' +
                ", result='" + result + '\'' +
                ", validityTime=" + validityTime +
                ", casValue=" + casValue +
                '}';
    }
}
