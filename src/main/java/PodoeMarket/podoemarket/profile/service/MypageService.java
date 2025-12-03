package PodoeMarket.podoemarket.profile.service;

import PodoeMarket.podoemarket.common.entity.*;
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
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import org.springframework.cglib.core.Local;
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
            final List<OrderItemEntity> allOrderItems = orderItemRepo.findAllByUserIdAndScript(userInfo.getId(), true, sort);

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

            // DateOrderDTO로 변환하여 OrderScriptsResponseDTO 생성 및 반환
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
                    orderItemDTO.setWriter(orderItem.getWriter());
                    orderItemDTO.setPerformanceAmount(orderItem.getPerformanceAmount());

                    if(LocalDateTime.now().isAfter(orderItem.getCreatedAt().plusYears(1)))
                        orderItemDTO.setPossibleCount(0);
                    else
                        orderItemDTO.setPossibleCount(orderItem.getPerformanceAmount() - dateCount);

                    if(orderItem.getProduct() == null) { // 완전히 삭제된 작품
                        orderItemDTO.setDelete(true);
                        orderItemDTO.setPerformancePrice(orderItem.getPerformanceAmount() > 0 ? orderItem.getPerformancePrice() : 0);
                        orderItemDTO.setPerformanceTotalPrice(orderItem.getPerformancePrice());
                    } else if(orderItem.getProduct().getIsDelete()) { // 삭제 표시가 된 작품
                        orderItemDTO.setDelete(true);
                        orderItemDTO.setPerformancePrice(orderItem.getPerformanceAmount() > 0 ? orderItem.getPerformancePrice() : 0);
                        orderItemDTO.setPerformanceTotalPrice(orderItem.getPerformancePrice());
                    } else { // 정상 작품
                        String encodedScriptImage = orderItem.getProduct().getImagePath() != null
                                ? bucketURL + URLEncoder.encode(orderItem.getProduct().getImagePath(), StandardCharsets.UTF_8)
                                : "";

                        orderItemDTO.setDelete(false);
                        orderItemDTO.setImagePath(encodedScriptImage);
                        orderItemDTO.setChecked(orderItem.getProduct().getChecked());
                        orderItemDTO.setPerformancePrice(orderItem.getPerformanceAmount() > 0 ? orderItem.getProduct().getPerformancePrice() : 0);
                        orderItemDTO.setPerformanceTotalPrice(orderItem.getPerformancePrice());
                        orderItemDTO.setProductId(orderItem.getProduct().getId());
                    }

                    final LocalDate orderDate = orderItem.getCreatedAt().toLocalDate(); // localdatetime -> localdate
                    // 날짜에 따른 리스트를 초기화하고 추가 - orderDate라는 key가 없으면 만들고, orderItemDTO를 value로 추가
                    OrderItems.computeIfAbsent(orderDate, k -> new ArrayList<>()).add(orderItemDTO);
                }
            }

            // DateOrderDTO로 변환
            List<OrderPerformanceResponseDTO.DatePerformanceOrderDTO> orderList = OrderItems.entrySet().stream()
                    .sorted(Map.Entry.<LocalDate, List<OrderPerformanceResponseDTO.DatePerformanceOrderDTO.OrderPerformanceDTO>>comparingByKey().reversed())
                    .map(entry -> new OrderPerformanceResponseDTO.DatePerformanceOrderDTO(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

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
            throw e;
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
            throw e;
        }
    }

    public ScriptInfoResponseDTO checkValidation(final UUID orderId) {
        try {
            final OrderItemEntity orderItem = getOrderItem(orderId);

            if(!orderItem.getScript())
                throw new RuntimeException("대본을 구매하세요");

            expire(orderItem.getCreatedAt());

            return ScriptInfoResponseDTO.builder()
                    .filePath(orderItem.getProduct().getFilePath())
                    .title(orderItem.getProduct().getTitle())
                    .build();
        } catch (Exception e) {
            throw e;
        }
    }

    public byte[] downloadFile(final String fileKey, final String email) throws IOException {
        // S3에서 파일 객체 가져오기
        try (S3Object s3Object = amazonS3.getObject(bucket, fileKey);
             InputStream inputStream = s3Object.getObjectContent();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            addWatermark(inputStream, outputStream, email);

            return outputStream.toByteArray();
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

            final int possibleAmount = orderItem.getPerformanceAmount() - performanceDateRepo.countByOrderItemId(orderItemId);
            final long possiblePrice = orderItem.getProduct().getPerformancePrice() * possibleAmount;

            return RefundResponseDTO.builder()
                    .scriptImage(orderItem.getProduct().getImagePath())
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

            final int possibleAmount = orderItem.getPerformanceAmount() - performanceDateRepo.countByOrderItemId(dto.getOrderItemId());
            final long possiblePrice = orderItem.getProduct().getPerformancePrice() * possibleAmount;
            final long refundPrice = orderItem.getProduct().getPerformancePrice() * dto.getRefundAmount();

            if(dto.getRefundAmount() > possibleAmount || refundPrice > possiblePrice || dto.getRefundAmount() == 0 || refundPrice < 0)
                throw new RuntimeException("환불 가능 수량과 가격이 아님");

            if(dto.getReason().isEmpty() || dto.getReason().length() > 50)
                throw new RuntimeException("환불 사유는 1 ~ 50자까지 가능");

            if(Duration.between(orderItem.getCreatedAt(), LocalDateTime.now()).toDays() > 14)
                throw new RuntimeException("구매 후 2주가 경과되어 환불 불가");

            // Nicepay 환불용 orderId 생성
            String refundOrderId = generatedRefundOrderId(orderItem.getOrder().getId());

            NicepayCancelResponseDTO res = nicepayCancel(
                    orderItem.getOrder().getTid(),
                    refundPrice,
                    refundOrderId,
                    dto.getReason());

            if(res == null || !"0000".equals(res.getResultCode())) {
                String error = (res != null) ? res.getResultMsg() : "환불 실패";

                log.error("환불 실패: tid={}, resultCode={}, msg={}", orderItem.getOrder().getTid(), res.getResultCode(), error);
                throw new RuntimeException("환불 처리 실패: " + error);
            }

            final RefundEntity refund = RefundEntity.builder()
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

            product.setIsDelete(true);

            productRepo.save(product);
        } catch (Exception e) {
            throw new RuntimeException("작품 파일 삭제 실패", e);
        }
    }

    private String generatedRefundOrderId(final Long orderId) {
        return orderId + "-R-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private NicepayCancelResponseDTO nicepayCancel(String tid, long cancelAmount, String refundOrderId, String reason) {
        try {
            String ediDate = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String signString = tid + ediDate + secretKey;
            String signData = sha256Hex(signString);

            String auth = clientKey + ":" + secretKey;
            String encodedKey = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> body = Map.of(
                    "reason", reason,
                    "orderId", refundOrderId,
                    "cancelAmt", cancelAmount,
                    "ediDate", ediDate,
                    "signData", signData
            );

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
}
