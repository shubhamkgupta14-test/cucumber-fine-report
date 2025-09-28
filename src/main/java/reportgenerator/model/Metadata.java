package reportgenerator.model;

public class Metadata {
    private String runBy;
    private String system;
    private String browser;
    private String startTime;
    private String endTime;
    private long duration;

    // Getters and Setters
    public String getRunBy() { return runBy; }
    public void setRunBy(String runBy) { this.runBy = runBy; }

    public String getSystem() { return system; }
    public void setSystem(String system) { this.system = system; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
}
