package com.wetech.demo.web3j.service;

import com.wetech.demo.web3j.contracts.escrow.Escrow;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscrowService {
    private static final String[] STATUS = {"Created", "Paid", "Completed", "Refunded", "Disputed"};

    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractGasProvider gasProvider;

    private Escrow contract;

    @Getter
    private String contractAddress;

    @Getter
    private BigInteger deployBlock;

    public CompletableFuture<Map<String, String>> deploy() {
        return Escrow.deploy(web3j, credentials, gasProvider, credentials.getAddress())
                .sendAsync()
                .thenApply(c -> {
                    this.contract = c;
                    this.contractAddress = c.getContractAddress();
                    this.deployBlock = c.getTransactionReceipt()
                            .map(TransactionReceipt::getBlockNumber)
                            .orElse(BigInteger.ZERO);
                    Map<String, String> res = new HashMap<>();
                    res.put("contractAddress", contractAddress);
                    res.put("transactionHash", c.getTransactionReceipt().get().getTransactionHash());
                    res.put("deployBlock", deployBlock.toString());
                    return res;
                });
    }

    public void load(String address, BigInteger fromBlock) {
        this.contract = Escrow.load(address, web3j, credentials, gasProvider);
        this.contractAddress = address;
        this.deployBlock = fromBlock;
    }

    public String getArbiterAddress() throws Exception {
        requireContract();
        return contract.arbiter().send();
    }

    // 修正了全局偏移量（跳过前 64 个字符）的硬核切片法，为了解决调用 /orders/1 查询订单详情时，后端报错
    public CompletableFuture<Map<String, String>> getOrder(BigInteger id) {
        requireContract();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Function function = new Function(
                        "getOrder",
                        java.util.Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(id)),
                        java.util.Collections.emptyList()
                );
                String encodedFunction = FunctionEncoder.encode(function);
                org.web3j.protocol.core.methods.response.EthCall response = web3j.ethCall(
                        Transaction.createEthCallTransaction(credentials.getAddress(), contractAddress, encodedFunction),
                        DefaultBlockParameterName.LATEST
                ).send();

                if (response.hasError()) {
                    throw new RuntimeException("Call failed: " + response.getError().getMessage());
                }

                Map<String, String> map = new HashMap<>();
                String hex = response.getValue().substring(2);

                if (hex.length() < 64 * 7) {
                    map.put("status", "Unknown");
                    return map;
                }

                // 跳过前 64 个字符的全局 offset 指针
                String structHex = hex.substring(64);

                // 从 structHex 开始按槽位提取数据
                map.put("buyer", "0x" + structHex.substring(24, 64));
                map.put("seller", "0x" + structHex.substring(88, 128));
                map.put("token", "0x" + structHex.substring(152, 192));
                map.put("amount", new BigInteger(structHex.substring(192, 256), 16).toString());
                map.put("deadline", new BigInteger(structHex.substring(256, 320), 16).toString());

                // 状态在槽位 5 (320~384)
                int statusInt = new BigInteger(structHex.substring(320, 384), 16).intValue();
                map.put("status", STATUS[statusInt]);

                // 解析动态字符串 description
                map.put("description", "");
                if (structHex.length() >= 448) {
                    int descOffset = new BigInteger(structHex.substring(384, 448), 16).intValue() * 2;
                    if (descOffset + 64 <= structHex.length()) {
                        int descLen = new BigInteger(structHex.substring(descOffset, descOffset + 64), 16).intValue() * 2;
                        if (descOffset + 64 + descLen <= structHex.length()) {
                            String descHex = structHex.substring(descOffset + 64, descOffset + 64 + descLen);
                            map.put("description", new String(Numeric.hexStringToByteArray(descHex), StandardCharsets.UTF_8));
                        }
                    }
                }

                return map;
            } catch (Exception e) {
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("error", e.getMessage());
                return errorMap;
            }
        });
    }

    public List<Map<String, String>> listOrders() throws Exception {
        requireContract();
        BigInteger latest = web3j.ethBlockNumber().send().getBlockNumber();
        BigInteger step = BigInteger.TEN;
        List<Log> logs = new ArrayList<>();

        for (BigInteger from = deployBlock; from.compareTo(latest) <= 0; from = from.add(step)) {
            BigInteger to = from.add(step).subtract(BigInteger.ONE).min(latest);
            EthFilter filter = new EthFilter(
                    DefaultBlockParameter.valueOf(from),
                    DefaultBlockParameter.valueOf(to),
                    contractAddress
            );
            filter.addSingleTopic(EventEncoder.encode(Escrow.ORDERCREATED_EVENT));
            web3j.ethGetLogs(filter).send().getLogs().forEach(r -> logs.add((Log) r.get()));
        }

        List<Map<String, String>> result = new ArrayList<>();
        for (Log l : logs) {
            Escrow.OrderCreatedEventResponse e = Escrow.getOrderCreatedEventFromLog(l);
            Map<String, String> map = new HashMap<>();
            map.put("id", e.id.toString());
            map.put("buyer", e.buyer);
            map.put("seller", e.seller);
            map.put("amount", e.amount.toString());
            map.put("blockNumber", l.getBlockNumber().toString());
            map.put("transactionHash", l.getTransactionHash());
            result.add(map);
        }
        return result;
    }

    public CompletableFuture<Map<String, String>> resolve(BigInteger id, boolean sellerWins) {
        requireContract();
        return contract.resolve(id, sellerWins).sendAsync().thenApply(receipt -> {
            Map<String, String> map = new HashMap<>();
            map.put("transactionHash", receipt.getTransactionHash());
            map.put("blockNumber", receipt.getBlockNumber().toString());
            map.put("status", receipt.isStatusOK() ? "Success" : "Failed");
            return map;
        });
    }

    private void requireContract() {
        if (contract == null) {
            throw new IllegalStateException("Contract not deployed or loaded. Call /deploy or /load first");
        }
    }
}