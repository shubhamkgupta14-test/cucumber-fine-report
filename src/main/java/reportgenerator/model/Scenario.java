package reportgenerator.model;

import java.util.List;

public class Scenario {
    private String id;
    private String keyword;
    private String scenario;
    private String name;
    private List<String> tags;
    private String status;
    private long duration;
    private List<Step> steps;
    private List<Hook> beforeHooks;
    private List<Hook> afterHooks;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public List<Hook> getBeforeHooks() {
        return beforeHooks;
    }

    public void setBeforeHooks(List<Hook> beforeHooks) {
        this.beforeHooks = beforeHooks;
    }

    public List<Hook> getAfterHooks() {
        return afterHooks;
    }

    public void setAfterHooks(List<Hook> afterHooks) {
        this.afterHooks = afterHooks;
    }
}
