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

import org.aion.base.db.IRepositoryCache;
import org.aion.core.AccountState;
import org.aion.db.IBlockStoreBase;
import org.aion.vm.types.DataWord;

/**
 * High-level interface of Aion virtual machine.
 *
 * @author yulong
 */
public interface VirtualMachine {

    /**
     * Run the given code, under the specified context.
     *
     * @param code  byte code
     * @param ctx   the execution context
     * @param track state repository track
     * @return the execution result
     */
    ExecutionResult run(byte[] code, ExecutionContext ctx, IRepositoryCache<AccountState, DataWord, IBlockStoreBase<?, ?>> track);
}
