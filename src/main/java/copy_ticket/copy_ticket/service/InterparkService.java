package copy_ticket.copy_ticket.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import copy_ticket.copy_ticket.dto.PerformanceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class InterparkService {

    /**
     * Playwright 사용 -> 인터파크 티켓 페이지 렌더링해서 파싱
     *
     * @param url Interpark 상품 URL (예: https://tickets.interpark.com/goods/26003042)
     * @return 파싱된 공연 정보
     */
    public PerformanceDto parseInterParkUrl(String url) {
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
            PerformanceDto result = htmlPerformanceFilter(content, url);

            log.info("Successfully parsed performance: {}", result.getTitle());
            return result;

        } catch (Exception e) {
            log.error("Error parsing Interpark URL: {}", url, e);
            throw new RuntimeException("Failed to parse Interpark page: " + e.getMessage(), e);
        }
    }

    /**
     * HTML 콘텐츠에서 fallback JSON 부분을 추출하고 공연 정보를 파싱합니다
     */
    private PerformanceDto htmlPerformanceFilter(String htmlContent, String sourceUrl) {
        PerformanceDto.PerformanceDtoBuilder builder = PerformanceDto.builder()
                .sourceUrl(sourceUrl)
                .parsedAt(LocalDateTime.now());

        try {
            // 1. URL에서 goodsCode 추출 (예: https://tickets.interpark.com/goods/26003042)
            String urlGoodsCode = extractGoodsCodeFromUrl(sourceUrl);
            log.debug("Extracted goods code from URL: {}", urlGoodsCode);

            // 2. HTML에서 "fallback": 부분 찾기
            int fallbackIdx = htmlContent.indexOf("\"fallback\":");
            if (fallbackIdx == -1) {
                log.warn("Could not find fallback section in HTML");
                builder.title("공연 정보 없음");
                return builder.build();
            }

            // 3. fallback 이후의 JSON 부분 추출
            String fallbackJson = htmlContent.substring(fallbackIdx);

            // 4. goodsCode 찾기
            int goodsCodeIdx = fallbackJson.indexOf("\"goodsCode\":\"");
            if (goodsCodeIdx != -1) {
                int startIdx = goodsCodeIdx + "\"goodsCode\":\"".length();
                int endIdx = fallbackJson.indexOf("\"", startIdx);
                String goodsCode = fallbackJson.substring(startIdx, endIdx);

                log.debug("Extracted goods code from fallback: {}", goodsCode);

                // URL의 goodsCode와 비교
                if (urlGoodsCode != null && !urlGoodsCode.equals(goodsCode)) {
                    log.warn("Goods code mismatch: URL={}, fallback={}", urlGoodsCode, goodsCode);
                } else {
                    log.info("Goods code matched: {}", goodsCode);
                }
            }

            // 5. goodsName 찾기
            int goodsNameIdx = fallbackJson.indexOf("\"goodsName\":\"");
            if (goodsNameIdx != -1) {
                int startIdx = goodsNameIdx + "\"goodsName\":\"".length();
                int endIdx = fallbackJson.indexOf("\"", startIdx);
                String goodsName = fallbackJson.substring(startIdx, endIdx);
                builder.title(goodsName);
                log.debug("Extracted goods name: {}", goodsName);
            }

            // 6. posterImageUrl 찾기
            int posterIdx = fallbackJson.indexOf("\"posterImageUrl\":\"");
            if (posterIdx != -1) {
                int startIdx = posterIdx + "\"posterImageUrl\":\"".length();
                int endIdx = fallbackJson.indexOf("\"", startIdx);
                String posterImageUrl = fallbackJson.substring(startIdx, endIdx);

                // URL 프로토콜 추가 (상대경로인 경우)
                if (posterImageUrl.startsWith("//")) {
                    posterImageUrl = "https:" + posterImageUrl;
                }
                builder.posterImageUrl(posterImageUrl);
                log.debug("Extracted poster image URL: {}", posterImageUrl);
            }

        } catch (Exception e) {
            log.error("Error filtering HTML performance data", e);
        }

        return builder.build();
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
