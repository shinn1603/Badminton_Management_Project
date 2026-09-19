package vn.yain.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping({"/", "/director/dashboard"})
    public String directorDashboard() {
        return "director/dashboard";
    }

    @GetMapping("/director/branches")
    public String directorBranches() {
        return "director/branches";
    }

    @GetMapping("/director/pricing")
    public String directorPricing() {
        return "director/pricing";
    }

    @GetMapping("/director/tournaments")
    public String directorTournaments() {
        return "director/tournaments";
    }

    @GetMapping("/admin/users")
    public String adminUsers() {
        return "admin/users";
    }

    @GetMapping("/admin/permissions")
    public String adminPermissions() {
        return "admin/permissions";
    }

    @GetMapping("/admin/settings")
    public String adminSettings() {
        return "admin/settings";
    }

    @GetMapping("/admin/logs")
    public String adminLogs() {
        return "admin/logs";
    }

    @GetMapping({"/booking/grid", "/pos/grid"})
    public String bookingGrid() {
        return "pos/grid";
    }
}

