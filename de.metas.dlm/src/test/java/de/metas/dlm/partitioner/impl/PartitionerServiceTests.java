package de.metas.dlm.partitioner.impl;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.test.AdempiereTestHelper;
import org.adempiere.util.lang.ITableRecordReference;
import org.compiere.model.I_AD_ChangeLog;
import org.compiere.model.I_AD_Field;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import ch.qos.logback.classic.Level;
import de.metas.dlm.partitioner.config.PartitionerConfig;
import de.metas.dlm.partitioner.config.TableReferenceDescriptor;
import de.metas.logging.LogManager;

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

public class PartitionerServiceTests
{

	@Before
	public void before()
	{
		AdempiereTestHelper.get().init();
		LogManager.setLevel(Level.DEBUG);
	}

	@Test
	public void testAugmentPartition()
	{
		final PartitionerConfig config = PartitionerConfig.builder().line(I_AD_Field.Table_Name).endLine().build();

		final List<TableReferenceDescriptor> descriptors = ImmutableList.of(
				TableReferenceDescriptor.of(I_AD_Field.Table_Name, I_AD_ChangeLog.Table_Name, I_AD_ChangeLog.COLUMNNAME_Record_ID));

		testAugmentPartitionSimple(config, descriptors);
	}

	private PartitionerConfig testAugmentPartitionSimple(final PartitionerConfig configtoAugment, final List<TableReferenceDescriptor> descriptors)
	{
		final I_AD_Field adField = InterfaceWrapperHelper.newInstance(I_AD_Field.class);
		InterfaceWrapperHelper.save(adField);
		final ITableRecordReference adFieldReference = ITableRecordReference.FromModelConverter.convert(adField);

		final I_AD_ChangeLog changelog = InterfaceWrapperHelper.newInstance(I_AD_ChangeLog.class);
		changelog.setAD_Table_ID(adFieldReference.getAD_Table_ID());
		changelog.setRecord_ID(adFieldReference.getRecord_ID());
		InterfaceWrapperHelper.save(changelog);

		final PartitionerConfig augmentedConfig = new PartitionerService()
				.augmentPartitionerConfig(configtoAugment, descriptors);

		assertThat(augmentedConfig.getLines().size(), is(2));
		assertThat(augmentedConfig.getLineNotNull(I_AD_ChangeLog.Table_Name).getReferences().size(), is(1));
		assertThat(augmentedConfig.getLineNotNull(I_AD_ChangeLog.Table_Name).getReferences().get(0).getReferencedTableName(), is(I_AD_Field.Table_Name));
		assertThat(augmentedConfig.getLineNotNull(I_AD_ChangeLog.Table_Name).getReferences().get(0).getReferencingColumnName(), is(I_AD_ChangeLog.COLUMNNAME_Record_ID));

		return augmentedConfig;
	}

	/**
	 * Calls {@link PartitionerService#augmentPartitionerConfig(PartitionerConfig, List)} twice in a row, with an equal descriptor.
	 * Veiries that there are no duplicated references because of this.
	 *
	 */
	@Test
	public void testAugmentPartitionAvoidDuplicates()
	{
		final PartitionerConfig config = PartitionerConfig.builder().line(I_AD_Field.Table_Name).endLine().build();

		final List<TableReferenceDescriptor> descriptors = ImmutableList.of(
				TableReferenceDescriptor.of(I_AD_Field.Table_Name, I_AD_ChangeLog.Table_Name, I_AD_ChangeLog.COLUMNNAME_Record_ID));

		final PartitionerConfig augmentedConfig = testAugmentPartitionSimple(config, descriptors);

		final List<TableReferenceDescriptor> descriptors2 = ImmutableList.of(
				TableReferenceDescriptor.of(I_AD_Field.Table_Name, I_AD_ChangeLog.Table_Name, I_AD_ChangeLog.COLUMNNAME_Record_ID));

		// make the same call again. there shall be no double lines or references.
		testAugmentPartitionSimple(augmentedConfig, descriptors2);

	}
}