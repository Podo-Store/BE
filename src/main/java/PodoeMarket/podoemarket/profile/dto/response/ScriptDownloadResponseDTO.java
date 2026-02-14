package PodoeMarket.podoemarket.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptDownloadResponseDTO {
    private String fileName;
    private byte[] fileData;
}
