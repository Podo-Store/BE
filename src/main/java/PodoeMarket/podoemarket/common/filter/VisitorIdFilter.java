package PodoeMarket.podoemarket.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.UUID;

@Component
public class VisitorIdFilter extends OncePerRequestFilter {
    private static final String VISITOR_COOKIE_NAME = "visitorId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Cookie cookie = WebUtils.getCookie(request, VISITOR_COOKIE_NAME);

        String visitorId;

        if(cookie == null) {
            visitorId = UUID.randomUUID().toString();

            Cookie newCookie = new Cookie(VISITOR_COOKIE_NAME, visitorId);
            newCookie.setPath("/");
            newCookie.setMaxAge(60 * 60 * 24 * 365); // 1년
            newCookie.setHttpOnly(true);
            newCookie.setSecure(true); // HTTPS 환경에서는 true
            response.addCookie(newCookie);
        } else {
            visitorId = cookie.getValue();
        }

        // 이후 다른 컨트롤러에서 사용할 수 있도록 request attribute에 저장
        request.setAttribute("visitorId", visitorId);

        filterChain.doFilter(request, response);
    }
}
