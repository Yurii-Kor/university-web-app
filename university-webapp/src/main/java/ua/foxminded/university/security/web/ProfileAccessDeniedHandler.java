package ua.foxminded.university.security.web;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.foxminded.university.security.web.enums.ProfileMessageCode;

@Component
public class ProfileAccessDeniedHandler implements AccessDeniedHandler {

    public static final String PROFILE_MSG = "profileMsg";

    @Override
    public void handle(HttpServletRequest req,
                       HttpServletResponse res,
                       AccessDeniedException ex) throws IOException {

        var ctx = req.getContextPath();
        var uri = req.getRequestURI();

        var flash = new FlashMap();

        if ((ctx + "/login").equals(uri)) {
            flash.put(PROFILE_MSG, ProfileMessageCode.ALREADY_SIGNED_IN.name());
            new SessionFlashMapManager().saveOutputFlashMap(flash, req, res);
            res.sendRedirect(ctx + "/profile");
            return;
        }

        flash.put(PROFILE_MSG, ProfileMessageCode.ACCESS_DENIED.name());
        new SessionFlashMapManager().saveOutputFlashMap(flash, req, res);
        res.sendRedirect(ctx + "/profile");
    }
}
