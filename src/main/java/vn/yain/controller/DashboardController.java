package vn.yain.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.yain.repository.BranchRepository;
import vn.yain.repository.UserRepository;
import vn.yain.service.UserService;

@Controller
public class DashboardController {

    @Autowired(required = false)
    private UserService userService;

    @Autowired(required = false)
    private BranchRepository branchRepository;

    @Autowired(required = false)
    private UserRepository userRepository;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // CUSTOMER PORTAL ENDPOINTS (Phan he Cong Khach Hang - Tran Bieu Huong)
    @GetMapping("/customer/home")
    public String customerHome() {
        return "customer/home";
    }

    @GetMapping({ "/customer/auth", "/customer/login" })
    public String customerAuth() {
        return "customer/auth";
    }

    @GetMapping("/customer/booking")
    public String customerBooking() {
        return "customer/booking";
    }

    @GetMapping("/customer/payment")
    public String customerPayment() {
        return "customer/payment";
    }

    @GetMapping("/customer/history")
    public String customerHistory() {
        return "customer/history";
    }

    @GetMapping("/customer/profile")
    public String customerProfile() {
        return "customer/profile";
    }

    @GetMapping("/customer/tournaments")
    public String customerTournaments() {
        return "customer/tournaments";
    }

    @GetMapping({ "/customer/policy", "/policy" })
    public String customerPolicy() {
        return "customer/policy";
    }

    @GetMapping("/director/dashboard")
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
    public String adminUsers(Model model) {
        if (userService != null) {
            model.addAttribute("users", userService.getAllUsers());
        }
        return "admin/users";
    }

    @GetMapping("/admin/permissions")
    public String adminPermissions(Model model) {
        if (userService != null) {
            model.addAttribute("users", userService.getAllUsers());
        }
        if (branchRepository != null) {
            model.addAttribute("branches", branchRepository.findAll());
        }
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

    @GetMapping({ "/booking/grid", "/pos/grid" })
    public String bookingGrid() {
        return "pos/grid";
    }

    // MANAGER ENDPOINTS
    @GetMapping("/manager/dashboard")
    public String managerDashboard() {
        return "manager/dashboard";
    }

    @GetMapping({ "/manager/bookings", "/pos/bookings" })
    public String managerBookings() {
        return "manager/bookings";
    }

    @GetMapping("/manager/courts")
    public String managerCourts() {
        return "manager/courts";
    }

    @GetMapping("/manager/pricing")
    public String managerPricing() {
        return "manager/pricing";
    }

    @GetMapping("/manager/inventory")
    public String managerInventory() {
        return "manager/inventory";
    }

    @GetMapping("/manager/receipts")
    public String managerReceipts() {
        return "manager/receipts";
    }

    @GetMapping("/manager/staff")
    public String managerStaff() {
        return "manager/staff";
    }

    @GetMapping("/manager/reports")
    public String managerReports() {
        return "manager/reports";
    }

}