package com.loan.closure.controller;

import com.loan.closure.download.TextFileService;
import com.loan.closure.entity.LoanRequest;
import com.loan.closure.entity.LoanResponse;
import com.loan.closure.entity.StrategyRequest;
import com.loan.closure.entity.StrategyResult;
import com.loan.closure.service.LoanService;
import com.loan.closure.service.StrategyFacadeService;
import jakarta.validation.Valid;
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

    private final LoanService loanService;

    private final TextFileService textFileService;

    private final StrategyFacadeService strategyFacadeService;

    public LoanController(LoanService loanService, TextFileService textFileService, StrategyFacadeService strategyFacadeService) {
        this.loanService = loanService;
        this.textFileService = textFileService;
        this.strategyFacadeService = strategyFacadeService;
    }

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

    @PostMapping("/strategy")
    public StrategyResult calculateStrategy(@Valid @RequestBody StrategyRequest request) {
        return strategyFacadeService.calculateStrategy(request);
    }

}