package PodoeMarket.podoemarket.profile.service;

import PodoeMarket.podoemarket.common.entity.*;
import PodoeMarket.podoemarket.common.entity.type.OrderStatus;
import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.common.repository.*;
import PodoeMarket.podoemarket.common.security.TokenProvider;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.profile.dto.request.*;
import PodoeMarket.podoemarket.profile.dto.response.RequestedPerformanceResponseDTO;
import PodoeMarket.podoemarket.profile.dto.response.*;
import PodoeMarket.podoemarket.service.ViewCountService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

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
    private final PdfDownloadLogRepository pdfDownloadLogRepo;
    private final ViewCountService viewCountService;
    private final TokenProvider tokenProvider;

    private final AmazonS3 amazonS3;
    private final PasswordEncoder pwdEncoder = new BCryptPasswordEncoder();

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    @Value("${nicepay.client-key}")
    private String clientKey;

    @Value("${nicepay.secret-key}")
    private String secretKey;

    private final Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

    // 사용자 계정 정보 업데이트 - 삭제 예정
    @Transactional
    public UserInfoResponseDTO updateUserAccount(UserEntity userInfo, ProfileUpdateRequestDTO dto) {
        try {
            if(!dto.getPassword().isBlank() && !dto.getPassword().equals(dto.getConfirmPassword()))
                throw new RuntimeException("비밀번호가 일치하지 않음");

            UserEntity user = userRepo.findById(userInfo.getId());

            if(user == null)
                throw new RuntimeException("로그인이 필요한 서비스입니다.");

            if(userInfo.getPassword() != null && !userInfo.getPassword().isBlank())
                user.setPassword(userInfo.getPassword());

            isValidPw(dto.getPassword());
            user.setPassword(pwdEncoder.encode(dto.getPassword()));

            if(!dto.getNickname().isBlank()) {
                isValidNickname(dto.getNickname());
                user.setNickname(dto.getNickname());

                // 모든 작품의 작가명 변경
                List<ProductEntity> products = productRepo.findAllByUserId(userInfo.getId());

                for(ProductEntity product : productRepo.findAllByUserId(userInfo.getId()))
                    product.setWriter(user.getNickname());

                productRepo.saveAll(products);
            }

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
            throw e;
        }
    }

    @Transactional
    public void updatePassword(UserEntity userInfo, PasswordUpdateRequestDTO dto) {
        try {
            if (dto.getPassword() == null || dto.getConfirmPassword() == null)
                throw new RuntimeException("비밀번호를 입력하세요.");

            if(!dto.getPassword().isBlank() && !dto.getPassword().equals(dto.getConfirmPassword()))
                throw new RuntimeException("비밀번호가 일치하지 않음");

            UserEntity user = userRepo.findById(userInfo.getId());

            if(user == null)
                throw new RuntimeException("로그인이 필요한 서비스입니다.");

            isValidPw(dto.getPassword());

            user.setPassword(pwdEncoder.encode(dto.getPassword()));

            userRepo.save(user);
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public NicknameUpdateResponseDTO updateNickname(UserEntity userInfo, NicknameUpdateRequestDTO dto) {
        try {
            UserEntity user = userRepo.findById(userInfo.getId());

            if(user == null)
                throw new RuntimeException("로그인이 필요한 서비스입니다.");

            if (!Objects.equals(user.getNickname(), dto.getNickname()) && userRepo.existsByNickname(dto.getNickname()))
                throw new RuntimeException("이미 사용 중인 닉네임");

            isValidNickname(dto.getNickname());

            user.setNickname(dto.getNickname());

            // 모든 작품의 작가명 변경
            for(ProductEntity product : productRepo.findAllByUserId(userInfo.getId()))
                product.setWriter(user.getNickname());

            // save() 없이 자동 저장 - Transactional 선언
            return NicknameUpdateResponseDTO.builder()
                    .nickname(user.getNickname())
                    .accessToken(tokenProvider.createAccessToken(user))
                    .refreshToken(tokenProvider.createRefreshToken(user))
                    .build();
        } catch (Exception e) {
            throw e;
        }
    }

    public Boolean checkUser(final UUID id, final String password) {
        try{
            final UserEntity originalUser = userRepo.findById(id);

            if(originalUser == null)
                throw new RuntimeException("사용자를 찾을 수 없습니다.");

            return pwdEncoder.matches(password, originalUser.getPassword());
        } catch (Exception e){
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
                    .email(user.getEmail())
                    .socialLoginType(user.getSocialLoginType())
                    .nickname(user.getNickname())
                    .build();
        } catch (Exception e) {
            throw e;
        }
    }

    public Boolean checkNickname(final String nickname) {
        try {
            return userRepo.existsByNickname(nickname);
        } catch (Exception e) {
            throw new RuntimeException("닉네임 확인 실패", e);
        }
    }

    public OrderScriptsResponseDTO getUserOrderScripts(UserEntity userInfo) {
        try {
            // 모든 필요한 OrderItemEntity를 한 번에 가져옴
            final List<OrderItemEntity> allOrderItems = orderItemRepo.findPaidScriptOrderItems(userInfo.getId(), OrderStatus.PAID, sort);

            // 날짜별로 주문 항목을 그룹화하기 위한 맵 선언
            final Map<LocalDate, List<OrderScriptsResponseDTO.DateScriptOrderResponseDTO.OrderScriptDTO>> orderItemsGroupedByDate = new HashMap<>();

            for (OrderItemEntity orderItem : allOrderItems) {
                final OrderScriptsResponseDTO.DateScriptOrderResponseDTO.OrderScriptDTO orderItemDTO =  new OrderScriptsResponseDTO.DateScriptOrderResponseDTO.OrderScriptDTO();

                orderItemDTO.setId(orderItem.getId());
                orderItemDTO.setTitle(orderItem.getTitle());
                orderItemDTO.setWriter(orderItem.getWriter());
                orderItemDTO.setScript(orderItem.getScript());

                if(orderItem.getProduct() == null) { // 완전히 삭제된 작품
                    orderItemDTO.setDelete(true);
                    orderItemDTO.setScriptPrice(orderItem.getScript() ? orderItem.getScriptPrice() : 0);
                } else if(orderItem.getProduct().getIsDelete()) { // 삭제 표시가 된 작품
                    orderItemDTO.setDelete(true);
                    orderItemDTO.setScriptPrice(orderItem.getScript() ? orderItem.getScriptPrice() : 0);
                } else { // 정상 작품
                    String encodedScriptImage = orderItem.getProduct().getImagePath() != null
                            ? bucketURL + URLEncoder.encode(orderItem.getProduct().getImagePath(), StandardCharsets.UTF_8)
                            : "";

                    orderItemDTO.setDelete(false);
                    orderItemDTO.setImagePath(encodedScriptImage);
                    orderItemDTO.setChecked(orderItem.getProduct().getChecked());
                    orderItemDTO.setScriptPrice(orderItem.getScript() ? orderItem.getProduct().getScriptPrice() : 0);
                    orderItemDTO.setProductId(orderItem.getProduct().getId());
                }

                // 날짜에 따른 리스트를 초기화하고 추가 - orderDate라는 key가 없으면 만들고, orderItemDTO를 value로 추가
                LocalDate orderDate = orderItem.getOrder().getCreatedAt().toLocalDate(); // localdatetime -> localdate
                orderItemsGroupedByDate.computeIfAbsent(orderDate, k -> new ArrayList<>()).add(orderItemDTO);
            }

            // DateOrderDTO로 변환
            List<OrderScriptsResponseDTO.DateScriptOrderResponseDTO> orderList = orderItemsGroupedByDate.entrySet().stream()
                    .sorted(Map.Entry.<LocalDate, List<OrderScriptsResponseDTO.DateScriptOrderResponseDTO.OrderScriptDTO>>comparingByKey().reversed())
                    .map(entry -> new OrderScriptsResponseDTO.DateScriptOrderResponseDTO(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

            return OrderScriptsResponseDTO.builder()
                    .nickname(userInfo.getNickname())
                    .orderList(orderList)
                    .build();
        } catch (Exception e) {
            throw e;
        }
    }

    public OrderPerformanceResponseDTO getUserOrderPerformances(UserEntity userInfo) {
        try {
            // 주문 항목 조회
            final List<OrderItemEntity> allOrderItems = orderItemRepo.findPaidPerformanceOrderItems(userInfo.getId(), OrderStatus.PAID, sort);

            // 다운로드 여부 한 번에 조회
            List<UUID> orderItemIds = allOrderItems.stream().map(OrderItemEntity::getId).toList();
            Set<UUID> downloadedSet = new HashSet<>(pdfDownloadLogRepo.findDownloadedOrderItemIds(orderItemIds, userInfo.getId()));

            final Map<UUID, Integer> performanceDateCountMap = getPerformanceDateCountMap(allOrderItems);

            final Map<Long, Integer> refundQuantityMap = getRefundQuantityMap(allOrderItems);

            // 날짜별로 주문 항목을 그룹화하기 위한 맵 선언
            final Map<LocalDate, List<OrderPerformanceResponseDTO.DatePerformanceOrderDTO.OrderPerformanceDTO>> orderItems = new HashMap<>();

            for (OrderItemEntity orderItem : allOrderItems) {
                int dateCount = performanceDateCountMap.getOrDefault(orderItem.getId(), 0);
                int refundCount = refundQuantityMap.getOrDefault(orderItem.getOrder().getId(), 0);

                // 각 주문 항목에 대한 제품 정보 가져옴
                final OrderPerformanceResponseDTO.DatePerformanceOrderDTO.OrderPerformanceDTO dto = new OrderPerformanceResponseDTO.DatePerformanceOrderDTO.OrderPerformanceDTO();

                dto.setId(orderItem.getId());
                dto.setTitle(orderItem.getTitle());
                dto.setWriter(orderItem.getWriter());
                dto.setIsDownloaded(downloadedSet.contains(orderItem.getId()));

                // 공연권 사용 가능 수량 계산
                if(LocalDateTime.now().isAfter(orderItem.getCreatedAt().plusMonths(3)))
                    dto.setPossibleCount(0);
                else
                    dto.setPossibleCount(orderItem.getPerformanceAmount() - dateCount -  refundCount);

                if(orderItem.getProduct() == null || orderItem.getProduct().getIsDelete()) { // 완전히 삭제되었거나, 삭제 표시가 된 작품
                    dto.setDelete(true);
                    dto.setPerformancePrice(orderItem.getPerformanceAmount() > 0 ? orderItem.getPerformancePrice() : 0);
                    dto.setPerformanceTotalPrice(orderItem.getPerformancePrice());
                } else { // 정상 작품
                    String encodedScriptImage = orderItem.getProduct().getImagePath() != null
                            ? bucketURL + URLEncoder.encode(orderItem.getProduct().getImagePath(), StandardCharsets.UTF_8)
                            : "";

                    dto.setDelete(false);
                    dto.setImagePath(encodedScriptImage);
                    dto.setChecked(orderItem.getProduct().getChecked());
                    dto.setPerformancePrice(orderItem.getPerformanceAmount() > 0 ? orderItem.getProduct().getPerformancePrice() : 0);
                    dto.setPerformanceTotalPrice(orderItem.getPerformancePrice());
                    dto.setProductId(orderItem.getProduct().getId());
                }

                LocalDate orderDate = orderItem.getCreatedAt().toLocalDate();
                // 날짜에 따른 리스트를 초기화하고 추가 - orderDate라는 key가 없으면 만들고, dto를 value로 추가
                orderItems.computeIfAbsent(orderDate, k -> new ArrayList<>()).add(dto);
            }

            // DateOrderDTO로 변환
            List<OrderPerformanceResponseDTO.DatePerformanceOrderDTO> orderList = orderItems.entrySet().stream()
                    .sorted(Map.Entry.<LocalDate, List<OrderPerformanceResponseDTO.DatePerformanceOrderDTO.OrderPerformanceDTO>>comparingByKey().reversed())
                    .map(entry -> new OrderPerformanceResponseDTO.DatePerformanceOrderDTO(entry.getKey(), entry.getValue()))
                    .toList();

            return OrderPerformanceResponseDTO.builder()
                    .nickname(userInfo.getNickname())
                    .orderList(orderList)
                    .build();
        } catch (Exception e) {
            throw e;
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
            throw e;
        }
    }

    public ApplyResponseDTO getApplyInfo(final UUID orderItemId) {
        try {
            final OrderItemEntity orderItem = getOrderItem(orderItemId);

            final ApplicantEntity applicant = applicantRepo.findByOrderItemId(orderItemId);

            if(applicant == null)
                throw new RuntimeException("일치하는 신청자 정보 없음");

            ApplyResponseDTO applyResponseDTO = new ApplyResponseDTO();

            applyResponseDTO.setOrderItemId(orderItem.getId());
            applyResponseDTO.setImagePath(orderItem.getProduct().getImagePath() != null ? bucketURL + URLEncoder.encode(orderItem.getProduct().getImagePath(), StandardCharsets.UTF_8): "");
            applyResponseDTO.setTitle(orderItem.getProduct().getTitle());
            applyResponseDTO.setWriter(orderItem.getProduct().getWriter());
            applyResponseDTO.setPerformanceAmount(orderItem.getPerformanceAmount() - refundRepo.sumRefundQuantityByOrderId(orderItem.getOrder().getId()));

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
            throw e;
        }
    }

    @Transactional
    public void processPerformanceApplication(final ApplyRequestDTO dto) {
        try {
            final OrderItemEntity orderItem = getOrderItem(dto.getOrderItemId());
            expire(orderItem.getCreatedAt());

            int availableAmount = orderItem.getPerformanceAmount() - performanceDateRepo.countByOrderItemId(dto.getOrderItemId()) - refundRepo.sumRefundQuantityByOrderId(orderItem.getOrder().getId());
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
            throw e;
        }
    }

    @Transactional
    public ScriptDownloadResponseDTO downloadFile(final UUID orderItemId, final UserEntity userInfo) throws IOException {
        try {
            final OrderItemEntity orderItem = getOrderItem(orderItemId);

            expire(orderItem.getCreatedAt());

            if(pdfDownloadLogRepo.existsByOrderItemIdAndUserId(orderItemId, userInfo.getId()))
                throw new RuntimeException("다운로드는 1회만 가능합니다.");

            if(!orderItem.getProduct().getPerformance())
                throw new RuntimeException("공연권을 구매했을 경우에만 다운로드가 가능합니다.");

            ScriptDownloadResponseDTO dto = ScriptDownloadResponseDTO.builder()
                    .fileName(URLEncoder.encode(orderItem.getProduct().getTitle(), StandardCharsets.UTF_8))
                    .build();

            byte[] pdfBytes;

            // S3에서 파일 객체 가져오기
            try (S3Object s3Object = amazonS3.getObject(bucket, orderItem.getProduct().getFilePath());
                 InputStream s3Stream = s3Object.getObjectContent()) {

                pdfBytes = extractPdfFromZip(s3Stream);
            }

            ByteArrayOutputStream watermarkOutput = new ByteArrayOutputStream();

            addWatermark(new ByteArrayInputStream(pdfBytes), watermarkOutput, userInfo.getId());

            dto.setFileData(watermarkOutput.toByteArray());

            PdfDownloadLogEntity log = PdfDownloadLogEntity.builder()
                    .orderItemId(orderItemId)
                    .userId(userInfo.getId())
                    .build();

            pdfDownloadLogRepo.save(log);

            return dto;
        } catch (Exception e) {
            throw e;
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
            throw e;
        }
    }

    public RefundResponseDTO getRefundInfo(final UUID orderItemId) {
        try {
            final OrderItemEntity orderItem = getOrderItem(orderItemId);

            final int refundAmount = refundRepo.sumRefundQuantityByOrderId(orderItem.getOrder().getId());
            final int possibleAmount = orderItem.getPerformanceAmount() - performanceDateRepo.countByOrderItemId(orderItemId) - refundAmount;
            final long possiblePrice = orderItem.getProduct().getPerformancePrice() * possibleAmount;
            String encodedScriptImage = orderItem.getProduct().getImagePath() != null
                    ? bucketURL + URLEncoder.encode(orderItem.getProduct().getImagePath(), StandardCharsets.UTF_8)
                    : "";

            return RefundResponseDTO.builder()
                    .scriptImage(encodedScriptImage)
                    .title(orderItem.getProduct().getTitle())
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
            throw e;
        }
    }

    @Transactional
    public void refundProcess(final UserEntity userInfo, final RefundRequestDTO dto) {
        try {
            final OrderItemEntity orderItem = getOrderItem(dto.getOrderItemId());

            // 수량, 금액 계산
            final int usedAmount = performanceDateRepo.countByOrderItemId(dto.getOrderItemId());
            final int possibleAmount = orderItem.getPerformanceAmount() - usedAmount;
            final long unitPrice = orderItem.getProduct().getPerformancePrice();
            final long refundPrice = unitPrice * dto.getRefundAmount();

            // 최초 결제 금액 (전체 취소 판단 기준)
            final long totalPrice = orderItem.getTotalPrice();

            // 기존 환불 금액
            final long refundedTotalPrice = refundRepo.sumRefundPriceByOrder(orderItem.getOrder().getId());
            boolean hasPreviousRefund = refundedTotalPrice > 0;

            // 검증
            if(refundPrice + refundedTotalPrice > totalPrice)
                throw new RuntimeException("누적 환불 금액 초과");

            if(dto.getRefundAmount() <= 0 || dto.getRefundAmount() > possibleAmount || refundPrice < 0 || refundPrice > totalPrice)
                throw new RuntimeException("환불 가능 가격과 수량이 아님");

            if(dto.getReason().isEmpty() || dto.getReason().length() > 50)
                throw new RuntimeException("환불 사유는 1 ~ 50자까지 가능");

            if(pdfDownloadLogRepo.existsByOrderItemIdAndUserId(orderItem.getId(), userInfo.getId()))
                throw new RuntimeException("대본을 다운로드 받은 경우는 환불이 불가합니다.");

            if(Duration.between(orderItem.getCreatedAt(), LocalDateTime.now()).toDays() > 14)
                throw new RuntimeException("구매 후 2주가 경과되어 환불이 불가합니다.");

            // 전체취소 여부 판단
            final boolean isFullCancel = refundPrice + refundedTotalPrice == totalPrice && !hasPreviousRefund;

            if(hasPreviousRefund && refundPrice == totalPrice)
                throw new RuntimeException("이미 부분환불된 주문의 전체 취소 시도");

            if (!isFullCancel && refundPrice == totalPrice)
                throw new RuntimeException("부분환불인데 전체 취소 금액과 동일함");

            log.info("refundPrice = {}, originalTotalPrice = {}, isFullCancel = {}", refundPrice, totalPrice, isFullCancel);

            // Nicepay 환불용 orderId 생성
            String refundOrderId = generatedRefundOrderId(orderItem.getOrder().getId());

            NicepayCancelResponseDTO res;

            if(isFullCancel)
                res = requestCancelToNicepay(orderItem.getOrder().getTid(), refundOrderId, dto.getReason(), null);
            else
                res = requestCancelToNicepay(orderItem.getOrder().getTid(), refundOrderId, dto.getReason(), refundPrice);


            if(res == null || !"0000".equals(res.getResultCode())) {
                String error = (res != null) ? res.getResultMsg() : "환불 실패";

                log.error("환불 실패: tid={}, resultCode={}, msg={}", orderItem.getOrder().getTid(), Objects.requireNonNull(res).getResultCode(), error);
                throw new RuntimeException("환불 처리 실패: " + error);
            }

            RefundEntity refund = RefundEntity.builder()
                    .quantity(dto.getRefundAmount())
                    .price(refundPrice)
                    .content(dto.getReason())
                    .order(orderItem.getOrder())
                    .user(userInfo)
                    .build();

            refundRepo.save(refund);

        } catch (Exception e) {
            throw e;
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
            throw e;
        }
    }

    public List<ScriptListResponseDTO.ProductListDTO> getLikePlayList(int page, UserEntity userInfo, PlayType playType, int pageSize) {
        try {
            final Pageable mainLikePage = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            final List<ProductLikeEntity> playLikes = productLikeRepo.findAllByUserAndProduct_PlayType(userInfo, playType, mainLikePage);

            return playLikes.stream()
                    .filter(playLike -> !playLike.getProduct().getIsDelete())
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
                                .likeCount(playLike.getProduct().getLikeCount())
                                .viewCount(viewCountService.getProductViewCount(playLike.getProduct().getId()))
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw e;
        }
    }

    public ScriptListResponseDTO getUserLikeList(UserEntity userInfo) {
        try {
            return new ScriptListResponseDTO(
                    getLikePlayList(0, userInfo, PlayType.LONG, 4),
                    getLikePlayList(0, userInfo, PlayType.SHORT, 4)
            );
        } catch (Exception e) {
            throw e;
        }
    }

    // =========== private (protected) method =============

    private void isValidPw(String password) {
        String regx_pwd = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[$@!%*#?&])[A-Za-z\\d$@!%*#?&]{5,11}$"; // 숫자 최소 1개, 대소문자 최소 1개, 특수문자 최소 1개, (5-11)

        if(password == null || password.isBlank() || password.length() < 4 || password.length() > 12 ||
                !Pattern.matches(regx_pwd, password)) //password가 null이거나 빈 값일때
            throw new RuntimeException("비밀번호 유효성 검사 실패");
    }

    private void isValidNickname(String nickname) {
        String regx_nick = "^[가-힣a-zA-Z0-9]{3,8}$";; // 한글, 영어, 숫자, (-8)

        if(nickname == null || nickname.isBlank() || !Pattern.matches(regx_nick, nickname) ||
                nickname.equals("삭제된 계정") || nickname.equals("삭제 계정"))
            throw new RuntimeException("닉네임 유효성 검사 실패");
    }

    private OrderItemEntity getOrderItem(final UUID orderItemId) {
        if(orderItemRepo.findById(orderItemId) == null)
            throw new RuntimeException("일치하는 구매 목록 없음");

        return orderItemRepo.findById(orderItemId);
    }

    private void deleteFile(final String bucket, final String sourceKey) {
        try {
            if(amazonS3.doesObjectExist(bucket, sourceKey))
                amazonS3.deleteObject(bucket, sourceKey);
        } catch (Exception e) {
            throw new RuntimeException("파일 삭제 실패", e);
        }
    }

    private void expire(final LocalDateTime time) {
        if(LocalDateTime.now().isAfter(time.plusMonths(3)))
            throw new RuntimeException("구매 후 3개월 경과");
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

    private void addWatermark(final InputStream src, final ByteArrayOutputStream dest, final UUID userId) throws IOException {
        try (PdfReader reader = new PdfReader(src);
             PdfWriter writer = new PdfWriter(dest);
             PdfDocument pdfDoc = new PdfDocument(reader, writer);
             InputStream logoInputStream = getClass().getClassLoader().getResourceAsStream("logo.png")) {

            if (logoInputStream == null)
                throw new FileNotFoundException("logo.png not found in resources");

            // ImageDataFactory를 사용하여 이미지 데이터를 생성
            ImageData imageData = ImageDataFactory.create(logoInputStream.readAllBytes());
            PdfFont font = PdfFontFactory.createFont();

            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                PdfPage page = pdfDoc.getPage(i);
                Rectangle pageSize = page.getPageSize();

                PdfCanvas canvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdfDoc);

                canvas.saveState();

                // 투명도 설정
                PdfExtGState gs =  new PdfExtGState();
                gs.setFillOpacity(0.25f);
                canvas.setExtGState(gs);

                // 로고 중앙 배치
                float logoWidth = 200;
                float logoHeight = 200;

                float logoX = (pageSize.getWidth() - logoWidth) / 2;
                float logoY = (pageSize.getHeight() - logoHeight) / 2;

                canvas.addImageWithTransformationMatrix(imageData, logoWidth, 0, 0, logoHeight, logoX, logoY, false);

                // UUID 텍스트 중앙 정렬
                String watermarkText = userId.toString();

                canvas.beginText();
                canvas.setFontAndSize(font, 18);
                canvas.setFillColor(new DeviceRgb(150, 150, 150));

                float textWidth = font.getWidth(watermarkText, 18);
                float textX = (pageSize.getWidth() - textWidth) / 2;

                // 로고 아래 30px
                float textY = logoY - 30;

                canvas.moveText(textX, textY);
                canvas.showText(watermarkText);
                canvas.endText();

                canvas.restoreState();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private byte[] extractPdfFromZip(InputStream zipStream) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipStream);
             ByteArrayOutputStream pdfOutput = new ByteArrayOutputStream()) {

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                if (entry.getName().toLowerCase().endsWith(".pdf")) {

                    byte[] buffer = new byte[8192]; // 8KB 씩 스트리밍
                    int len;

                    while ((len = zis.read(buffer)) > 0) {
                        pdfOutput.write(buffer, 0, len);
                    }

                    return pdfOutput.toByteArray();
                }
            }

            throw new RuntimeException("ZIP 내부에 PDF 없음");
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
                    .performanceQuantity(Math.max(0, orderItemRepo.sumPaidPerformanceAmountByProductId(productId, OrderStatus.PAID) - refundRepo.sumRefundQuantityByProductId(productId)))
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
                    .sorted(Map.Entry.<LocalDate, List<OrderItemEntity>>comparingByKey().reversed())
                    .map(entry -> {
                        LocalDate date = entry.getKey();
                        List<OrderItemEntity> orderItemList = entry.getValue();

                        // 각 주문에 대한 신청자 정보
                        List<RequestedPerformanceResponseDTO.ApplicantInfo> applicantInfoList = orderItemList.stream()
                                .sorted(Comparator.comparing(OrderItemEntity::getCreatedAt).reversed())
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

            product.setIsDelete(true);

            productRepo.save(product);
        } catch (Exception e) {
            throw new RuntimeException("작품 파일 삭제 실패", e);
        }
    }

    private String generatedRefundOrderId(final Long orderId) {
        return orderId + "-R-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private NicepayCancelResponseDTO requestCancelToNicepay(String tid, String refundOrderId, String reason, Long cancelAmt) {
        try {
            String ediDate = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String signString = tid + ediDate + secretKey;
            String signData = sha256Hex(signString);

            String auth = clientKey + ":" + secretKey;
            String encodedKey = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> body = new HashMap<>();
            body.put("reason", reason);
            body.put("orderId", refundOrderId);
            body.put("ediDate", ediDate);
            body.put("signData", signData);

            if (cancelAmt != null) // 부분 취소
                body.put("cancelAmt", cancelAmt);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + encodedKey);

            HttpEntity<?> entity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<NicepayCancelResponseDTO> res = restTemplate.postForEntity(
                    "https://api.nicepay.co.kr/v1/payments/" + tid + "/cancel",
                    entity,
                    NicepayCancelResponseDTO.class
            );

            return res.getBody();
         } catch (Exception e) {
            log.error("환불 API 오류 발생", e);
            throw new RuntimeException("나이스페이 환불 실패", e);
        }
    }

    private Map<UUID, Integer> getPerformanceDateCountMap(List<OrderItemEntity> orderItems) {
        List<UUID> orderItemIds = orderItems.stream()
                .map(OrderItemEntity::getId)
                .toList();

        if (orderItemIds.isEmpty())
            return Map.of();

        return performanceDateRepo.countByOrderItemIds(orderItemIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Long) row[1]).intValue()
                ));
    }

    private Map<Long, Integer> getRefundQuantityMap(List<OrderItemEntity> orderItems) {
        List<Long> orderIds = orderItems.stream()
                .map(oi -> oi.getOrder().getId())
                .distinct()
                .toList();

        if (orderIds.isEmpty())
            return Map.of();

        return refundRepo.sumRefundQuantityByOrderIds(orderIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0], // orderId
                        row -> ((Long) row[1]).intValue() // sum(quantity)
                ));
    }
}
