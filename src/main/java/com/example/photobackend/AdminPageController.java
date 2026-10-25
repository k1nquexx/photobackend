package com.example.photobackend;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контроллер защищенных административных страниц и сценариев.
 * Предотвращает неавторизованный доступ и утечку DOM-структуры,
 * внутренних полей и логики интерфейса админки через публичную статику.
 */
@Controller
public class AdminPageController {

    private final AdminAuthService adminAuthService;

    @Autowired
    public AdminPageController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @GetMapping({"/admin", "/admin.html"})
    public ResponseEntity<Resource> getAdminPage(HttpServletRequest request) {
        String token = AdminAuthInterceptor.extractToken(request);
        if (adminAuthService.isValid(token)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                    .body(new ClassPathResource("protected/admin.html"));
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .body(new ClassPathResource("protected/login.html"));
    }

    @GetMapping({"/schedule", "/schedule.html"})
    public ResponseEntity<Resource> getSchedulePage(HttpServletRequest request) {
        String token = AdminAuthInterceptor.extractToken(request);
        if (adminAuthService.isValid(token)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                    .body(new ClassPathResource("protected/schedule.html"));
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .body(new ClassPathResource("protected/login.html"));
    }

    @GetMapping("/admin.js")
    public ResponseEntity<?> getAdminJs(HttpServletRequest request) {
        String token = AdminAuthInterceptor.extractToken(request);
        if (adminAuthService.isValid(token)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/javascript;charset=UTF-8"))
                    .body(new ClassPathResource("protected/admin.js"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.parseMediaType("application/javascript;charset=UTF-8"))
                .body("/* 403 Forbidden: Доступ к административным скриптам защищен */");
    }

    @GetMapping("/schedule.js")
    public ResponseEntity<?> getScheduleJs(HttpServletRequest request) {
        String token = AdminAuthInterceptor.extractToken(request);
        if (adminAuthService.isValid(token)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/javascript;charset=UTF-8"))
                    .body(new ClassPathResource("protected/schedule.js"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.parseMediaType("application/javascript;charset=UTF-8"))
                .body("/* 403 Forbidden: Доступ к административным скриптам защищен */");
    }
}
