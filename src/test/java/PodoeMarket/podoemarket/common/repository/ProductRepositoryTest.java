package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static PodoeMarket.podoemarket.common.entity.type.PlayType.LONG;
import static PodoeMarket.podoemarket.common.entity.type.PlayType.SHORT;
import static PodoeMarket.podoemarket.common.entity.type.ProductStatus.PASS;
import static PodoeMarket.podoemarket.common.entity.type.ProductStatus.RE_PASS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("작품 목록을 playType으로 필터링한다")
    @ParameterizedTest(name = "playType={0}일 때 {1}개 조회된다")
    @CsvSource({"LONG, 3", "SHORT, 2"})
    void findAllValidProductsByPlayType(PlayType playType, int expectedCount) {
        // given
        ProductEntity product1 = createProduct("희곡1", "작가1", true, true, SHORT, PASS);
        ProductEntity product2 = createProduct("희곡2", "작가2", true, true, LONG, PASS);
        ProductEntity product3 = createProduct("희곡3", "작가3", true, true, LONG, PASS);
        ProductEntity product4 = createProduct("희곡4", "작가4", true, true, SHORT, RE_PASS);
        ProductEntity product5 = createProduct("희곡5", "작가5", true, true, LONG, RE_PASS);
        productRepository.saveAll(List.of(product1, product2, product3, product4, product5));

        // when
        Page<ProductEntity> products = productRepository.findAllValidProducts(List.of(PASS, RE_PASS), playType, "", PageRequest.of(0, 20));

        // then
        assertThat(products.getContent())
                .extracting("playType")
                .containsOnly(playType)
                .hasSize(expectedCount);
    }

    @DisplayName("작품 목록은 playType이 null이면 전체 조회한다")
    @Test
    void findAllValidProductsWhenPlayTypeIsNull() {
        // given
        ProductEntity product1 = createProduct("희곡1", "작가1", true, true, SHORT, PASS);
        ProductEntity product2 = createProduct("희곡2", "작가2", true, true, LONG, PASS);
        ProductEntity product3 = createProduct("희곡3", "작가3", true, true, SHORT, RE_PASS);
        ProductEntity product4 = createProduct("희곡4", "작가4", true, true, LONG, RE_PASS);
        productRepository.saveAll(List.of(product1, product2, product3, product4));

        // when
        Page<ProductEntity> products = productRepository.findAllValidProducts(List.of(PASS, RE_PASS), null, "", PageRequest.of(0, 20));

        // then
        assertThat(products.getContent())
                .hasSize(4);
    }

    @DisplayName("작품 목록에서 제목 또는 작가명의 검색어가 포함된 상품만 조회한다")
    @ParameterizedTest(name = "search={0}일 때 {1}개 조회된다")
    @CsvSource({"희곡, 4", "작가, 3", "단막극, 0", "공모작, 1", "'',5"})
    void findAllValidProductsBySearch(String search, int expectedCount) {
        // given
        ProductEntity product1 = createProduct("희곡1", "작가1", true, true, SHORT, PASS);
        ProductEntity product2 = createProduct("희곡2", "작가2", true, true, LONG, PASS);
        ProductEntity product3 = createProduct("희곡3", "작가3", true, true, SHORT, RE_PASS);
        ProductEntity product4 = createProduct("희곡4", "시인1", true, true, LONG, RE_PASS);
        ProductEntity product5 = createProduct("공모작품", "공모작_김아무개", true, true, LONG, RE_PASS);
        productRepository.saveAll(List.of(product1, product2, product3, product4, product5));

        // when
        Page<ProductEntity> products = productRepository.findAllValidProducts(List.of(PASS, RE_PASS), null, search, PageRequest.of(0, 20));

        // then
        assertThat(products.getContent())
                .hasSize(expectedCount);
    }

    @DisplayName("작품 목록은 심사, 삭제, 판매 조건을 만족한다")
    @Test
    void findAllValidProductsWithDefaultConditions() {
        // given

        // when

        // then
    }

    @DisplayName("작품 목록에서 playType과 검색어 조건을 동시에 적용한다")
    @Test
    void findAllValidProductsByPlayTypeAndSearch() {
        // given

        // when

        // then
    }

    @DisplayName("작품 목록이 정렬 조건에 따라 조회된다")
    @Test
    void findAllValidProductsWithSort() {
        // given

        // when

        // then
    }

    private ProductEntity createProduct(String title, String writer, boolean script, boolean performance, PlayType playType, ProductStatus checked) {
        return ProductEntity.builder()
                .title(title)
                .writer(writer)
                .script(script)
                .performance(performance)
                .playType(playType)
                .checked(checked)
                .filePath("dummy/path")
                .build();
    }

}