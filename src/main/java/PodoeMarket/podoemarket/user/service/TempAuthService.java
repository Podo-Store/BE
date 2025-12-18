package PodoeMarket.podoemarket.user.service;

import PodoeMarket.podoemarket.service.VerificationService;
import PodoeMarket.podoemarket.user.dto.OAuthUserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class TempAuthService {
    private static final String PREFIX = "temp-auth:";
    private static final long TTL_SECONDS = 300; // 5분

    private final VerificationService verificationService;
    private final ObjectMapper objectMapper;

    /** tempCode 저장 (TTL 포함) */
    public String store(OAuthUserDTO oauthUser) {
        try {
            String tempCode = UUID.randomUUID().toString();
            String key = PREFIX + tempCode;

            String value = objectMapper.writeValueAsString(oauthUser);
            verificationService.setDataExpire(key, value, TTL_SECONDS);

            return tempCode;
        } catch (Exception e) {
            throw new IllegalStateException("tempCode 저장 실패", e);
        }
    }

    /** tempCode 조회 */
    public OAuthUserDTO get(String tempCode) {
        String key = PREFIX + tempCode;
        String value = verificationService.getData(key);

        if (value == null)
            throw new RuntimeException("만료되었거나 유효하지 않은 tempCode");

        try {
            return objectMapper.readValue(value, OAuthUserDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("tempCode 파싱 실패", e);
        }
    }

    /** 1회 사용 후 삭제 */
    public void remove(String tempCode) {
        verificationService.deleteData(PREFIX + tempCode);
    }
}
