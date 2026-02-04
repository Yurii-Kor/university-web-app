package ua.foxminded.university.security.web;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.foxminded.university.security.web.enums.LoginMessageCode;

@Component
public class LoginAuthEntryPoint implements AuthenticationEntryPoint {
	
	private static final String LOGIN_MSG = "loginMsg"; 

    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    @Override
    public void commence(HttpServletRequest req,
                         HttpServletResponse res,
                         AuthenticationException authEx) throws IOException {

        doLogoutIfAuthenticated(req, res);

        var hadSessionId = req.getRequestedSessionId() != null;
        var sessionInvalid = hadSessionId && !req.isRequestedSessionIdValid();

        var code = sessionInvalid
                ? LoginMessageCode.SESSION_EXPIRED
                : LoginMessageCode.AUTH_REQUIRED;

        var flashMap = new FlashMap();
        flashMap.put(LOGIN_MSG, code.name());

        new SessionFlashMapManager().saveOutputFlashMap(flashMap, req, res);
        res.sendRedirect(req.getContextPath() + "/login");
    }

    private void doLogoutIfAuthenticated(HttpServletRequest req, HttpServletResponse res) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean authenticated = auth != null
                && !(auth instanceof AnonymousAuthenticationToken)
                && auth.isAuthenticated();

        if (authenticated) {
            logoutHandler.logout(req, res, auth);
        }
    }
}
