package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.dto.ProductDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.repository.ProductLikeRepository;
import PodoeMarket.podoemarket.repository.ProductRepository;
import PodoeMarket.podoemarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class MypageService {
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final ProductLikeRepository productLikeRepo;

    public void userUpdate(UUID id, final UserEntity userEntity) {
        final String password = userEntity.getPassword();
        final String nickname = userEntity.getNickname();
        final String type = userEntity.getType();
        final String filepath = userEntity.getFilePath();

        final UserEntity user = userRepo.findById(id);

        if(!user.getNickname().equals(nickname)){
            if(userRepo.existsByNickname(nickname)){
                throw new RuntimeException("Nickname is already exists");
            }
        }


        if(userEntity.getType() != null && userEntity.getFilePath() != null) {
            if(!Objects.equals(type, "image/jpeg") && !Objects.equals(type, "image/png")) {
                throw new RuntimeException("file type is wrong");
            }
        }

        user.setPassword(password);
        user.setNickname(nickname);
        user.setType(type);
        user.setFilePath(filepath);

        userRepo.save(user);
    }

    public Boolean checkUser(UUID id, final String password, final PasswordEncoder encoder) {
        try{
            final UserEntity originalUser = userRepo.findById(id);

            return originalUser != null && encoder.matches(password, originalUser.getPassword());
        } catch (Exception e){
            log.error("MypageService.checkUser 메소드 중 예외 발생", e);
            return false;
        }
    }

    public UserEntity originalUser(UUID id) {
        return userRepo.findById(id);
    }

    public List<ProductDTO> getAllProducts(String nickname) {
        List<ProductEntity> products = productRepo.findAllByWriter(nickname);

        return products.stream()
                .map(product -> EntityToDTOConverter.converToProductDTO(product, productLikeRepo))
                .collect(Collectors.toList());
    }

    public ProductEntity product(UUID id) {
        return productRepo.findById(id);
    }

    public void productUpdate(UUID id, final ProductEntity productEntity) {
        final ProductEntity product = productRepo.findById(id);

        // 사진 추가
        product.setGenre(productEntity.getGenre());
        product.setCharacterNumber(productEntity.getCharacterNumber());
        product.setRuntime(productEntity.getRuntime());
        product.setTitle(productEntity.getTitle());
        product.setStory(productEntity.getStory());
        product.setScript(productEntity.isScript());
        product.setPerformance(productEntity.isPerformance());
        product.setScriptPrice(productEntity.getScriptPrice());
        product.setPerformancePrice(productEntity.getPerformancePrice());
        product.setContent(productEntity.getContent());

        productRepo.save(product);
    }
}
