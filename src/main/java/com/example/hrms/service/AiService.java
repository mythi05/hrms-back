package com.example.hrms.service;

import com.example.hrms.entity.Employee;
import com.example.hrms.entity.LeaveRequest;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.repository.LeaveRequestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.api-key:${OPENAI_API_KEY:}}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.chat.options.model:gpt-3.5-turbo}")
    private String openAiModel;

    public AiService(EmployeeRepository employeeRepository, LeaveRequestRepository leaveRequestRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String getAiAnswer(String userQuestion) {
        String question = userQuestion == null ? "" : userQuestion.trim();
        if (question.isEmpty()) {
            return "Bạn hãy nhập câu hỏi về nhân sự.";
        }

        String apiKey = sanitizeApiKey(openAiApiKey);
        if (apiKey.isEmpty()) {
            return answerWithoutLlm(question);
        }

        if (!apiKey.startsWith("sk-")) {
            return answerWithoutLlm(question);
        }

        String hrContext = buildHrContext();
        return callOpenAi(question, hrContext, apiKey);
    }

    private String sanitizeApiKey(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("Bearer ")) {
            s = s.substring("Bearer ".length()).trim();
        }
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private String buildHrContext() {
        List<Employee> employees = employeeRepository.findAll();

        Map<String, Long> departmentCounts = employees.stream()
                .filter(e -> e.getDepartment() != null && !e.getDepartment().trim().isEmpty())
                .collect(Collectors.groupingBy(e -> e.getDepartment().trim(), Collectors.counting()));

        Optional<Map.Entry<String, Long>> maxDepartment = departmentCounts.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue));

        List<Employee> itEmployees = employees.stream()
                .filter(e -> e.getDepartment() != null && e.getDepartment().toLowerCase(Locale.ROOT).contains("it"))
                .limit(30)
                .toList();

        List<Employee> lowPerformanceEmployees = employees.stream()
                .sorted(Comparator.comparingInt(Employee::getPerformanceRate))
                .limit(10)
                .toList();

        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        Map<Long, BigDecimal> leaveDaysThisMonth = new HashMap<>();
        for (LeaveRequest lr : leaveRequestRepository.findAll()) {
            if (lr.getStatus() != LeaveRequest.Status.APPROVED) {
                continue;
            }
            if (lr.getStartDate() == null || lr.getEndDate() == null) {
                continue;
            }
            if (lr.getEndDate().isBefore(monthStart) || lr.getStartDate().isAfter(monthEnd)) {
                continue;
            }
            BigDecimal days = lr.getDaysCount() == null ? BigDecimal.ZERO : lr.getDaysCount();
            leaveDaysThisMonth.merge(lr.getEmployeeId(), days, BigDecimal::add);
        }

        Map<Long, Employee> employeeById = employees.stream()
                .filter(e -> e.getId() != null)
                .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));

        String topLeaveText = leaveDaysThisMonth.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .map(e -> {
                    Employee emp = employeeById.get(e.getKey());
                    String name = emp != null ? emp.getFullName() : ("EmployeeId=" + e.getKey());
                    String dept = emp != null ? emp.getDepartment() : null;
                    return dept == null ? (name + ": " + e.getValue()) : (name + " (" + dept + "): " + e.getValue());
                })
                .collect(Collectors.joining("; "));

        String itEmployeesText = itEmployees.stream()
                .map(e -> e.getFullName() + " - " + (e.getPosition() == null ? "" : e.getPosition()))
                .collect(Collectors.joining("; "));

        String lowPerformanceText = lowPerformanceEmployees.stream()
                .map(e -> e.getFullName() + " (" + (e.getDepartment() == null ? "N/A" : e.getDepartment()) + ")" + ": " + e.getPerformanceRate())
                .collect(Collectors.joining("; "));

        String departmentStatsText = departmentCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(15)
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("; "));

        StringBuilder sb = new StringBuilder();
        sb.append("Tổng số nhân viên: ").append(employees.size()).append("\n");
        if (maxDepartment.isPresent()) {
            sb.append("Phòng đông nhân viên nhất: ")
                    .append(maxDepartment.get().getKey())
                    .append(" (")
                    .append(maxDepartment.get().getValue())
                    .append(")\n");
        }
        sb.append("Thống kê phòng ban (top): ").append(departmentStatsText).append("\n");
        sb.append("Danh sách nhân viên phòng IT (mẫu, tối đa 30): ").append(itEmployeesText).append("\n");
        sb.append("Nhân viên có performanceRate thấp (top 10 thấp nhất): ").append(lowPerformanceText).append("\n");
        sb.append("Top nghỉ nhiều nhất tháng ").append(currentMonth).append(" (tổng daysCount, APPROVED): ").append(topLeaveText).append("\n");
        sb.append("Nếu câu hỏi cần dữ liệu ngoài context này thì hãy trả lời không đủ dữ liệu.");
        return sb.toString();
    }

    private String callOpenAi(String question, String hrContext, String apiKey) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";

            Map<String, Object> systemMsg = new LinkedHashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "Bạn là Chatbot AI Nhân Sự trong hệ thống HRMS. Hãy trả lời ngắn gọn, đúng dữ liệu được cung cấp. Trả lời tiếng Việt. Nếu thiếu dữ liệu để kết luận, hãy nói rõ thiếu dữ liệu.");

            Map<String, Object> contextMsg = new LinkedHashMap<>();
            contextMsg.put("role", "system");
            contextMsg.put("content", "Dữ liệu HRMS (context) để trả lời:\n" + hrContext);

            Map<String, Object> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", question);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", openAiModel);
            body.put("messages", List.of(systemMsg, contextMsg, userMsg));
            body.put("temperature", 0.2);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return "Không gọi được AI (status=" + response.getStatusCode() + ")";
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                return "AI không trả về nội dung hợp lệ.";
            }
            return content.asText().trim();
        } catch (HttpStatusCodeException e) {
            return answerWithoutLlm(question);
        } catch (Exception e) {
            return answerWithoutLlm(question);
        }
    }

    private String answerWithoutLlm(String userQuestion) {
        String q = userQuestion == null ? "" : userQuestion.trim().toLowerCase(Locale.ROOT);
        List<Employee> employees = employeeRepository.findAll();

        Map<String, Long> deptCounts = employees.stream()
                .filter(e -> e.getDepartment() != null && !e.getDepartment().trim().isEmpty())
                .collect(Collectors.groupingBy(e -> e.getDepartment().trim(), Collectors.counting()));

        if (q.isBlank() || q.equals("hi") || q.equals("hello") || q.equals("hey") || q.equals("xin chào") || q.equals("xin chao")
                || q.equals("chào") || q.equals("chao") || q.equals("help") || q.equals("giúp") || q.equals("giup")
                || q.contains("bạn làm được gì") || q.contains("ban lam duoc gi") || q.contains("hướng dẫn") || q.contains("huong dan")) {
            return "Xin chào! Mình là AI trợ lý nhân sự trong HRMS. Bạn có thể hỏi:\n\n" +
                    "- Có bao nhiêu nhân viên?\n" +
                    "- Danh sách phòng ban\n" +
                    "- Thống kê nhân viên theo phòng ban\n" +
                    "- Phòng nào đông nhân viên nhất?\n" +
                    "- Danh sách nhân viên phòng IT\n" +
                    "- Tìm nhân viên Nguyễn\n" +
                    "- Thống kê nhân viên theo role\n" +
                    "- Lương trung bình / cao nhất / thấp nhất\n" +
                    "- Có bao nhiêu đơn nghỉ phép đang chờ duyệt?\n" +
                    "- Ai nghỉ nhiều nhất tháng này?\n" +
                    "- Nhân viên nào có hiệu suất thấp?";
        }

        if ((q.contains("bao nhiêu") || q.contains("bao nhieu")) && (q.contains("nhân viên") || q.contains("nhan vien"))) {
            return "Hiện hệ thống có " + employees.size() + " nhân viên.";
        }

        if ((q.contains("danh sách") || q.contains("danh sach") || q.contains("list"))
                && (q.contains("phòng ban") || q.contains("phong ban") || q.contains("department"))) {
            if (deptCounts.isEmpty()) return "Chưa có dữ liệu phòng ban.";
            String res = deptCounts.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(50)
                    .map(e -> "- " + e.getKey())
                    .collect(Collectors.joining("\n"));
            return "Danh sách phòng ban:\n" + res;
        }

        if ((q.contains("thống kê") || q.contains("thong ke") || q.contains("stats"))
                && (q.contains("phòng") || q.contains("phong") || q.contains("department"))) {
            if (deptCounts.isEmpty()) return "Chưa có dữ liệu phòng ban để thống kê.";
            String res = deptCounts.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(50)
                    .map(e -> "- " + e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("\n"));
            return "Thống kê nhân viên theo phòng ban:\n" + res;
        }

        if (q.contains("phòng") && (q.contains("đông") || q.contains("dong") || q.contains("nhiều") || q.contains("nhieu"))) {
            Optional<Map.Entry<String, Long>> maxDept = deptCounts.entrySet().stream()
                    .max(Comparator.comparingLong(Map.Entry::getValue));
            if (maxDept.isPresent()) {
                return "Phòng đông nhân viên nhất: " + maxDept.get().getKey() + " (" + maxDept.get().getValue() + " nhân viên).";
            }
            return "Chưa có dữ liệu phòng ban để thống kê.";
        }

        if (q.contains("tìm") || q.contains("tim") || q.contains("search")) {
            String nameKeyword = q;
            nameKeyword = nameKeyword.replace("tìm", "").replace("tim", "").replace("nhân viên", "").replace("nhan vien", "").trim();
            if (nameKeyword.length() >= 2) {
                String kw = nameKeyword.toLowerCase(Locale.ROOT);
                List<Employee> found = employees.stream()
                        .filter(e -> e.getFullName() != null && e.getFullName().toLowerCase(Locale.ROOT).contains(kw))
                        .limit(20)
                        .toList();
                if (found.isEmpty()) return "Không tìm thấy nhân viên theo từ khóa: " + nameKeyword;
                String res = found.stream()
                        .map(e -> "- " + e.getFullName() +
                                (e.getDepartment() == null ? "" : (" (" + e.getDepartment() + ")")) +
                                (e.getPosition() == null ? "" : (" - " + e.getPosition())))
                        .collect(Collectors.joining("\n"));
                return "Kết quả tìm kiếm (tối đa 20):\n" + res;
            }
        }

        if ((q.contains("role") || q.contains("vai trò") || q.contains("vai tro"))
                && (q.contains("thống kê") || q.contains("thong ke") || q.contains("bao nhiêu") || q.contains("bao nhieu"))) {
            Map<String, Long> roleCounts = employees.stream()
                    .filter(e -> e.getRole() != null)
                    .collect(Collectors.groupingBy(e -> String.valueOf(e.getRole()), Collectors.counting()));
            if (roleCounts.isEmpty()) return "Chưa có dữ liệu role.";
            String res = roleCounts.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(20)
                    .map(e -> "- " + e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("\n"));
            return "Thống kê nhân viên theo role:\n" + res;
        }

        if (q.contains("lương") || q.contains("luong") || q.contains("salary")) {
            double[] salaries = employees.stream().mapToDouble(Employee::getSalary).toArray();
            if (salaries.length == 0) return "Chưa có dữ liệu lương để thống kê.";
            double avg = employees.stream().mapToDouble(Employee::getSalary).average().orElse(0);
            double min = employees.stream().mapToDouble(Employee::getSalary).min().orElse(0);
            double max = employees.stream().mapToDouble(Employee::getSalary).max().orElse(0);
            return "Thống kê lương (dữ liệu hiện có):\n" +
                    "- Lương trung bình: " + String.format(Locale.ROOT, "%.2f", avg) + "\n" +
                    "- Lương thấp nhất: " + String.format(Locale.ROOT, "%.2f", min) + "\n" +
                    "- Lương cao nhất: " + String.format(Locale.ROOT, "%.2f", max);
        }

        if (q.contains("chờ duyệt") || q.contains("cho duyet") || q.contains("pending") || q.contains("đang chờ") || q.contains("dang cho")) {
            long pending = leaveRequestRepository.findAll().stream()
                    .filter(lr -> lr.getStatus() == LeaveRequest.Status.PENDING)
                    .count();
            return "Hiện có " + pending + " đơn nghỉ phép đang chờ duyệt.";
        }

        if (q.contains("danh sách") && (q.contains("phòng") || q.contains("phong") || q.contains("department"))) {
            String dept = null;
            if (q.contains("it")) dept = "it";
            if (dept == null) {
                Set<String> known = deptCounts.keySet().stream().limit(30).collect(Collectors.toSet());
                for (String d : known) {
                    if (q.contains(d.toLowerCase(Locale.ROOT))) {
                        dept = d;
                        break;
                    }
                }
            }

            List<Employee> filtered;
            if (dept == null) {
                filtered = employees;
            } else {
                String finalDept = dept;
                filtered = employees.stream()
                        .filter(e -> e.getDepartment() != null && e.getDepartment().toLowerCase(Locale.ROOT).contains(finalDept.toLowerCase(Locale.ROOT)))
                        .toList();
            }

            if (filtered.isEmpty()) {
                return "Không tìm thấy nhân viên phù hợp.";
            }

            String title = dept == null ? "Danh sách nhân viên" : ("Danh sách nhân viên phòng " + dept.toUpperCase(Locale.ROOT));
            String list = filtered.stream()
                    .limit(30)
                    .map(e -> "- " + e.getFullName() + (e.getPosition() == null ? "" : (" (" + e.getPosition() + ")")))
                    .collect(Collectors.joining("\n"));
            return title + ":\n" + list;
        }

        if (q.contains("nghỉ") || q.contains("nghi") || q.contains("leave")) {
            YearMonth currentMonth = YearMonth.now();
            LocalDate monthStart = currentMonth.atDay(1);
            LocalDate monthEnd = currentMonth.atEndOfMonth();

            Map<Long, BigDecimal> leaveDays = new HashMap<>();
            for (LeaveRequest lr : leaveRequestRepository.findAll()) {
                if (lr.getStatus() != LeaveRequest.Status.APPROVED) continue;
                if (lr.getStartDate() == null || lr.getEndDate() == null) continue;
                if (lr.getEndDate().isBefore(monthStart) || lr.getStartDate().isAfter(monthEnd)) continue;
                BigDecimal days = lr.getDaysCount() == null ? BigDecimal.ZERO : lr.getDaysCount();
                leaveDays.merge(lr.getEmployeeId(), days, BigDecimal::add);
            }

            if (leaveDays.isEmpty()) {
                return "Tháng này chưa có đơn nghỉ phép APPROVED.";
            }

            Map<Long, Employee> byId = employees.stream()
                    .filter(e -> e.getId() != null)
                    .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));

            List<Map.Entry<Long, BigDecimal>> top = leaveDays.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(5)
                    .toList();

            String res = top.stream()
                    .map(e -> {
                        Employee emp = byId.get(e.getKey());
                        String name = emp != null ? emp.getFullName() : ("EmployeeId=" + e.getKey());
                        return "- " + name + ": " + e.getValue() + " ngày";
                    })
                    .collect(Collectors.joining("\n"));
            return "Top nghỉ nhiều nhất tháng " + currentMonth + ":\n" + res;
        }

        if (q.contains("hiệu suất") || q.contains("hieu suat") || q.contains("performance") || q.contains("thấp") || q.contains("thap")) {
            List<Employee> low = employees.stream()
                    .sorted(Comparator.comparingInt(Employee::getPerformanceRate))
                    .limit(10)
                    .toList();
            if (low.isEmpty()) {
                return "Chưa có dữ liệu nhân viên.";
            }
            String res = low.stream()
                    .map(e -> "- " + e.getFullName() + " (" + (e.getDepartment() == null ? "N/A" : e.getDepartment()) + ")" + ": " + e.getPerformanceRate())
                    .collect(Collectors.joining("\n"));
            return "Nhân viên có hiệu suất thấp (Top 10):\n" + res;
        }

        String deptStatsText = deptCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(15)
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("; "));

        return "Mình chưa hiểu rõ câu hỏi. Bạn thử hỏi:\n" +
                "- Có bao nhiêu nhân viên?\n" +
                "- Danh sách phòng ban\n" +
                "- Thống kê nhân viên theo phòng ban\n" +
                "- Danh sách nhân viên phòng IT\n" +
                "- Tìm nhân viên Nguyễn\n" +
                "- Thống kê nhân viên theo role\n" +
                "- Lương trung bình\n" +
                "- Có bao nhiêu đơn nghỉ phép đang chờ duyệt?\n\n" +
                "Tổng nhân viên: " + employees.size() + "\n" +
                "Thống kê phòng ban (Top): " + deptStatsText;
    }
}