package PodoeMarket.podoemarket.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindUserIdRequestDTO {
    private String email;
    private String authNum;
}
