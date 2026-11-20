package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.admin.dto.response.AccessLogResponseDTO;
import PodoeMarket.podoemarket.common.entity.UserAccessLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserAccessLogRepository extends JpaRepository<UserAccessLogEntity, Long> {
    // 검색 대상이 아닌 컬럼은 null로 전달 (모두 null이면 검색 조건 없음)
    @Query(value = """
    SELECT new PodoeMarket.podoemarket.admin.dto.response.AccessLogResponseDTO(
        l.id, l.userId, u.nickname, u.email, l.accessedAt)
    FROM UserAccessLogEntity l
    LEFT JOIN UserEntity u ON u.id = l.userId
    WHERE (:start IS NULL OR l.accessedAt >= :start)
    AND (:end IS NULL OR l.accessedAt < :end)
    AND ((:nickname IS NULL AND :email IS NULL)
        OR u.nickname LIKE CONCAT('%', :nickname, '%')
        OR u.email LIKE CONCAT('%', :email, '%'))
    ORDER BY l.accessedAt DESC, l.id DESC
""", countQuery = """
    SELECT COUNT(l)
    FROM UserAccessLogEntity l
    LEFT JOIN UserEntity u ON u.id = l.userId
    WHERE (:start IS NULL OR l.accessedAt >= :start)
    AND (:end IS NULL OR l.accessedAt < :end)
    AND ((:nickname IS NULL AND :email IS NULL)
        OR u.nickname LIKE CONCAT('%', :nickname, '%')
        OR u.email LIKE CONCAT('%', :email, '%'))
""")
    Page<AccessLogResponseDTO> findAccessLogs(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              @Param("nickname") String nickname,
                                              @Param("email") String email,
                                              Pageable pageable);
}
