package com.wetech.demo.web3j.controller;

import com.wetech.demo.web3j.service.EscrowService;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/escrow")
public class EscrowController {

    private final EscrowService escrowService;

    public EscrowController(EscrowService escrowService) {
        this.escrowService = escrowService;
    }

    @PostMapping("/deploy")
    public Map<String, String> deploy() throws Exception {
        return escrowService.deploy().get();
    }

    @PostMapping("/load")
    public String load(@RequestParam String address, @RequestParam BigInteger fromBlock) {
        escrowService.load(address, fromBlock);
        return "Loaded successfully";
    }

    @GetMapping("/address")
    public Map<String, String> getAddress() throws Exception {
        return Map.of(
                "contractAddress", escrowService.getContractAddress(),
                "arbiter", escrowService.getArbiterAddress()
        );
    }

    @GetMapping("/orders/{id}")
    public Map<String, String> getOrder(@PathVariable BigInteger id) throws Exception {
        return escrowService.getOrder(id).get();
    }

    @GetMapping("/orders")
    public List<Map<String, String>> listOrders() throws Exception {
        return escrowService.listOrders();
    }

    @PostMapping("/orders/{id}/resolve")
    public Map<String, String> resolve(@PathVariable BigInteger id, @RequestParam boolean sellerWins) throws Exception {
        return escrowService.resolve(id, sellerWins).get();
    }
}