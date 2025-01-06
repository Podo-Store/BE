package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.dto.response.*;
import PodoeMarket.podoemarket.entity.*;
import PodoeMarket.podoemarket.entity.type.ProductStatus;
import PodoeMarket.podoemarket.repository.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
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
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static PodoeMarket.podoemarket.Utils.EntityToDTOConverter.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class MypageService {
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final OrderItemRepository orderItemRepo;
    private final ApplicantRepository applicantRepo;
    private final PerformanceDateRepository performanceDateRepo;
    private final RefundRepository refundRepo;
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.folder.folderName2}")
    private String scriptImageBucketFolder;

    @Value("${cloud.aws.s3.folder.folderName3}")
    private String descriptionBucketFolder;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    private final Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

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
        try {
            final List<ProductEntity> products = productRepo.findAllByUserId(id, sort);

            if (products.isEmpty())
                return Collections.emptyList();

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
        } catch (Exception e) {
            log.error("Error fetching products for user ID: {}", id, e);
            return Collections.emptyList();
        }
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

        if(product.getChecked() == ProductStatus.WAIT)
            throw new RuntimeException("등록 심사 중인 작품");

        product.setImagePath(productEntity.getImagePath());
        product.setTitle(productEntity.getTitle());
        product.setScript(productEntity.isScript());
        product.setPerformance(productEntity.isPerformance());
        product.setScriptPrice(productEntity.getScriptPrice());
        product.setPerformancePrice(productEntity.getPerformancePrice());
        product.setScriptPrice(productEntity.getScriptPrice());
        product.setPerformancePrice(productEntity.getPerformancePrice());
        product.setDescriptionPath(productEntity.getDescriptionPath());
        product.setPlot(productEntity.getPlot());

        productRepo.save(product);
    }

    public String uploadScriptImage(final MultipartFile[] files, final String title, final UUID id) throws IOException {
        if(files.length > 1) {
            throw new RuntimeException("작품 이미지가 1개를 초과함");
        }

        if(!Objects.equals(files[0].getContentType(), "image/jpeg") && !Objects.equals(files[0].getContentType(), "image/jpeg") && !Objects.equals(files[0].getContentType(), "image/png")) {
            throw new RuntimeException("ScriptImage file type is only jpg and png");
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
        if(productRepo.findById(id).getImagePath() != null) {
            final String sourceKey = productRepo.findById(id).getImagePath();

            deleteFile(bucket, sourceKey);
        }

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

        try (InputStream inputStream = files[0].getInputStream()) {
            final PdfDocument doc = new PdfDocument(new PdfReader(inputStream));

            if(doc.getNumberOfPages() > 5)
                throw new RuntimeException("작품 설명 파일이 5페이지를 초과함");
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
        if(productRepo.findById(id).getDescriptionPath() != null) {
            final String sourceKey = productRepo.findById(id).getDescriptionPath();

            deleteFile(bucket, sourceKey);
        }

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

        if(!product.getUser().getId().equals(userId))
            throw new RuntimeException("작가가 아님");

        if(product.getChecked() == ProductStatus.WAIT)
            throw new RuntimeException("심사 중");

        // 탈퇴와 동일한 파일 삭제 처리 필요
        final String filePath = product.getFilePath().replace("script", "delete");
        moveFile(bucket, product.getFilePath(), filePath);
        deleteFile(bucket, product.getFilePath());
        product.setFilePath(filePath);

        product.setImagePath(null);

        if(product.getDescriptionPath() != null) {
            final String descriptionPath = product.getDescriptionPath().replace("description", "delete");
            moveFile(bucket, product.getDescriptionPath(), descriptionPath);
            deleteFile(bucket, product.getDescriptionPath());
            product.setDescriptionPath(descriptionPath);
        }

        productRepo.save(product);
    }

    public List<DateScriptOrderDTO> getAllMyOrderScriptWithProducts(final UUID userId) {
        // 모든 필요한 OrderItemEntity를 한 번에 가져옴
        final List<OrderItemEntity> allOrderItems = orderItemRepo.findAllByUserIdAndScript(userId, true, sort);

        // 날짜별로 주문 항목을 그룹화하기 위한 맵 선언
        final Map<LocalDate, List<OrderScriptDTO>> orderItemsGroupedByDate = new HashMap<>();

        for (OrderItemEntity orderItem : allOrderItems) {
           final OrderScriptDTO orderItemDTO = convertToScriptOrderItemDTO(orderItem, orderItem.getProduct(), bucketURL);

           // 날짜에 따른 리스트를 초기화하고 추가 - orderDate라는 key가 없으면 만들고, orderItemDTO를 value로 추가
           LocalDate orderDate = orderItem.getOrder().getCreatedAt().toLocalDate(); // localdatetime -> localdate
           orderItemsGroupedByDate.computeIfAbsent(orderDate, k -> new ArrayList<>()).add(orderItemDTO);
       }

        // DateOrderDTO로 변환
        return orderItemsGroupedByDate.entrySet().stream()
                .map(entry -> new DateScriptOrderDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public List<DatePerformanceOrderDTO> getAllMyOrderPerformanceWithProducts(final UUID userId) {
        // 각 주문의 주문 항목을 가져옴
        final List<OrderItemEntity> allOrderItems = orderItemRepo.findAllByUserId(userId, sort);

        // 날짜별로 주문 항목을 그룹화하기 위한 맵 선언
        final Map<LocalDate, List<OrderPerformanceDTO>> OrderItems = new HashMap<>();

        for (OrderItemEntity orderItem : allOrderItems) {
            if (orderItem.getPerformanceAmount() > 0) {
                final int dateCount = performanceDateRepo.countByOrderItemId(orderItem.getId());

                // 각 주문 항목에 대한 제품 정보 가져옴
                final OrderPerformanceDTO orderItemDTO = convertToPerformanceOrderItemDTO(orderItem, orderItem.getProduct(), bucketURL, dateCount);

                final LocalDate orderDate = orderItem.getCreatedAt().toLocalDate(); // localdatetime -> localdate
                // 날짜에 따른 리스트를 초기화하고 추가 - orderDate라는 key가 없으면 만들고, orderItemDTO를 value로 추가
                OrderItems.computeIfAbsent(orderDate, k -> new ArrayList<>()).add(orderItemDTO);
            }
        }

        // DateOrderDTO로 변환
        return OrderItems.entrySet().stream()
                .map(entry -> new DatePerformanceOrderDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public OrderItemEntity getOrderItem(final UUID orderItemId) {
        if(orderItemRepo.findById(orderItemId) == null)
            throw new RuntimeException("일치하는 구매 목록 없음");

        return orderItemRepo.findById(orderItemId);
    }

    public byte[] downloadFile(final String fileKey, final String email) {
        // S3에서 파일 객체 가져오기
        final S3Object s3Object = amazonS3.getObject("podobucket", fileKey);

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

            final InputStream logoInputStream = getClass().getClassLoader().getResourceAsStream("logo.png");
            if (logoInputStream == null)
                throw new FileNotFoundException("Resource not found: logo.png");

            // ImageDataFactory를 사용하여 이미지 데이터를 생성
            final ImageData imageData = ImageDataFactory.create(logoInputStream.readAllBytes());
            final Image image = new Image(imageData);

            image.setOpacity(0.3f);

            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                final PdfPage page = pdfDoc.getPage(i);
                final PdfCanvas canvas = new PdfCanvas(page);

                image.setFixedPosition(i, (page.getPageSize().getWidth() - image.getImageWidth()) / 2,
                        (page.getPageSize().getHeight() - image.getImageHeight()) / 2);

                // 텍스트 설정
                canvas.saveState();
                canvas.setFillColor(new DeviceRgb(200, 200, 200));
                canvas.beginText();
                canvas.setFontAndSize(PdfFontFactory.createFont(), 20); // 폰트 및 크기 설정

                // 텍스트 추가
                float x = page.getPageSize().getWidth() / 2 - 100; // X 좌표: 페이지 중앙
                float y = (page.getPageSize().getHeight() - image.getImageHeight()) / 2; // Y 좌표: 이미지 중앙 위로 이동

                canvas.setTextMatrix(x, y); // 텍스트 위치 설정
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
    public void moveFile(final String bucket, final String sourceKey, final String destinationKey) {
        final CopyObjectRequest copyFile = new CopyObjectRequest(bucket,sourceKey, bucket, destinationKey);

        if(amazonS3.doesObjectExist(bucket, sourceKey))
            amazonS3.copyObject(copyFile);
    }

    @Transactional
    public void deleteUser(final UserEntity userEntity) {
        // s3에 저장된 파일 이전 및 삭제
        for(ProductEntity product : productRepo.findAllByUserId(userEntity.getId())) {
            final String filePath = product.getFilePath().replace("script", "delete");
            moveFile(bucket, product.getFilePath(), filePath);
            deleteFile(bucket, product.getFilePath());
            product.setFilePath(filePath);

            product.setImagePath(null);

            if (product.getDescriptionPath() != null) {
                final String descriptionPath = product.getDescriptionPath().replace("description", "delete");
                moveFile(bucket, product.getDescriptionPath(), descriptionPath);
                deleteFile(bucket, product.getDescriptionPath());
                product.setDescriptionPath(descriptionPath);
            }

            productRepo.save(product);
        }

        // DB 계정 삭제
        userRepo.delete(userEntity);
    }

    public void deleteFile(final String bucket, final String sourceKey) {
        if(amazonS3.doesObjectExist(bucket, sourceKey))
            amazonS3.deleteObject(bucket, sourceKey);
    }

    public ApplicantEntity getApplicant(final UUID orderItemId) {
        if(applicantRepo.findByOrderItemId(orderItemId) == null)
            throw new RuntimeException("일치하는 신청자 정보 없음");

        return applicantRepo.findByOrderItemId(orderItemId);
    }

    public void dateRegister(final PerformanceDateEntity performanceDateEntity) {
        performanceDateRepo.save(performanceDateEntity);
    }

    public int registerDatesNum(final UUID orderItemId) {
        return performanceDateRepo.countByOrderItemId(orderItemId);
    }

    public void refundRegister(final RefundEntity refundEntity) {
        refundRepo.save(refundEntity);
    }

    public void expire(final LocalDateTime time) {
        if(LocalDateTime.now().isAfter(time.plusYears(1)))
            throw new RuntimeException("구매 후 1년 경과");
    }

    public void setScriptImageDefault(final UUID productId) {
        final ProductEntity product = productRepo.findById(productId);

        if(product.getImagePath() != null) {
            final String imagePath = product.getImagePath().replace("scriptImage", "delete");
            moveFile(bucket, product.getImagePath(), imagePath);
            deleteFile(bucket, product.getImagePath());
        }
    }

    public void setDescriptionDefault(final UUID productId) {
        final ProductEntity product = productRepo.findById(productId);

        if(product.getDescriptionPath() != null) {
            final String descriptionPath = product.getDescriptionPath().replace("description", "delete");
            moveFile(bucket, product.getDescriptionPath(), descriptionPath);
            deleteFile(bucket, product.getDescriptionPath());
        }
    }

    public RequestedPerformanceDTO.ProductInfo getProductInfo(final UUID productId, final UserEntity userInfo) throws UnsupportedEncodingException {
        final ProductEntity product = productRepo.findById(productId);

        if(!product.getUser().getId().equals(userInfo.getId()))
            throw new RuntimeException("접근 권한이 없습니다.");

        final String encodedScriptImage = product.getImagePath() != null ? bucketURL + URLEncoder.encode(product.getImagePath(), "UTF-8") : "";

        return RequestedPerformanceDTO.ProductInfo.builder()
                .imagePath(encodedScriptImage)
                .title(product.getTitle())
                .writer(product.getWriter())
                .plot(product.getPlot())
                .script(product.isScript())
                .scriptPrice(product.getScriptPrice())
                .scriptQuantity(orderItemRepo.sumScriptByProductId(productId))
                .performance(product.isPerformance())
                .performancePrice(product.getPerformancePrice())
                .performanceQuantity(orderItemRepo.sumPerformanceAmountByProductId(productId))
                .build();
    }

    public List<RequestedPerformanceDTO.DateRequestedList> getDateRequestedList (final UUID productId) {
        // 모든 주문 데이터 가져오기
        final List<OrderItemEntity> orderItems = orderItemRepo.findAllByProductId(productId);

        List<OrderItemEntity> filteredOrderItems = orderItems.stream()
                .filter(orderItem -> orderItem.getPerformanceAmount() >= 1)
                .filter(orderItem -> orderItem.getApplicant() != null)
                .toList();

        // 날짜별 그룹화
        Map<LocalDate, List<OrderItemEntity>> groupedByOrderDate = filteredOrderItems.stream()
                .collect(Collectors.groupingBy(orderItem -> orderItem.getCreatedAt().toLocalDate()));

        return groupedByOrderDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<OrderItemEntity> orderItemList = entry.getValue();

                    // 각 주문에 대한 신청자 정보
                    List<RequestedPerformanceDTO.ApplicantInfo> applicantInfoList = orderItemList.stream()
                            .map(orderItem -> RequestedPerformanceDTO.ApplicantInfo.builder()
                                    .amount(orderItem.getPerformanceAmount())
                                    .name(orderItem.getApplicant().getName())
                                    .phoneNumber(orderItem.getApplicant().getPhoneNumber())
                                    .address(orderItem.getApplicant().getAddress())
                                    .performanceDateList(orderItem.getPerformanceDate().stream()
                                            .map(performanceDate -> RequestedPerformanceDTO.PerformanceDate.builder()
                                                    .date(performanceDate.getDate())
                                                    .build())
                                            .collect(Collectors.toList()))
                                    .build())
                            .toList();

                    return RequestedPerformanceDTO.DateRequestedList.builder()
                            .date(date)
                            .requestedInfo(applicantInfoList)
                            .build();
                }).toList();
    }
}
