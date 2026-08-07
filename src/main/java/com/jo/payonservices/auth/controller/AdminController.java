package com.jo.payonservices.auth.controller;

import com.jo.payonservices.auth.service.AdminService;
import com.jo.payonservices.merchant.model.Merchant;
import com.jo.payonservices.merchant.service.EnterpriseMerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private EnterpriseMerchantService enterpriseMerchantService;
    @Autowired
    private AdminService adminService;
    @GetMapping("/get")
    public ResponseEntity<?> getAdmin(){
        adminService.getAdmin();
        return ResponseEntity.ok().build();
    }
    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }
    @GetMapping("/dashboard")
    public String admin() {
        return "admin-dashboard";
    }
    @GetMapping("/pending")
    public String pending(Model model) {
        List<Merchant> merchants = enterpriseMerchantService.getPendingMerchants();
        model.addAttribute("pendingMerchants", merchants);
        return "merchant-requests";
    }
    @GetMapping("/merchant/{id}")
    public String merchant(Model model, @PathVariable("id") UUID merchantId) {
        Merchant merchant = enterpriseMerchantService.getPendingMerchant(merchantId);
        model.addAttribute("merchant", merchant);
        return "merchant-detail";
    }
    @PostMapping("/{merchantId}/approve")
    public String approveMerchant(@PathVariable UUID merchantId) {
        enterpriseMerchantService.approveMerchant(merchantId);
        return "redirect:/admin/pending";
    }
    @PostMapping("/{merchantId}/reject")
    public String rejectMerchant(@PathVariable UUID merchantId) {
        enterpriseMerchantService.rejectAndDeleteMerchant(merchantId);
        return "redirect:/admin/pending";
    }
    @GetMapping("/logout")
    public String logout(Model model) {
        return "redirect:/admin/login?logout";
    }
}
