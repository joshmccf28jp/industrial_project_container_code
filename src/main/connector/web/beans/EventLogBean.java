/*
 * Copyright 2017 EnergySys Limited. All Rights Reserved.
 *
 * This software is the proprietary information of EnergySys Limited.
 * Use is subject to licence terms.
 * This software is not designed or supplied to be used in circumstances where
 * personal injury, death or loss of or damage to property could result from any
 * defect in the software.
 * In no event shall the developers or owners be liable for personal injury,
 * death or loss or damage to property, loss of business, revenue, profits, use,
 * data or other economic advantage or for any indirect, punitive, special,
 * incidental, consequential or exemplary loss or damage resulting from the use
 * of the software or documentation.
 * Developer and owner make no warranties, representations or undertakings of
 * any nature in relation to the software and documentation.
 */
package com.energysys.connector.web.beans;

import com.energysys.calendar.CurrentDateTime;
import com.energysys.connector.EventResult;
import com.energysys.connector.database.GenericDAO;
import jakarta.persistence.Query;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


/**
 * JSF Backing Bean for scheduling quartz jobs.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
@ManagedBean(name = "EventLogBean")
@ViewScoped
public final class EventLogBean extends AbstractEventLoggingBackingBean
{
    private static final Logger LOG = Logger.getLogger(EventLogBean.class.getName());

    private static final Long CUTT_OFF_DAYS = 30L;

    private EventResult theSelectedRow;

    private final LazyDataModel<EventResult> theModel;

    /**
     * Default Constructor.
     */
    public EventLogBean()
    {
        theModel = new PaginatingModel();
    }

    public LazyDataModel<EventResult> getModel()
    {
        return theModel;
    }

    /**
     * Adds a log Entry.
     *
     * @param anEntry the event result
     */
    public static void addLog(EventResult anEntry)
    {
        try (GenericDAO myDAO = new GenericDAO())
        {
            myDAO.save(anEntry);
            Date myCutOffDate = Date.from(
                    CurrentDateTime.getCurrentDate().toInstant().minus(CUTT_OFF_DAYS, ChronoUnit.DAYS));

            Query myQuery = myDAO.getQuery("delete EventResult where theRunTimestamp < :date");
            myQuery.setParameter("date", myCutOffDate);
            myQuery.executeUpdate();
        }
    }

    public EventResult getSelectedRow()
    {
        return theSelectedRow;
    }

    public void setSelectedRow(EventResult aSelectedRow)
    {
        this.theSelectedRow = aSelectedRow;
    }

    /**
     * Paginating model for EventResult objects.
     */
    public class PaginatingModel extends LazyDataModel<EventResult>
    {

        private List<EventResult> myRowData;

        /**
         * Default Constructor.
         */
        public PaginatingModel()
        {

        }

        @Override
        public String getRowKey(EventResult object)
        {
            if (object == null)
            {
                return null;
            }
            return object.getId().toString();
        }

        /**
         * Gets the row data.
         * @param rowKey
         * @return the row data
         */
        public EventResult getRowData(String rowKey)
        {
            for (EventResult myEventResult : myRowData)
            {
                if (Integer.valueOf(rowKey).equals(myEventResult.getId()))
                {
                    return myEventResult;
                }
            }

            return null;
        }


        /**
         * Gets a row count.
         * @param map
         * @return the row count
         */
        public int count(Map<String, FilterMeta> map)
        {
            try (GenericDAO myDAO = new GenericDAO())
            {
                return myDAO.countAll(EventResult.class).intValue();
            }
        }

        @Override
        public List<EventResult> load(int aFirstRow,
                                      int aPageSize,
                                      Map<String, SortMeta> map,
                                      Map<String, FilterMeta> map1)
        {
            try (GenericDAO myDAO = new GenericDAO())
            {
                setRowCount(myDAO.countAll(EventResult.class).intValue());
                myRowData = myDAO.list(
                        EventResult.class,
                        aFirstRow,
                        aPageSize,
                        "theRunTimestamp",
                        GenericDAO.ORDERBY.DESC);
                return myRowData;
            }
        }

    }
}
