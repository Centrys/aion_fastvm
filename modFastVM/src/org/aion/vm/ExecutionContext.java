/*******************************************************************************
 *
 * Copyright (c) 2017 Aion foundation.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributors:
 *     Aion foundation.
 ******************************************************************************/
package org.aion.vm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.aion.base.type.Address;
import org.aion.mcf.vm.types.DataWord;

/**
 * Execution context, including both transaction and block information.
 *
 * @author yulong
 */
public class ExecutionContext {
    private static final String NULL_MSG = "Create ExecutionContext with null ";
    private static final String NEG_MSG = " must be non-negative.";
    public static int CALL = 0;
    public static int DELEGATECALL = 1;
    public static int CALLCODE = 2;
    public static int CREATE = 3;

    private TransactionResult result;
    private Address recipient;
    private Address origin;
    private Address caller;
    private Address blockCoinbase;
    private DataWord nrgPrice;
    private DataWord callValue;
    private DataWord blockDifficulty;
    private byte[] txHash;
    private byte[] callData;
    private long nrgLimit; // NOTE: nrg_limit = tx_nrg_limit - tx_basic_cost
    private long blockNumber;
    private long blockTimestamp;
    private long blockNrgLimit;
    private int depth;
    private int kind;
    private int flags;

    /**
     * Create a VM execution context.
     *
     * @param txHash The transaction hash.
     * @param recipient The transaction recipient.
     * @param origin The originator of the transaction (the sender of the original transaction).
     * @param caller The caller of the transaction.
     * @param nrgPrice The energy price in the current environment.
     * @param nrgLimit The energy limit in the current environment.
     * @param callValue The deposited value by the instruction/transaction.
     * @param callData The data associated with the transaction.
     * @param depth The depth into the call stack at which the transaction resides.
     * @param kind The kind of transaction.
     * @param flags The flags for the transaction.
     * @param blockCoinbase The beneficiary associated with the block that holds the transaction.
     * @param blockNumber The block number of the block that holds the transaction.
     * @param blockTimestamp The timestamp of the block that holds the transaction.
     * @param blockNrgLimit The energy limit of the block that holds the transaction.
     * @param blockDifficulty The difficulty of the block that holds the transaction.
     * @param result The result of executing the transaction.
     * @throws NullPointerException if any of the object parameters are null.
     * @throws IllegalArgumentException if any numeric quantities are negative or txHash is not 32
     * bytes.
     */
    public ExecutionContext(byte[] txHash, Address recipient, Address origin, Address caller,
        DataWord nrgPrice, long nrgLimit, DataWord callValue, byte[] callData, int depth, int kind,
        int flags, Address blockCoinbase, long blockNumber, long blockTimestamp, long blockNrgLimit,
        DataWord blockDifficulty, TransactionResult result) {

        super();

        if (txHash == null) { throw new NullPointerException(NULL_MSG + " txHash."); }
        if (recipient == null) { throw new NullPointerException(NULL_MSG + " recipient."); }
        if (origin == null) { throw new NullPointerException(NULL_MSG + " origin."); }
        if (caller == null) { throw new NullPointerException(NULL_MSG + " caller."); }
        if (nrgPrice == null) { throw new NullPointerException(NULL_MSG + " nrgPrice."); }
        if (callValue == null) { throw new NullPointerException(NULL_MSG + " callValue."); }
        if (callData == null) { throw new NullPointerException(NULL_MSG + " callData."); }
        if (blockCoinbase == null) { throw new NullPointerException(NULL_MSG + " blockCoinbase."); }
        if (blockDifficulty == null) { throw new NullPointerException(NULL_MSG + " blockDifficulty."); }
        if (result == null) { throw new NullPointerException(NULL_MSG + " result."); }
        if (txHash.length != 32) { throw new IllegalArgumentException("txHash length must be 32."); }
        if (nrgLimit < 0) { throw new IllegalArgumentException("nrgLimit " + NEG_MSG); }
        if (depth < 0) { throw new IllegalArgumentException("depth " + NEG_MSG); }
        if (blockNumber < 0) { throw new IllegalArgumentException("blockNumber " + NEG_MSG); }
        if (blockTimestamp < 0) { throw new IllegalArgumentException("blockTimestamp " + NEG_MSG); }
        if (blockNrgLimit < 0) { throw new IllegalArgumentException("blockNrgLimit " + NEG_MSG); }

        this.recipient = recipient;
        this.origin = origin;
        this.caller = caller;
        this.nrgPrice = nrgPrice;
        this.nrgLimit = nrgLimit;
        this.callValue = callValue;
        this.callData = callData;
        this.depth = depth;
        this.kind = kind;
        this.flags = flags;
        this.blockCoinbase = blockCoinbase;
        this.blockNumber = blockNumber;
        this.blockTimestamp = blockTimestamp;
        this.blockNrgLimit = blockNrgLimit;
        this.blockDifficulty = blockDifficulty;
        this.txHash = txHash;
        this.result = result;
    }

