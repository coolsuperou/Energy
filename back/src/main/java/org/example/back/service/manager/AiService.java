package org.example.back.service.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.example.back.config.AiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 讯飞星火大模型服务
 * 通过 WebSocket 调用星火 X1.5 API
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    @Autowired
    private AiConfig aiConfig;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 调用星火大模型进行对话
     */
    public String chat(String userMessage) {
        try {
            String authUrl = buildAuthUrl();
            log.info("===== 星火AI WebSocket请求开始 =====");
            log.info("鉴权URL: {}", authUrl.substring(0, Math.min(authUrl.length(), 150)) + "...");
            log.info("用户消息长度: {} 字符", userMessage.length());

            CompletableFuture<String> future = new CompletableFuture<>();
            StringBuilder result = new StringBuilder();

            Request request = new Request.Builder().url(authUrl).build();
            client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info("WebSocket连接成功, 状态码: {}", response.code());
                    String requestJson = buildRequestJson(userMessage);
                    log.info("发送请求: {}", requestJson.length() > 300 ? requestJson.substring(0, 300) + "..." : requestJson);
                    webSocket.send(requestJson);
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    try {
                        log.debug("收到消息: {}", text.length() > 200 ? text.substring(0, 200) + "..." : text);
                        JsonNode root = objectMapper.readTree(text);

                        // 检查header中的错误码
                        JsonNode header = root.path("header");
                        int code = header.path("code").asInt(0);
                        if (code != 0) {
                            String msg = header.path("message").asText("未知错误");
                            log.error("星火API返回错误, code: {}, message: {}", code, msg);
                            future.completeExceptionally(new RuntimeException("星火API错误[" + code + "]: " + msg));
                            webSocket.close(1000, "error");
                            return;
                        }

                        // 提取文本内容
                        JsonNode choices = root.path("payload").path("choices");
                        int status = choices.path("status").asInt(0);
                        JsonNode textArray = choices.path("text");
                        if (textArray.isArray()) {
                            for (JsonNode item : textArray) {
                                String content = item.path("content").asText("");
                                result.append(content);
                            }
                        }

                        // status=2 表示最后一条消息
                        if (status == 2) {
                            log.info("AI回复完成, 总长度: {} 字符", result.length());
                            future.complete(result.toString());
                            webSocket.close(1000, "done");
                        }
                    } catch (Exception e) {
                        log.error("解析响应失败", e);
                        future.completeExceptionally(e);
                    }
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    String respInfo = response != null ? "状态码: " + response.code() : "无响应";
                    log.error("WebSocket连接失败, {}", respInfo, t);
                    future.completeExceptionally(new RuntimeException("WebSocket连接失败: " + t.getMessage()));
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    log.info("WebSocket关闭, code: {}, reason: {}", code, reason);
                }
            });

            String reply = future.get(120, TimeUnit.SECONDS);
            log.info("===== 星火AI WebSocket请求完成 =====");
            return reply;
        } catch (Exception e) {
            log.error("星火AI调用异常", e);
            throw new RuntimeException("AI 分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式调用星火大模型，通过 SseEmitter 实时推送内容
     */
    public void chatStream(String userMessage, org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter) {
        try {
            String authUrl = buildAuthUrl();
            log.info("===== 星火AI 流式请求开始 =====");

            Request request = new Request.Builder().url(authUrl).build();
            client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info("流式WebSocket连接成功");
                    String requestJson = buildRequestJson(userMessage);
                    webSocket.send(requestJson);
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    try {
                        JsonNode root = objectMapper.readTree(text);
                        JsonNode header = root.path("header");
                        int code = header.path("code").asInt(0);
                        if (code != 0) {
                            String msg = header.path("message").asText("未知错误");
                            log.error("星火API错误: {}", msg);
                            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                    .name("error").data("星火API错误: " + msg));
                            emitter.complete();
                            webSocket.close(1000, "error");
                            return;
                        }

                        JsonNode choices = root.path("payload").path("choices");
                        int status = choices.path("status").asInt(0);
                        JsonNode textArray = choices.path("text");
                        if (textArray.isArray()) {
                            for (JsonNode item : textArray) {
                                String content = item.path("content").asText("");
                                if (!content.isEmpty()) {
                                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                            .name("message").data(content));
                                }
                            }
                        }

                        if (status == 2) {
                            log.info("流式AI回复完成");
                            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                    .name("done").data("[DONE]"));
                            emitter.complete();
                            webSocket.close(1000, "done");
                        }
                    } catch (Exception e) {
                        log.error("流式推送失败", e);
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    log.error("流式WebSocket失败", t);
                    try {
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .name("error").data("连接失败: " + t.getMessage()));
                        emitter.complete();
                    } catch (Exception ignored) {}
                }
            });
        } catch (Exception e) {
            log.error("流式请求异常", e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 构建鉴权 URL（严格按照讯飞通用ws鉴权文档）
     */
    private String buildAuthUrl() throws Exception {
        // 将 wss 转为 https 用于解析和签名
        String httpUrl = aiConfig.getHostUrl().replace("wss://", "https://").replace("ws://", "http://");
        URL url = new URL(httpUrl);

        // RFC1123 格式的日期
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());

        // 拼接签名原文
        String preStr = "host: " + url.getHost() + "\n"
                + "date: " + date + "\n"
                + "GET " + url.getPath() + " HTTP/1.1";

        // HmacSHA256 签名
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec spec = new SecretKeySpec(
                aiConfig.getApiSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(spec);
        byte[] digest = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(digest);

        // 拼接 authorization
        String authOrigin = String.format(
                "api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                aiConfig.getApiKey(), "hmac-sha256", "host date request-line", signature);
        String authorization = Base64.getEncoder().encodeToString(
                authOrigin.getBytes(StandardCharsets.UTF_8));

        // 拼接最终鉴权 URL（使用 wss 协议）
        return aiConfig.getHostUrl() + "?"
                + "authorization=" + URLEncoder.encode(authorization, "UTF-8")
                + "&date=" + URLEncoder.encode(date, "UTF-8")
                + "&host=" + URLEncoder.encode(url.getHost(), "UTF-8");
    }

    /**
     * 构建 WebSocket 请求 JSON（按照讯飞 X1 文档格式）
     */
    private String buildRequestJson(String userMessage) {
        try {
            var root = objectMapper.createObjectNode();

            // header
            var header = root.putObject("header");
            header.put("app_id", aiConfig.getAppId());
            header.put("uid", "user");

            // parameter.chat
            var parameter = root.putObject("parameter");
            var chat = parameter.putObject("chat");
            chat.put("domain", aiConfig.getDomain());
            chat.put("temperature", 0.5);
            chat.put("max_tokens", 4096);

            // 关闭深度思考，加快响应速度
            var thinking = chat.putObject("thinking");
            thinking.put("type", "disabled");

            // payload.message.text
            var payload = root.putObject("payload");
            var message = payload.putObject("message");
            var text = message.putArray("text");

            var systemMsg = text.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是一个专业的电能数据分析师，擅长分析工厂的能耗数据，提供节能优化建议。请用中文回答，分析要专业且有条理。");

            var userMsg = text.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("构建请求体失败", e);
        }
    }
}
