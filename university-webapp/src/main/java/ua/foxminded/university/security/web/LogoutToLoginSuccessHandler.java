package ua.foxminded.university.security.web;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.foxminded.university.security.web.enums.LoginMessageCode;

@Component
public class LogoutToLoginSuccessHandler implements LogoutSuccessHandler {
	
	private static final String LOGIN_MSG = "loginMsg";

    @Override
    public void onLogoutSuccess(HttpServletRequest req,
                                HttpServletResponse res,
                                Authentication authentication) throws IOException, ServletException {

        FlashMap flashMap = new FlashMap();
        flashMap.put(LOGIN_MSG, LoginMessageCode.LOGGED_OUT.name());

        new SessionFlashMapManager().saveOutputFlashMap(flashMap, req, res);
        res.sendRedirect(req.getContextPath() + "/login");
    }
}
