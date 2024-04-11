package PodoeMarket.podoemarket.Utils;

import PodoeMarket.podoemarket.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class ValidUser {
    // 유효성 검사
    public static boolean isValidUser(UserDTO userDTO){ // 아이디, 이메일, 비밀번호, 전화번호
        String regx_userId = "^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9]{5,10}$"; // 영어, 숫자, (5-10)
        String regx_pwd = "^(?=.*[0-9])([a-z|A-Z]*)(?=.*[$@$!%*#?&]).{5,11}$"; // 숫자 최소 1개, 대소문자 최소 1개, 특수문자 최소 1개, (5-11)
        String regx_email = "^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)*(\\.[a-zA-Z]{2,})$";
        String regx_tel = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$";
        String regx_nick = "^[0-9a-zA-Zㄱ-ㅎ가-힣 ]{0,8}$"; // 한글, 영어, 숫자, (-8)

        if(userDTO.getUserId() == null || userDTO.getUserId().isBlank()){ //userId가 null이거나 빈 값일 때
            log.warn("userId is null or empty");
            return false;
        }else if(userDTO.getEmail() == null || userDTO.getEmail().isBlank()){ //이메일이 null이거나 빈 값일때
            log.warn("email is null or empty");
            return false;
        }else if(userDTO.getPassword() == null || userDTO.getPassword().isBlank()){ //password가 null이거나 빈 값일때
            log.warn("password is null or empty");
            return false;
        }else if(userDTO.getPassword().length() < 4 || userDTO.getPassword().length() > 12) { // password의 길이는 4 초과, 12 미만
            log.warn("password is too long or short");
            return false;
        }else if(!userDTO.getPassword().equals(userDTO.getConfirmPassword())){
            log.warn("passwords are not same");
            return false;
        }else if(!Pattern.matches(regx_userId, userDTO.getUserId())) {
            log.warn("userId is not fit in the rule");
            return false;
        }else if(!Pattern.matches(regx_pwd, userDTO.getPassword())){
            log.warn("password is not fit in the rule");
            return false;
        }else if(!Pattern.matches(regx_email, userDTO.getEmail())){
            log.warn("email is not fit in the rule");
            return false;
        }else if(!Pattern.matches(regx_tel, userDTO.getPhoneNumber())) {
            log.warn("phonenumber is not fit in the rule");
            return false;
        }else if(!Pattern.matches(regx_nick, userDTO.getNickname())) {
            log.warn("nickname is not fit in the rule");
            return false;
        }else {
            log.info("user valid checked");
            return true;
        }
    }
}
