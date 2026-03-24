package copy_ticket.copy_ticket.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import copy_ticket.copy_ticket.dto.PerformanceDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

            // 5. Jsoup으로 파싱 후 document 객체로 변환
            Document doc = Jsoup.parse(content);

            // 6. extractPerformanceInfo 메서드로 객체 전달 -> HTML 구조 분석 및 정보 추출
            PerformanceDto result = extractPerformanceInfo(doc, url);

            log.info("Successfully parsed performance: {}", result.getTitle());
            return result;

        } catch (Exception e) {
            log.error("Error parsing Interpark URL: {}", url, e);
            throw new RuntimeException("Failed to parse Interpark page: " + e.getMessage(), e);
        }
    }

    /**
     * Jsoup Document에서 공연 정보를 추출하는 메서드
     */
    private PerformanceDto extractPerformanceInfo(Document doc, String sourceUrl) {
        PerformanceDto.PerformanceDtoBuilder builder = PerformanceDto.builder()
                .sourceUrl(sourceUrl)
                .parsedAt(LocalDateTime.now());

        // 1. 페이지 제목 (기본 제목으로 사용)
        String pageTitle = doc.title();
        log.debug("Page title: {}", pageTitle);

        // 2. Meta 태그에서 OG 정보 추출 (있으면)
        String ogTitle = doc.selectFirst("meta[property=og:title]") != null
                ? doc.selectFirst("meta[property=og:title]").attr("content")
                : null;
        String ogImage = doc.selectFirst("meta[property=og:image]") != null
                ? doc.selectFirst("meta[property=og:image]").attr("content")
                : null;
        String ogDescription = doc.selectFirst("meta[property=og:description]") != null
                ? doc.selectFirst("meta[property=og:description]").attr("content")
                : null;

        if (ogTitle != null) builder.title(ogTitle);
        if (ogImage != null) builder.posterImageUrl(ogImage);
        if (ogDescription != null) builder.description(ogDescription);

        // 3. 주요 제목 요소 탐색 (OG 태그가 없는 경우)
        if (builder.build().getTitle() == null) {
            Element h1 = doc.selectFirst("h1");
            if (h1 != null) {
                builder.title(h1.text());
            } else {
                Element h2 = doc.selectFirst("h2");
                if (h2 != null) {
                    builder.title(h2.text());
                } else {
                    builder.title(pageTitle);
                }
            }
        }

        // 4. 이미지 탐색 (포스터 후보)
        if (builder.build().getPosterImageUrl() == null) {
            Elements images = doc.select("img");
            if (!images.isEmpty()) {
                // 가장 큰 이미지 또는 특정 패턴의 이미지 찾기
                String posterUrl = images.stream()
                        .filter(img -> {
                            String src = img.attr("src");
                            String alt = img.attr("alt");
                            String cls = img.attr("class");
                            // 포스터 후보: src나 alt에 특정 키워드 포함
                            return src.contains("poster") || alt.contains("poster") ||
                                    cls.contains("poster") || cls.contains("image");
                        })
                        .findFirst()
                        .map(img -> img.attr("src"))
                        .orElse(null);

                if (posterUrl == null && !images.isEmpty()) {
                    posterUrl = images.get(0).attr("src");
                }

                if (posterUrl != null) {
                    builder.posterImageUrl(posterUrl);
                }
            }
        }

        // 5. 설명 텍스트 탐색
        if (builder.build().getDescription() == null) {
            Element description = doc.selectFirst("p");
            if (description != null) {
                builder.description(description.text());
            }
        }

        // 6. 공연 일정 탐색 (간단한 예시)
        List<PerformanceDto.PerformanceSchedule> schedules = extractSchedules(doc);
        if (!schedules.isEmpty()) {
            builder.schedules(schedules);
        }

        return builder.build();
    }

    /**
     * 문서에서 공연 일정을 추출합니다.
     */
    private List<PerformanceDto.PerformanceSchedule> extractSchedules(Document doc) {
        List<PerformanceDto.PerformanceSchedule> schedules = new ArrayList<>();

        // 날짜/시간 정보를 찾기 위해 일반적인 패턴 사용
        Elements dateElements = doc.select("[class*='date'], [class*='time'], [class*='schedule']");

        dateElements.stream()
                .limit(5) // 최대 5개까지만
                .forEach(element -> {
                    String text = element.text();
                    if (!text.isEmpty()) {
                        // 실제 파싱은 인터파크의 정확한 HTML 구조에 따라 수정 필요
                        log.debug("Schedule candidate: {}", text);
                    }
                });

        return schedules;
    }
}
