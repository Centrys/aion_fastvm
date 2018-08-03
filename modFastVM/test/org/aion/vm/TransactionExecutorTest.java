package org.aion.vm;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import org.aion.base.db.IRepository;
import org.aion.base.db.IRepositoryCache;
import org.aion.base.type.Address;
import org.aion.base.util.Hex;
import org.aion.contract.ContractUtils;
import org.aion.crypto.ECKey;
import org.aion.fastvm.TestVMProvider;
import org.aion.log.AionLoggerFactory;
import org.aion.log.LogEnum;
import org.aion.mcf.core.ImportResult;
import org.aion.vm.AbstractExecutionResult.ResultCode;
import org.aion.zero.impl.BlockContext;
import org.aion.zero.impl.StandaloneBlockchain;
import org.aion.zero.impl.StandaloneBlockchain.Builder;
import org.aion.zero.types.AionInternalTx;
import org.aion.zero.types.AionTransaction;
import org.aion.zero.types.AionTxExecSummary;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * Tests TransactionExecutor in more of an integration style testing.
 */
public class TransactionExecutorTest {
    private static final Logger LOGGER_VM = AionLoggerFactory.getLogger(LogEnum.VM.toString());
    private static final String dat = "75ed1235";
    private static final String f_func = "26121ff0";
    private static final String g_func = "e2179b8e";
    private StandaloneBlockchain blockchain;
    private ECKey deployerKey;
    private Address deployer;

    @Before
    public void setup() {
        StandaloneBlockchain.Bundle bundle = (new StandaloneBlockchain.Builder())
            .withValidatorConfiguration("simple")
            .withDefaultAccounts()
            .build();
        blockchain = bundle.bc;
        deployerKey = bundle.privateKeys.get(0);
        deployer = new Address(deployerKey.getAddress());
    }

    @After
    public void tearDown() {
        blockchain = null;
        deployerKey = null;
        deployer = null;
    }

    @Test
    public void testExecutor() throws IOException {
        Address to = getNewRecipient(true);
        byte[] deployCode = ContractUtils.getContractDeployer("ByteArrayMap.sol", "ByteArrayMap");
        long nrg = 1_000_000;
        long nrgPrice = 1;
        BigInteger value = BigInteger.ZERO;
        BigInteger nonce = BigInteger.ZERO;

        AionTransaction tx = new AionTransaction(nonce.toByteArray(), to, value.toByteArray(),
            deployCode, nrg, nrgPrice);
        tx.sign(deployerKey);
        assertTrue(tx.isContractCreation());
        assertEquals(Builder.DEFAULT_BALANCE, blockchain.getRepository().getBalance(deployer));
        assertEquals(BigInteger.ZERO, blockchain.getRepository().getNonce(deployer));
        BlockContext context = blockchain.createNewBlockContext(blockchain.getBestBlock(),
            Collections.singletonList(tx), false);

        IRepositoryCache repo = blockchain.getRepository().startTracking();
        TransactionExecutor exec = new TransactionExecutor(tx, context.block, repo, LOGGER_VM);
        exec.setExecutorProvider(new TestVMProvider());
        AionTxExecSummary summary = exec.execute();
        BigInteger refund = summary.getRefund();

        // We expect that there is a new account created, the contract, with 0 balance and 0 nonce
        // and that its code is the contract body. We also expect that the deployer (sender) has
        // its nonce incremented and its balance is now equal to its old balance minus the transaction
        // fee plus the refund
        byte[] body = ContractUtils.getContractBody("ByteArrayMap.sol", "ByteArrayMap");

        ExecutionResult res = (ExecutionResult) exec.exeResult;
        assertEquals(ResultCode.SUCCESS, res.getResultCode());
        assertArrayEquals(body, res.getOutput());

        Address contract = summary.getTransaction().getContractAddress();
        assertArrayEquals(body, repo.getCode(contract));
        assertEquals(BigInteger.ZERO, repo.getBalance(contract));
        assertEquals(BigInteger.ZERO, repo.getNonce(contract));

        BigInteger txFee = BigInteger.valueOf(nrg).multiply(BigInteger.valueOf(nrgPrice));
        assertEquals(Builder.DEFAULT_BALANCE.subtract(txFee).add(refund), repo.getBalance(deployer));
        assertEquals(BigInteger.ONE, repo.getNonce(deployer));
    }

    @Test
    public void testExecutorBlind() throws IOException {
        Address to = getNewRecipient(true);
        byte[] deployCode = ContractUtils.getContractDeployer("ByteArrayMap.sol", "ByteArrayMap");
        long nrg = 1_000_000;
        long nrgPrice = 1;
        BigInteger value = BigInteger.ZERO;
        BigInteger nonce = BigInteger.ZERO;

        AionTransaction tx = new AionTransaction(nonce.toByteArray(), to, value.toByteArray(),
            deployCode, nrg, nrgPrice);
        tx.sign(deployerKey);
        assertTrue(tx.isContractCreation());

        assertEquals(Builder.DEFAULT_BALANCE, blockchain.getRepository().getBalance(deployer));
        assertEquals(BigInteger.ZERO, blockchain.getRepository().getNonce(deployer));

        BlockContext context = blockchain.createNewBlockContext(blockchain.getBestBlock(),
            Collections.singletonList(tx), false);
        ImportResult result = blockchain.tryToConnect(context.block);
        assertEquals(ImportResult.IMPORTED_BEST, result);

        // We expect that there is a new account created, the contract, with 0 balance and 0 nonce
        // and that its code is the contract body. We also expect that the deployer (sender) has
        // its nonce incremented and its balance is now equal to its old balance minus the transaction
        // fee plus the refund
        byte[] body = ContractUtils.getContractBody("ByteArrayMap.sol", "ByteArrayMap");
        Address contract = tx.getContractAddress();

        assertArrayEquals(body, blockchain.getRepository().getCode(contract));
        assertEquals(BigInteger.ZERO, blockchain.getRepository().getBalance(contract));
        assertEquals(BigInteger.ZERO, blockchain.getRepository().getNonce(contract));
        assertEquals(BigInteger.ONE, blockchain.getRepository().getNonce(deployer));
        assertEquals(Builder.DEFAULT_BALANCE.subtract(
            BigInteger.valueOf(tx.getNrgConsume()).multiply(BigInteger.valueOf(nrgPrice))),
            blockchain.getRepository().getBalance(deployer));
    }

