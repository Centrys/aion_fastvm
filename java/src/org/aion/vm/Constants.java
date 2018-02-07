/*******************************************************************************
 *
 * Copyright (c) 2017-2018 Aion foundation.
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

/**
 * Virtual machine constants.
 *
 * @author yulong
 */
public class Constants {

    public static final int NRG_CODE_DEPOSIT = 1000;

    public static final int NRG_TX_CREATE = 200000;

    public static final int NRG_TX_DATA_ZERO = 4;

    public static final int NRG_TX_DATA_NONZERO = 64;

    public static final int NRG_TRANSACTION = 21000;

    /**
     * Call stack depth limit. Based on EIP-150, the theoretical limit is ~340.
     */
    public static final int MAX_CALL_DEPTH = 128;
}
