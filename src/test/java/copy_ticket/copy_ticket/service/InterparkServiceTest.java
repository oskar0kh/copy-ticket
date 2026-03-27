package copy_ticket.copy_ticket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import copy_ticket.copy_ticket.dto.PerformanceResponseDto;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;

/**
 * InterparkService API 파싱 로직 테스트
 * - HTTP 직접 호출로 Interpark 공연 정보 파싱
 * - 요청 API URL과 Response 결과를 파일로 출력
 */
public class InterparkServiceTest {

    private final InterparkService interparkService = new InterparkService();
    private StringBuilder output = new StringBuilder();

    @Test
    public void testParseInterParkUrl() {
        String url = "https://tickets.interpark.com/goods/26003142";
        String projectRoot = System.getProperty("user.dir");
        String outputFilePath = projectRoot + "/InterparkServiceTest_result.txt";

        output = new StringBuilder();

        appendLine("\n" + "=".repeat(100));
        appendLine("🌐 INTERPARK URL PARSING TEST");
        appendLine("=".repeat(100));
        appendLine("📍 Input URL: " + url);
        appendLine("📁 Output file: " + outputFilePath);

        long startTime = System.currentTimeMillis();

        try {
            // 1. parseInterParkUrl 호출
            appendLine("\n⏳ Calling InterparkService.parseInterParkUrl()...");
            PerformanceResponseDto result = interparkService.parseInterParkUrl(url);
            long elapsedTime = System.currentTimeMillis() - startTime;

            // 2. 결과 출력
            appendLine("\n" + "✅".repeat(50));
            appendLine("✅ PARSING COMPLETED ✅");
            appendLine("✅".repeat(50));
            appendLine(String.format("⏱️  Execution time: %.2f seconds", elapsedTime / 1000.0));

            // 3. 파싱된 12개 핵심 필드 출력
            printExtractedFields(result);

            // 4. 결과를 JSON 형식으로 출력
            printResultAsJson(result);

        } catch (Exception e) {
            appendLine("❌ Error: " + e.getMessage());
            appendLine("\n Stack Trace:");
            StringBuffer stackTrace = new StringBuffer();
            e.printStackTrace(new java.io.PrintWriter(new java.io.StringWriter()) {
                @Override
                public void println(String x) {
                    stackTrace.append(x).append("\n");
                }
            });
            appendLine(stackTrace.toString());
        }

        appendLine("\n" + "=".repeat(100));
        appendLine("✅ Test completed!");
        appendLine("=".repeat(100));

        // 파일로 결과 저장
        try {
            FileWriter writer = new FileWriter(outputFilePath);
            writer.write(output.toString());
            writer.close();
            System.out.println("\n✅ Test result saved to: " + outputFilePath);
        } catch (Exception e) {
            System.err.println("❌ Error writing to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 파싱된 12개 핵심 필드를 테이블 형식으로 출력
     */
    private void printExtractedFields(PerformanceResponseDto result) {
        appendLine("\n" + "─".repeat(100));
        appendLine("📊 12 CORE FIELDS EXTRACTED:");
        appendLine("─".repeat(100));

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("1. sourceUrl", result.getSourceUrl());
        fields.put("2. goodsName", result.getGoodsName());
        fields.put("3. subGoodsName", result.getSubGoodsName());
        fields.put("4. placeName", result.getPlaceName());
        fields.put("5. viewRateName", result.getViewRateName());
        fields.put("6. runningTime", result.getRunningTime());
        fields.put("7. playStartDate", result.getPlayStartDate());
        fields.put("8. playEndDate", result.getPlayEndDate());
        fields.put("9. goodsLargeImageUrl", result.getGoodsLargeImageUrl());
        fields.put("10. ticketOpenDate", result.getTicketOpenDate());
        fields.put("11. bookingEndDate", result.getBookingEndDate());
        fields.put("12. ticketCastCount", result.getTicketCastCount());

        int index = 1;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            Object value = entry.getValue();
            String status = value != null && !value.toString().isEmpty() ? "✅" : "⚠️ ";
            String displayValue = value != null ?
                (value.toString().length() > 70 ? value.toString().substring(0, 70) + "..." : value.toString())
                : "(null)";
            appendLine(String.format("%s [%2d] %-30s = %s", status, index, entry.getKey(), displayValue));
            index++;
        }
    }

    /**
     * API 요청 URL과 파싱 결과를 JSON 형식으로 출력
     */
    private void printResultAsJson(PerformanceResponseDto result) {
        appendLine("\n" + "─".repeat(100));
        appendLine("🔍 API REQUEST & RESPONSE:");
        appendLine("─".repeat(100));

        // 1. Goods Code 추출 및 요청 URL 표시
        String goodsCode = extractGoodsCodeFromUrl(result.getSourceUrl());
        if (goodsCode != null) {
            String apiUrl = buildSummaryApiUrl(goodsCode);
            appendLine("\n📡 API REQUEST URL:");
            appendLine("   " + apiUrl);
        }

        // 2. 파싱 결과를 Pretty JSON으로 출력
        appendLine("\n📋 PARSED RESULT (JSON Format):");
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            String jsonOutput = mapper.writeValueAsString(result);
            appendLine(jsonOutput);
        } catch (Exception e) {
            appendLine("❌ Error converting to JSON: " + e.getMessage());
        }
    }

    /**
     * URL에서 goods code 추출 (예: https://tickets.interpark.com/goods/26003042 -> 26003042)
     */
    private String extractGoodsCodeFromUrl(String url) {
        try {
            int goodsIdx = url.indexOf("/goods/");
            if (goodsIdx != -1) {
                int startIdx = goodsIdx + "/goods/".length();
                int endIdx = url.indexOf("/", startIdx);
                if (endIdx == -1) {
                    endIdx = url.indexOf("?", startIdx);
                }
                if (endIdx == -1) {
                    endIdx = url.length();
                }
                return url.substring(startIdx, endIdx);
            }
        } catch (Exception e) {
            appendLine("Error extracting goods code: " + e.getMessage());
        }
        return null;
    }

    /**
     * summary API URL 생성
     */
    private String buildSummaryApiUrl(String goodsCode) {
        long timestamp = System.currentTimeMillis();
        return String.format(
            "https://api-ticketfront.interpark.com/v1/goods/%s/summary?goodsCode=%s&passCode=&priceGrade=&seatGrade=&ts=%d",
            goodsCode, goodsCode, timestamp
        );
    }

    /**
     * 출력 버퍼에 라인 추가
     */
    private void appendLine(String line) {
        output.append(line).append("\n");
        System.out.println(line);
    }

    /**
     * 전체 API 응답 로깅 테스트
     * - Interpark API에서 반환하는 모든 필드를 확인
     */
    @Test
    public void testLogFullApiResponse() {
        String goodsCode = "26003142";
        String projectRoot = System.getProperty("user.dir");
        String outputFilePath = projectRoot + "/InterparkServiceTest_raw_response.txt";

        StringBuilder rawOutput = new StringBuilder();

        rawOutput.append("\n").append("=".repeat(100)).append("\n");
        rawOutput.append("🌐 FULL INTERPARK API RESPONSE LOG\n");
        rawOutput.append("=".repeat(100)).append("\n");
        rawOutput.append("📍 Goods Code: ").append(goodsCode).append("\n");
        rawOutput.append("📁 Output file: ").append(outputFilePath).append("\n");

        long startTime = System.currentTimeMillis();

        try {
            // 1. API 호출
            String apiUrl = buildSummaryApiUrl(goodsCode);
            rawOutput.append("\n📡 API REQUEST URL:\n");
            rawOutput.append("   ").append(apiUrl).append("\n");

            rawOutput.append("\n⏳ Calling Interpark API...\n");

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsedTime = System.currentTimeMillis() - startTime;

            // 2. 응답 상태 확인
            rawOutput.append("\n📊 API RESPONSE STATUS:\n");
            rawOutput.append("   - HTTP Status: ").append(response.statusCode()).append("\n");
            rawOutput.append("   - Response Size: ").append(response.body().length()).append(" bytes\n");
            rawOutput.append("   - Elapsed Time: ").append(String.format("%.2f", elapsedTime / 1000.0)).append(" seconds\n");

            if (response.statusCode() == 200) {
                // 3. 전체 응답 출력
                rawOutput.append("\n" + "─".repeat(100) + "\n");
                rawOutput.append("📋 FULL RAW API RESPONSE (Pretty JSON):\n");
                rawOutput.append("─".repeat(100) + "\n");

                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.enable(SerializationFeature.INDENT_OUTPUT);

                // Raw JSON을 pretty하게 포맷
                Object jsonObject = mapper.readValue(response.body(), Object.class);
                String prettyJson = mapper.writeValueAsString(jsonObject);
                rawOutput.append(prettyJson).append("\n");

                // 4. 필드 분석
                rawOutput.append("\n" + "─".repeat(100) + "\n");
                rawOutput.append("🔍 API RESPONSE STRUCTURE ANALYSIS:\n");
                rawOutput.append("─".repeat(100) + "\n");

                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = mapper.readValue(response.body(), Map.class);

                rawOutput.append("\n📍 Top-level keys:\n");
                for (String key : responseMap.keySet()) {
                    Object value = responseMap.get(key);
                    String valuePreview = value != null ? value.getClass().getSimpleName() : "null";
                    rawOutput.append("   - ").append(key).append(" (").append(valuePreview).append(")\n");
                }

                // "data" 객체 분석
                if (responseMap.containsKey("data")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataObj = (Map<String, Object>) responseMap.get("data");
                    if (dataObj != null) {
                        rawOutput.append("\n📦 'data' object contains ").append(dataObj.size()).append(" fields:\n");
                        for (String key : dataObj.keySet()) {
                            Object value = dataObj.get(key);
                            String valueType = value != null ? value.getClass().getSimpleName() : "null";
                            String valuePreview = value != null && value.toString().length() < 60
                                    ? value.toString()
                                    : (value != null ? value.toString().substring(0, 60) + "..." : "null");
                            rawOutput.append("   - ").append(String.format("%-30s", key)).append(" (").append(String.format("%-15s", valueType)).append(") = ").append(valuePreview).append("\n");
                        }
                    }
                }

                rawOutput.append("\n" + "=".repeat(100) + "\n");
                rawOutput.append("✅ Full API response logged successfully!\n");
                rawOutput.append("=".repeat(100) + "\n");

            } else {
                rawOutput.append("\n❌ API returned error status: ").append(response.statusCode()).append("\n");
                rawOutput.append("Response body: ").append(response.body()).append("\n");
            }

        } catch (Exception e) {
            rawOutput.append("\n❌ Error: ").append(e.getMessage()).append("\n");
            e.printStackTrace(new java.io.PrintWriter(new java.io.StringWriter()) {
                @Override
                public void println(String x) {
                    rawOutput.append(x).append("\n");
                }
            });
        }

        // 파일로 결과 저장
        try {
            FileWriter writer = new FileWriter(outputFilePath);
            writer.write(rawOutput.toString());
            writer.close();
            System.out.println("\n✅ Raw API response saved to: " + outputFilePath);
        } catch (Exception e) {
            System.err.println("❌ Error writing to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
