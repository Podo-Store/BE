package PodoeMarket.podoemarket.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindPasswordResponseDTO {
    private UUID id;
    private String userId;
    private String email;
    private String password;
    private String nickname;
    private String accessToken; // jwt 저장공간
    private String refreshToken; // jwt 저장공간
}
