package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.entity.OrderItemEntity;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.repository.OrderItemRepository;
import PodoeMarket.podoemarket.repository.ProductRepository;
import PodoeMarket.podoemarket.repository.UserRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static PodoeMarket.podoemarket.Utils.EntityToDTOConverter.convertToOrderItemDTO;
import static PodoeMarket.podoemarket.Utils.EntityToDTOConverter.convertToProductList;

@RequiredArgsConstructor
@Slf4j
@Service
public class MypageService {
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final OrderItemRepository orderItemRepo;
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.folder.folderName2}")
    private String scriptImageBucketFolder;

    @Value("${cloud.aws.s3.folder.folderName3}")
    private String descriptionBucketFolder;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    @Transactional
    public UserEntity updateUser(final UserEntity userEntity) {
        final UserEntity user = userRepo.findById(userEntity.getId());

        if(userEntity.getPassword() != null && !userEntity.getPassword().isBlank())
            user.setPassword(userEntity.getPassword());

        if(!userEntity.getNickname().isEmpty()){
            if(!userEntity.getNickname().equals(user.getNickname())) {
                if(userRepo.existsByNickname(userEntity.getNickname())){
                    throw new RuntimeException("이미 존재하는 닉네임");
                }
            }
            user.setNickname(userEntity.getNickname());
            updateWriter(userEntity.getId(), userEntity.getNickname());
        }

        return userRepo.save(user);
    }

    public Boolean checkUser(final UUID id, final String password, final PasswordEncoder encoder) {
        try{
            final UserEntity originalUser = userRepo.findById(id);

            return originalUser != null && encoder.matches(password, originalUser.getPassword());
        } catch (Exception e){
            log.error("MypageService.checkUser 메소드 중 예외 발생", e);
            return false;
        }
    }

    public UserEntity originalUser(final UUID id) {
        return userRepo.findById(id);
    }

    public List<DateProductDTO> getAllMyProducts(final UUID id) {
        final List<ProductEntity> products = productRepo.findAllByUserId(id);

        // 날짜별로 작품을 그룹화하기 위한 맵 선언
        final Map<LocalDate, List<ProductListDTO>> myProducts = new HashMap<>();

        for (ProductEntity product : products) {
            final ProductListDTO productListDTO = convertToProductList(product, bucketURL);

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
    public void updateWriter(final UUID id, final String writer) {
        final List<ProductEntity> products = productRepo.findAllByUserId(id);

        for (ProductEntity product : products) {
            product.setWriter(writer);
        }

        productRepo.saveAll(products);
    }

    public void productUpdate(final UUID id, final ProductEntity productEntity) {
        final ProductEntity product = productRepo.findById(id);

        if(!product.isChecked()) {
            throw new RuntimeException("등록 심사 중인 작품");
        }

        product.setImagePath(productEntity.getImagePath());
        product.setTitle(productEntity.getTitle());
        product.setScript(productEntity.isScript());
        product.setPerformance(productEntity.isPerformance());
        product.setScriptPrice(productEntity.getScriptPrice());
        product.setPerformancePrice(productEntity.getPerformancePrice());
        product.setDescriptionPath(productEntity.getDescriptionPath());

        productRepo.save(product);
    }

    public void deleteScript(final UUID id) {
        final String S3Key = productRepo.findById(id).getFilePath();

        if(amazonS3.doesObjectExist(bucket, S3Key)) {
            amazonS3.deleteObject(bucket, S3Key);
        }
    }

    public void deleteScriptImage(final UUID id) {
        if(productRepo.findById(id).getImagePath() != null) {
            final String formalS3Key = productRepo.findById(id).getImagePath();

            if(amazonS3.doesObjectExist(bucket, formalS3Key)) {
                amazonS3.deleteObject(bucket, formalS3Key);
            }
        }
    }

    public void deleteDescription(final UUID id) {
        if(productRepo.findById(id).getDescriptionPath() != null) {
            final String formalS3Key = productRepo.findById(id).getDescriptionPath();

            if(amazonS3.doesObjectExist(bucket, formalS3Key)) {
                amazonS3.deleteObject(bucket, formalS3Key);
            }
        }
    }

    public String uploadScriptImage(final MultipartFile[] files, final String title, final UUID id) throws IOException {
        if(files.length > 1) {
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

        // S3 Key 구성
        final String S3Key = scriptImageBucketFolder + fileName[0] + "\\" + title + "\\" + dateFormat.format(time) + ".jpg";

        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(files[0].getSize());
        metadata.setContentType(files[0].getContentType());

        // 기존 파일 삭제
        deleteScriptImage(id);

        // 저장
        amazonS3.putObject(bucket, S3Key, files[0].getInputStream(), metadata);

        return S3Key;
    }

    public String uploadDescription(final MultipartFile[] files, final String title, final UUID id) throws IOException {
        if(files.length > 1) {
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

        // S3 Key 구성
        final String S3Key = descriptionBucketFolder + fileName[0] + "\\" + title + "\\" + dateFormat.format(time) + ".pdf";

        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(files[0].getSize());
        metadata.setContentType("application/pdf");

        // 기존 파일 삭제
        deleteDescription(id);

        // 저장
        amazonS3.putObject(bucket, S3Key, files[0].getInputStream(), metadata);

        return S3Key;
    }

    public String extractS3KeyFromURL(final String S3URL) throws Exception {
        String decodedUrl = URLDecoder.decode(S3URL, StandardCharsets.UTF_8);
        final URL url = new URL(decodedUrl);

        return url.getPath().startsWith("/") ? url.getPath().substring(1) : url.getPath();
    }

    @Transactional
    public void deleteProduct(final UUID productId, final UUID userId) {
        final ProductEntity product =  productRepo.findById(productId);

        if(!product.getUser().getId().equals(userId)) {
            throw new RuntimeException("작가가 아님");
        }

        if(!product.isChecked()) {
            throw new RuntimeException("심사 중");
        }

        // 예비용
//        // OrderItemEntity의 외래 키를 NULL로 설정
//        for (OrderItemEntity orderItem : product.getOrderItem()) {
//            orderItem.setProduct(null);
//            orderItemRepo.save(orderItem);
//        }

        deleteScript(product.getId());
        deleteScriptImage(product.getId());
        deleteDescription(product.getId());

        productRepo.delete(product);
    }

    public List<DateOrderDTO> getAllMyOrderScriptWithProducts(final UUID userId) {
        // 모든 필요한 OrderItemEntity를 한 번에 가져옴
        final List<OrderItemEntity> allOrderItems = orderItemRepo.findAllByUserIdAndScript(userId, true);

        // 날짜별로 주문 항목을 그룹화하기 위한 맵 선언
        final Map<LocalDate, List<OrderItemDTO>> orderItemsGroupedByDate = new HashMap<>();

        // 제품별로 공연권 구매 상태를 캐싱할 맵 (Integer로 contractStatus 값을 저장)
        final Map<ProductEntity, Integer> performanceLogMap = new HashMap<>();

        for (OrderItemEntity orderItem : allOrderItems) {
           ProductEntity product = orderItem.getProduct();

           // product가 null인 경우를 처리
           if (product == null) {
               continue; // product가 null이면 이 항목을 건너뜀
           }

           final OrderItemDTO orderItemDTO = convertToOrderItemDTO(orderItem, orderItem.getProduct(), bucketURL);

           // 날짜에 따른 리스트를 초기화하고 추가 - orderDate라는 key가 없으면 만들고, orderItemDTO를 value로 추가
           LocalDate orderDate = orderItem.getOrder().getCreatedAt().toLocalDate(); // localdatetime -> localdate
           orderItemsGroupedByDate.computeIfAbsent(orderDate, k -> new ArrayList<>()).add(orderItemDTO);
       }

        // DateOrderDTO로 변환
        return orderItemsGroupedByDate.entrySet().stream()
                .map(entry -> new DateOrderDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public List<DateOrderDTO> getAllMyOrderPerformanceWithProducts(final UUID userId) {
        // 각 주문의 주문 항목을 가져옴
        final List<OrderItemEntity> allOrderItems = orderItemRepo.findAllByUserId(userId);

        // 날짜별로 주문 항목을 그룹화하기 위한 맵 선언
        final Map<LocalDate, List<OrderItemDTO>> OrderItems = new HashMap<>();

        for (OrderItemEntity orderItem : allOrderItems) {
            if (orderItem.getPerformanceAmount() > 0) {
                // 각 주문 항목에 대한 제품 정보 가져옴
                final OrderItemDTO orderItemDTO = convertToOrderItemDTO(orderItem, orderItem.getProduct(), bucketURL);

                final LocalDate orderDate = orderItem.getCreatedAt().toLocalDate(); // localdatetime -> localdate
                // 날짜에 따른 리스트를 초기화하고 추가 - orderDate라는 key가 없으면 만들고, orderItemDTO를 value로 추가
                OrderItems.computeIfAbsent(orderDate, k -> new ArrayList<>()).add(orderItemDTO);
            }
        }

        // DateOrderDTO로 변환
        return OrderItems.entrySet().stream()
                .map(entry -> new DateOrderDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public OrderItemEntity orderItem(final UUID orderId) {
        if(orderItemRepo.findById(orderId) == null) {
            throw new RuntimeException("일치하는 구매 목록 없음");
        }

        return orderItemRepo.findById(orderId);
    }

    public void contractStatusUpdate(final UUID id) {
        final OrderItemEntity item = orderItemRepo.findById(id);
        final int contractStatus = item.getContractStatus();

        if (contractStatus == 1) {
            item.setContractStatus(2);
            orderItemRepo.save(item);
        }
    }

    public byte[] downloadFile(final String fileKey, final String email) {
        // S3에서 파일 객체 가져오기
        S3Object s3Object = amazonS3.getObject("podobucket", fileKey);

        try (InputStream inputStream = s3Object.getObjectContent();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            addWatermark(inputStream, outputStream, email);

            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("파일 다운로드 중 오류 발생");
        }
    }

    public void addWatermark(final InputStream src, final ByteArrayOutputStream dest, final String email) {
        try (PdfReader reader = new PdfReader(src);
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdfDoc = new PdfDocument(reader, writer); // PDF 문서를 생성하거나 수정
            Document document = new Document(pdfDoc)) { // PdfDocument를 래핑하여 더 높은 수준의 문서 조작을 가능하게 함

            InputStream logoInputStream = getClass().getClassLoader().getResourceAsStream("logo.png");
            if (logoInputStream == null) {
                throw new FileNotFoundException("Resource not found: logo.png");
            }

            // ImageDataFactory를 사용하여 이미지 데이터를 생성
            ImageData imageData = ImageDataFactory.create(logoInputStream.readAllBytes());
            Image image = new Image(imageData);

            image.setOpacity(0.3f);

            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                PdfPage page = pdfDoc.getPage(i);
                PdfCanvas canvas = new PdfCanvas(page);

                image.setFixedPosition(i, (page.getPageSize().getWidth() - image.getImageWidth()) / 2,
                        (page.getPageSize().getHeight() - image.getImageHeight()) / 2);

                // 텍스트 설정
                canvas.saveState();
                canvas.setFillColor(new DeviceRgb(200, 200, 200));
                canvas.beginText();
                canvas.setFontAndSize(PdfFontFactory.createFont(), 20); // 폰트 및 크기 설정

                // 텍스트 추가
                canvas.showText(email); // showText 메소드를 사용하여 텍스트 추가
                canvas.endText();
                canvas.restoreState();

                // 페이지에 이미지 추가
                document.add(image);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Transactional
    public void deleteUser(final UserEntity userEntity) {
        // s3에 저장된 파일 삭제
        for(ProductEntity product : productRepo.findAllByUserId(userEntity.getId())) {
            deleteScript(product.getId());
            deleteScriptImage(product.getId());
            deleteDescription(product.getId());
        }

        // DB 계정 삭제
        userRepo.delete(userEntity);
    }
}
