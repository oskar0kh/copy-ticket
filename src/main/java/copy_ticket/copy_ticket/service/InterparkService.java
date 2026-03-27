package copy_ticket.copy_ticket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import copy_ticket.copy_ticket.dto.PerformanceResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class InterparkService {

    /**
     * HTTP API 직접 호출로 인터파크 공연 정보 파싱
     * summary API에 직접 요청해서 공연 정보 조회
     *
     * @param url Interpark 상품 URL (예: https://tickets.interpark.com/goods/26003042)
     * @return 파싱된 공연 정보
     */
    public PerformanceResponseDto parseInterParkUrl(String url) {
        log.info("Starting to parse Interpark URL: {}", url);

        try {
            // 1. URL에서 goodsCode 추출
            String goodsCode = extractGoodsCodeFromUrl(url);
            if (goodsCode == null) {
                log.error("Could not extract goods code from URL: {}", url);
                return PerformanceResponseDto.builder()
                        .sourceUrl(url)
                        .goodsName("URL에서 상품코드를 찾을 수 없습니다")
                        .parsedAt(LocalDateTime.now())
                        .build();
            }

            log.info("Extracted goodsCode: {}", goodsCode);

            // 2. API 직접 호출
            String apiUrl = buildSummaryApiUrl(goodsCode);
            log.info("Calling API: {}", apiUrl);

            String apiResponse = callInterparkSummaryApi(apiUrl);

            // 3. API 응답 파싱
            if (apiResponse == null || apiResponse.isEmpty()) {
                log.error("Empty API response");
                return PerformanceResponseDto.builder()
                        .sourceUrl(url)
                        .goodsName("API 응답이 없습니다")
                        .parsedAt(LocalDateTime.now())
                        .build();
            }

            PerformanceResponseDto result = extractPerformanceFromApiResponse(apiResponse, url);
            log.info("✅ Successfully parsed performance: {}", result.getGoodsName());
            return result;

        } catch (Exception e) {
            log.error("Error parsing Interpark URL", e);
            return PerformanceResponseDto.builder()
                    .sourceUrl(url)
                    .goodsName("파싱 오류: " + e.getMessage())
                    .parsedAt(LocalDateTime.now())
                    .build();
        }
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
     * HTTP 요청으로 API 호출 (Java 11+ HttpClient 사용)
     */
    private String callInterparkSummaryApi(String apiUrl) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("✅ API call successful (status: 200)");
                log.info("   - Response size: {} bytes", response.body().length());
                return response.body();
            } else {
                log.error("API call failed with status: {}", response.statusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error calling Interpark summary API", e);
            return null;
        }
    }

    /**
     * API 응답(JSON)에서 핵심 공연 정보 추출
     */
    private PerformanceResponseDto extractPerformanceFromApiResponse(String jsonResponse, String sourceUrl) {
        PerformanceResponseDto.PerformanceResponseDtoBuilder builder = PerformanceResponseDto.builder()
                .sourceUrl(sourceUrl)
                .parsedAt(LocalDateTime.now());

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> responseData = mapper.readValue(jsonResponse,
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

            log.debug("Parsed API response with {} top-level keys", responseData.size());

            // 공연 정보 추출
            extractPerformanceFields(responseData, builder);

            log.debug("Successfully extracted performance fields from API response");
            return builder.build();

        } catch (Exception e) {
            log.error("Error extracting performance from API response", e);
            builder.goodsName("API 응답 파싱 오류: " + e.getMessage());
            return builder.build();
        }
    }

    private void extractPerformanceFields(Map<String, Object> performance,
                                         PerformanceResponseDto.PerformanceResponseDtoBuilder builder) {
        // API 응답이 nested된 경우 "data" 필드에서 실제 데이터 추출
        @SuppressWarnings("unchecked")
        Map<String, Object> dataObj = (Map<String, Object>) performance.get("data");
        if (dataObj != null) {
            performance = dataObj;
        }

        // ========== 12개 핵심 필드 추출 ==========

        // 1. goodsName - 공연 제목
        String goodsName = getStringValue(performance, "goodsName");
        if (goodsName != null) {
            builder.goodsName(goodsName);
            log.info("✅ [1/12] Extracted goodsName: {}", goodsName);
        }

        // 2. subGoodsName - 공연 부제목
        String subGoodsName = getStringValue(performance, "subGoodsName");
        if (subGoodsName != null) {
            builder.subGoodsName(subGoodsName);
            log.info("✅ [2/12] Extracted subGoodsName: {}", subGoodsName);
        }

        // 3. placeName - 공연 장소
        String placeName = getStringValue(performance, "placeName");
        if (placeName != null) {
            builder.placeName(placeName);
            log.info("✅ [3/12] Extracted placeName: {}", placeName);
        }

        // 4. viewRateName - 관람 연령
        String viewRateName = getStringValue(performance, "viewRateName");
        if (viewRateName != null) {
            builder.viewRateName(viewRateName);
            log.info("✅ [4/12] Extracted viewRateName: {}", viewRateName);
        }

        // 5. runningTime - 공연 시간
        String runningTime = getStringValue(performance, "runningTime");
        if (runningTime != null) {
            builder.runningTime(runningTime);
            log.info("✅ [5/12] Extracted runningTime: {} minutes", runningTime);
        }

        // 6. playStartDate - 공연 시작일 (YYYYMMDD → YYYY.MM.DD)
        String playStartDateStr = getStringValue(performance, "playStartDate");
        if (playStartDateStr != null) {
            String parsedStartDate = parsePlayDate(playStartDateStr);
            builder.playStartDate(parsedStartDate);
            log.info("✅ [6/12] Extracted playStartDate: {} → {}", playStartDateStr, parsedStartDate);
        }

        // 7. playEndDate - 공연 종료일 (YYYYMMDD → YYYY.MM.DD)
        String playEndDateStr = getStringValue(performance, "playEndDate");
        if (playEndDateStr != null) {
            String parsedEndDate = parsePlayDate(playEndDateStr);
            builder.playEndDate(parsedEndDate);
            log.info("✅ [7/12] Extracted playEndDate: {} → {}", playEndDateStr, parsedEndDate);
        }

        // 8. goodsLargeImageUrl - 공연 포스터 이미지 URL
        String imageUrl = getStringValue(performance, "goodsLargeImageUrl");
        if (imageUrl != null) {
            if (imageUrl.startsWith("//")) {
                imageUrl = "https:" + imageUrl;
            }
            builder.goodsLargeImageUrl(imageUrl);
            log.info("✅ [8/12] Extracted goodsLargeImageUrl: {}", imageUrl);
        }

        // 9. ticketOpenDate - 티켓 오픈 날짜
        String ticketOpenDate = getStringValue(performance, "ticketOpenDate");
        if (ticketOpenDate != null) {
            builder.ticketOpenDate(ticketOpenDate);
            log.info("✅ [9/12] Extracted ticketOpenDate: {}", ticketOpenDate);
        }

        // 10. bookingEndDate - 예매 종료 날짜
        String bookingEndDate = getStringValue(performance, "bookingEndDate");
        if (bookingEndDate != null) {
            builder.bookingEndDate(bookingEndDate);
            log.info("✅ [10/12] Extracted bookingEndDate: {}", bookingEndDate);
        }

        // 11. ticketCastCount - 티켓캐스트 개수
        Object ticketCastCountObj = performance.get("ticketCastCount");
        if (ticketCastCountObj != null) {
            try {
                Integer ticketCastCount = Integer.valueOf(ticketCastCountObj.toString());
                builder.ticketCastCount(ticketCastCount);
                log.info("✅ [11/12] Extracted ticketCastCount: {}", ticketCastCount);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse ticketCastCount: {}", ticketCastCountObj);
            }
        }

        log.info("✅ Successfully extracted all 12 core performance fields");

        // 12. weekRank - 콘서트 주간 순위
        String weekRank = getStringValue(performance, "weekRank");
        if (weekRank != null) {
            builder.weekRank(weekRank);
            log.info("✅ Extracted weekRank: {}", weekRank);
        }
    }

    /**
     * YYYYMMDD 형식의 날짜 문자열을 YYYY.MM.DD 형식으로 변환
     */
    private String parsePlayDate(String dateStr) {
        try {
            if (dateStr == null || dateStr.length() < 8) {
                return null;
            }
            // YYYYMMDD 형식 파싱
            int year = Integer.parseInt(dateStr.substring(0, 4));
            int month = Integer.parseInt(dateStr.substring(4, 6));
            int day = Integer.parseInt(dateStr.substring(6, 8));

            return String.format("%04d.%02d.%02d", year, month, day);
        } catch (Exception e) {
            log.warn("Failed to parse play date: {}", dateStr, e);
            return null;
        }
    }

    /**
     * URL에서 goods code 추출 (예: https://tickets.interpark.com/goods/26003042 -> 26003042)
     */
    private String extractGoodsCodeFromUrl(String url) {
        try {
            // /goods/ 이후의 숫자 추출
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
            log.debug("Error extracting goods code from URL: {}", url, e);
        }
        return null;
    }

    /**
     * Map에서 String 값 추출 (null 안전)
     */
    private String getStringValue(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof String) return (String) value;
        return value.toString();
    }
}
