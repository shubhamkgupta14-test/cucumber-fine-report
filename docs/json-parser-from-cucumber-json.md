# Phase 1 Documentation: Cucumber JSON → Custom Report JSON

## Objective

We are transforming the standard `cucumber.json` (generated after Cucumber BDD execution) into a customized `report.json` format. The goal is to:

* Include only **all scenarios with details** (steps, hooks, errors).
* Count **all scenarios, features, and steps** for summary.
* Handle special cases like **Backgrounds**, **Hooks (Before/After)**, **DataTables**, and **Durations**.

---

## Input: cucumber.json Structure

Cucumber JSON contains **features** which in turn contain **elements** (backgrounds, scenarios). Each scenario contains **steps**, and optionally **before/after hooks**.

---

## Output: report.json Structure

The generated `report.json` consolidates data into a structured format.

### Example:

```json
{
  "features": [
    {
      "id": "sample-first-feature-to-test",
      "name": "Sample First Feature to Test",
      "tags": ["@first"],
      "scenarios": [
        {
          "id": "sample-first-feature-to-test;one-sample-first-scenario",
          "name": "One Sample first scenario",
          "tags": ["@smoke"],
          "status": "failed",
          "duration": 35789,
          "beforeHooks": [
            {
              "location": "stepdefinitions.Hooks.before()",
              "status": "passed",
              "duration": 12345
            }
          ],
          "steps": [
            {
              "keyword": "Given",
              "name": "This step will always pass",
              "status": "passed",
              "duration": 1000
            },
            {
              "keyword": "When",
              "name": "This step will fail",
              "status": "failed",
              "error_message": "AssertionError",
              "duration": 5678
            }
          ],
          "afterHooks": [
            {
              "location": "stepdefinitions.Hooks.after()",
              "status": "passed",
              "duration": 23456
            }
          ]
        }
      ]
    }
  ],
  "summary": {
    "features": {"total": 1, "passed": 0, "failed": 1},
    "scenarios": {"total": 1, "passed": 0, "failed": 1},
    "steps": {"total": 2, "passed": 1, "failed": 1, "skipped": 0}
  }
}
```

---

## Key Nodes & Mappings

### 1. **Features**

* **id** → from `feature.id`
* **name** → from `feature.name`
* **tags** → from `feature.tags[].name`
* **keyword** → `Feature`

### 2. **Scenarios**

* **id** → from `element.id`
* **name** → from `element.name`
* **tags** → from `element.tags[].name`
* **status** → derived from step/hook results (failed if any failed)
* **duration** → sum of step + hook durations

### 3. **Steps**

* **keyword** → from `step.keyword`
* **name** → from `step.name`
* **status** → `passed` | `failed` | `skipped`
* **error_message** → only for failed steps
* **rows** → if DataTable exists (`rows[].cells`)

### 4. **Hooks (Before/After)**

* **location** → from `match.location`
* **status** → from `result.status`
* **error_message** → from `result.error_message` (if failed)
* **duration** → from `result.duration`

### 5. **Background**

* Only **first background** is applied to each scenario.
* Background steps are prepended to scenario steps.

### 6. **Summary**

* **Features** → total, passed, failed
* **Scenarios** → total, passed, failed
* **Steps** → total, passed, failed, skipped

---
