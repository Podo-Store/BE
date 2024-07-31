package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.dto.ProductListDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.repository.ProductRepository;
import PodoeMarket.podoemarket.repository.UserRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
                .map(EntityToDTOConverter::convertToProductList)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateWriter(UUID id, String writer) {
        List<ProductEntity> products = productRepo.findAllByUserId(id);

        for (ProductEntity product : products) {
            product.setWriter(writer);
        }

        productRepo.saveAll(products);
    }

    public void productUpdate(UUID id, final ProductEntity productEntity) {
        final ProductEntity product = productRepo.findById(id);

        if(!product.isChecked()) {
            throw new RuntimeException("등록 심사 중인 작품");
        }

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

    public String uploadScriptImage(MultipartFile[] files, String title) throws IOException {
        if(files[0].isEmpty()) {
            throw new RuntimeException("선택된 작품 이미지가 없음");
        }
        else if(files.length > 1) {
            throw new RuntimeException("작품 이미지가 1개를 초과함");
        }

        if(!Objects.equals(files[0].getContentType(), "image/jpeg") && !Objects.equals(files[0].getContentType(), "image/jpeg")) {
            throw new RuntimeException("ScriptImage file type is not jpg");
        }

        // 파일 이름 가공
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date time = new Date();
        String name = files[0].getOriginalFilename();
        String[] fileName = new String[]{Objects.requireNonNull(name).substring(0, name.length() - 4)};

        String filePath = scriptImageBucketFolder + fileName[0] + "\\" + title + "\\" + dateFormat.format(time);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(files[0].getSize());
        metadata.setContentType(files[0].getContentType());

        amazonS3.putObject(bucket, filePath, files[0].getInputStream(), metadata);

        return amazonS3.getUrl(bucket, filePath).toString();
    }

    public String uploadDescription(MultipartFile[] files, String title) throws IOException {
        if(files[0].isEmpty()) {
            throw new RuntimeException("선택된 작품 설명 파일이 없음");
        }
        else if(files.length > 1) {
            throw new RuntimeException("작품 설명 파일 수가 1개를 초과함");
        }

        if(!Objects.equals(files[0].getContentType(), "application/pdf") && !Objects.equals(files[0].getContentType(), "application/pdf")) {
            throw new RuntimeException("Description file type is not PDF");
        }

        // 파일 이름 가공
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date time = new Date();
        String name = files[0].getOriginalFilename();
        String[] fileName = new String[]{Objects.requireNonNull(name).substring(0, name.length() - 4)};

        String filePath = descriptionBucketFolder + fileName[0] + "\\" + title + "\\" + dateFormat.format(time);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(files[0].getSize());
        metadata.setContentType(files[0].getContentType());

        amazonS3.putObject(bucket, filePath, files[0].getInputStream(), metadata);

        return amazonS3.getUrl(bucket, filePath).toString();
    }
}
