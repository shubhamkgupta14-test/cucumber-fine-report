package reportgenerator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import reportgenerator.model.Feature;
import reportgenerator.model.Scenario;
import reportgenerator.model.Step;
import reportgenerator.model.Hook;

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

            // Feature tags
            if (f.containsKey("tags")) {
                List<Map<String, Object>> tagObjects = (List<Map<String, Object>>) f.get("tags");
                List<String> tagNames = new ArrayList<>();
                for (Map<String, Object> t : tagObjects) tagNames.add((String) t.get("name"));
                feature.setTags(tagNames);
            } else {
                feature.setTags(new ArrayList<>());
            }

            feature.setKeyword(((String) f.get("keyword")).trim());
            feature.setFeature(feature.getKeyword().trim() + ": " + feature.getName().trim());
            feature.setScenarios(new ArrayList<>());
            boolean featureFailed = false;

            // Store background steps (if any)
            List<Step> backgroundSteps = new ArrayList<>();

            List<Map<String, Object>> elements = (List<Map<String, Object>>) f.get("elements");
            if (elements != null) {
                // First pass: collect background steps
                for (Map<String, Object> e : elements) {
                    String type = (String) e.get("type");
                    if ("background".equals(type)) {
                        List<Map<String, Object>> stepsList = (List<Map<String, Object>>) e.get("steps");
                        if (stepsList != null) {
                            for (Map<String, Object> s : stepsList) {
                                Step step = parseStep(s);
                                backgroundSteps.add(step);
                            }
                        }
                        break;
                    }
                }

                // Second pass: process scenarios
                for (Map<String, Object> e : elements) {
                    String type = (String) e.get("type");
                    if (!"scenario".equals(type)) continue;

                    Scenario scenario = new Scenario();
                    scenario.setId(((String) e.get("id")).replaceAll(" ", "-").toLowerCase());
                    scenario.setName((String) e.get("name"));
                    scenario.setKeyword(((String) e.get("keyword")).trim());
                    scenario.setScenario(scenario.getKeyword().trim() + ": " + scenario.getName().trim());

                    // Scenario tags
                    if (e.containsKey("tags")) {
                        List<Map<String, Object>> tagObjects = (List<Map<String, Object>>) e.get("tags");
                        List<String> tagNames = new ArrayList<>();
                        for (Map<String, Object> t : tagObjects) tagNames.add((String) t.get("name"));
                        scenario.setTags(tagNames);
                    } else {
                        scenario.setTags(new ArrayList<>());
                    }

                    scenario.setSteps(new ArrayList<>());
                    scenario.setDuration(0);

                    String scenarioStatus = "passed";

                    // ✅ Parse before hooks
                    if (e.containsKey("before")) {
                        List<Hook> beforeHooks = parseHooks((List<Map<String, Object>>) e.get("before"));
                        scenario.setBeforeHooks(beforeHooks);
                        for (Hook hook : beforeHooks) {
                            if ("failed".equals(hook.getStatus())) {
                                scenarioStatus = "failed";
                                featureFailed = true;
                            } ;
                        }
                    }

                    // ✅ Add background steps (shared)
                    for (Step bg : backgroundSteps) {
                        Step copy = copyStep(bg);
                        scenario.getSteps().add(copy);
                        totalSteps++;
                        scenario.setDuration(scenario.getDuration() + copy.getDuration());

                        if ("failed".equals(copy.getStatus())) {
                            scenarioStatus = "failed";
                            featureFailed = true;
                            failedSteps++;
                        } else if ("passed".equals(copy.getStatus())) {
                            passedSteps++;
                        } else {
                            skippedSteps++;
                        }
                    }

                    // ✅ Parse scenario steps
                    List<Map<String, Object>> stepsList = (List<Map<String, Object>>) e.get("steps");
                    if (stepsList != null) {
                        for (Map<String, Object> s : stepsList) {
                            Step step = parseStep(s);

                            if ("failed".equals(step.getStatus())) {
                                scenarioStatus = "failed";
                                featureFailed = true;
                                failedSteps++;
                            } else if ("passed".equals(step.getStatus())) {
                                if ("failed".equals(scenarioStatus)) {
                                    step.setStatus("skipped");
                                    skippedSteps++;
                                } else {
                                    passedSteps++;
                                }
                            } else {
                                skippedSteps++;
                            }

                            scenario.getSteps().add(step);
                            totalSteps++;
                            scenario.setDuration(scenario.getDuration() + step.getDuration());
                        }
                    }

                    // ✅ Parse after hooks
                    if (e.containsKey("after")) {
                        List<Hook> afterHooks = parseHooks((List<Map<String, Object>>) e.get("after"));
                        scenario.setAfterHooks(afterHooks);
                        for (Hook hook : afterHooks) {
                            if ("failed".equals(hook.getStatus())) {
                                scenarioStatus = "failed";
                                featureFailed = true;
                            } ;
                        }
                    }

                    // Final scenario status
                    scenario.setStatus(scenarioStatus);

                    feature.getScenarios().add(scenario);
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

        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(reportJsonPath), report);
    }

    // --- Helpers ---

    private Step parseStep(Map<String, Object> s) {
        Step step = new Step();
        step.setName((String) s.get("name"));
        step.setKeyword(((String) s.get("keyword")).trim());
        step.setStep(step.getKeyword().trim() + " " + step.getName().trim());
        step.setDuration(s.get("result") != null && ((Map<String, Object>) s.get("result")).get("duration") != null
                ? ((Number) ((Map<String, Object>) s.get("result")).get("duration")).longValue() : 0);

        String status = (s.get("result") != null) ? ((Map<String, Object>) s.get("result")).get("status").toString() : "skipped";
        step.setStatus(status);

        if ("failed".equals(status) && ((Map<String, Object>) s.get("result")).get("error_message") != null) {
            step.setError_message(((Map<String, Object>) s.get("result")).get("error_message").toString());
        }

        if (s.containsKey("rows")) {
            List<Map<String, Object>> tableRows = (List<Map<String, Object>>) s.get("rows");
            List<List<String>> rowsList = new ArrayList<>();
            for (Map<String, Object> r : tableRows) {
                List<String> cells = (List<String>) r.get("cells");
                rowsList.add(cells);
            }
            step.setRows(rowsList);
        }
        return step;
    }

    private Step copyStep(Step original) {
        Step copy = new Step();
        copy.setName(original.getName());
        copy.setKeyword(original.getKeyword());
        copy.setStep(original.getStep());
        copy.setDuration(original.getDuration());
        copy.setStatus(original.getStatus());
        copy.setError_message(original.getError_message());
        copy.setRows(original.getRows());
        return copy;
    }

    private List<Hook> parseHooks(List<Map<String, Object>> hooksData) {
        List<Hook> hooks = new ArrayList<>();
        for (Map<String, Object> h : hooksData) {
            Hook hook = new Hook();
            if (h.containsKey("match")) {
                Map<String, Object> match = (Map<String, Object>) h.get("match");
                hook.setLocation((String) match.get("location"));
            }
            if (h.containsKey("result")) {
                Map<String, Object> result = (Map<String, Object>) h.get("result");
                hook.setStatus((String) result.get("status"));
                hook.setDuration(result.get("duration") != null ? ((Number) result.get("duration")).longValue() : 0);
                if (result.containsKey("error_message")) {
                    hook.setError_message((String) result.get("error_message"));
                }
            }
            hooks.add(hook);
        }
        return hooks;
    }
}
