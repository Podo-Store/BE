package PodoeMarket.podoemarket.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),

    // 작품 관리
    NOT_PRODUCT_OWNER(HttpStatus.FORBIDDEN, "작가가 아닙니다."),
    REVIEW_ALREADY_REQUESTED(HttpStatus.CONFLICT, "재심사 결과 대기 중에는 재신청이 불가합니다."),
    REVIEW_NOT_ALLOWED(HttpStatus.CONFLICT, "심사 승인된 작품만 재심사를 신청할 수 있습니다."),

    // 파일
    FILE_NOT_SELECTED(HttpStatus.BAD_REQUEST, "선택된 파일이 없음"),
    INVALID_SCRIPT_TYPE(HttpStatus.BAD_REQUEST, "PDF 파일만 업로드 가능합니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일은 50MB 이하만 업로드 가능합니다.");

    private final HttpStatus status;
    private final String message;
}