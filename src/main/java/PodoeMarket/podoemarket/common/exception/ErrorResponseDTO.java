package PodoeMarket.podoemarket.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ErrorResponseDTO {

    private final int status;
    private final String code;
    private final String message;

    public ErrorResponseDTO(HttpStatus status, String message) {
        this.status = status.value();
        this.code = status.name();
        this.message = message;
    }
}
