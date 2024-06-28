package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.dto.ProductDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.repository.ProductLikeRepository;
import PodoeMarket.podoemarket.repository.ProductRepository;
import PodoeMarket.podoemarket.repository.UserRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.folder.folderName1}")
    private String userImageBucketFolder;

    @Value("${cloud.aws.s3.folder.folderName3}")
    private String scriptImageBucketFolder;

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

        user.setPassword(password);
        user.setNickname(nickname);
        user.setType(type);
        user.setFilePath(filepath);

        userRepo.save(user);
    }

    public String uploadUserImage(MultipartFile file) throws IOException {
        if(!Objects.equals(file.getContentType(), "image/jpeg") && !Objects.equals(file.getContentType(), "image/png")) {
            throw new RuntimeException("file type is wrong");
        }

        String filePath = userImageBucketFolder + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        amazonS3.putObject(bucket, filePath, file.getInputStream(), metadata);

        return amazonS3.getUrl(bucket, filePath).toString();
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

    public void productUpdate(UUID id, final ProductEntity productEntity) {
        final ProductEntity product = productRepo.findById(id);

        product.setImagePath(productEntity.getImagePath());
        product.setImageType(productEntity.getImageType());
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

    public String uploadScriptImage(MultipartFile file) throws IOException {
        if(!Objects.equals(file.getContentType(), "image/jpeg") && !Objects.equals(file.getContentType(), "image/png")) {
            throw new RuntimeException("file type is wrong");
        }

        String filePath = scriptImageBucketFolder + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        amazonS3.putObject(bucket, filePath, file.getInputStream(), metadata);

        return amazonS3.getUrl(bucket, filePath).toString();
    }
}
