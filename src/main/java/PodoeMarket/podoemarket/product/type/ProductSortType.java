package PodoeMarket.podoemarket.product.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Sort;

@Getter
@AllArgsConstructor
public enum ProductSortType {
    POPULAR("viewCount", Sort.Direction.DESC, true), // JPA 정렬 무시
    LIKE_COUNT("likeCount", Sort.Direction.DESC, true),
    LATEST("createdAt", Sort.Direction.DESC, false);

    private final String property;
    private final Sort.Direction direction;
    private final boolean secondarySort;

    public Sort createSort() {
        Sort primarySort = Sort.by(direction, property);

        if (secondarySort)
            return primarySort.and(Sort.by(Sort.Direction.DESC, "createdAt"));

        return primarySort;
    }
}
