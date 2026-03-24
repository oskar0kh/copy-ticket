package copy_ticket.copy_ticket.controller;

import copy_ticket.copy_ticket.domain.entity.Performance;
import copy_ticket.copy_ticket.dto.PerformanceResponseDto;
import copy_ticket.copy_ticket.dto.PerformanceSaveRequestDto;
import copy_ticket.copy_ticket.service.InterparkService;
import copy_ticket.copy_ticket.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceParsingController {

    private final InterparkService interparkService;
    private final PerformanceService performanceService;

    /**
     * Interpark 콘서트 URL을 파싱해서 공연 정보를 반환하는 API
     *  1. React의 `performanceApi.js` 의 parseInterParkUrl() 메서드에서 호출
     *  2. URL 유효성 검증 (인터파크 티켓 URL 형식인지 확인) 후
     *  3. URL이 유효하다면, InterparkService의 parseInterParkUrl() 메서드 호출
     *      -> Playwright로 페이지 렌더링, Jsoup으로 HTML 파싱, 공연 정보 추출
     *
     * @param request URL을 포함한 요청 (Map)
     * @return 파싱된 공연 정보 (PerformanceResponseDto)
     */
    @PostMapping("/parse")
    public ResponseEntity<PerformanceResponseDto> parseInterParkUrl(@RequestBody Map<String, String> request) {
        try {
            // 1. 프론트에서 보낸 request body에서 URL 추출
            String url = request.get("url");

            // 2. URL이 들어있는지 확인
            if (url == null || url.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "URL은 필수 입력값입니다."
                );
            }

            // 3. URL이 인터파크 티켓 URL 형식인지 검증 (프론트에서도 검증했지만, 한번 더 검증)
            if (!url.startsWith("https://tickets.interpark.com/goods/")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "유효한 인터파크 티켓 URL이 아닙니다. (형식: https://tickets.interpark.com/goods/{id})"
                );
            }

            // 4. URL이 유효하다면, InterparkService의 parseInterParkUrl() 메서드 호출
            //    -> Playwright로 페이지 렌더링, Jsoup으로 HTML 파싱, 공연 정보 추출
            //    -> 파싱된 공연 정보를 PerformanceResponseDto로 반환
            PerformanceResponseDto result = interparkService.parseInterParkUrl(url);
            return ResponseEntity.ok(result);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "페이지 파싱 중 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }

    /**
     * 파싱된 공연 정보를 DB에 저장하는 API
     * - 사용자당 최대 5개까지만 저장
     * - 5개 초과 시 가장 오래된 공연을 soft delete한 후 새로운 공연 저장
     *
     * @param saveRequestDto DB에 저장할 공연 정보 (PerformanceSaveRequestDto)
     * @param authentication 인증된 사용자 정보 (Spring Security)
     * @return ResponseEntity: { id, title, message }
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> savePerformance(
            @RequestBody PerformanceSaveRequestDto saveRequestDto,
            Authentication authentication
    ) {
        try {
            // 1. 인증 확인
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "로그인이 필요합니다."
                );
            }

            // 2. 인증된 사용자의 username 추출
            String username = authentication.getName();

            // 3. Service 호출 (DTO → Entity 변환 후 DB 저장)
            Performance savedPerformance = performanceService.savePerformance(saveRequestDto, username);

            // 4. 저장된 공연 정보 응답
            return ResponseEntity.ok(Map.of(
                    "id", savedPerformance.getId(),
                    "title", savedPerformance.getTitle(),
                    "message", "공연 정보가 저장되었습니다."
            ));

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "공연 정보 저장 중 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }
}


