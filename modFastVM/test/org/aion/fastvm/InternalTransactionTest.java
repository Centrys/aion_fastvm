package org.aion.fastvm;

import org.aion.base.type.Address;
import org.aion.base.util.ByteUtil;
import org.aion.crypto.ECKey;
import org.aion.mcf.core.ImportResult;
import org.aion.mcf.vm.types.DataWord;
import org.aion.zero.impl.BlockContext;
import org.aion.zero.impl.StandaloneBlockchain;
import org.aion.zero.impl.types.AionTxInfo;
import org.aion.zero.types.AionTransaction;
import org.junit.Test;

import java.math.BigInteger;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

public class InternalTransactionTest {

/*
pragma solidity ^0.4.0;

contract A {
  event X();

  function f(address addr, uint gas) public {
    X();

    // B(addr).f.gas(gas)();
    addr.call.gas(gas).value(0)(bytes4(sha3("f()")));
  }
}

contract B {
  event Y();

  function f() public {
    Y();

    uint n = 0;
    for (uint i = 0; i < 1000; i++) {
      n += i;
    }
  }
}
*/

    @Test
    public void testLogs() throws InterruptedException {
        String contractA = "0x605060405234156100105760006000fd5b610015565b61013c806100246000396000f30060506040526000356c01000000000000000000000000900463ffffffff1680632d7df21a146100335761002d565b60006000fd5b341561003f5760006000fd5b61006660048080806010013590359091602001909192908035906010019091905050610068565b005b7fc1599bd9a91e57420b9b93745d7475dc054736a3f2becd4f08b450b7012e125760405160405180910390a1828282600060405180806f662829000000000000000000000000008152601001506003019050604051809103902090506c01000000000000000000000000900491906040518363ffffffff166c01000000000000000000000000028152600401600060405180830381858a8a89f195505050505050505b5050505600a165627a7a723058205e51c42347e4353247e8419ef6cda02250d358868e2cb3782d0d5d74065f2ef70029";
        String contractB = "0x605060405234156100105760006000fd5b610015565b60cb806100236000396000f30060506040526000356c01000000000000000000000000900463ffffffff16806326121ff014603157602b565b60006000fd5b3415603c5760006000fd5b60426044565b005b600060007f45b3fe4256d6d198dc4c34457a04e8c048ce54df933a93061f1a0e386b52f7a260405160405180910390a160009150600090505b6103e8811015609a57808201915081505b8080600101915050607d565b5b50505600a165627a7a72305820b2bf8aef36001079d347d250e50b098ad52629336644a841d19db288f30667470029";

        StandaloneBlockchain.Bundle bundle = (new StandaloneBlockchain.Builder())
                .withValidatorConfiguration("simple")
                .withDefaultAccounts()
                .build();
        StandaloneBlockchain bc = bundle.bc;
        ECKey deployerAccount = bundle.privateKeys.get(0);

        //======================
        // DEPLOY
        //======================
        BigInteger nonce = BigInteger.ZERO;
        AionTransaction tx1 = new AionTransaction(
                nonce.toByteArray(),
                null,
                new byte[0],
                ByteUtil.hexStringToBytes(contractA),
                1_000_000L,
                1L
        );
        tx1.sign(deployerAccount);

        nonce = nonce.add(BigInteger.ONE);
        AionTransaction tx2 = new AionTransaction(
                nonce.toByteArray(),
                null,
                new byte[0],
                ByteUtil.hexStringToBytes(contractB),
                1_000_000L,
                1L
        );
        tx2.sign(deployerAccount);

        BlockContext context = bc.createNewBlockContext(bc.getBestBlock(), List.of(tx1, tx2), false);
        ImportResult result = bc.tryToConnect(context.block);
        assertThat(result).isEqualTo(ImportResult.IMPORTED_BEST);

        Address addressA = tx1.getContractAddress();
        System.out.println("contract A = " + addressA);
        Address addressB = tx2.getContractAddress();
        System.out.println("contract B = " + addressB);
        Thread.sleep(1000);

        //======================
        // CALL B
        //======================
        nonce = nonce.add(BigInteger.ONE);
        AionTransaction tx3 = new AionTransaction(
                nonce.toByteArray(),
                addressB,
                new byte[0],
                ByteUtil.hexStringToBytes("0x26121ff0"),
                1_000_000L,
                1L
        );
        tx3.sign(deployerAccount);

        context = bc.createNewBlockContext(bc.getBestBlock(), List.of(tx3), false);
        result = bc.tryToConnect(context.block);
        assertThat(result).isEqualTo(ImportResult.IMPORTED_BEST);

        AionTxInfo info = bc.getTransactionInfo(tx3.getHash());
        System.out.println(info.getReceipt());
        assertEquals(1, info.getReceipt().getLogInfoList().size());
        Thread.sleep(1000);

        //======================
        // CALL A (calls B, 80k)
        //======================
        nonce = nonce.add(BigInteger.ONE);
        AionTransaction tx4 = new AionTransaction(
                nonce.toByteArray(),
                addressA,
                new byte[0],
                ByteUtil.merge(ByteUtil.hexStringToBytes("0x2d7df21a"), addressB.toBytes(), new DataWord(80_000).getData()),
                1_000_000L,
                1L
        );
        tx4.sign(deployerAccount);

        context = bc.createNewBlockContext(bc.getBestBlock(), List.of(tx4), false);
        result = bc.tryToConnect(context.block);
        assertThat(result).isEqualTo(ImportResult.IMPORTED_BEST);

        info = bc.getTransactionInfo(tx4.getHash());
        System.out.println(info.getReceipt());
        assertEquals(2, info.getReceipt().getLogInfoList().size());
        Thread.sleep(1000);

        //======================
        // CALL A (calls B, 20k)
        //======================

        //TODO: confirm with Yulong
//        nonce = nonce.add(BigInteger.ONE);
//        AionTransaction tx5 = new AionTransaction(
//                nonce.toByteArray(),
//                addressA,
//                new byte[0],
//                ByteUtil.merge(ByteUtil.hexStringToBytes("0x2d7df21a"), addressB.toBytes(), new DataWord(20_000).getData()),
//                1_000_000L,
//                1L
//        );
//        tx5.sign(deployerAccount);
//
//        context = bc.createNewBlockContext(bc.getBestBlock(), List.of(tx5), false);
//        result = bc.tryToConnect(context.block);
//        assertThat(result).isEqualTo(ImportResult.IMPORTED_BEST);
//
//        info = bc.getTransactionInfo(tx5.getHash());
//        System.out.println(info.getReceipt());
//        assertEquals(2, info.getReceipt().getLogInfoList().size());
//        Thread.sleep(1000);

        //======================
        // CALL A (calls B, 20k)
        //======================

        nonce = nonce.add(BigInteger.ONE);
        AionTransaction tx6 = new AionTransaction(
                nonce.toByteArray(),
                addressA,
                new byte[0],
                ByteUtil.merge(ByteUtil.hexStringToBytes("0x2d7df21a"), addressB.toBytes(), new DataWord(20_000).getData()),
                1_000_000L,
                1L
        );
        tx6.sign(deployerAccount);

        context = bc.createNewBlockContext(bc.getBestBlock(), List.of(tx6), false);
        result = bc.tryToConnect(context.block);
        assertThat(result).isEqualTo(ImportResult.IMPORTED_BEST);

        info = bc.getTransactionInfo(tx6.getHash());
        System.out.println(info.getReceipt());
        assertEquals(1, info.getReceipt().getLogInfoList().size());
        Thread.sleep(1000);
    }

}