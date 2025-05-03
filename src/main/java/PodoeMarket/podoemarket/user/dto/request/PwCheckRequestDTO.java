package PodoeMarket.podoemarket.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PwCheckRequestDTO {
    private String password;
    private String confirmPassword;
}
