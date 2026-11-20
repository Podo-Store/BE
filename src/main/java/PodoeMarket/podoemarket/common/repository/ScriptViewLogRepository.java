package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.admin.dto.response.ViewLogResponseDTO;
import PodoeMarket.podoemarket.common.entity.ScriptViewLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ScriptViewLogRepository extends JpaRepository<ScriptViewLogEntity, Long> {
    // 검색 대상이 아닌 컬럼은 null로 전달 (모두 null이면 검색 조건 없음)
    @Query(value = """
    SELECT new PodoeMarket.podoemarket.admin.dto.response.ViewLogResponseDTO(
        l.id, l.userId, u.nickname, u.email, l.productId, p.title, l.viewedAt)
    FROM ScriptViewLogEntity l
    LEFT JOIN UserEntity u ON u.id = l.userId
    LEFT JOIN ProductEntity p ON p.id = l.productId
    WHERE (:start IS NULL OR l.viewedAt >= :start)
    AND (:end IS NULL OR l.viewedAt < :end)
    AND ((:nickname IS NULL AND :email IS NULL AND :title IS NULL)
        OR u.nickname LIKE CONCAT('%', :nickname, '%')
        OR u.email LIKE CONCAT('%', :email, '%')
        OR p.title LIKE CONCAT('%', :title, '%'))
    ORDER BY l.viewedAt DESC, l.id DESC
""", countQuery = """
    SELECT COUNT(l)
    FROM ScriptViewLogEntity l
    LEFT JOIN UserEntity u ON u.id = l.userId
    LEFT JOIN ProductEntity p ON p.id = l.productId
    WHERE (:start IS NULL OR l.viewedAt >= :start)
    AND (:end IS NULL OR l.viewedAt < :end)
    AND ((:nickname IS NULL AND :email IS NULL AND :title IS NULL)
        OR u.nickname LIKE CONCAT('%', :nickname, '%')
        OR u.email LIKE CONCAT('%', :email, '%')
        OR p.title LIKE CONCAT('%', :title, '%'))
""")
    Page<ViewLogResponseDTO> findViewLogs(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("nickname") String nickname,
                                          @Param("email") String email,
                                          @Param("title") String title,
                                          Pageable pageable);
}
