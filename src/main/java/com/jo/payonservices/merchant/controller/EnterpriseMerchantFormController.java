package com.jo.payonservices.merchant.controller;

import com.jo.payonservices.merchant.dto.EnterpriseMerchantRegisterRequest;
import com.jo.payonservices.merchant.service.EnterpriseMerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/api/v1/enterprise/merchant/form")
public class EnterpriseMerchantFormController {
    @Autowired
    private EnterpriseMerchantService enterpriseMerchantService;
    @GetMapping
    public String showRegisterForm() {
        return "merchant-form";
    }
    @PostMapping
    public String handleRegisterSubmission(@ModelAttribute EnterpriseMerchantRegisterRequest request, Model model) {

        enterpriseMerchantService.registerPendingMerchant(request);

        model.addAttribute("successMessage", "Your request has been successfully submitted! It is currently under review by the administration, and you will be contacted via the Response URL.");
        return "merchant-form";
    }

}
