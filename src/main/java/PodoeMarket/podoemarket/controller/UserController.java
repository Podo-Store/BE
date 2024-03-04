package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.UserDTO;
import PodoeMarket.podoemarket.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@RequiredArgsConstructor
@Controller
@Slf4j
public class UserController {
    private final UserService service;

    @PostMapping("/user")
    public String signup(UserDTO dto) {
        service.save(dto);
        return "redirect:/login";
    }
}
