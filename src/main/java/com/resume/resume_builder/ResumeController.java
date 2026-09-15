package com.resume.resume_builder;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Controller
public class ResumeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    // =========================================================
    // PASTE RESUME - ORIGINAL ENDPOINT
    // =========================================================
    @PostMapping("/analyze")
    public String analyzeResume(
            @RequestParam(required = false) String resume,
            @RequestParam(required = false) String jobDescription,
            @RequestParam(required = false) String resumeText,
            @RequestParam(required = false) String targetJob,
            Model model) {

        String finalResume = firstNonEmpty(resume, resumeText);
        String finalJob = firstNonEmpty(jobDescription, targetJob);

        if (finalResume == null || finalResume.trim().isEmpty()
                || finalJob == null || finalJob.trim().isEmpty()) {

            model.addAttribute("error",
                    "Please provide both your resume and the target job description.");

            return "index";
        }

        return performAnalysis(finalResume, finalJob, model);
    }

    // =========================================================
    // PDF / DOCX UPLOAD
    // =========================================================
    @PostMapping("/analyze-upload")
    public String analyzeUploadedResume(
            @RequestParam("resumeFile") MultipartFile resumeFile,
            @RequestParam(required = false) String jobDescription,
            @RequestParam(required = false) String targetJob,
            Model model) {

        try {

            if (resumeFile == null || resumeFile.isEmpty()) {
                model.addAttribute("error",
                        "Please select a PDF or DOCX resume.");

                return "index";
            }

            String finalJob = firstNonEmpty(jobDescription, targetJob);

            if (finalJob == null || finalJob.trim().isEmpty()) {
                model.addAttribute("error",
                        "Please enter the target job description.");

                return "index";
            }

            String resumeText = extractText(resumeFile);

            return performAnalysis(resumeText, finalJob, model);

        } catch (Exception e) {

            model.addAttribute("error",
                    "Unable to read the uploaded resume. Please upload a valid PDF or DOCX file.");

            return "index";
        }
    }

    // =========================================================
    // NEW UPLOAD ENDPOINT USED BY THE NEW HOMEPAGE
    // =========================================================
    @PostMapping("/analyze-file")
    public String analyzeFile(
            @RequestParam("resumeFile") MultipartFile resumeFile,
            @RequestParam("targetJob") String targetJob,
            Model model) {

        try {

            if (resumeFile == null || resumeFile.isEmpty()) {
                model.addAttribute("error",
                        "Please select a PDF or DOCX resume.");

                return "index";
            }

            String resumeText = extractText(resumeFile);

            return performAnalysis(resumeText, targetJob, model);

        } catch (Exception e) {

            model.addAttribute("error",
                    "Unable to read the uploaded resume. Please upload a valid PDF or DOCX file.");

            return "index";
        }
    }

    // =========================================================
    // TEXT ANALYSIS ENDPOINT USED BY THE NEW HOMEPAGE
    // =========================================================
    @PostMapping("/analyze-text")
    public String analyzeText(
            @RequestParam("resumeText") String resumeText,
            @RequestParam("targetJob") String targetJob,
            Model model) {

        if (resumeText == null || resumeText.trim().isEmpty()
                || targetJob == null || targetJob.trim().isEmpty()) {

            model.addAttribute("error",
                    "Please provide both resume content and the target job description.");

            return "index";
        }

        return performAnalysis(resumeText, targetJob, model);
    }

    // =========================================================
    // PDF / DOCX TEXT EXTRACTION
    // =========================================================
    private String extractText(MultipartFile file) throws IOException {

        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            throw new IOException("Invalid file.");
        }

        String lowerName = fileName.toLowerCase(Locale.ROOT);

        // ---------------- PDF ----------------
        if (lowerName.endsWith(".pdf")) {

            byte[] fileBytes = file.getBytes();

            try (PDDocument document = Loader.loadPDF(fileBytes)) {

                PDFTextStripper stripper = new PDFTextStripper();

                return stripper.getText(document);
            }
        }

        // ---------------- DOCX ----------------
        if (lowerName.endsWith(".docx")) {

            try (XWPFDocument document =
                         new XWPFDocument(file.getInputStream())) {

                StringBuilder text = new StringBuilder();

                document.getParagraphs().forEach(paragraph -> {
                    text.append(paragraph.getText()).append("\n");
                });

                // Also read DOCX tables
                document.getTables().forEach(table -> {

                    table.getRows().forEach(row -> {

                        row.getTableCells().forEach(cell -> {

                            text.append(cell.getText())
                                    .append(" ");

                        });

                        text.append("\n");
                    });
                });

                return text.toString();
            }
        }

        throw new IOException(
                "Only PDF and DOCX files are supported.");
    }

    // =========================================================
    // MAIN CAREERLENS AI ANALYSIS ENGINE
    // =========================================================
    private String performAnalysis(
            String resume,
            String jobDescription,
            Model model) {

        String resumeText = normalize(resume);
        String jobText = normalize(jobDescription);

        /*
         * Weighted technical skill intelligence.
         * Higher weights represent skills that usually have
         * greater impact on technical job matching.
         */
        Map<String, Integer> skillWeights =
                new LinkedHashMap<>();

        skillWeights.put("java", 10);
        skillWeights.put("spring boot", 15);
        skillWeights.put("spring", 10);
        skillWeights.put("python", 10);
        skillWeights.put("javascript", 8);
        skillWeights.put("typescript", 7);

        skillWeights.put("html", 5);
        skillWeights.put("css", 5);

        skillWeights.put("react", 10);
        skillWeights.put("angular", 10);
        skillWeights.put("node.js", 10);
        skillWeights.put("express.js", 8);

        skillWeights.put("sql", 8);
        skillWeights.put("mysql", 8);
        skillWeights.put("postgresql", 8);
        skillWeights.put("mongodb", 8);

        skillWeights.put("git", 5);
        skillWeights.put("github", 4);

        skillWeights.put("docker", 8);
        skillWeights.put("kubernetes", 10);
        skillWeights.put("aws", 10);
        skillWeights.put("azure", 9);

        skillWeights.put("machine learning", 12);
        skillWeights.put("artificial intelligence", 12);
        skillWeights.put("data science", 10);

        skillWeights.put("rest api", 9);
        skillWeights.put("rest", 7);
        skillWeights.put("api", 5);

        skillWeights.put("hibernate", 8);
        skillWeights.put("jpa", 7);

        skillWeights.put("maven", 5);
        skillWeights.put("microservices", 10);

        List<String> matchedSkills =
                new ArrayList<>();

        List<String> missingSkills =
                new ArrayList<>();

        List<String> recommendations =
                new ArrayList<>();

        int totalWeight = 0;
        int matchedWeight = 0;

        // -----------------------------------------------------
        // Skill matching
        // -----------------------------------------------------
        for (Map.Entry<String, Integer> entry :
                skillWeights.entrySet()) {

            String skill = entry.getKey();
            int weight = entry.getValue();

            boolean required =
                    containsSkill(jobText, skill);

            boolean available =
                    containsSkill(resumeText, skill);

            if (required) {

                totalWeight += weight;

                if (available) {

                    String displaySkill =
                            formatSkillName(skill);

                    if (!matchedSkills.contains(displaySkill)) {
                        matchedSkills.add(displaySkill);
                    }

                    matchedWeight += weight;

                } else {

                    String displaySkill =
                            formatSkillName(skill);

                    if (!missingSkills.contains(displaySkill)) {
                        missingSkills.add(displaySkill);
                    }

                    if (weight >= 10) {

                        recommendations.add(
                                "High priority: strengthen your "
                                        + displaySkill
                                        + " skills and demonstrate them through a project or practical experience.");

                    } else {

                        recommendations.add(
                                "Consider adding "
                                        + displaySkill
                                        + " knowledge or project experience to strengthen your profile.");
                    }
                }
            }
        }

        // -----------------------------------------------------
        // Calculate weighted match percentage
        // -----------------------------------------------------
        int score = 0;

        if (totalWeight > 0) {

            score =
                    (matchedWeight * 100) / totalWeight;

        }

        // -----------------------------------------------------
        // If no recognized skills were found
        // -----------------------------------------------------
        if (totalWeight == 0) {

            score = calculateBasicTextSimilarity(
                    resumeText,
                    jobText);

            if (score < 1) {
                score = 1;
            }

            recommendations.add(
                    "Add more technical skills and role-specific keywords from the target job description.");

            recommendations.add(
                    "Include relevant projects that demonstrate practical experience.");

            recommendations.add(
                    "Use measurable achievements and action-oriented descriptions in your resume.");
        }

        // -----------------------------------------------------
        // Additional intelligent recommendations
        // -----------------------------------------------------

        if (matchedSkills.size() >= 5) {

            recommendations.add(
                    "Your resume demonstrates a strong technical foundation. Highlight your strongest matching skills near the top of your resume.");

        } else if (matchedSkills.size() > 0) {

            recommendations.add(
                    "Move your most relevant technical skills and projects higher in your resume so recruiters can identify your job fit quickly.");

        }

        if (missingSkills.size() >= 3) {

            recommendations.add(
                    "Focus on the highest-priority missing skills first instead of adding every technology. Prioritize skills repeatedly mentioned in the job description.");

        }

        if (!resumeText.contains("project")
                && !resumeText.contains("projects")) {

            recommendations.add(
                    "Add a Projects section with technologies used, your contribution and the outcome of each project.");

        }

        if (!resumeText.contains("internship")
                && !resumeText.contains("experience")) {

            recommendations.add(
                    "Add internship, training or practical experience details where applicable to strengthen your professional profile.");

        }

        // Keep recommendations clean
        recommendations =
                removeDuplicates(recommendations);

        // -----------------------------------------------------
        // Match classification
        // -----------------------------------------------------
        String matchLevel;

        if (score >= 80) {

            matchLevel = "Excellent Match";

        } else if (score >= 60) {

            matchLevel = "Good Match";

        } else if (score >= 40) {

            matchLevel = "Moderate Match";

        } else {

            matchLevel = "Low Match";
        }

        // -----------------------------------------------------
        // Send analysis to result.html
        // -----------------------------------------------------

        model.addAttribute(
                "score",
                score);

        // New result page variable
        model.addAttribute(
                "matchPercentage",
                score);

        model.addAttribute(
                "matchLevel",
                matchLevel);

        model.addAttribute(
                "matchedSkills",
                matchedSkills);

        model.addAttribute(
                "missingSkills",
                missingSkills);

        model.addAttribute(
                "recommendations",
                recommendations);

        return "result";
    }

    // =========================================================
    // HELPER: CHECK SKILL
    // =========================================================
    private boolean containsSkill(
            String text,
            String skill) {

        if (text == null || skill == null) {
            return false;
        }

        String normalizedSkill =
                skill.toLowerCase(Locale.ROOT);

        // Handle common technology variations
        if (normalizedSkill.equals("spring boot")) {

            return text.contains("spring boot")
                    || text.contains("springboot");
        }

        if (normalizedSkill.equals("node.js")) {

            return text.contains("node.js")
                    || text.contains("nodejs")
                    || text.contains("node js");
        }

        if (normalizedSkill.equals("express.js")) {

            return text.contains("express.js")
                    || text.contains("expressjs")
                    || text.contains("express js");
        }

        if (normalizedSkill.equals("rest api")) {

            return text.contains("rest api")
                    || text.contains("restful api")
                    || text.contains("restful");
        }

        return text.contains(normalizedSkill);
    }

    // =========================================================
    // HELPER: NORMALIZE TEXT
    // =========================================================
    private String normalize(String text) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    // =========================================================
    // HELPER: DISPLAY SKILL NAMES
    // =========================================================
    private String formatSkillName(String skill) {

        switch (skill) {

            case "java":
                return "Java";

            case "spring boot":
                return "Spring Boot";

            case "spring":
                return "Spring";

            case "python":
                return "Python";

            case "javascript":
                return "JavaScript";

            case "typescript":
                return "TypeScript";

            case "html":
                return "HTML";

            case "css":
                return "CSS";

            case "react":
                return "React";

            case "angular":
                return "Angular";

            case "node.js":
                return "Node.js";

            case "express.js":
                return "Express.js";

            case "sql":
                return "SQL";

            case "mysql":
                return "MySQL";

            case "postgresql":
                return "PostgreSQL";

            case "mongodb":
                return "MongoDB";

            case "git":
                return "Git";

            case "github":
                return "GitHub";

            case "docker":
                return "Docker";

            case "kubernetes":
                return "Kubernetes";

            case "aws":
                return "AWS";

            case "azure":
                return "Azure";

            case "machine learning":
                return "Machine Learning";

            case "artificial intelligence":
                return "Artificial Intelligence";

            case "data science":
                return "Data Science";

            case "rest api":
                return "REST API";

            case "rest":
                return "REST";

            case "api":
                return "API";

            case "hibernate":
                return "Hibernate";

            case "jpa":
                return "JPA";

            case "maven":
                return "Maven";

            case "microservices":
                return "Microservices";

            default:
                return skill;
        }
    }

    // =========================================================
    // HELPER: BASIC TEXT SIMILARITY
    // =========================================================
    private int calculateBasicTextSimilarity(
            String resume,
            String job) {

        Set<String> resumeWords =
                new HashSet<>(
                        Arrays.asList(
                                resume.split("[^a-z0-9+#.]+")));

        Set<String> jobWords =
                new HashSet<>(
                        Arrays.asList(
                                job.split("[^a-z0-9+#.]+")));

        jobWords.removeIf(
                word -> word.length() < 4);

        if (jobWords.isEmpty()) {
            return 0;
        }

        int matches = 0;

        for (String word : jobWords) {

            if (resumeWords.contains(word)) {
                matches++;
            }
        }

        return Math.min(
                100,
                (matches * 100) / jobWords.size());
    }

    // =========================================================
    // HELPER: REMOVE DUPLICATE RECOMMENDATIONS
    // =========================================================
    private List<String> removeDuplicates(
            List<String> list) {

        return new ArrayList<>(
                new LinkedHashSet<>(list));
    }

    // =========================================================
    // HELPER: FIRST NON-EMPTY VALUE
    // =========================================================
    private String firstNonEmpty(
            String first,
            String second) {

        if (first != null && !first.trim().isEmpty()) {
            return first;
        }

        if (second != null && !second.trim().isEmpty()) {
            return second;
        }

        return null;
    }
}