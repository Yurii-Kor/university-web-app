package ua.foxminded.university.security.web;

import java.io.IOException;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.foxminded.university.security.web.enums.LoginMessageCode;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

	private static final String LOGIN_MSG = "loginMsg";

    @Override
    public void onAuthenticationFailure(HttpServletRequest req,
                                        HttpServletResponse res,
                                        AuthenticationException ex) throws IOException, ServletException {

        var code = (ex instanceof DisabledException || ex instanceof LockedException)
                ? LoginMessageCode.ACCOUNT_BLOCKED
                : LoginMessageCode.BAD_CREDENTIALS;

        var flashMap = new FlashMap();
        flashMap.put(LOGIN_MSG, code.name());
        
        new SessionFlashMapManager().saveOutputFlashMap(flashMap, req, res);
        res.sendRedirect(req.getContextPath() + "/login");
    }
}
