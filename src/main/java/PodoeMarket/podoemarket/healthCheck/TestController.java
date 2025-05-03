package PodoeMarket.podoemarket.healthCheck;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class TestController {
    @GetMapping("/")
    public String healthCheck() {
        return "Application is running";
    }
}
