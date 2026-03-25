package copy_ticket.copy_ticket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import copy_ticket.copy_ticket.dto.PerformanceResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class InterparkService {

    /**
     * Playwright 사용 -> 인터파크 티켓 페이지 렌더링해서 파싱
     *
     * @param url Interpark 상품 URL (예: https://tickets.interpark.com/goods/26003042)
     * @return 파싱된 공연 정보
     */
    public PerformanceResponseDto parseInterParkUrl(String url) {
        log.info("Starting to parse Interpark URL: {}", url);

        // 1. Playwright 객체를 생성하고, URL을 랜더링 할 headless 브라우저(화면 안뜨는 브라우저), 컨택스트, 페이지 생성
        try (Playwright playwright = Playwright.create()) {   // Playwright 인스턴스 생성
            Browser browser = playwright.chromium().launch(); // Chromium 브라우저 실행
            BrowserContext context = browser.newContext();    // 새로운 브라우저 컨텍스트 생성 (세션 격리)
            Page page = context.newPage();                    // 새로운 페이지 생성

            // 2. URL로 페이지 네비게이트
            log.info("Navigating to URL...");
            page.navigate(url);

            // 3. 완전히 렌더링 될때까지 대기
            page.waitForLoadState(LoadState.NETWORKIDLE);
            log.info("Page loaded successfully");

            // 4. 완전히 렌더링 된 HTML 수집
            String content = page.content();

            // 5. HTML 필터링을 통해 핵심 공연 정보 추출
            PerformanceResponseDto result = htmlPerformanceFilter(content, url);

            log.info("Successfully parsed performance: {}", result.getTitle());
            return result;

        } catch (Exception e) {
            log.error("Error parsing Interpark URL: {}", url, e);
            throw new RuntimeException("Failed to parse Interpark page: " + e.getMessage(), e);
        }
    }

    /**
     * HTML 콘텐츠에서 fallback JSON 부분을 추출하고 공연 정보 파싱하는 메서드 (개선 버전)
     */
    private PerformanceResponseDto htmlPerformanceFilter(String htmlContent, String sourceUrl) {
        PerformanceResponseDto.PerformanceResponseDtoBuilder builder = PerformanceResponseDto.builder()
                .sourceUrl(sourceUrl)
                .parsedAt(LocalDateTime.now());

        try {
            // 1. URL에서 goodsCode 추출
            String urlGoodsCode = extractGoodsCodeFromUrl(sourceUrl);
            log.debug("Extracted goods code from URL: {}", urlGoodsCode);

            // 2. HTML에서 "fallback": 부분 찾기
            int fallbackIdx = htmlContent.indexOf("\"fallback\":");
            if (fallbackIdx == -1) {
                log.warn("Could not find fallback section in HTML");
                builder.title("공연 정보 없음");
                return builder.build();
            }

            // 3. fallback 이후의 JSON 추출 (객체 또는 배열)
            String fallbackContent = htmlContent.substring(fallbackIdx + "\"fallback\":".length()).trim();

            // 4. JSON 값 추출 (객체 또는 배열)
            int jsonStart = -1;
            char firstChar = fallbackContent.charAt(0);

            if (firstChar == '{') {
                jsonStart = 0;
            } else if (firstChar == '[') {
                jsonStart = 0;
            } else if (firstChar == '"') {
                // 문자열인 경우 처리 안 함
                log.warn("Fallback is a string, skipping");
                builder.title("공연 정보 없음");
                return builder.build();
            }

            if (jsonStart == -1) {
                log.warn("Could not determine fallback type");
                builder.title("공연 정보 없음");
                return builder.build();
            }

            // 5. JSON 값의 끝 위치 찾기 (중괄호 또는 대괄호 매칭)
            int jsonEnd = firstChar == '{'
                ? findMatchingBracket(fallbackContent, 0, '{', '}')
                : findMatchingBracket(fallbackContent, 0, '[', ']');

            if (jsonEnd == -1) {
                log.warn("Could not find matching bracket");
                builder.title("공연 정보 없음");
                return builder.build();
            }

            String jsonStr = fallbackContent.substring(jsonStart, jsonEnd + 1);
            log.debug("Extracted JSON length: {}", jsonStr.length());

            // 6. Jackson으로 파싱 및 공연 정보 추출
            ObjectMapper mapper = new ObjectMapper();

            if (firstChar == '[') {
                // fallback이 배열인 경우
                List<Map<String, Object>> list = mapper.readValue(jsonStr,
                        mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                Map<String, Object> matched = findMatchingPerformance(list, urlGoodsCode);
                if (matched != null) {
                    extractPerformanceFields(matched, builder);
                    return builder.build();
                }
            } else {
                // fallback이 객체인 경우: {"@\"/goods/recent\",\"?goodsCodes=XXX\",":[...], ...}
                Map<String, Object> fallbackObj = mapper.readValue(jsonStr,
                        mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

                log.debug("Fallback object has {} keys", fallbackObj.size());

                // 1단계: fallback의 모든 배열에서 goodsCode와 일치하는 항목 수집
                List<Map<String, Object>> allMatchedPerformances = new ArrayList<>();

                for (Map.Entry<String, Object> entry : fallbackObj.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    // 직접 배열인 경우
                    if (value instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> items = (List<Map<String, Object>>) value;
                        log.debug("Searching in direct array at key: {}", key);
                        for (Map<String, Object> item : items) {
                            String itemGoodsCode = getStringValue(item, "goodsCode");
                            if (urlGoodsCode != null && urlGoodsCode.equals(itemGoodsCode)) {
                                allMatchedPerformances.add(item);
                                log.debug("Found match in {}: {}", key, urlGoodsCode);
                            }
                        }
                    }
                    // 객체 내부의 배열인 경우
                    else if (value instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> innerObj = (Map<String, Object>) value;
                        for (Map.Entry<String, Object> innerEntry : innerObj.entrySet()) {
                            Object innerValue = innerEntry.getValue();
                            if (innerValue instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> items = (List<Map<String, Object>>) innerValue;
                                log.debug("Searching in nested array at key: {}.{}", key, innerEntry.getKey());
                                for (Map<String, Object> item : items) {
                                    String itemGoodsCode = getStringValue(item, "goodsCode");
                                    if (urlGoodsCode != null && urlGoodsCode.equals(itemGoodsCode)) {
                                        allMatchedPerformances.add(item);
                                        log.debug("Found match in {}.{}: {}", key, innerEntry.getKey(), urlGoodsCode);
                                    }
                                }
                            }
                        }
                    }
                }

                // 2단계: 찾은 항목들을 병합 (기본 정보 + 상세 정보)
                if (!allMatchedPerformances.isEmpty()) {
                    Map<String, Object> mergedPerformance = mergePerformanceData(allMatchedPerformances);
                    extractPerformanceFields(mergedPerformance, builder);
                    log.info("Successfully extracted merged performance data with {} items", allMatchedPerformances.size());
                    return builder.build();
                }
            }

            log.warn("No matching performance found in all fallback arrays");
            builder.title("공연 정보 없음");

        } catch (Exception e) {
            log.error("Error filtering HTML performance data", e);
            builder.title("파싱 오류: " + e.getMessage());
        }

        return builder.build();
    }

    /**
     * 여러 공연 정보 항목을 병합 (기본 정보 + 상세 정보)
     * /goods/recent의 기본 정보와 /banner의 상세 정보를 합치기
     */
    private Map<String, Object> mergePerformanceData(List<Map<String, Object>> performances) {
        if (performances.isEmpty()) {
            return null;
        }

        // 1. 기본 정보를 담을 merged map (첫 번째 항목으로 시작)
        Map<String, Object> merged = new java.util.HashMap<>(performances.get(0));

        // 2. 나머지 항목들에서 누락된 필드를 채우기
        for (int i = 1; i < performances.size(); i++) {
            Map<String, Object> item = performances.get(i);
            for (Map.Entry<String, Object> entry : item.entrySet()) {
                // null이거나 빈 값인 필드만 채우기
                String key = entry.getKey();
                Object value = entry.getValue();

                if (!merged.containsKey(key) || merged.get(key) == null ||
                    (merged.get(key) instanceof String && ((String) merged.get(key)).isEmpty())) {
                    if (value != null && !(value instanceof String && ((String) value).isEmpty())) {
                        merged.put(key, value);
                        log.debug("Merged field {}: {}", key, value);
                    }
                }
            }
        }

        log.info("Successfully merged {} performance items", performances.size());
        return merged;
    }

    /**
     * 공연 목록에서 urlGoodsCode와 일치하는 공연 찾기
     */
    private Map<String, Object> findMatchingPerformance(List<Map<String, Object>> performanceList, String urlGoodsCode) {
        for (Map<String, Object> item : performanceList) {
            String itemGoodsCode = getStringValue(item, "goodsCode");
            if (urlGoodsCode != null && urlGoodsCode.equals(itemGoodsCode)) {
                log.info("Found matching performance with goodsCode: {}", urlGoodsCode);
                return item;
            }
        }

        // 일치하는 것이 없으면 첫 번째 반환
        if (!performanceList.isEmpty()) {
            log.warn("No exact match found, using first item");
            return performanceList.get(0);
        }

        return null;
    }

    /**
     * JSON에서 일치하는 닫는 괄호의 위치를 찾기 (여러 타입의 괄호 지원)
     * 문자열 내부의 괄호는 무시
     */
    private int findMatchingBracket(String text, int openIdx, char openChar, char closeChar) {
        int count = 1;
        boolean inString = false;
        boolean escaped = false;

        for (int i = openIdx + 1; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == openChar) {
                    count++;
                } else if (c == closeChar) {
                    count--;
                    if (count == 0) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    /**
     * JSON 배열에서 일치하는 닫는 대괄호의 위치를 찾기 (하위 호환성)
     * 문자열 내부의 괄호는 무시
     */
    private int findMatchingBracket(String text, int openIdx) {
        return findMatchingBracket(text, openIdx, '[', ']');
    }

    /**
     * Map에서 문자열 값을 안전하게 추출
     */
    private String getStringValue(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * JSON 객체에서 공연 정보 필드 추출 및 builder 설정
     */
    private void extractPerformanceFields(Map<String, Object> performance,
                                         PerformanceResponseDto.PerformanceResponseDtoBuilder builder) {
        // 기본 공연 정보
        String title = getStringValue(performance, "goodsName");
        if (title != null) {
            builder.title(title);
        }

        String imageUrl = getStringValue(performance, "posterImageUrl");
        if (imageUrl != null) {
            if (imageUrl.startsWith("//")) {
                imageUrl = "https:" + imageUrl;
            }
            builder.imageUrl(imageUrl);
        }

        // 예매 시간
        builder.startDate(getStringValue(performance, "bookStartDate"));
        builder.endDate(getStringValue(performance, "bookEndDate"));

        // 공연 링크
        builder.link(getStringValue(performance, "goodsUrl"));

        // 인터파크 상품 정보
        builder.goodsCode(getStringValue(performance, "goodsCode"));
        builder.goodsName(getStringValue(performance, "goodsName"));

        // 공연장 정보
        builder.placeCode(getStringValue(performance, "placeCode"));
        builder.placeName(getStringValue(performance, "placeName"));

        // 공연 일정
        builder.playDate(getStringValue(performance, "playDate"));

        // playStartDate, playEndDate 파싱
        String playStartDateStr = getStringValue(performance, "playStartDate");
        String playEndDateStr = getStringValue(performance, "playEndDate");

        if (playStartDateStr != null) {
            builder.playStartDate(parsePlayDate(playStartDateStr));
        }
        if (playEndDateStr != null) {
            builder.playEndDate(parsePlayDate(playEndDateStr));
        }

        log.debug("Successfully extracted all performance fields");
    }

    /**
     * YYYYMMDD 형식의 날짜 문자열을 LocalDateTime으로 변환
     */
    private LocalDateTime parsePlayDate(String dateStr) {
        try {
            if (dateStr == null || dateStr.length() < 8) {
                return null;
            }
            // YYYYMMDD 형식 파싱
            int year = Integer.parseInt(dateStr.substring(0, 4));
            int month = Integer.parseInt(dateStr.substring(4, 6));
            int day = Integer.parseInt(dateStr.substring(6, 8));

            return LocalDate.of(year, month, day).atStartOfDay();
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
}
