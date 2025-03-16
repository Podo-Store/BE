package PodoeMarket.podoemarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private UUID id;
    private String userId;
    private String email;
    private String password;
    private String confirmPassword;
    private String nickname;
    private boolean auth;
    private String authNum;
    private boolean check; // 회원가입, 비밀번호 찾기 구분
    private String accessToken; // jwt 저장공간
    private String refreshToken; // jwt 저장공간
}