    /**
     * A big-endian binary encoding of this ExecutionContext in the following format:
     *
     * |32b - recipient|32b - getOrigin|32b - getCaller|16b - getNrgPrice|8b - getNrgLimit|
     * 16b - getCallValue|4b - callDataLength |?b - getCallData|4b - getDepth|4b - getKind|
     * 4b - getFlags|32b - getBlockCoinbase|8b - getBlockNumber|8b - getBlockTimestamp|
     * 8b - getBlockNrgLimit|16b - getBlockDifficulty|
     *
     * where every label in the above description corresponds to a field in this class except for
     * callDataLength, which is an integer representing the length of getCallData.
     *
     * @return a big-endian binary encoding of this ExecutionContext
     */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(getEncodingLength());
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(recipient.toBytes());
        buffer.put(origin.toBytes());
        buffer.put(caller.toBytes());
        buffer.put(nrgPrice.getData());
        buffer.putLong(nrgLimit);
        buffer.put(callValue.getData());
        buffer.putInt(callData.length);
        buffer.put(callData);
        buffer.putInt(depth);
        buffer.putInt(kind);
        buffer.putInt(flags);
        buffer.put(blockCoinbase.toBytes());
        buffer.putLong(blockNumber);
        buffer.putLong(blockTimestamp);
        buffer.putLong(blockNrgLimit);
        buffer.put(blockDifficulty.getData());
        return buffer.array();
    }

    /**
     * @return The transaction hash.
     */
    public byte[] getTransactionHash() { return txHash; }

    /**
     * @return The recipient of the transaction.
     */
    public Address getRecipient() { return recipient; }

    /**
     * @return the sender of the original transaction.
     */
    public Address getOrigin() { return origin; }

    /**
     * @return the caller of the transaction.
     */
    public Address getCaller() { return caller; }

    /**
     * @return The energy price in the current environment.
     */
    public DataWord getNrgPrice() { return nrgPrice; }

    /**
     * @return The energy limit in the current environment.
     */
    public long getNrgLimit() { return nrgLimit; }

    /**
     * @return the deposited value by the instruction/transaction.
     */
    public DataWord getCallValue() { return callValue; }

    /**
     * @return The call data of the transaction.
     */
    public byte[] getCallData() { return callData; }

    /**
     * @return the execution stack depth.
     */
    public int getDepth() { return depth; }

    /**
     * @return The transaction kind.
     */
    public int getKind() { return kind; }

    /**
     * @return The transaction flags.
     */
    public int getFlags() { return flags; }

    /**
     * @return The block's beneficiary.
     */
    public Address getBlockCoinbase() { return blockCoinbase; }

    /**
     * @return The block number.
     */
    public long getBlockNumber() { return blockNumber; }

    /**
     * @return The block timestamp
     */
    public long getBlockTimestamp() { return blockTimestamp; }

    /**
     * @return The block energy limit.
     */
    public long getBlockNrgLimit() { return blockNrgLimit; }

    /**
     * @return The block difficulty.
     */
    public DataWord getBlockDifficulty() { return blockDifficulty; }

    /**
     * @return The transaction result.
     */
    public TransactionResult getResult() { return result; }

    /**
     * Sets the recipient to recipient.
     *
     * @param recipient The new recipient.
     */
    public void setRecipient(Address recipient) {
        if (recipient == null) { throw new NullPointerException("set null recipient."); }
        this.recipient = recipient;
    }

    /**
     * Returns the length of the binary encoding of this ExecutionContext.
     *
     * @return the length of the binary encoding of this ExecutionContext.
     */
    private int getEncodingLength() {
        return (Address.ADDRESS_LEN * 4)+ (DataWord.BYTES * 3) + (Long.BYTES * 4) +
            (Integer.BYTES * 4) + callData.length;
    }
}
