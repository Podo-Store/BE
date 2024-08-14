package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.entity.OrderItemEntity;
import PodoeMarket.podoemarket.entity.OrdersEntity;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.repository.OrderItemRepository;
import PodoeMarket.podoemarket.repository.OrderRepository;
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
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static PodoeMarket.podoemarket.Utils.EntityToDTOConverter.convertToOrderItemDTO;
import static PodoeMarket.podoemarket.Utils.EntityToDTOConverter.convertToProductList;

@RequiredArgsConstructor
@Slf4j
@Service
public class MypageService {
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
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

    public List<DateProductDTO> getAllMyProducts(UUID id) {
        final List<ProductEntity> products = productRepo.findAllByUserId(id);

        // 날짜별로 작품을 그룹화하기 위한 맵 선언
        final Map<LocalDate, List<ProductListDTO>> myProducts = new HashMap<>();

        for (ProductEntity product : products) {
            final ProductListDTO productListDTO = convertToProductList(product);

            final LocalDate date = product.getCreatedAt().toLocalDate(); // localdatetime -> localdate
            // 날짜에 따른 리스트를 초기화하고 추가 - date라는 key가 없으면 만들고, productListDTO을 value로 추가
            myProducts.computeIfAbsent(date, k -> new ArrayList<>()).add(productListDTO);
        }

        // DateProductDTO로 변환
        return myProducts.entrySet().stream()
                .map(entry -> new DateProductDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateWriter(UUID id, String writer) {
        final List<ProductEntity> products = productRepo.findAllByUserId(id);

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
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        final Date time = new Date();
        final String name = files[0].getOriginalFilename();
        final String[] fileName = new String[]{Objects.requireNonNull(name).substring(0, name.length() - 4)};

        final String filePath = scriptImageBucketFolder + fileName[0] + "\\" + title + "\\" + dateFormat.format(time);

        final ObjectMetadata metadata = new ObjectMetadata();
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
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        final Date time = new Date();
        final String name = files[0].getOriginalFilename();
        final String[] fileName = new String[]{Objects.requireNonNull(name).substring(0, name.length() - 4)};

        final String filePath = descriptionBucketFolder + fileName[0] + "\\" + title + "\\" + dateFormat.format(time);

        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(files[0].getSize());
        metadata.setContentType("application/pdf");

        amazonS3.putObject(bucket, filePath, files[0].getInputStream(), metadata);

        return amazonS3.getUrl(bucket, filePath).toString();
    }

    public List<DateOrderDTO> getAllMyOrdersWithProducts(UUID userId) {
        final List<OrdersEntity> orders = orderRepo.findAllByUserId(userId);

        // 날짜별로 주문 항목을 그룹화하기 위한 맵 선언
        final Map<LocalDate, List<OrderItemDTO>> OrderItems = new HashMap<>();

        for (OrdersEntity order : orders) {
            // 각 주문의 주문 항목을 가져옴
            final List<OrderItemEntity> orderItems = orderItemRepo.findByOrderId(order.getId());

            for (OrderItemEntity orderItem : orderItems) {
                // 각 주문 항목에 대한 제품 정보 가져옴
                final ProductEntity product = productRepo.findById(orderItem.getProduct().getId());
                final OrderItemDTO orderItemDTO = convertToOrderItemDTO(orderItem, product);

                final LocalDate orderDate = order.getCreatedAt().toLocalDate(); // localdatetime -> localdate
                // 날짜에 따른 리스트를 초기화하고 추가 - orderDate라는 key가 없으면 만들고, orderItemDTO를 value로 추가
                OrderItems.computeIfAbsent(orderDate, k -> new ArrayList<>()).add(orderItemDTO);
            }
        }

        // DateOrderDTO로 변환
        return OrderItems.entrySet().stream()
                .map(entry -> new DateOrderDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public OrderItemEntity orderItem(UUID orderId) {
        if(orderItemRepo.findById(orderId) == null) {
            throw new RuntimeException("일치하는 구매 목록 없음");
        }

        return orderItemRepo.findById(orderId);
    }

    public void contractStatusUpdate(UUID id) {
        final OrderItemEntity item = orderItemRepo.findById(id);
        final int contractStatus = item.getContractStatus();

        if (contractStatus == 1) {
            item.setContractStatus(2);
            orderItemRepo.save(item);
        }
    }
}
