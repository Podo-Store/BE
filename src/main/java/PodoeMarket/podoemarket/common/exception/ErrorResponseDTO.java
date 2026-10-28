package PodoeMarket.podoemarket.common.exception;

import lombok.Getter;

@Getter
public class ErrorResponseDTO {

    private final String error; // 기존 ResponseDTO.error와 호환 - 메세지를 담음
    private final String code;

    public ErrorResponseDTO(ErrorCode errorCode) {
        this.error = errorCode.getMessage();
        this.code = errorCode.name();
    }
}
