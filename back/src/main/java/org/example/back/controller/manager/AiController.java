package org.example.back.controller.manager;

import jakarta.servlet.http.HttpSession;
import org.example.back.entity.EnergyData;
import org.example.back.entity.User;
import org.example.back.service.manager.AiService;
import org.example.back.service.common.EnergyDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * =============================================
 * Author:	每天十点睡
 * Create date: 2024
 * Description:	AI 智能分析控制器 - 调用讯飞星火大模型对能耗数据进行分析
 * =============================================
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @Autowired
    private EnergyDataService energyDataService;

    /**
     * 流式 AI 对话（SSE）
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam String message, HttpSession session) {
        SseEmitter emitter = new SseEmitter(120000L);

        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            sendError(emitter, "未登录");
            return emitter;
        }

        if (message == null || message.trim().isEmpty()) {
            sendError(emitter, "消息不能为空");
            return emitter;
        }

        aiService.chatStream(message, emitter);
        return emitter;
    }

    /**
     * 流式 AI 能耗分析（SSE）
     * 支持5种快速操作指令：overview、peak-valley、anomaly、saving、forecast
     * @param type 分析类型：overview(能耗总览)、peak-valley(峰谷分析)、anomaly(异常检测)、saving(节能建议)、forecast(趋势预测)
     * @param question 用户额外问题（可选）
     */
    @GetMapping(value = "/analyze-energy/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeEnergyStream(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String question, 
            HttpSession session) {
        SseEmitter emitter = new SseEmitter(120000L);

        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            sendError(emitter, "未登录");
            return emitter;
        }

        try {
            String prompt = buildAnalysisPrompt(currentUser, type, question);
            aiService.chatStream(prompt, emitter);
        } catch (Exception e) {
            sendError(emitter, "分析失败: " + e.getMessage());
        }
        return emitter;
    }

    /**
     * 根据分析类型构建针对性的提示词
     */
    private String buildAnalysisPrompt(User currentUser, String type, String question) {
        LocalDate today = LocalDate.now();
        StringBuilder prompt = new StringBuilder();
        
        // 获取基础能耗数据
        Map<String, Object> todayStats = energyDataService.getWorkshopEnergyStats(currentUser, today);
        List<EnergyData> todayHourlyData = energyDataService.getTodayHourlyData(currentUser, null);
        
        // 获取本月数据用于对比分析
        LocalDate monthStart = YearMonth.now().atDay(1);
        List<EnergyData> monthData = energyDataService.getEnergyDataByDateRange(currentUser, null, monthStart, today);
        
        // 计算汇总数据
        double todayTotalEnergy = todayHourlyData.stream().mapToDouble(d -> d.getEnergy().doubleValue()).sum();
        double todayTotalCost = todayHourlyData.stream().mapToDouble(d -> d.getCost().doubleValue()).sum();
        double monthTotalEnergy = monthData.stream().mapToDouble(d -> d.getEnergy().doubleValue()).sum();
        double monthTotalCost = monthData.stream().mapToDouble(d -> d.getCost().doubleValue()).sum();
        
        // 计算峰谷平电量
        double peakEnergy = todayHourlyData.stream()
                .filter(d -> "PEAK".equals(d.getPeriodType().name()))
                .mapToDouble(d -> d.getEnergy().doubleValue()).sum();
        double normalEnergy = todayHourlyData.stream()
                .filter(d -> "NORMAL".equals(d.getPeriodType().name()))
                .mapToDouble(d -> d.getEnergy().doubleValue()).sum();
        double valleyEnergy = todayHourlyData.stream()
                .filter(d -> "VALLEY".equals(d.getPeriodType().name()))
                .mapToDouble(d -> d.getEnergy().doubleValue()).sum();
        
        // 根据类型构建不同的提示词
        if (type == null || type.isEmpty()) {
            type = "overview"; // 默认为总览分析
        }
        
        // 计算总电量（用于占比计算）
        double totalEnergy = peakEnergy + normalEnergy + valleyEnergy;
        
        switch (type.toLowerCase()) {
            case "overview":
                prompt.append("【全厂用电概览分析任务】\n\n");
                prompt.append("请基于以下数据，给出全厂用电概览分析报告：\n\n");
                prompt.append("=== 今日用电数据 ===\n");
                prompt.append(String.format("- 今日总用电量：%.2f kWh\n", todayTotalEnergy));
                prompt.append(String.format("- 今日总电费：%.2f 元\n", todayTotalCost));
                prompt.append(String.format("- 各车间统计：%s\n\n", todayStats.get("stats")));
                prompt.append("=== 本月累计数据 ===\n");
                prompt.append(String.format("- 本月总用电量：%.2f kWh\n", monthTotalEnergy));
                prompt.append(String.format("- 本月总电费：%.2f 元\n\n", monthTotalCost));
                prompt.append("请从以下方面进行分析：\n");
                prompt.append("1. 今日用电总体情况评估\n");
                prompt.append("2. 各车间用电量排名和占比\n");
                prompt.append("3. 与本月平均水平对比\n");
                prompt.append("4. 整体用电趋势判断\n");
                break;
                
            case "peak-valley":
                prompt.append("【峰谷电价优化分析任务】\n\n");
                prompt.append("请基于以下峰谷用电数据，给出电价优化建议：\n\n");
                prompt.append("=== 今日峰谷用电分布 ===\n");
                prompt.append(String.format("- 峰时用电量（8-12点、18-22点，1.2元/kWh）：%.2f kWh\n", peakEnergy));
                prompt.append(String.format("- 平时用电量（12-18点，0.8元/kWh）：%.2f kWh\n", normalEnergy));
                prompt.append(String.format("- 谷时用电量（22-8点，0.4元/kWh）：%.2f kWh\n\n", valleyEnergy));
                if (totalEnergy > 0) {
                    prompt.append("=== 用电占比 ===\n");
                    prompt.append(String.format("- 峰时占比：%.1f%%\n", peakEnergy / totalEnergy * 100));
                    prompt.append(String.format("- 平时占比：%.1f%%\n", normalEnergy / totalEnergy * 100));
                    prompt.append(String.format("- 谷时占比：%.1f%%\n\n", valleyEnergy / totalEnergy * 100));
                }
                prompt.append("请从以下方面进行分析：\n");
                prompt.append("1. 当前峰谷用电结构评估\n");
                prompt.append("2. 峰时用电过高的原因分析\n");
                prompt.append("3. 具体的错峰用电建议\n");
                prompt.append("4. 预估可节省的电费金额\n");
                break;
                
            case "anomaly":
                prompt.append("【异常用电检测任务】\n\n");
                prompt.append("请基于以下数据，检测是否存在异常用电情况：\n\n");
                prompt.append("=== 今日各小时用电数据 ===\n");
                for (EnergyData data : todayHourlyData) {
                    prompt.append(String.format("- %02d:00 车间%d：功率 %.2f kW，电量 %.2f kWh\n", 
                            data.getRecordHour(), data.getWorkshopId(), data.getPower(), data.getEnergy()));
                }
                prompt.append("\n=== 各车间汇总 ===\n");
                prompt.append(String.format("%s\n\n", todayStats.get("stats")));
                prompt.append("请从以下方面进行检测和分析：\n");
                prompt.append("1. 是否存在功率异常波动\n");
                prompt.append("2. 是否有车间用电量异常偏高或偏低\n");
                prompt.append("3. 是否存在非工作时段的异常用电\n");
                prompt.append("4. 定位可能存在问题的车间和时段\n");
                prompt.append("5. 给出排查建议\n");
                break;
                
            case "saving":
                prompt.append("【节能降耗建议任务】\n\n");
                prompt.append("请基于以下用电数据，给出具体的节能降耗建议：\n\n");
                prompt.append("=== 用电现状 ===\n");
                prompt.append(String.format("- 今日用电量：%.2f kWh，电费：%.2f 元\n", todayTotalEnergy, todayTotalCost));
                prompt.append(String.format("- 本月累计用电量：%.2f kWh，电费：%.2f 元\n", monthTotalEnergy, monthTotalCost));
                prompt.append(String.format("- 峰时用电占比：%.1f%%\n", totalEnergy > 0 ? peakEnergy / totalEnergy * 100 : 0));
                prompt.append(String.format("- 各车间数据：%s\n\n", todayStats.get("stats")));
                prompt.append("请从以下方面给出节能建议：\n");
                prompt.append("1. 设备运行优化建议\n");
                prompt.append("2. 生产排程优化建议（错峰生产）\n");
                prompt.append("3. 能源管理制度建议\n");
                prompt.append("4. 技术改造建议\n");
                prompt.append("5. 预估节能效果和投资回报\n");
                break;
                
            case "forecast":
                prompt.append("【用电趋势预测任务】\n\n");
                prompt.append("请基于以下历史数据，预测未来用电趋势：\n\n");
                prompt.append("=== 近期用电数据 ===\n");
                prompt.append(String.format("- 今日用电量：%.2f kWh\n", todayTotalEnergy));
                prompt.append(String.format("- 本月累计用电量：%.2f kWh（截至今日）\n", monthTotalEnergy));
                prompt.append(String.format("- 本月已过天数：%d 天\n", today.getDayOfMonth()));
                prompt.append(String.format("- 日均用电量：%.2f kWh\n", monthTotalEnergy / today.getDayOfMonth()));
                prompt.append(String.format("- 各车间数据：%s\n\n", todayStats.get("stats")));
                prompt.append("请从以下方面进行预测和预警：\n");
                prompt.append("1. 本月剩余天数用电量预测\n");
                prompt.append("2. 本月总用电量和电费预估\n");
                prompt.append("3. 是否可能超出月度配额\n");
                prompt.append("4. 用电高峰期预警\n");
                prompt.append("5. 建议采取的预防措施\n");
                break;
                
            default:
                // 默认综合分析
                prompt.append("请分析以下工厂电能数据，给出专业的能耗分析报告：\n\n");
                prompt.append(String.format("日期: %s\n", today));
                prompt.append(String.format("能耗数据: %s\n\n", todayStats));
        }
        
        // 添加用户额外问题
        if (question != null && !question.isEmpty()) {
            prompt.append("\n=== 用户额外问题 ===\n");
            prompt.append(question).append("\n");
        }
        
        prompt.append("\n请用专业、简洁的语言回答，给出具体可行的建议。");
        
        return prompt.toString();
    }

    /**
     * 发送错误消息并关闭连接
     */
    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
            emitter.complete();
        } catch (Exception ignored) {}
    }
}
