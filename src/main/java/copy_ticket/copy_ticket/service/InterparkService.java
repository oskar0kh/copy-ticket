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
                String posterUrl = images.stream()
                        .filter(img -> {
                            String src = img.attr("src");
                            String alt = img.attr("alt");
                            String cls = img.attr("class");
                            return src.contains("poster") || alt.contains("poster") ||
                                    cls.contains("poster") || cls.contains("image") ||
                                    src.contains("goods");
                        })
                        .findFirst()
                        .map(img -> img.attr("src"))
                        .orElse(null);

                if (posterUrl == null && !images.isEmpty()) {
                    posterUrl = images.get(0).attr("src");
                }

                if (posterUrl != null && !posterUrl.isEmpty()) {
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

        // 6. 공연장 정보 추출
        String venue = extractVenue(doc);
        if (venue != null) {
            builder.venue(venue);
            log.debug("Extracted venue: {}", venue);
        }

        // 7. 가격 정보 추출
        String priceRange = extractPriceRange(doc);
        if (priceRange != null) {
            builder.priceRange(priceRange);
            log.debug("Extracted price range: {}", priceRange);
        }

        // 8. 공연 일정 추출
        List<PerformanceDto.PerformanceSchedule> schedules = extractSchedules(doc);
        if (!schedules.isEmpty()) {
            builder.schedules(schedules);
            log.debug("Extracted {} schedules", schedules.size());
        }

        // 9. 예매 오픈 시간 추출
        LocalDateTime reservationOpenAt = extractReservationOpenAt(doc);
        if (reservationOpenAt != null) {
            builder.reservationOpenAt(reservationOpenAt);
            log.debug("Extracted reservation open time: {}", reservationOpenAt);
        }

        // 10. 예매 URL 추출
        String reservationUrl = extractReservationUrl(doc);
        if (reservationUrl != null) {
            builder.reservationUrl(reservationUrl);
            log.debug("Extracted reservation URL: {}", reservationUrl);
        }

        return builder.build();
    }

    /**
     * 문서에서 공연 일정을 추출합니다.
     */
    private List<PerformanceDto.PerformanceSchedule> extractSchedules(Document doc) {
        List<PerformanceDto.PerformanceSchedule> schedules = new ArrayList<>();

        // 1. 특정 클래스에서 날짜 찾기
        Elements dateElements = doc.select("[class*='date'], [class*='time'], [class*='schedule'], [class*='performance']");

        for (Element element : dateElements) {
            String text = element.text().trim();
            if (text.isEmpty()) continue;

            // 2. 텍스트에서 날짜 패턴 찾기 (예: "2024.03.15 19:00")
            String datePattern = "(\\d{4})\\.(\\d{1,2})\\.(\\d{1,2})\\s+(\\d{1,2}):(\\d{2})";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(datePattern);
            java.util.regex.Matcher matcher = pattern.matcher(text);

            while (matcher.find() && schedules.size() < 10) {
                try {
                    int year = Integer.parseInt(matcher.group(1));
                    int month = Integer.parseInt(matcher.group(2));
                    int day = Integer.parseInt(matcher.group(3));
                    int hour = Integer.parseInt(matcher.group(4));
                    int minute = Integer.parseInt(matcher.group(5));

                    LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute);

                    PerformanceDto.PerformanceSchedule schedule = PerformanceDto.PerformanceSchedule.builder()
                            .startDateTime(dateTime)
                            .runtimeMinutes("정보없음")
                            .build();

                    if (!schedules.contains(schedule)) {
                        schedules.add(schedule);
                        log.debug("Extracted schedule: {}", dateTime);
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse schedule: {}", text, e);
                }
            }
        }

        return schedules;
    }

    /**
     * 문서에서 공연장 정보를 추출합니다.
     */
    private String extractVenue(Document doc) {
        // 1. 특정 클래스/속성 탐색
        Element venueElement = doc.selectFirst("[class*='venue'], [class*='hall'], [class*='location'], [class*='place']");
        if (venueElement != null && !venueElement.text().isEmpty()) {
            return venueElement.text().trim();
        }

        // 2. Meta 태그에서 탐색
        Element metaVenue = doc.selectFirst("meta[name='venue'], meta[property='venue']");
        if (metaVenue != null) {
            return metaVenue.attr("content");
        }

        // 3. dt/dd 구조에서 탐색 (전형적인 정보 제시 패턴)
        Elements dts = doc.select("dt, th");
        for (Element dt : dts) {
            String text = dt.text().toLowerCase();
            if (text.contains("공연장") || text.contains("장소") || text.contains("venue")) {
                Element dd = dt.nextElementSibling();
                if (dd != null) {
                    return dd.text().trim();
                }
            }
        }

        return null;
    }

    /**
     * 문서에서 가격 정보를 추출합니다.
     */
    private String extractPriceRange(Document doc) {
        // 1. 특정 클래스 탐색
        Element priceElement = doc.selectFirst("[class*='price'], [class*='amount'], [class*='cost']");
        if (priceElement != null && !priceElement.text().isEmpty()) {
            return priceElement.text().trim();
        }

        // 2. Meta 태그에서 탐색
        Element metaPrice = doc.selectFirst("meta[name='price'], meta[property='price']");
        if (metaPrice != null) {
            return metaPrice.attr("content");
        }

        // 3. dt/dd 구조에서 탐색
        Elements dts = doc.select("dt, th");
        for (Element dt : dts) {
            String text = dt.text().toLowerCase();
            if (text.contains("가격") || text.contains("금액") || text.contains("price")) {
                Element dd = dt.nextElementSibling();
                if (dd != null) {
                    return dd.text().trim();
                }
            }
        }

        // 4. 텍스트에서 가격 패턴 찾기 (숫자 + 원)
        String pricePattern = "(\\d{1,3}(?:,\\d{3})*원|\\d+원)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(pricePattern);
        java.util.regex.Matcher matcher = pattern.matcher(doc.body().text());
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * 문서에서 예매 오픈 시간을 추출합니다.
     */
    private LocalDateTime extractReservationOpenAt(Document doc) {
        // 1. Meta 태그에서 탐색
        Element metaOpenAt = doc.selectFirst("meta[name='reservation-open-at'], meta[property='reservation-open-at']");
        if (metaOpenAt != null) {
            try {
                return LocalDateTime.parse(metaOpenAt.attr("content"));
            } catch (Exception e) {
                log.debug("Failed to parse reservation open time from meta tag");
            }
        }

        // 2. 특정 클래스에서 탐색
        Elements openElements = doc.select("[class*='open'], [class*='start'], [class*='reservation']");
        for (Element elem : openElements) {
            String text = elem.text().toLowerCase();
            if (text.contains("예매") && (text.contains("시작") || text.contains("오픈"))) {
                String datePattern = "(\\d{4})(?:\\.(\\d{1,2}))?(?:\\.(\\d{1,2}))?\\s+(\\d{1,2}):(\\d{2})";
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(datePattern);
                java.util.regex.Matcher matcher = pattern.matcher(elem.text());

                if (matcher.find()) {
                    try {
                        int year = Integer.parseInt(matcher.group(1));
                        int month = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 1;
                        int day = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 1;
                        int hour = Integer.parseInt(matcher.group(4));
                        int minute = Integer.parseInt(matcher.group(5));

                        return LocalDateTime.of(year, month, day, hour, minute);
                    } catch (Exception e) {
                        log.debug("Failed to parse reservation open time");
                    }
                }
            }
        }

        return null;
    }

    /**
     * 문서에서 예매 URL을 추출합니다.
     */
    private String extractReservationUrl(Document doc) {
        // 1. 예매 버튼 찾기
        Elements buttons = doc.select("a[href*='reservation'], a[href*='booking'], button, a");
        for (Element button : buttons) {
            String text = button.text().toLowerCase();
            String href = button.attr("href");
            if ((text.contains("예매") || text.contains("booking")) && !href.isEmpty()) {
                if (href.startsWith("http")) {
                    return href;
                } else if (href.startsWith("/")) {
                    return "https://tickets.interpark.com" + href;
                }
            }
        }

        return null;
    }
}