    @Test
    public void testDeployedCodeFunctionality() throws IOException {
        Address contract = deployByteArrayContract();
        byte[] callingCode = Hex.decode(f_func);
        BigInteger nonce = blockchain.getRepository().getNonce(deployer);
        AionTransaction tx = new AionTransaction(nonce.toByteArray(), contract, BigInteger.ZERO.toByteArray(),
            callingCode, 1_000_000, 1);
        tx.sign(deployerKey);
        assertFalse(tx.isContractCreation());

        BlockContext context = blockchain.createNewBlockContext(blockchain.getBestBlock(),
            Collections.singletonList(tx), false);
        IRepositoryCache repo = blockchain.getRepository().startTracking();
        TransactionExecutor exec = new TransactionExecutor(tx, context.block, repo, LOGGER_VM);
        exec.setExecutorProvider(new TestVMProvider());
        AionTxExecSummary summary = exec.execute();
        ExecutionResult res = (ExecutionResult) exec.exeResult;
        assertEquals(ResultCode.SUCCESS, res.getResultCode());
        System.out.println(Hex.toHexString(res.getOutput()));

        // We called the function f() which returns nothing.
        assertEquals(0, summary.getReceipt().getExecutionResult().length);

        // Now we call the g() function, which returns a byte array of 1024 bytes that starts with
        // 'a' and ends with 'b'
        callingCode = Hex.decode(g_func);
        nonce = blockchain.getRepository().getNonce(deployer);
        tx = new AionTransaction(nonce.toByteArray(), contract, BigInteger.ZERO.toByteArray(),
            callingCode, 1_000_000, 1);
        tx.sign(deployerKey);
        assertFalse(tx.isContractCreation());

        context = blockchain.createNewBlockContext(blockchain.getBestBlock(),
            Collections.singletonList(tx), false);
        repo = blockchain.getRepository().startTracking();
        exec = new TransactionExecutor(tx, context.block, repo, LOGGER_VM);
        exec.setExecutorProvider(new TestVMProvider());
        summary = exec.execute();
        System.out.println(summary.getReceipt());

        res = (ExecutionResult) exec.exeResult;
        System.out.println(Hex.toHexString(res.getOutput()));
        System.out.println(res.getOutput().length);
    }

    @Test
    public void testGfunction() throws IOException {
        Address contract = deployByteArrayContract();
        byte[] callingCode = Hex.decode(g_func);
        BigInteger nonce = blockchain.getRepository().getNonce(deployer);
        AionTransaction tx = new AionTransaction(nonce.toByteArray(), contract, BigInteger.ZERO.toByteArray(),
            callingCode, 1_000_000, 1);
        tx.sign(deployerKey);
        assertFalse(tx.isContractCreation());

        BlockContext context = blockchain.createNewBlockContext(blockchain.getBestBlock(),
            Collections.singletonList(tx), false);
        IRepositoryCache repo = blockchain.getRepository().startTracking();
        TransactionExecutor exec = new TransactionExecutor(tx, context.block, repo, LOGGER_VM);
        exec.setExecutorProvider(new TestVMProvider());
        AionTxExecSummary summary = exec.execute();
        System.out.println(summary.getReceipt());

        ExecutionResult res = (ExecutionResult) exec.exeResult;
        System.out.println(Hex.toHexString(res.getOutput()));
        System.out.println(res.getOutput().length);
    }

    private Address deployByteArrayContract() throws IOException {
        Address to = getNewRecipient(true);
        byte[] deployCode = ContractUtils.getContractDeployer("ByteArrayMap.sol", "ByteArrayMap");
        long nrg = 1_000_000;
        long nrgPrice = 1;
        BigInteger value = BigInteger.ZERO;
        BigInteger nonce = BigInteger.ZERO;

        AionTransaction tx = new AionTransaction(nonce.toByteArray(), to, value.toByteArray(),
            deployCode, nrg, nrgPrice);
        tx.sign(deployerKey);
        assertTrue(tx.isContractCreation());

        assertEquals(Builder.DEFAULT_BALANCE, blockchain.getRepository().getBalance(deployer));
        assertEquals(BigInteger.ZERO, blockchain.getRepository().getNonce(deployer));

        BlockContext context = blockchain.createNewBlockContext(blockchain.getBestBlock(),
            Collections.singletonList(tx), false);
        blockchain.tryToConnect(context.block);

        return tx.getContractAddress();
    }

    private Address getNewRecipient(boolean isContractCreation) {
        return (isContractCreation) ? null : new Address(RandomUtils.nextBytes(Address.ADDRESS_LEN));
    }

}
