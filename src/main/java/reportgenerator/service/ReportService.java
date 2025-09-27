package reportgenerator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import reportgenerator.model.Feature;
import reportgenerator.model.Scenario;
import reportgenerator.model.Step;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportService {

    private final ObjectMapper mapper = new ObjectMapper();

    public void generateReport(String cucumberJsonPath, String reportJsonPath) throws Exception {
        List<Map<String, Object>> cucumberData = mapper.readValue(new File(cucumberJsonPath),
                new TypeReference<>() {
                });

        List<Feature> features = new ArrayList<>();

        int totalFeatures = 0, passedFeatures = 0, failedFeatures = 0;
        int totalScenarios = 0, passedScenarios = 0, failedScenarios = 0;
        int totalSteps = 0, passedSteps = 0, failedSteps = 0, skippedSteps = 0;

        for (Map<String, Object> f : cucumberData) {
            Feature feature = new Feature();
            feature.setId(((String) f.get("id")).replaceAll(" ", "-").toLowerCase());
            feature.setName((String) f.get("name"));

            // Extract feature tags
            if (f.containsKey("tags")) {
                List<Map<String, Object>> tagObjects = (List<Map<String, Object>>) f.get("tags");
                List<String> tagNames = new ArrayList<>();
                for (Map<String, Object> t : tagObjects) {
                    tagNames.add((String) t.get("name"));
                }
                feature.setTags(tagNames);
            } else {
                feature.setTags(new ArrayList<>());
            }
            feature.setKeyword(((String) f.get("keyword")).trim());
            feature.setFeature(feature.getKeyword().trim() + ": " + feature.getName().trim());
            feature.setScenarios(new ArrayList<>());
            boolean featureFailed = false;

            List<Map<String, Object>> elements = (List<Map<String, Object>>) f.get("elements");
            if (elements != null) {
                for (Map<String, Object> e : elements) {
                    String type = (String) e.get("type");
                    if (!"scenario".equals(type)) continue;

                    Scenario scenario = new Scenario();
                    scenario.setId(((String) e.get("id")).replaceAll(" ", "-").toLowerCase());
                    scenario.setName((String) e.get("name"));
                    scenario.setKeyword(((String) e.get("keyword")).trim());
                    scenario.setScenario(scenario.getKeyword().trim() + ": " + scenario.getName().trim());

                    // Extract scenario tags
                    if (e.containsKey("tags")) {
                        List<Map<String, Object>> tagObjects = (List<Map<String, Object>>) e.get("tags");
                        List<String> tagNames = new ArrayList<>();
                        for (Map<String, Object> t : tagObjects) {
                            tagNames.add((String) t.get("name"));
                        }
                        scenario.setTags(tagNames);
                    } else {
                        scenario.setTags(new ArrayList<>());
                    }

                    scenario.setSteps(new ArrayList<>());
                    scenario.setDuration(0);

                    String scenarioStatus = "passed";

                    List<Map<String, Object>> stepsList = (List<Map<String, Object>>) e.get("steps");
                    if (stepsList != null) {
                        for (Map<String, Object> s : stepsList) {
                            Step step = new Step();
                            step.setName((String) s.get("name"));
                            step.setDuration(s.get("result") != null && ((Map<String, Object>) s.get("result")).get("duration") != null
                                    ? ((Number) ((Map<String, Object>) s.get("result")).get("duration")).longValue() : 0);

                            String status = ((Map<String, Object>) s.get("result")).get("status").toString();
                            step.setKeyword(((String) s.get("keyword")).trim());
                            step.setStep(step.getKeyword().trim() + step.getName().trim());

                            if (s.containsKey("rows")) {
                                List<Map<String, Object>> tableRows = (List<Map<String, Object>>) s.get("rows");
                                List<List<String>> rowsList = new ArrayList<>();
                                for (Map<String, Object> r : tableRows) {
                                    List<String> cells = (List<String>) r.get("cells");
                                    rowsList.add(cells);
                                }
                                step.setRows(rowsList);
                            }

                            if ("failed".equals(status)) {
                                step.setStatus("failed");
                                step.setError_message(((Map<String, Object>) s.get("result")).get("error_message").toString());
                                scenarioStatus = "failed";
                                featureFailed = true;
                                failedSteps++;
                            } else if ("passed".equals(status)) {
                                if ("failed".equals(scenarioStatus)) {
                                    step.setStatus("skipped");
                                    skippedSteps++;
                                } else {
                                    step.setStatus("passed");
                                    passedSteps++;
                                }
                            } else {
                                step.setStatus(status);
                                skippedSteps++;
                            }

                            scenario.getSteps().add(step);
                            totalSteps++;
                            scenario.setDuration(scenario.getDuration() + step.getDuration());
                        }
                    }

                    scenario.setStatus(scenarioStatus);
                    feature.getScenarios().add(scenario); // for all scenario details
//                    if ("failed".equals(scenarioStatus)) {
//                        feature.getScenarios().add(scenario); // Only add failed scenario details
//                    }
                    totalScenarios++;
                    if ("failed".equals(scenarioStatus)) failedScenarios++;
                    else passedScenarios++;
                }
            }

            features.add(feature);
            totalFeatures++;
            if (featureFailed) failedFeatures++;
            else passedFeatures++;
        }

        // Build final JSON
        ObjectNode report = mapper.createObjectNode();
        report.set("features", mapper.valueToTree(features));

        ObjectNode summary = mapper.createObjectNode();
        ObjectNode fSummary = mapper.createObjectNode();
        fSummary.put("total", totalFeatures);
        fSummary.put("passed", passedFeatures);
        fSummary.put("failed", failedFeatures);

        ObjectNode sSummary = mapper.createObjectNode();
        sSummary.put("total", totalScenarios);
        sSummary.put("passed", passedScenarios);
        sSummary.put("failed", failedScenarios);

        ObjectNode stepSummary = mapper.createObjectNode();
        stepSummary.put("total", totalSteps);
        stepSummary.put("passed", passedSteps);
        stepSummary.put("failed", failedSteps);
        stepSummary.put("skipped", skippedSteps);

        summary.set("features", fSummary);
        summary.set("scenarios", sSummary);
        summary.set("steps", stepSummary);

        report.set("summary", summary);

        // Write report.json
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(reportJsonPath), report);
    }
}
