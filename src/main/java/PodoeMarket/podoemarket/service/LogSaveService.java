package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.common.entity.ScriptViewLogEntity;
import PodoeMarket.podoemarket.common.entity.UserAccessLogEntity;
import PodoeMarket.podoemarket.common.repository.ScriptViewLogRepository;
import PodoeMarket.podoemarket.common.repository.UserAccessLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

// 로그 저장은 요청 흐름에 영향을 주지 않도록 비동기로 처리
@Slf4j
@Service
@RequiredArgsConstructor
public class LogSaveService {
    private final UserAccessLogRepository userAccessLogRepo;
    private final ScriptViewLogRepository scriptViewLogRepo;

    @Async
    public void saveAccessLog(UUID userId, LocalDateTime accessedAt) {
        try {
            userAccessLogRepo.save(UserAccessLogEntity.builder()
                    .userId(userId)
                    .accessedAt(accessedAt)
                    .build());
        } catch (Exception e) {
            log.warn("접속 로그 저장 실패 userId={}", userId, e);
        }
    }

    @Async
    public void saveScriptViewLog(UUID productId, UUID userId, LocalDateTime viewedAt) {
        try {
            scriptViewLogRepo.save(ScriptViewLogEntity.builder()
                    .productId(productId)
                    .userId(userId)
                    .viewedAt(viewedAt)
                    .build());
        } catch (Exception e) {
            log.warn("작품 열람 로그 저장 실패 productId={}, userId={}", productId, userId, e);
        }
    }
}
