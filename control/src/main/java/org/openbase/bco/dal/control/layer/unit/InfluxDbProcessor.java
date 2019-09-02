package org.openbase.bco.dal.control.layer.unit;

/*-
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.influxdata.client.InfluxDBClient;
import org.influxdata.client.InfluxDBClientFactory;
import org.influxdata.client.QueryApi;
import org.influxdata.query.FluxRecord;
import org.influxdata.query.FluxTable;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.configuration.EntryType;
import org.openbase.type.domotic.database.QueryType;
import org.openbase.type.domotic.database.RecordCollectionType;
import org.openbase.type.domotic.database.RecordType;
import org.openbase.type.domotic.service.ServiceTemplateType;
import org.openbase.type.domotic.state.AggregatedServiceStateType;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.openbase.type.timing.TimestampType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public class InfluxDbProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(InfluxDbProcessor.class);

    private static final String INFLUXDB_BUCKET_DEFAULT = "bco-persistence";
    private static final String INFLUXDB_BUCKET = "INFLUXDB_BUCKET";
    private static final String INFLUXDB_BATCH_TIME = "INFLUXDB_BATCH_TIME";
    private static final String INFLUXDB_BATCH_TIME_DEFAULT = "1000";
    private static final String INFLUXDB_BATCH_LIMIT = "INFLUXDB_BATCH_LIMIT";
    private static final String INFLUXDB_BATCH_LIMIT_DEFAULT = "100";
    private static final String INFLUXDB_URL = "INFLUXDB_URL";
    private static final String INFLUXDB_ORG = "INFLUXDB_ORG";
    private static final String INFLUXDB_ORG_DEFAULT = "openbase";
    private static final String INFLUXDB_TOKEN = "INFLUXDB_TOKEN";
    private static final String INFLUXDB_ORG_ID = "INFLUXDB_ORG_ID";
    private static final long READ_TIMEOUT = 60;
    private static final long WRITE_TIMEOUT = 60;
    private static final long CONNECT_TIMOUT = 40;
    private static String INFLUXDB_APP_CLASS_ID = "e6d9a242-58de-4e44-8e56-64c8da560fe4";
    private static MetaConfigPool metaConfigPool = new MetaConfigPool();

    static {
        try {
            Registries.getUnitRegistry().addDataObserver((source, data) -> {
                updateConfiguration();
            });
        } catch (NotAvailableException ex) {
            LOGGER.error("Unit Registry not available", ex);
        }
    }

    private static void updateConfiguration() {
        try {
            List<UnitConfigType.UnitConfig> influxdbConnectorApps = Registries.getUnitRegistry().getAppUnitConfigsByAppClassId(INFLUXDB_APP_CLASS_ID);
            if (influxdbConnectorApps.size() > 1) {
                LOGGER.warn("More than one influxdbConnectorApp found!");
            }
            for (UnitConfigType.UnitConfig influxdbConnectorApp : influxdbConnectorApps) {
                metaConfigPool.register(new MetaConfigVariableProvider(LabelProcessor.getBestMatch(influxdbConnectorApp.getLabel()), influxdbConnectorApp.getMetaConfig()));
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not update configuration!", ex, LOGGER);
        } catch (InterruptedException ex) {
            ExceptionPrinter.printHistory("Update configuration interrupted!", ex, LOGGER);

        }

    }

    public static String getInfluxdbUrl() throws NotAvailableException {
        try {
            return metaConfigPool.getValue(INFLUXDB_URL);
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("Influxddb_url", ex);
        }
    }

    public static String getInfluxdbBucket() {
        return metaConfigPool.getValue(INFLUXDB_BUCKET, INFLUXDB_BUCKET_DEFAULT);
    }

    public static String getInfluxdbBatchTime() {
        return metaConfigPool.getValue(INFLUXDB_BATCH_TIME, INFLUXDB_BATCH_TIME_DEFAULT);
    }

    public static String getInfluxdbBatchLimit() {
        return metaConfigPool.getValue(INFLUXDB_BATCH_LIMIT, INFLUXDB_BATCH_LIMIT_DEFAULT);
    }

    public static String getInfluxdbOrg() {
        return metaConfigPool.getValue(INFLUXDB_ORG, INFLUXDB_ORG_DEFAULT);
    }

    public static String getInfluxdbToken() throws NotAvailableException {
        try {
            return metaConfigPool.getValue(INFLUXDB_TOKEN);
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("Influxdb_Token", ex);
        }
    }

    public static String getInfluxdbOrgId() throws NotAvailableException {
        try {
            return metaConfigPool.getValue(INFLUXDB_ORG_ID);
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("Influxdb_org_id", ex);
        }
    }

    /**
     * Creates a connection to the influxdb and sends a query.
     *
     * @param query Query to send
     * @return List of FluxTables
     * @throws CouldNotPerformException
     */
    private static List<FluxTable> sendQuery(final String query) throws CouldNotPerformException {

        try (
                InfluxDBClient influxDBClient = InfluxDBClientFactory
                        .create(getInfluxdbUrl() + "?readTimeout=" + READ_TIMEOUT + "&connectTimeout=" + CONNECT_TIMOUT + "&writeTimeout=" + WRITE_TIMEOUT + "&logLevel=BASIC", getInfluxdbToken().toCharArray())) {

            if (!influxDBClient.health().getStatus().getValue().equals("pass")) {
                throw new CouldNotPerformException("Could not connect to database server at " + getInfluxdbUrl() + "!");

            }
            QueryApi queryApi = influxDBClient.getQueryApi();

            return queryApi.query(query, getInfluxdbOrgId());

        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not send query to database!", ex);

        }
    }

    /**
     * Builds a  flux query string for aggregating values.
     *
     * @param databaseQuery The databaseQuery Object which is used to build the Flux query.
     * @param isEnum        If the aggregated fields consists of enum values.
     * @return
     */
    private static String buildGetAggregatedQuery(final QueryType.Query databaseQuery, boolean isEnum) {
        String measurement = databaseQuery.getMeasurement();
        String timeStart = String.valueOf(databaseQuery.getTimeRangeStart().getTime());
        String timeStop = String.valueOf(databaseQuery.getTimeRangeStop().getTime());
        List<EntryType.Entry> filterList = databaseQuery.getFilterList();

        String query = "from(bucket: \"" + getInfluxdbBucket() + "\")" +
                " |> range(start: " + timeStart + ", stop: " + timeStop + ")" +
                " |> filter(fn: (r) => r._measurement == \"" + measurement + "\")";

        for (EntryType.Entry entry : filterList) {
            query = addFilterToQuery(query, entry);

        }

        if (isEnum) {

            query += " |> group(columns: [\"_value\"])" +
                    " |> map(fn: (r) => ({_time: r._time, index: 1}))" +
                    "|> cumulativeSum(columns: [\"index\"])" +
                    "|> last()";

            return query;
        } else {

            String window = databaseQuery.getAggregatedWindow();
            // add filters
            query += "|> group(columns: [\"_field\"], mode:\"by\")" +
                    " |> aggregateWindow(every:" + window + " , fn: mean)" +
                    " |> mean(column: \"_value\")";

            return query;
        }
    }

    /**
     * Add a filter to a flux query string.
     *
     * @param query  The flux query string.
     * @param filter Entry object with key and value
     * @return
     */
    private static String addFilterToQuery(String query, EntryType.Entry filter) {
        String field = filter.getKey();
        String value = filter.getValue();

        String filterString = " |> filter(fn: (r) => r." + field + " == \"" + value + "\")";

        query += filterString;
        return query;
    }

    /**
     * Calculates the percentages of the different (enum) values in a List of FluxTables
     *
     * @param tables List of FluxTables with the occurrence ( as index key) of the enum values (as _value)
     * @return Map with the Enum Value Index as Key and the percentage (between 0-1) as value.
     */
    private static Map<Integer, Double> calculateEnumStatePercentage(List<FluxTable> tables) {
        Map<Integer, Double> percentages = new HashMap<>();
        double sum = 0;

        for (FluxTable fluxTable : tables) {
            List<FluxRecord> records = fluxTable.getRecords();
            for (FluxRecord fluxRecord : records) {
                percentages.put((int) Double.parseDouble(fluxRecord.getValueByKey("_value").toString()), Double.parseDouble(fluxRecord.getValueByKey("index").toString()));
                sum += Double.parseDouble(fluxRecord.getValueByKey("index").toString());
            }
        }
        for (Map.Entry<Integer, Double> entry : percentages.entrySet()) {
            entry.setValue(entry.getValue() / sum);
        }

        return percentages;
    }

    public static Future<RecordCollectionType.RecordCollection> queryRecord(final QueryType.Query databaseQuery) {
        try {

            List<FluxTable> fluxTableList = sendQuery(databaseQuery.getRawQuery());

            return FutureProcessor.completedFuture(convertFluxTablesToRecordCollections(fluxTableList));

        } catch (CouldNotPerformException ex) {

            ex.printStackTrace();

            return FutureProcessor.canceledFuture((new CouldNotPerformException("Could not query Record", ex)));
        }
    }

    private static RecordType.Record convertFluxRecordToProtoRecord(FluxRecord record) {
        RecordType.Record.Builder builder = RecordType.Record.newBuilder();

        if (record.getTime() != null) {
            builder.setTimestamp(TimestampType.Timestamp.newBuilder().setTime(record.getTime().getEpochSecond()).build());
        }
        if (record.getStart() != null) {
            builder.setTimeRangeStart(TimestampType.Timestamp.newBuilder().setTime(record.getStart().getEpochSecond()).build());
        }
        if (record.getStop() != null) {
            builder.setTimeRangeStop(TimestampType.Timestamp.newBuilder().setTime(record.getStop().getEpochSecond()).build());
        }
        if (record.getMeasurement() != null) {
            builder.setMeasurement(record.getMeasurement());
        }
        if (record.getField() != null) {
            builder.setField(record.getField());
        }
        if (record.getValue() != null) {
            builder.setValue(Double.valueOf(record.getValue().toString()));
        }

        builder.setTable(record.getTable());

        for (Map.Entry<String, Object> entry : record.getValues().entrySet()) {
            builder.addEntryBuilder().setKey(entry.getKey()).setValue(entry.getValue().toString());
        }

        return builder.build();
    }

    private static RecordCollectionType.RecordCollection convertFluxTablesToRecordCollections(List<FluxTable> tables) {
        RecordCollectionType.RecordCollection.Builder builder = RecordCollectionType.RecordCollection.newBuilder();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                builder.addRecord(convertFluxRecordToProtoRecord(record));
            }
        }
        return builder.build();
    }

    /**
     * Build a Map of String,Double pairs out of FluxTables for easy filtering.
     *
     * @param tables List of FluxTables.
     * @return Map<String, Double> with the fields as key and the values as value.
     */
    private static Map<String, Double> aggregatedFluxTablesToMap(List<FluxTable> tables) {
        Map<String, Double> aggregatedValues = new HashMap<>();
        for (FluxTable fluxTable : tables) {
            List<FluxRecord> records = fluxTable.getRecords();
            for (FluxRecord fluxRecord : records) {
                aggregatedValues.put(fluxRecord.getField(), (double) fluxRecord.getValueByKey("_value"));
            }
        }
        return aggregatedValues;
    }

    /**
     * Get the aggregated value coverage of a service state.
     *
     * @param databaseQuery DatabaseQuery which will be used to build a query which will be send to the influxdb.
     * @return AggregatedServiceState  with the aggregated value coverage, the databaseQuery the service_type and the unit_id.
     */
    public static Future<AggregatedServiceStateType.AggregatedServiceState> queryAggregatedServiceState(final QueryType.Query databaseQuery) {
        try {
            ServiceTemplateType.ServiceTemplate.ServiceType serviceType = databaseQuery.getServiceType();
            Message.Builder serviceStateBuilder = Services.generateServiceStateBuilder(serviceType);
            AggregatedServiceStateType.AggregatedServiceState.Builder builder = AggregatedServiceStateType.AggregatedServiceState.newBuilder();
            builder.setServiceType(serviceType);
            builder.setQuery(databaseQuery);

            String query = null;
            Map<String, Double> aggregatedValues = new HashMap<>();
            Map<Integer, Double> aggregatedEnumValues;
            List<FluxTable> fluxTableList;

            for (Descriptors.FieldDescriptor fieldDescriptor : serviceStateBuilder.getDescriptorForType().getFields()) {

                if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.ENUM) {
                    if (query == null) {
                        query = buildGetAggregatedQuery(databaseQuery, true);
                        fluxTableList = sendQuery(query);
                        aggregatedEnumValues = calculateEnumStatePercentage(fluxTableList);
                        Descriptors.FieldDescriptor aggregatedValueCoverageField = ProtoBufFieldProcessor.getFieldDescriptor(serviceStateBuilder, "aggregated_value_coverage");
                        for (Map.Entry<Integer, Double> entry : aggregatedEnumValues.entrySet()) {
                            Message.Builder aggregatedValueBuilder = serviceStateBuilder.newBuilderForField(aggregatedValueCoverageField);
                            Descriptors.FieldDescriptor key = aggregatedValueBuilder.getDescriptorForType().findFieldByName("key");
                            Descriptors.FieldDescriptor coverage = aggregatedValueBuilder.getDescriptorForType().findFieldByName("coverage");
                            aggregatedValueBuilder.setField(coverage, entry.getValue());
                            aggregatedValueBuilder.setField(key, key.getEnumType().getValues().get(entry.getKey()));
                            serviceStateBuilder.addRepeatedField(aggregatedValueCoverageField, aggregatedValueBuilder.build());
                        }
                    }

                } else if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.DOUBLE) {
                    if (query == null) {
                        query = buildGetAggregatedQuery(databaseQuery, false);
                        fluxTableList = sendQuery(query);
                        aggregatedValues = aggregatedFluxTablesToMap(fluxTableList);
                    }
                    if (aggregatedValues.containsKey(fieldDescriptor.getName())) {
                        serviceStateBuilder.setField(fieldDescriptor, aggregatedValues.get(fieldDescriptor.getName()));
                    }
                }
            }
            Services.invokeOperationServiceMethod(serviceType, builder, serviceStateBuilder.build());
            AggregatedServiceStateType.AggregatedServiceState newAggregatedServiceState = builder.build();

            return FutureProcessor.completedFuture(newAggregatedServiceState);
        } catch (CouldNotPerformException ex) {

            ex.printStackTrace();

            return FutureProcessor.canceledFuture((new CouldNotPerformException("Could not query aggregated service state", ex)));
        }
    }

}