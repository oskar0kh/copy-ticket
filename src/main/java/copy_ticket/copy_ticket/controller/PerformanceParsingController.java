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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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

    /**
     * 사용자의 저장된 공연 목록을 조회하는 API
     * - 저장된 공연의 ID, title, goodsCode 반환
     * - 최신순 정렬
     *
     * @param authentication 인증된 사용자 정보 (Spring Security)
     * @return ResponseEntity: { id, title, goodsCode } 리스트
     */
    @GetMapping("/list")
    public ResponseEntity<List<PerformanceService.PerformanceListItemDto>> getPerformanceList(
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
            String userId = authentication.getName();

            // 3. Service 호출 (저장된 공연 목록 조회)
            List<PerformanceService.PerformanceListItemDto> performanceList = performanceService.getPerformanceListByUserId(userId);

            return ResponseEntity.ok(performanceList);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "공연 목록 조회 중 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }

    /**
     * 특정 공연 정보를 조회하는 API
     * - 저장된 공연의 전체 정보 반환
     * - 보안: 조회하려는 사용자가 해당 공연의 소유자인지 확인
     *
     * @param performanceId 공연 ID
     * @param authentication 인증된 사용자 정보 (Spring Security)
     * @return ResponseEntity: PerformanceResponseDto (공연 상세 정보)
     */
    @GetMapping("/{performanceId}")
    public ResponseEntity<PerformanceResponseDto> getPerformanceById(
            @PathVariable Long performanceId,
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
            String userId = authentication.getName();

            // 3. Service 호출 (특정 공연 정보 조회)
            PerformanceResponseDto performance = performanceService.getPerformanceById(performanceId, userId);

            return ResponseEntity.ok(performance);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            // 공연 정보 없음 또는 권한 없음
            int statusCode = e.getMessage().contains("not found") ? HttpStatus.NOT_FOUND.value() : HttpStatus.FORBIDDEN.value();
            throw new ResponseStatusException(
                    HttpStatus.resolve(statusCode),
                    e.getMessage()
            );
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "공연 정보 조회 중 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }
}


