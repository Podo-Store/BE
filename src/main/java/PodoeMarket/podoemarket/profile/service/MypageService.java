package PodoeMarket.podoemarket.profile.service;

import PodoeMarket.podoemarket.Utils.ValidCheck;
import PodoeMarket.podoemarket.common.entity.*;
import PodoeMarket.podoemarket.common.entity.type.OrderStatus;
import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.common.repository.*;
import PodoeMarket.podoemarket.common.security.TokenProvider;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.profile.dto.request.ApplyRequestDTO;
import PodoeMarket.podoemarket.profile.dto.request.RefundRequestDTO;
import PodoeMarket.podoemarket.profile.dto.response.RequestedPerformanceResponseDTO;
import PodoeMarket.podoemarket.profile.dto.request.DetailUpdateRequestDTO;
import PodoeMarket.podoemarket.profile.dto.request.ProfileUpdateRequestDTO;
import PodoeMarket.podoemarket.profile.dto.response.*;
import PodoeMarket.podoemarket.service.ViewCountService;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class MypageService {
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final OrderItemRepository orderItemRepo;
    private final ApplicantRepository applicantRepo;
    private final PerformanceDateRepository performanceDateRepo;
    private final RefundRepository refundRepo;
    private final ProductLikeRepository productLikeRepo;
    private final AmazonS3 amazonS3;
    private final ViewCountService viewCountService;
    private final TokenProvider tokenProvider;

    private final PasswordEncoder pwdEncoder = new BCryptPasswordEncoder();

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.folder.folderName2}")
    private String scriptImageBucketFolder;

    @Value("${cloud.aws.s3.folder.folderName3}")
    private String descriptionBucketFolder;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    private final Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

    // 사용자 계정 정보 업데이트
    @Transactional
    public UserInfoResponseDTO updateUserAccount(UserEntity userInfo, ProfileUpdateRequestDTO dto) {
        try {
            if (!dto.getPassword().isBlank() && !dto.getPassword().equals(dto.getConfirmPassword()))
                throw new RuntimeException("비밀번호가 일치하지 않음");

            if (!dto.getPassword().isBlank()) {
                if (!ValidCheck.isValidPw(dto.getPassword()))
                    throw new RuntimeException("비밀번호 유효성 검사 실패");

                userInfo.setPassword(pwdEncoder.encode(dto.getPassword()));
            }

            if (!dto.getNickname().isBlank()) {
                if (!ValidCheck.isValidNickname(dto.getNickname()))
                    throw new RuntimeException("닉네임 유효성 검사 실패");

                userInfo.setNickname(dto.getNickname());
            }

            UserEntity user = userRepo.findById(userInfo.getId());

            if (user == null)
                throw new RuntimeException("로그인이 필요한 서비스입니다.");

            if (userInfo.getPassword() != null && !userInfo.getPassword().isBlank())
                user.setPassword(userInfo.getPassword());

            // 모든 작품의 작가명 변경
            List<ProductEntity> products = productRepo.findAllByUserId(userInfo.getId());

            for (ProductEntity product : productRepo.findAllByUserId(userInfo.getId())) {
                product.setWriter(userInfo.getNickname());
            }
            productRepo.saveAll(products);
            userRepo.save(user);

            return UserInfoResponseDTO.builder()
                    .id(user.getId())
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .nickname(user.getNickname())
                    .accessToken(tokenProvider.createAccessToken(user))
                    .refreshToken(tokenProvider.createRefreshToken(user))
                    .build();
        } catch (Exception e) {
            log.error("사용자 계정 정보 업데이트 중 오류 발생: userId={}, error={}",
                    userInfo.getId(), e.getMessage());
            throw new RuntimeException("사용자 계정 정보 업데이트 실패", e);
        }
    }

    public Boolean checkUser(final UUID id, final String password) {
        try{
            final UserEntity originalUser = userRepo.findById(id);

            if(originalUser == null)
                throw new RuntimeException("사용자를 찾을 수 없습니다.");

            return pwdEncoder.matches(password, originalUser.getPassword());
        } catch (Exception e){
            log.error("사용자 확인 중 오류 발생", e);
            return false;
        }
    }

    public ProfileInfoResponseDTO getProfileInfo(final UUID id) {
        try {
            final UserEntity user = userRepo.findById(id);

            if(user == null)
                throw new RuntimeException("로그인이 필요한 서비스입니다.");

            return ProfileInfoResponseDTO.builder()
                    .id(user.getId())
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("프로필 정보 조회 실패", e);
        }
    }

    @Transactional
    public void updateProductDetail(DetailUpdateRequestDTO dto, MultipartFile[] file1, MultipartFile[] file2) {
        try {
            // 입력 받은 제목을 NFKC 정규화 적용
            String normalizedTitle = Normalizer.normalize(dto.getTitle(), Normalizer.Form.NFKC);
            final ProductEntity product = productRepo.findById(dto.getId());

            if(product == null)
                throw new RuntimeException("상품을 찾을 수 업습니다.");

            if(!ValidCheck.isValidTitle(normalizedTitle))
                throw new RuntimeException("제목 유효성 검사 실패");

            if(product.getChecked() == ProductStatus.WAIT)
                throw new RuntimeException("등록 심사 중인 작품");

            if(!ValidCheck.isValidPlot(dto.getPlot()))
                throw new RuntimeException("줄거리 유효성 검사 실패");

            String scriptImageFilePath = null;
            if(file1 != null && file1.length > 0 && !file1[0].isEmpty())
                scriptImageFilePath = uploadScriptImage(file1, dto.getTitle(), dto.getId());
            else if (dto.getImagePath() != null)
                scriptImageFilePath = extractS3KeyFromURL(dto.getImagePath());
            else {
                if(product.getImagePath() != null) {
                    final String imagePath = product.getImagePath().replace("scriptImage", "delete");
                    moveFile(bucket, product.getImagePath(), imagePath);
                    deleteFile(bucket, product.getImagePath());
                }
            }

            String descriptionFilePath = null;
            if(file2 != null && file2.length > 0 && !file2[0].isEmpty())
                descriptionFilePath = uploadDescription(file2, dto.getTitle(), dto.getId());
            else if (dto.getDescriptionPath() != null)
                descriptionFilePath = extractS3KeyFromURL(dto.getDescriptionPath());
            else {
                if (product.getDescriptionPath() != null) {
                    final String descriptionPath = product.getDescriptionPath().replace("description", "delete");
                    moveFile(bucket, product.getDescriptionPath(), descriptionPath);
                    deleteFile(bucket, product.getDescriptionPath());
                }
            }

            if(product.getChecked() == ProductStatus.WAIT)
                throw new RuntimeException("등록 심사 중인 작품");

            product.setImagePath(scriptImageFilePath);
            product.setTitle(normalizedTitle);
            product.setScript(dto.getScript());
            product.setPerformance(dto.getPerformance());
            product.setScriptPrice(dto.getScriptPrice());
            product.setPerformancePrice(dto.getPerformancePrice());
            product.setScriptPrice(dto.getScriptPrice());
            product.setPerformancePrice(dto.getPerformancePrice());
            product.setDescriptionPath(descriptionFilePath);
            product.setPlot(dto.getPlot());

            productRepo.save(product);
        } catch (Exception e) {
            throw new RuntimeException("상품 상세 정보 업데이트 실패", e);
        }
    }

    public OrderScriptsResponseDTO getUserOrderScripts(UserEntity userInfo) {
        try {
            // 모든 필요한 OrderItemEntity를 한 번에 가져옴
            final List<OrderItemEntity> allOrderItems = orderItemRepo.findAllByUserIdAndScript(userInfo.getId(), true, sort);

            // 날짜별로 주문 항목을 그룹화하기 위한 맵 선언
            final Map<LocalDate, List<OrderScriptsResponseDTO.DateScriptOrderResponseDTO.OrderScriptDTO>> orderItemsGroupedByDate = new HashMap<>();

            for (OrderItemEntity orderItem : allOrderItems) {
                final OrderScriptsResponseDTO.DateScriptOrderResponseDTO.OrderScriptDTO orderItemDTO =  new OrderScriptsResponseDTO.DateScriptOrderResponseDTO.OrderScriptDTO();
                orderItemDTO.setId(orderItem.getId());
                orderItemDTO.setTitle(orderItem.getTitle());
                orderItemDTO.setScript(orderItem.getScript());

                if(orderItem.getProduct() != null) { // 삭제된 작품이 아닐 경우
                    String encodedScriptImage = orderItem.getProduct().getImagePath() != null ? bucketURL + URLEncoder.encode(orderItem.getProduct().getImagePath(), StandardCharsets.UTF_8) : "";

                    orderItemDTO.setDelete(false);
                    orderItemDTO.setWriter(orderItem.getProduct().getWriter());
                    orderItemDTO.setImagePath(encodedScriptImage);
                    orderItemDTO.setChecked(orderItem.getProduct().getChecked());
                    orderItemDTO.setScriptPrice(orderItem.getScript() ? orderItem.getProduct().getScriptPrice() : 0);
                    orderItemDTO.setProductId(orderItem.getProduct().getId());
                    orderItemDTO.setOrderStatus(orderItem.getOrder().getOrderStatus());
                } else { // 삭제된 작품일 경우
                    orderItemDTO.setDelete(true);
                    orderItemDTO.setScriptPrice(orderItem.getScript() ? orderItem.getScriptPrice() : 0);
                }

                // 날짜에 따른 리스트를 초기화하고 추가 - orderDate라는 key가 없으면 만들고, orderItemDTO를 value로 추가
                LocalDate orderDate = orderItem.getOrder().getCreatedAt().toLocalDate(); // localdatetime -> localdate
                orderItemsGroupedByDate.computeIfAbsent(orderDate, k -> new ArrayList<>()).add(orderItemDTO);
            }

            // DateOrderDTO로 변환하여 OrderScriptsResponseDTO 생성 및 반환
            List<OrderScriptsResponseDTO.DateScriptOrderResponseDTO> orderList = orderItemsGroupedByDate.entrySet().stream()
                    .map(entry -> new OrderScriptsResponseDTO.DateScriptOrderResponseDTO(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

            return OrderScriptsResponseDTO.builder()
                    .nickname(userInfo.getNickname())
                    .orderList(orderList)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("사용자 대본 주문 스크립트 조회 실패", e);
        }
    }

    public OrderPerformanceResponseDTO getUserOrderPerformances(UserEntity userInfo) {
        try {
            // 각 주문의 주문 항목을 가져옴
            final List<OrderItemEntity> allOrderItems = orderItemRepo.findAllByUserId(userInfo.getId(), sort);
            // 날짜별로 주문 항목을 그룹화하기 위한 맵 선언
            final Map<LocalDate, List<OrderPerformanceResponseDTO.DatePerformanceOrderDTO.OrderPerformanceDTO>> OrderItems = new HashMap<>();

            for (OrderItemEntity orderItem : allOrderItems) {
                if (orderItem.getPerformanceAmount() > 0) {
                    final int dateCount = performanceDateRepo.countByOrderItemId(orderItem.getId());

                    // 각 주문 항목에 대한 제품 정보 가져옴
                    final OrderPerformanceResponseDTO.DatePerformanceOrderDTO.OrderPerformanceDTO orderItemDTO = new OrderPerformanceResponseDTO.DatePerformanceOrderDTO.OrderPerformanceDTO();
                    orderItemDTO.setId(orderItem.getId());
                    orderItemDTO.setTitle(orderItem.getTitle());
                    orderItemDTO.setPerformanceAmount(orderItem.getPerformanceAmount());

                    if(LocalDateTime.now().isAfter(orderItem.getCreatedAt().plusYears(1)))
                        orderItemDTO.setPossibleCount(0);
                    else
                        orderItemDTO.setPossibleCount(orderItem.getPerformanceAmount() - dateCount);

                    if(orderItem.getProduct() != null) { // 삭제된 작품이 아닐 경우
                        String encodedScriptImage = orderItem.getProduct().getImagePath() != null ? bucketURL + URLEncoder.encode(orderItem.getProduct().getImagePath(), StandardCharsets.UTF_8) : "";

                        orderItemDTO.setDelete(false);
                        orderItemDTO.setWriter(orderItem.getProduct().getWriter());
                        orderItemDTO.setImagePath(encodedScriptImage);
                        orderItemDTO.setChecked(orderItem.getProduct().getChecked());
                        orderItemDTO.setPerformancePrice(orderItem.getPerformanceAmount() > 0 ? orderItem.getProduct().getPerformancePrice() : 0);
                        orderItemDTO.setPerformanceTotalPrice(orderItem.getPerformancePrice());
                        orderItemDTO.setProductId(orderItem.getProduct().getId());
                        orderItemDTO.setOrderStatus(orderItem.getOrder().getOrderStatus());
                    } else { // 삭제된 작품일 경우
                        orderItemDTO.setDelete(true);
                        orderItemDTO.setPerformancePrice(orderItem.getPerformanceAmount() > 0 ? orderItem.getProduct().getPerformancePrice() : 0);
                        orderItemDTO.setPerformanceTotalPrice(orderItem.getPerformancePrice());
                    }

                    final LocalDate orderDate = orderItem.getCreatedAt().toLocalDate(); // localdatetime -> localdate
                    // 날짜에 따른 리스트를 초기화하고 추가 - orderDate라는 key가 없으면 만들고, orderItemDTO를 value로 추가
                    OrderItems.computeIfAbsent(orderDate, k -> new ArrayList<>()).add(orderItemDTO);
                }
            }

            // DateOrderDTO로 변환
            List<OrderPerformanceResponseDTO.DatePerformanceOrderDTO> orderList = OrderItems.entrySet().stream()
                    .map(entry -> new OrderPerformanceResponseDTO.DatePerformanceOrderDTO(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

            return OrderPerformanceResponseDTO.builder()
                .nickname(userInfo.getNickname())
                .orderList(orderList)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("사용자 공연권 주문 스크립트 조회 실패", e);
        }
    }

    @Transactional
    public void deleteProduct(final UUID productId, final UUID userId) {
        try {
            final ProductEntity product = productRepo.findById(productId);

            if(product == null)
                throw new RuntimeException("상품을 찾을 수 없습니다.");

            if(!product.getUser().getId().equals(userId))
                throw new RuntimeException("작가가 아님");

            if(product.getChecked() == ProductStatus.WAIT)
                throw new RuntimeException("심사 중");

            // 탈퇴와 동일한 파일 삭제 처리 필요
            deleteScripts(product);
        } catch (Exception e) {
            throw new RuntimeException("상품 삭제 실패", e);
        }
    }

    public ApplyResponseDTO getApplyInfo(final UUID orderItemId) {
        try {
            final OrderItemEntity orderItem = getOrderItem(orderItemId);

            if(orderItem.getOrder().getOrderStatus() != OrderStatus.PASS)
                throw new RuntimeException("결제 상태를 확인해주십시오.");

            final ApplicantEntity applicant = applicantRepo.findByOrderItemId(orderItemId);

            if(applicant == null)
                throw new RuntimeException("일치하는 신청자 정보 없음");

            ApplyResponseDTO applyResponseDTO = new ApplyResponseDTO();

            applyResponseDTO.setOrderItemId(orderItem.getId());
            applyResponseDTO.setImagePath(orderItem.getProduct().getImagePath() != null ? bucketURL + URLEncoder.encode(orderItem.getProduct().getImagePath(), StandardCharsets.UTF_8): "");
            applyResponseDTO.setTitle(orderItem.getTitle());
            applyResponseDTO.setWriter(orderItem.getProduct().getWriter());
            applyResponseDTO.setPerformanceAmount(orderItem.getPerformanceAmount());

            ApplyResponseDTO.ApplicantDTO applicantDTO = ApplyResponseDTO.ApplicantDTO.builder()
                    .name(applicant.getName())
                    .phoneNumber(applicant.getPhoneNumber())
                    .address(applicant.getAddress())
                    .build();

            applyResponseDTO.setApplicant(applicantDTO);

            List<ApplyResponseDTO.PerformanceDateDTO> performanceDateDTO = orderItem.getPerformanceDate()
                    .stream()
                    .map(performanceDate -> new ApplyResponseDTO.PerformanceDateDTO(performanceDate.getDate()))
                    .toList();

            applyResponseDTO.setPerformanceDate(performanceDateDTO);

            return applyResponseDTO;
        } catch (Exception e) {
            throw new RuntimeException("공연 신청 정보 조회 실패", e);
        }
    }

    @Transactional
    public void processPerformanceApplication(final ApplyRequestDTO dto) {
        try {
            final OrderItemEntity orderItem = getOrderItem(dto.getOrderItemId());
            expire(orderItem.getCreatedAt());

            int availableAmount = orderItem.getPerformanceAmount() - performanceDateRepo.countByOrderItemId(dto.getOrderItemId());
            if(dto.getPerformanceDate().size() > availableAmount)
                throw new RuntimeException("공연권 구매량 초과");

            if(dto.getPerformanceDate().isEmpty())
                throw new RuntimeException("신청 날짜가 비어있음");

            List<PerformanceDateEntity> dates = new ArrayList<>();
            for(ApplyRequestDTO.PerformanceDateDTO dateDTO : dto.getPerformanceDate()) {
                final PerformanceDateEntity date = PerformanceDateEntity.builder()
                        .date(dateDTO.getDate())
                        .orderItem(orderItem)
                        .build();

                dates.add(date);
            }

            performanceDateRepo.saveAll(dates);
        } catch (Exception e) {
            throw new RuntimeException("공연 신청 처리 실패", e);
        }
    }

    public ScriptInfoResponseDTO checkValidation(final UUID orderId) {
        try {
            final OrderItemEntity orderItem = getOrderItem(orderId);

            if(!orderItem.getScript())
                throw new RuntimeException("대본을 구매하세요");

            if(orderItem.getOrder().getOrderStatus() != OrderStatus.PASS)
                throw new RuntimeException("결제 상태를 확인해주세요");

            expire(orderItem.getCreatedAt());

            return ScriptInfoResponseDTO.builder()
                    .filePath(orderItem.getProduct().getFilePath())
                    .title(orderItem.getProduct().getTitle())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("대본 다운로드 유효성 검사 실패", e);
        }
    }

    public byte[] downloadFile(final String fileKey, final String email) {
        // S3에서 파일 객체 가져오기
        try (S3Object s3Object = amazonS3.getObject(bucket, fileKey);
             InputStream inputStream = s3Object.getObjectContent();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            addWatermark(inputStream, outputStream, email);

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("파일 다운로드 실패", e);
        }
    }

    @Transactional
    public void deleteUser(final UserEntity userEntity) {
        try {
            // s3에 저장된 파일 이전 및 삭제
            List<ProductEntity> products = productRepo.findAllByUserId(userEntity.getId());

            for(ProductEntity product : products)
                deleteScripts(product);

            // DB 계정 삭제
            userRepo.delete(userEntity);
        } catch (Exception e) {
            throw new RuntimeException("사용자 계정 삭제 실패", e);
        }
    }

    public RefundResponseDTO getRefundInfo(final UUID orderItemId) {
        try {
            final OrderItemEntity orderItem = getOrderItem(orderItemId);

            final int possibleAmount = orderItem.getPerformanceAmount() - performanceDateRepo.countByOrderItemId(orderItemId);
            final int possiblePrice = orderItem.getProduct().getPerformancePrice() * possibleAmount;

            return RefundResponseDTO.builder()
                    .scriptImage(orderItem.getProduct().getImagePath())
                    .title(orderItem.getTitle())
                    .writer(orderItem.getProduct().getWriter())
                    .performancePrice(orderItem.getProduct().getPerformancePrice())
                    .orderDate(orderItem.getCreatedAt())
                    .orderNum(orderItem.getOrder().getId())
                    .orderAmount(orderItem.getPerformanceAmount())
                    .orderPrice(orderItem.getPerformancePrice())
                    .possibleAmount(possibleAmount)
                    .possiblePrice(possiblePrice)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("환불 정보 조회 실패", e);
        }
    }

    @Transactional
    public void refundProcess(final UserEntity userInfo, final RefundRequestDTO dto) {
        try {
            final OrderItemEntity orderItem = getOrderItem(userInfo.getId());
            final int possibleAmount = orderItem.getPerformanceAmount() - performanceDateRepo.countByOrderItemId(dto.getOrderItemId());
            final int possiblePrice = orderItem.getProduct().getPerformancePrice() * possibleAmount;
            final int refundPrice = orderItem.getProduct().getPerformancePrice() * dto.getRefundAmount();

            if(dto.getRefundAmount() > possibleAmount || refundPrice > possiblePrice || dto.getRefundAmount() == 0 || refundPrice < 0)
                throw new RuntimeException("환불 가능 수량과 가격이 아님");

            if(dto.getReason().isEmpty() || dto.getReason().length() > 50)
                throw new RuntimeException("환불 사유는 1 ~ 50자까지 가능");

            final RefundEntity refund = RefundEntity.builder()
                    .quantity(dto.getRefundAmount())
                    .price(refundPrice)
                    .content(dto.getReason())
                    .order(orderItem.getOrder())
                    .user(userInfo)
                    .build();

            refundRepo.save(refund);
        } catch (Exception e) {
            throw new RuntimeException("환불 처리 실패", e);
        }
    }

    public RequestedPerformanceResponseDTO getRequestedPerformances(final UUID productId, final UserEntity userInfo) {
        try {
            final RequestedPerformanceResponseDTO.ProductInfo productInfo = getProductInfo(productId, userInfo);
            final List<RequestedPerformanceResponseDTO.DateRequestedList> list = getDateRequestedList(productId);

            return RequestedPerformanceResponseDTO.builder()
                    .productInfo(productInfo)
                    .dateRequestedList(list)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("요청된 공연 목록 조회 실패", e);
        }
    }

    public ScriptDetailResponseDTO productDetail(UUID productId, int buyStatus) throws UnsupportedEncodingException {
        try {
            final ProductEntity script = productRepo.findById(productId);

            if(script == null)
                throw new RuntimeException("상품을 찾을 수 없습니다.");

            ScriptDetailResponseDTO scriptDetailDTO = new ScriptDetailResponseDTO();
            String encodedScriptImage = script.getImagePath() != null ? bucketURL + URLEncoder.encode(script.getImagePath(), StandardCharsets.UTF_8) : "";
            String encodedDescription = script.getDescriptionPath() != null ? bucketURL + URLEncoder.encode(script.getDescriptionPath(), "UTF-8") : "";

            scriptDetailDTO.setId(script.getId());
            scriptDetailDTO.setTitle(script.getTitle());
            scriptDetailDTO.setWriter(script.getWriter());
            scriptDetailDTO.setImagePath(encodedScriptImage);
            scriptDetailDTO.setScript(script.getScript());
            scriptDetailDTO.setScriptPrice(script.getScriptPrice());
            scriptDetailDTO.setPerformance(script.getPerformance());
            scriptDetailDTO.setPerformancePrice(script.getPerformancePrice());
            scriptDetailDTO.setDescriptionPath(encodedDescription);
            scriptDetailDTO.setDate(script.getCreatedAt());
            scriptDetailDTO.setChecked(script.getChecked());
            scriptDetailDTO.setPlayType(script.getPlayType());
            scriptDetailDTO.setPlot(script.getPlot());

            scriptDetailDTO.setBuyStatus(buyStatus);

            return scriptDetailDTO;
        } catch (Exception e) {
            throw new RuntimeException("상품 상세 정보 조회 실패", e);
        }
    }

    public List<ScriptListResponseDTO.ProductListDTO> getLikePlayList(int page, UserEntity userInfo, PlayType playType, int pageSize) {
        try {
            final Pageable mainLikePage = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            final List<ProductLikeEntity> playLikes = productLikeRepo.findAllByUserAndProduct_PlayType(userInfo, playType, mainLikePage);

            return playLikes.stream()
                    .map(playLike -> {
                        String encodedScriptImage = playLike.getProduct().getImagePath() != null ? bucketURL + URLEncoder.encode(playLike.getProduct().getImagePath(), StandardCharsets.UTF_8) : "";

                        return ScriptListResponseDTO.ProductListDTO.builder()
                                .id(playLike.getProduct().getId())
                                .title(playLike.getProduct().getTitle())
                                .writer(playLike.getProduct().getWriter())
                                .imagePath(encodedScriptImage)
                                .script(playLike.getProduct().getScript())
                                .scriptPrice(playLike.getProduct().getScriptPrice())
                                .performance(playLike.getProduct().getPerformance())
                                .performancePrice(playLike.getProduct().getPerformancePrice())
                                .date(playLike.getProduct().getCreatedAt())
                                .checked(playLike.getProduct().getChecked())
                                .like(productLikeRepo.existsByUserAndProduct(userInfo, playLike.getProduct()))
                                .likeCount(productLikeRepo.countByProduct(playLike.getProduct()))
                                .viewCount(viewCountService.getProductViewCount(playLike.getProduct().getId()))
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("좋아요 상품 목록 조회 실패", e);
        }
    }

    public ScriptListResponseDTO getUserLikeList(UserEntity userInfo) {
        try {
            return new ScriptListResponseDTO(
                    getLikePlayList(0, userInfo, PlayType.LONG, 4),
                    getLikePlayList(0, userInfo, PlayType.SHORT, 4)
            );
        } catch (Exception e) {
            throw new RuntimeException("사용자 좋아요 목록 조회 실패", e);
        }
    }

    // =========== private (protected) method =============

    private OrderItemEntity getOrderItem(final UUID orderItemId) {
        try {
            if(orderItemRepo.findById(orderItemId) == null)
                throw new RuntimeException("일치하는 구매 목록 없음");

            return orderItemRepo.findById(orderItemId);
        } catch (Exception e) {
            throw new RuntimeException("주문 항목 조회 실패", e);
        }
    }

    private void deleteFile(final String bucket, final String sourceKey) {
        try {
            if(amazonS3.doesObjectExist(bucket, sourceKey))
                amazonS3.deleteObject(bucket, sourceKey);
        } catch (Exception e) {
            throw new RuntimeException("파일 삭제 실패", e);
        }
    }

    @Transactional
    protected String uploadScriptImage(final MultipartFile[] files, final String title, final UUID id) {
        try {
            if(files.length > 1)
                throw new RuntimeException("작품 이미지가 1개를 초과함");

            if(!Objects.equals(files[0].getContentType(), "image/jpeg") && !Objects.equals(files[0].getContentType(), "image/jpeg") && !Objects.equals(files[0].getContentType(), "image/png"))
                throw new RuntimeException("ScriptImage file type is only jpg and png");

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
            final ProductEntity product = productRepo.findById(id);

            if(product == null)
                throw new RuntimeException("상품을 찾을 수 없습니다.");

            if(product.getImagePath() != null) {
                final String sourceKey = product.getImagePath();

                deleteFile(bucket, sourceKey);
            }

            // 저장
            amazonS3.putObject(bucket, S3Key, files[0].getInputStream(), metadata);

            return S3Key;
        } catch (Exception e) {
            throw new RuntimeException("스크립트 이미지 업로드 실패", e);
        }
    }

    @Transactional
    protected String uploadDescription(final MultipartFile[] files, final String title, final UUID id) throws IOException {
        try {
            if(files.length > 1)
                throw new RuntimeException("작품 설명 파일 수가 1개를 초과함");

            if(!Objects.equals(files[0].getContentType(), "application/pdf") && !Objects.equals(files[0].getContentType(), "application/pdf"))
                throw new RuntimeException("Description file type is not PDF");

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
            final ProductEntity product = productRepo.findById(id);

            if(product == null)
                throw new RuntimeException("상품을 찾을 수 없습니다.");

            if(product.getDescriptionPath() != null) {
                final String sourceKey = product.getDescriptionPath();

                deleteFile(bucket, sourceKey);
            }

            // 저장
            amazonS3.putObject(bucket, S3Key, files[0].getInputStream(), metadata);

            return S3Key;
        } catch (Exception e) {
            throw new RuntimeException("설명 파일 업로드 실패", e);
        }
    }

    private String extractS3KeyFromURL(final String S3URL) throws Exception {
        try {
            String decodedUrl = URLDecoder.decode(S3URL, StandardCharsets.UTF_8);
            final URL url = (new URI(decodedUrl)).toURL();

            return url.getPath().startsWith("/") ? url.getPath().substring(1) : url.getPath();
        } catch (Exception e) {
            throw new RuntimeException("S3 URL에서 키 추출 실패", e);
        }
    }

    private void expire(final LocalDateTime time) {
        if(LocalDateTime.now().isAfter(time.plusYears(1)))
            throw new RuntimeException("구매 후 1년 경과");
    }

    private void moveFile(final String bucket, final String sourceKey, final String destinationKey) {
        try {
            final CopyObjectRequest copyFile = new CopyObjectRequest(bucket,sourceKey, bucket, destinationKey);

            if(amazonS3.doesObjectExist(bucket, sourceKey))
                amazonS3.copyObject(copyFile);
        } catch (Exception e) {
            throw new RuntimeException("파일 이동 실패", e);
        }
    }

    private void addWatermark(final InputStream src, final ByteArrayOutputStream dest, final String email) {
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
            throw new RuntimeException("워터마크 추가 실패", e);
        }
    }

    private RequestedPerformanceResponseDTO.ProductInfo getProductInfo(final UUID productId, final UserEntity userInfo) {
        try {
            final ProductEntity product = productRepo.findById(productId);

            if(product == null)
                throw new RuntimeException("상품을 찾을 수 없습니다.");

            if(!product.getUser().getId().equals(userInfo.getId()))
                throw new RuntimeException("접근 권한이 없습니다.");

            final String encodedScriptImage = product.getImagePath() != null ? bucketURL + URLEncoder.encode(product.getImagePath(), StandardCharsets.UTF_8) : "";

            return RequestedPerformanceResponseDTO.ProductInfo.builder()
                    .imagePath(encodedScriptImage)
                    .title(product.getTitle())
                    .writer(product.getWriter())
                    .plot(product.getPlot())
                    .script(product.getScript())
                    .scriptPrice(product.getScriptPrice())
                    .scriptQuantity(orderItemRepo.sumScriptByProductId(productId))
                    .performance(product.getPerformance())
                    .performancePrice(product.getPerformancePrice())
                    .performanceQuantity(orderItemRepo.sumPerformanceAmountByProductId(productId))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("상품 정보 조회 실패", e);
        }
    }

    private List<RequestedPerformanceResponseDTO.DateRequestedList> getDateRequestedList (final UUID productId) {
        try {
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
                        List<RequestedPerformanceResponseDTO.ApplicantInfo> applicantInfoList = orderItemList.stream()
                                .map(orderItem -> RequestedPerformanceResponseDTO.ApplicantInfo.builder()
                                        .amount(orderItem.getPerformanceAmount())
                                        .name(orderItem.getApplicant().getName())
                                        .phoneNumber(orderItem.getApplicant().getPhoneNumber())
                                        .address(orderItem.getApplicant().getAddress())
                                        .performanceDateList(orderItem.getPerformanceDate().stream()
                                                .map(performanceDate -> RequestedPerformanceResponseDTO.PerformanceDate.builder()
                                                        .date(performanceDate.getDate())
                                                        .build())
                                                .collect(Collectors.toList()))
                                        .build())
                                .toList();

                        return RequestedPerformanceResponseDTO.DateRequestedList.builder()
                                .date(date)
                                .requestedInfo(applicantInfoList)
                                .build();
                    }).toList();
        } catch (Exception e) {
            throw new RuntimeException("날짜별 요청 목록 조회 실패", e);
        }
    }

    private void deleteScripts(final ProductEntity product) {
        try {
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
        } catch (Exception e) {
            throw new RuntimeException("작품 파일 삭제 실패", e);
        }
    }
}
