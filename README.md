区块链大作业 路线A：Spring Boot + Web3j Escrow 后端

【运行方法】

1. 设置环境变量（删除默认私钥）：
export WEB3J_PRIVATE_KEY=0x<填仲裁人私钥>

2. 启动：
./gradlew bootRun
服务默认端口 8080。

【环境变量】

变量名：WEB3J_PRIVATE_KEY
说明：后端账户/仲裁人私钥，通过环境变量注入。

application.properties 中已配置：
web3j.client-address=https://rpc-testnet.potos.hk
web3j.private-key=${WEB3J_PRIVATE_KEY}
web3j.gas-price=1000000
web3j.gas-limit=6721975

【关键数据与地址】

仓库地址：https://github.com/huihuan6/spring-boot-web3j-escrow
后端账户地址（路线A）：0x71c908841946976A719ec577940Daa0438E89ffd
Escrow 合约地址：0x9C270a3Ef0E598a9589974c221D606e539837E4f
代币合约地址：0x2B6978eD470B09D00b515dB9d4ACF9e8a02ECc2c
买家A：0xF65D25C589F3C9fd254C6469C4df541c1c9c4e0c
卖家B：0xA329e024A5E220061Fb1712b0657b49028289F58

【REST 接口】

1. 部署 Escrow
curl -X POST http://localhost:8080/api/escrow/deploy

2. 加载已有 Escrow
curl -X POST "http://localhost:8080/api/escrow/load?address=0x9C270a3Ef0E598a9589974c221D606e539837E4f&fromBlock=<填入部署区块数字>"

3. 查询合约地址和仲裁人
curl -X GET http://localhost:8080/api/escrow/address

4. 查询单个订单
curl -X GET http://localhost:8080/api/escrow/orders/0
curl -X GET http://localhost:8080/api/escrow/orders/1

5. 查询订单列表（从 OrderCreated 事件重建）
curl -X GET http://localhost:8080/api/escrow/orders

6. 仲裁
curl -X POST "http://localhost:8080/api/escrow/orders/0/resolve?sellerWins=true"
curl -X POST "http://localhost:8080/api/escrow/orders/1/resolve?sellerWins=false"

【交易记录表】

操作：部署 Escrow
交易哈希：0x873d637c10bb468055222d290f3f3683d13ce29042ed6a2045349bd94ef0ae1a
链接：https://scan-testnet.potos.hk/tx/0x873d637c10bb468055222d290f3f3683d13ce29042ed6a2045349bd94ef0ae1a
发起账户：仲裁人 0x71c908841946976A719ec577940Daa0438E89ffd

操作：createOrder 订单 0
交易哈希：0x67527eaf17e1c886cfdd68079ebb40dba36dff4dcb1ab3ba773c7ba6352fbd7d
链接：https://scan-testnet.potos.hk/tx/0x67527eaf17e1c886cfdd68079ebb40dba36dff4dcb1ab3ba773c7ba6352fbd7d
发起账户：买家A 0xF65D25C589F3C9fd254C6469C4df541c1c9c4e0c

操作：approve
交易哈希：0xfded0057adf1598306d9d94388a0c3f055b94b3d55ff35070c0f7e3dbb450596
链接：https://scan-testnet.potos.hk/tx/0xfded0057adf1598306d9d94388a0c3f055b94b3d55ff35070c0f7e3dbb450596
发起账户：买家A 0xF65D25C589F3C9fd254C6469C4df541c1c9c4e0c

操作：deposit 订单 0
交易哈希：0xff1e67b497d292c16df76f00a542481850f438f80b81b07d917000e06c86ee47
链接：https://scan-testnet.potos.hk/tx/0xff1e67b497d292c16df76f00a542481850f438f80b81b07d917000e06c86ee47
发起账户：买家A 0xF65D25C589F3C9fd254C6469C4df541c1c9c4e0c

操作：dispute 订单 1
交易哈希：0xb549049302decd5ac09a013e28acc586e2e3e81a4497f9166e7d0c820ab6b169
链接：https://scan-testnet.potos.hk/tx/0xb549049302decd5ac09a013e28acc586e2e3e81a4497f9166e7d0c820ab6b169
发起账户：卖家B 0xA329e024A5E220061Fb1712b0657b49028289F58

操作：resolve 订单 1
交易哈希：0xffcee3b4a1df187f8cff7ff9c03b29bb937a00915b49f77c422ee35f10059159
链接：https://scan-testnet.potos.hk/tx/0xffcee3b4a1df187f8cff7ff9c03b29bb937a00915b49f77c422ee35f10059159
发起账户：后端 0x71c908841946976A719ec577940Daa0438E89ffd

操作：confirm 订单 0
交易哈希：0x8bf326731a1aa635787b70f84d0f8bdb8d10adf31bd7e72d93f2b10a42411283
链接：https://scan-testnet.potos.hk/tx/0x8bf326731a1aa635787b70f84d0f8bdb8d10adf31bd7e72d93f2b10a42411283
发起账户：买家A 0xF65D25C589F3C9fd254C6469C4df541c1c9c4e0c

操作：resolve 失败（路线 A）
交易哈希：0x599179fd05ed8131df5e95f34adda803c7d99379f4f24d62047a4a45b17a1b6e
链接：https://scan-testnet.potos.hk/tx/0x599179fd05ed8131df5e95f34adda803c7d99379f4f24d62047a4a45b17a1b6e
发起账户：后端 0x71c908841946976A719ec577940Daa0438E89ffd

【补充说明】
Escrow.java 中的 BINARY 因 WSL 下 Web3j 4.14.0 插件生成报错，改为从 Remix 手动复制。
getOrder 使用十六进制字符串切片解析，避免 Web3j 动态 struct 解析越界。
订单列表从 OrderCreated 事件日志按 10 个区块分批重建。