package PodoeMarket.podoemarket.Utils;

import PodoeMarket.podoemarket.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class ValidCheck {
    // 유효성 검사
    public static boolean isValidUser(UserDTO userDTO){ // 아이디, 이메일, 비밀번호, 전화번호
        String regx_userId = "^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9]{5,10}$"; // 영어, 숫자, (5-10)
        String regx_pwd = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[$@!%*#?&])[A-Za-z\\d$@!%*#?&]{5,11}$"; // 숫자 최소 1개, 대소문자 최소 1개, 특수문자 최소 1개, (5-11)
        String regx_email = "^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)*(\\.[a-zA-Z]{2,})$";
        String regx_nick = "^[가-힣a-zA-Z0-9]{3,8}$"; // 한글, 영어, 숫자, (-8)

        if(userDTO.getUserId() == null || userDTO.getUserId().isBlank()){ //userId가 null이거나 빈 값일 때
            log.warn("userId is null or empty");
            return false;
        }else if(userDTO.getEmail() == null || userDTO.getEmail().isBlank()){ //이메일이 null이거나 빈 값일때
            log.warn("email is null or empty");
            return false;
        }else if(userDTO.getPassword() == null || userDTO.getPassword().isBlank()){ //password가 null이거나 빈 값일때
            log.warn("password is null or empty");
            return false;
        }else if(userDTO.getNickname() == null || userDTO.getNickname().isBlank()) {
            log.warn("nickname is null or empty");
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
        }else if(!Pattern.matches(regx_nick, userDTO.getNickname())) {
            log.warn("nickname is not fit in the rule");
            return false;
        }else {
            log.info("user valid checked");
            return true;
        }
    }

    public static boolean isValidPw(String password) {
        String regx_pwd = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[$@!%*#?&])[A-Za-z\\d$@!%*#?&]{5,11}$"; // 숫자 최소 1개, 대소문자 최소 1개, 특수문자 최소 1개, (5-11)

        if(password == null || password.isBlank()){ //password가 null이거나 빈 값일때
            log.warn("password is null or empty");
            return false;
        } else if(password.length() < 4 || password.length() > 12) { // password의 길이는 4 초과, 12 미만
            log.warn("password is too long or short");
            return false;
        } else if(!Pattern.matches(regx_pwd, password)){
            log.warn("password is not fit in the rule");
            return false;
        } else {
            log.info("password valid checked");
            return true;
        }
    }

    public static boolean isValidNickname(String nickname) {
        String regx_nick = "^[가-힣a-zA-Z0-9]{3,8}$";; // 한글, 영어, 숫자, (-8)

        if(nickname == null || nickname.isBlank()) {
            log.warn("nickname is null or empty");
            return false;
        } else if(!Pattern.matches(regx_nick, nickname)) {
            log.warn("nickname is not fit in the rule");
            return false;
        } else if(nickname.equals("삭제된 계정") || nickname.equals("삭제 계정")) {
            log.warn("this nickname cannot use");
            return false;
        } else {
            log.info("nickname valid checked");
            return true;
        }
    }

    public static boolean isValidEmail(String email) {
        String regx_email = "^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)*(\\.[a-zA-Z]{2,})$";

        if(email == null || email.isBlank()) {
            log.warn("email is null or empty");
            return false;
        } else if(!Pattern.matches(regx_email, email)){
            log.warn("email is not fit in the rule");
            return false;
        } else {
            log.info("email valid checked");
            return true;
        }
    }

    public static boolean isValidTitle(String title) {
        String regx_title = "^.{1,20}$";

        if(title == null) {
            log.warn("title is null or empty");
            return false;
        } else if(!Pattern.matches(regx_title, title)) {
            log.warn("title is not fit in the rule");
            return false;
        } else {
            log.info("title valid checked");
            return true;
        }
    }

    public static boolean isValidPlot(String plot) {
        String regx_plot = "^.{1,150}$";

        if(plot == null) {
            log.warn("plot is null or empty");
            return false;
        } else if(!Pattern.matches(regx_plot, plot)) {
            log.warn("plot is not fit in the rule");
            return false;
        } else {
            log.info("plot valid checked");
            return true;
        }
    }
}
