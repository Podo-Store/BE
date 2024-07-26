package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.dto.ProductListDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
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
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.folder.folderName2}")
    private String scriptImageBucketFolder;

    @Value("${cloud.aws.s3.folder.folderName3}")
    private String descriptionBucketFolder;

    public void userUpdate(UUID id, final UserEntity userEntity) {
        final String password = userEntity.getPassword();
        final String nickname = userEntity.getNickname();

        final UserEntity user = userRepo.findById(id);

        // 비밀번호
        if(password == null) {
            throw new RuntimeException("password가 올바르지 않음");        }

        // 닉네임
        if(nickname == null || nickname.isBlank()) {
            throw new RuntimeException("nickname이 올바르지 않음");
        }

        if(!user.getNickname().equals(nickname)){
            if(userRepo.existsByNickname(nickname)){
                throw new RuntimeException("이미 존재하는 닉네임");
            }
        }

        user.setPassword(password);
        user.setNickname(nickname);

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

    public List<ProductListDTO> getAllMyProducts(UUID id) {
        List<ProductEntity> products = productRepo.findAllByUserId(id);

        return products.stream()
                .map(EntityToDTOConverter::converToProductList)
                .collect(Collectors.toList());
    }

    public void productUpdate(UUID id, final ProductEntity productEntity) {
        final ProductEntity product = productRepo.findById(id);

        product.setImagePath(productEntity.getImagePath());
        product.setImageType(productEntity.getImageType());
        product.setTitle(productEntity.getTitle());
        product.setScript(productEntity.isScript());
        product.setPerformance(productEntity.isPerformance());
        product.setScriptPrice(productEntity.getScriptPrice());
        product.setPerformancePrice(productEntity.getPerformancePrice());
        product.setDescriptionPath(productEntity.getDescriptionPath());
        product.setDescriptionType(productEntity.getDescriptionType());

        productRepo.save(product);
    }

    public String uploadScriptImage(MultipartFile[] file) throws IOException {
        if(!Objects.equals(file[0].getContentType(), "image/jpeg") && !Objects.equals(file[0].getContentType(), "image/jpeg")) {
            throw new RuntimeException("file type is wrong");
        }

        String filePath = scriptImageBucketFolder + file[0].getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file[0].getSize());
        metadata.setContentType(file[0].getContentType());

        amazonS3.putObject(bucket, filePath, file[0].getInputStream(), metadata);

        return amazonS3.getUrl(bucket, filePath).toString();
    }

    public String uploadDescription(MultipartFile[] file) throws IOException {
        if(!Objects.equals(file[0].getContentType(), "application/pdf") && !Objects.equals(file[0].getContentType(), "application/pdf")) {
            throw new RuntimeException("file type is wrong");
        }

        String filePath = descriptionBucketFolder + file[0].getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file[0].getSize());
        metadata.setContentType(file[0].getContentType());

        amazonS3.putObject(bucket, filePath, file[0].getInputStream(), metadata);

        return amazonS3.getUrl(bucket, filePath).toString();
    }
}
