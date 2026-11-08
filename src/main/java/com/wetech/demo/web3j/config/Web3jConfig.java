package com.wetech.demo.web3j.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;

@Slf4j
@Configuration
public class Web3jConfig {

    @Value("${web3j.client-address:http://localhost:8545}")
    private String clientAddress;

    // 私钥由环境变量注入，不设默认值，避免明文私钥进入仓库
    @Value("${web3j.private-key}")
    private String privateKey;

    // 默认值调小，避免配置缺失时变为 20 gwei ，导致手续费过高，后端账户被高额手续费耗尽
    @Value("${web3j.gas-price:1000000}")
    private String gasPrice;

    @Value("${web3j.gas-limit:6721975}")
    private String gasLimit;

    @Bean
    public Web3j web3j() {
        log.info("Connecting to Ethereum client: {}", clientAddress);
        return Web3j.build(new HttpService(clientAddress));
    }

    @Bean
    public Credentials credentials() {
        return Credentials.create(privateKey);
    }

    @Bean
    public ContractGasProvider contractGasProvider() {
        return new StaticGasProvider(
                new BigInteger(gasPrice),
                new BigInteger(gasLimit)
        );
    }
}