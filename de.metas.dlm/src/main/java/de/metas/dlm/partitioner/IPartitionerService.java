package de.metas.dlm.partitioner;

import java.util.List;

import org.adempiere.util.ISingletonService;

import de.metas.connection.ITemporaryConnectionCustomizer;
import de.metas.dlm.Partition;
import de.metas.dlm.model.IDLMAware;
import de.metas.dlm.model.I_DLM_Partition;
import de.metas.dlm.model.I_DLM_Partition_Config;
import de.metas.dlm.partitioner.PartitionRequestFactory.CreatePartitionRequest;
import de.metas.dlm.partitioner.config.PartitionerConfig;
import de.metas.dlm.partitioner.config.TableReferenceDescriptor;

/*
 * #%L
 * metasfresh-dlm
 * %%
 * Copyright (C) 2016 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public interface IPartitionerService extends ISingletonService
{
	/**
	 * Use the given config and create a partition. Note that the records which are part of the partition are not modified by this method.
	 *
	 * @param request
	 * @return
	 */
	Partition createPartition(CreatePartitionRequest request);

	/**
	 * Create a new DLM_Partition_Record for the given <code>partition</code> and update the {@link IDLMAware#COLUMNNAME_DLM_Partition_ID} values of the given <code>partition</code>'s records.
	 *
	 * @param partition
	 * @return
	 */
	I_DLM_Partition storePartition(Partition partition);

	/**
	 * Persist the given config in the DB and update the ID properties on the given <code>config</code> that is stored.
	 * <p>
	 * Does <b>not</b> touch existing DB records which are missing in the given <code>config</code>.
	 *
	 * @param config
	 * @return
	 */
	I_DLM_Partition_Config storePartitionConfig(PartitionerConfig config);

	/**
	 * Create a new config instance for the given database record.
	 *
	 * @param configDB
	 * @return
	 */
	PartitionerConfig loadPartitionConfig(I_DLM_Partition_Config configDB);

	/**
	 * Creates a new config that has both everything from the given <code>config</code> and the reference specified by the given <code>descriptor</code>.
	 *
	 * @param config
	 * @param descriptor
	 * @return
	 */
	PartitionerConfig augmentPartitionerConfig(PartitionerConfig config, List<TableReferenceDescriptor> descriptor);

	/**
	 * Create a connection customizer that can (and should) be registered via {@link de.metas.connection.IConnectionCustomizerService#registerTemporaryCustomizer(ITemporaryConnectionCustomizer)}
	 * for the time that {@link #createPartition(CreatePartitionRequest)} and {@link #storePartition(Partition)} are invoked.
	 *
	 * @return
	 */
	ITemporaryConnectionCustomizer createConnectionCustomizer();
}