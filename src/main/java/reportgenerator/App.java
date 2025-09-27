package reportgenerator;

import reportgenerator.service.ReportService;

public class App {
    public static void main(String[] args) {
//        String cucumberJson = "src/main/resources/cucumber.json";
        String cucumberJson = "D:\\IntelliJ\\Automation\\Selenium\\cucumber-selenium-testng\\target\\cucumber-reports\\cucumber.json";
        String reportJson = "report.json";

        ReportService service = new ReportService();
        try {
            service.generateReport(cucumberJson, reportJson);
            System.out.println("report.json generated successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
