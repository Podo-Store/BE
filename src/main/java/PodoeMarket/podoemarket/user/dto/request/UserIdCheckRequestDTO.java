package PodoeMarket.podoemarket.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdCheckRequestDTO {
    private String userId;
    private Boolean check; // 회원가입, 비밀번호 찾기 구분
}
