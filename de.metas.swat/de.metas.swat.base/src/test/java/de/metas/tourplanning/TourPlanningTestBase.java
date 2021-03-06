package de.metas.tourplanning;

/*
 * #%L
 * de.metas.swat.base
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.adempiere.ad.modelvalidator.IModelInterceptorRegistry;
import org.adempiere.ad.trx.api.ITrxManager;
import org.adempiere.model.IContextAware;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.model.PlainContextAware;
import org.adempiere.test.AdempiereTestHelper;
import org.adempiere.test.AdempiereTestWatcher;
import org.adempiere.util.Services;
import org.adempiere.util.time.SystemTime;
import org.adempiere.util.time.TimeSource;
import org.compiere.model.I_C_BPartner;
import org.compiere.model.I_C_BPartner_Location;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;

import de.metas.tourplanning.api.IDeliveryDayAllocable;
import de.metas.tourplanning.api.IDeliveryDayBL;
import de.metas.tourplanning.api.IDeliveryDayDAO;
import de.metas.tourplanning.api.IShipmentScheduleDeliveryDayBL;
import de.metas.tourplanning.api.ITourDAO;
import de.metas.tourplanning.api.ITourInstanceBL;
import de.metas.tourplanning.api.ITourInstanceDAO;
import de.metas.tourplanning.api.impl.DeliveryDayBL;
import de.metas.tourplanning.api.impl.DeliveryDayDAO;
import de.metas.tourplanning.api.impl.ShipmentScheduleDeliveryDayBL;
import de.metas.tourplanning.model.I_M_DeliveryDay;
import de.metas.tourplanning.model.I_M_DeliveryDay_Alloc;
import de.metas.tourplanning.model.I_M_ShipmentSchedule;
import de.metas.tourplanning.model.I_M_Tour;
import de.metas.tourplanning.model.I_M_TourVersion;
import de.metas.tourplanning.model.I_M_Tour_Instance;

/**
 * Base class (to be extended) for all Tour Planning tests.
 * 
 * @author tsa
 *
 */
public abstract class TourPlanningTestBase
{
	protected IContextAware contextProvider;
	protected DateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSSS");
	protected final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	//
	// Services
	protected DeliveryDayBL deliveryDayBL;
	protected DeliveryDayDAO deliveryDayDAO;
	protected ShipmentScheduleDeliveryDayBL shipmentScheduleDeliveryDayBL;
	protected ITourDAO tourDAO;
	protected ITourInstanceBL tourInstanceBL;
	protected ITourInstanceDAO tourInstanceDAO;

	@Rule
	public TestWatcher testWatchman = new AdempiereTestWatcher();

	@Before
	public void init()
	{
		AdempiereTestHelper.get().init();

		final ITrxManager trxManager = Services.get(ITrxManager.class);
		final String trxName = trxManager.createTrxName("Dummy_ThreadInherited", true);
		trxManager.setThreadInheritedTrxName(trxName);

		this.contextProvider = new PlainContextAware(Env.getCtx(), trxName);

		//
		// Model Interceptors
		Services.get(IModelInterceptorRegistry.class)
				.addModelInterceptor(new de.metas.tourplanning.model.validator.TourPlanningModuleActivator());

		//
		// Services
		this.tourDAO = Services.get(ITourDAO.class);
		this.deliveryDayBL = (DeliveryDayBL)Services.get(IDeliveryDayBL.class);
		this.deliveryDayDAO = (DeliveryDayDAO)Services.get(IDeliveryDayDAO.class);
		this.shipmentScheduleDeliveryDayBL = (ShipmentScheduleDeliveryDayBL)Services.get(IShipmentScheduleDeliveryDayBL.class);
		this.tourInstanceBL = Services.get(ITourInstanceBL.class);
		this.tourInstanceDAO = Services.get(ITourInstanceDAO.class);

		afterInit();
	}

	protected abstract void afterInit();

	@After
	public void resetSystemTime()
	{
		SystemTime.resetTimeSource();
	}

	protected I_C_BPartner createBPartner(final String name)
	{
		final I_C_BPartner bpartner = InterfaceWrapperHelper.newInstance(I_C_BPartner.class, contextProvider);
		bpartner.setValue(name);
		bpartner.setName(name);
		InterfaceWrapperHelper.save(bpartner);
		return bpartner;
	}

