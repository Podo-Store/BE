package PodoeMarket.podoemarket.profile.dto.response;

import PodoeMarket.podoemarket.dto.response.DateScriptOrderDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderScriptsResponseDTO {
    private String nickname;
    List<DateScriptOrderDTO> orderList;
}
