package vn.yain.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping({"/", "/director/dashboard"})
    public String directorDashboard() {
        return "director-dashboard";
    }

    @GetMapping("/admin/users")
    public String adminUsers() {
        return "admin-users";
    }

    @GetMapping("/booking/grid")
    public String bookingGrid() {
        return "booking-grid";
    }
}