	protected I_C_BPartner_Location createBPLocation(final I_C_BPartner bpartner)
	{
		final I_C_BPartner_Location bpLocation = InterfaceWrapperHelper.newInstance(I_C_BPartner_Location.class, contextProvider);
		bpLocation.setC_BPartner_ID(bpartner.getC_BPartner_ID());
		InterfaceWrapperHelper.save(bpLocation);

		return bpLocation;
	}

	protected final I_M_Tour createTour(final String name)
	{
		final I_M_Tour tour = InterfaceWrapperHelper.newInstance(I_M_Tour.class, contextProvider);
		tour.setName(name);
		InterfaceWrapperHelper.save(tour);

		return tour;
	}

	protected final I_M_TourVersion createTourVersion(final I_M_Tour tour, final Date validFrom)
	{
		final I_M_TourVersion tourVersion = InterfaceWrapperHelper.newInstance(I_M_TourVersion.class, tour);
		tourVersion.setName(tour.getName() + "-" + dateFormat.format(validFrom));
		tourVersion.setM_Tour(tour);
		tourVersion.setValidFrom(TimeUtil.asTimestamp(validFrom));
		InterfaceWrapperHelper.save(tourVersion);

		return tourVersion;
	}

	protected final Date createDate(final String yyyyMMdd)
	{
		try
		{
			final Date date = dateFormat.parse(yyyyMMdd);
			return date;
		}
		catch (ParseException e)
		{
			throw new IllegalArgumentException("Invalid date string: " + yyyyMMdd, e);
		}
	}

	protected Timestamp toDateTimeTimestamp(final String dateTimeStr)
	{
		try
		{
			final Date date = dateTimeFormat.parse(dateTimeStr);
			return TimeUtil.asTimestamp(date);
		}
		catch (ParseException e)
		{
			throw new RuntimeException("Cannot parse: " + dateTimeStr, e);
		}
	}

	protected void assertProcessed(final boolean processedExpected, final List<I_M_DeliveryDay> deliveryDays)
	{
		for (final I_M_DeliveryDay dd : deliveryDays)
		{
			final boolean processedActual = dd.isProcessed();
			Assert.assertEquals("Invalid Processed: " + dd, processedExpected, processedActual);
		}
	}

	protected void assertProcessed(final boolean processedExpected, final I_M_Tour_Instance tourInstance)
	{
		Assert.assertNotNull("tour instance shall not be null", tourInstance);
		Assert.assertEquals("Invalid Processed: " + tourInstance, processedExpected, tourInstance.isProcessed());

		final List<I_M_DeliveryDay> deliveryDays = deliveryDayDAO.retrieveDeliveryDaysForTourInstance(tourInstance);
		assertProcessed(processedExpected, deliveryDays);
	}

	/**
	 * Asserts given shipment schedule is allocated to expected delivery day.
	 * 
	 * @param deliveryDayExpected
	 * @param shipmentSchedule
	 */
	protected I_M_DeliveryDay_Alloc assertDeliveryDayAlloc(final I_M_DeliveryDay deliveryDayExpected, final I_M_ShipmentSchedule shipmentSchedule)
	{
		final IDeliveryDayAllocable deliveryDayAllocable = shipmentScheduleDeliveryDayBL.asDeliveryDayAllocable(shipmentSchedule);

		final I_M_DeliveryDay_Alloc alloc = deliveryDayDAO.retrieveDeliveryDayAllocForModel(contextProvider, deliveryDayAllocable);
		Assert.assertNotNull("Delivery Day allocation shall exist for " + shipmentSchedule, alloc);

		final I_M_DeliveryDay deliveryDayActual = alloc.getM_DeliveryDay();
		Assert.assertEquals("Invalid delivery day for " + shipmentSchedule,
				deliveryDayExpected == null ? -1 : deliveryDayExpected.getM_DeliveryDay_ID(),
				deliveryDayActual == null ? -1 : deliveryDayActual.getM_DeliveryDay_ID());

		return alloc;
	}

	/**
	 * sets the system time to a static value. Note that this is reset after each test by {@link #resetSystemTime()}.
	 * 
	 * @param currentTime
	 */
	protected void setSystemTime(final String currentTime)
	{
		SystemTime.setTimeSource(new TimeSource()
		{
			@Override
			public long millis()
			{
				return toDateTimeTimestamp(currentTime).getTime();
			}
		});
	}

}
