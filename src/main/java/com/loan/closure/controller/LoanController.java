package com.loan.closure.controller;

import com.loan.closure.download.TextFileService;
import com.loan.closure.entity.LoanRequest;
import com.loan.closure.entity.LoanResponse;
import com.loan.closure.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/loan")
public class LoanController {

    @Autowired
    private LoanService loanService;

    @Autowired
    private TextFileService textFileService;

    @PostMapping("/summary")
    public List<LoanResponse> getLoanSummary(@RequestBody LoanRequest request) {
        return loanService.calculateAllStrategies(request, false); // no amortization yet
    }

    @PostMapping("/amortization")
    public List<LoanResponse> getAllAmortizations(@RequestBody LoanRequest request) {
        return loanService.calculateAllStrategies(request, true);
    }

    @PostMapping("/download")
    public ResponseEntity<byte[]> downloadTextFile(@RequestBody LoanRequest request) {
        List<LoanResponse> strategies = loanService.calculateAllStrategies(request, true);
        
        byte[] fileBytes = textFileService.generateLoanTextFile(strategies, request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=loan_report.txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(fileBytes);
    }

}