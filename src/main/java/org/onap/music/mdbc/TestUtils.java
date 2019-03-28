/*
 * ============LICENSE_START====================================================
 * org.onap.music.mdbc
 * =============================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
 * =============================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END======================================================
 */

package org.onap.music.mdbc;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.utils.UUIDs;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;

public class TestUtils {
    final Boolean USE_CASSANDRA;
    final Boolean USE_TRACING;
    final String BASELINE_KEYSPACE1 = "baseline1";
    final String BASELINE_KEYSPACE2 = "baseline2";
    final String KEYSPACE1 = "metrictest";
    final String KEYSPACE2 = "metrictest2";
    final String INTERNAL_KEYSPACE = "music_internal";
    final String MRI_TABLE_NAME = "musicrangeinformation";
    final String MTD_TABLE_NAME = "musictransactiondigest";
    final String BASELINE_TABLE1= "simpletable1";
    final String BASELINE_TABLE2= "simpletable2";
    final int REPLICATION_FACTOR;
    List<Cluster> clusters;
    List<Session> sessions;

   Properties readPropertiesFile(){
       Properties prop = new Properties();
       InputStream input = null;
        try {
            input = new FileInputStream("music.properties");
            // load a properties file
            prop.load(input);

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return prop;
    }

    TestUtils(int replicationFactor, Boolean useCassandra, int parallelSessions, Boolean useTracing) throws Exception {
        USE_CASSANDRA=useCassandra;
        USE_TRACING=useTracing;
        REPLICATION_FACTOR=replicationFactor;
        Properties prop = readPropertiesFile();
        clusters = new ArrayList<>();
        sessions = new ArrayList<>();
        //Use this just to populate the required field for cassandra
        for(int sessionId=0;sessionId<parallelSessions;sessionId++) {
            SocketOptions options = new SocketOptions();
            options.setTcpNoDelay(true);
            Cluster cluster = Cluster.builder().withPort(9042)
                .addContactPoint(prop.getProperty("cassandra.host"))
                .withLoadBalancingPolicy(
                    DCAwareRoundRobinPolicy.builder().withLocalDc("dc1").withUsedHostsPerRemoteDc(0).build()
                )
                .withSocketOptions(options)
                .build();
                /*Cluster cluster = Cluster.builder()
                    .withPort(9042)
                    .withCredentials(MusicUtil.getCassName(), MusicUtil.getCassPwd())
                    .addContactPoint(MusicUtil.getMyCassaHost())
                    .withSocketOptions(options)
                    .build();*/

            Session session = cluster.connect();
            clusters.add(cluster);
            sessions.add(session);
        }
        createKeyspace(KEYSPACE1);
        createKeyspace(KEYSPACE2);
        createKeyspace(BASELINE_KEYSPACE1);
        createKeyspace(BASELINE_KEYSPACE2);
        createKeyspace(INTERNAL_KEYSPACE);
    }

    public class MriRow{
        public final UUID mriId;
        public final String lockId;
        public MriRow(UUID mriId, String lockId){
            this.mriId = mriId;
            this.lockId=lockId;
        }
    }

    private String createKeyspaceQuery(String keyspace){
        Map<String,Object> replicationInfo = new HashMap<>();
        replicationInfo.put("'class'", "'NetworkTopologyStrategy'");
        if(REPLICATION_FACTOR==3){
            replicationInfo.put("'dc1'", 1);
            replicationInfo.put("'dc2'", 1);
            replicationInfo.put("'dc3'", 1);
        }
        else {
            replicationInfo.put("'class'", "'SimpleStrategy'");
            replicationInfo.put("'replication_factor'", REPLICATION_FACTOR);
        }

        return  "CREATE KEYSPACE IF NOT EXISTS " + keyspace +
                " WITH REPLICATION = " + replicationInfo.toString().replaceAll("=", ":");
    }

    public void createKeyspace(String keyspace) throws RuntimeException {
        String queryObject = createKeyspaceQuery(keyspace);
        executeCassandraWriteQuery(new SimpleStatement(queryObject), 0);
    }

    private SimpleStatement createBaselineQuery(int version){
        if(version>2 || version < 0){
            System.err.println("Invalid baseline query");
            System.exit(1);
        }
        String keyspace = (version==0)?BASELINE_KEYSPACE1:BASELINE_KEYSPACE2;
        String table = (version==0)?BASELINE_TABLE1:BASELINE_TABLE2;
        final UUID uuid = generateTimebasedUniqueKey();
        final int counter = 0;


        String cql = String.format("INSERT INTO %s.%s (id,counter) VALUES (?,?);",keyspace,
            table);
        ArrayList<Object> objects = new ArrayList<>();
        objects.add(uuid);
        objects.add(counter);
        SimpleStatement statement = new SimpleStatement(cql, objects);
        return statement;
    }

    public void hardcodedBaselineQuery(int version, int index){
        SimpleStatement query = createBaselineQuery(version);
        //\TODO check if I am not shooting on my own foot
        executeQuorumQuery(index, query);
    }

    private void executeQuorumQuery(int index, SimpleStatement query) {
        executeCassandraWriteQuery(query,index);
    }

    private SimpleStatement createAddTransactionQuery(int size){
        final UUID uuid = generateTimebasedUniqueKey();
        ByteBuffer serializedTransactionDigest = ByteBuffer.allocate(size);
        for(int i=0;i<size;i++){
            serializedTransactionDigest.put((byte)i);
        }
        String cql = String.format("INSERT INTO %s.%s (txid,transactiondigest,compressed ) VALUES (?,?,?);",KEYSPACE1,
            MTD_TABLE_NAME);
        ArrayList<Object> objects = new ArrayList<>();
        objects.add(uuid);
        objects.add(serializedTransactionDigest);
        objects.add(false);
        SimpleStatement statement = new SimpleStatement(cql,objects);
        return statement;
    }

    public void hardcodedAddtransaction(int size, int index){
        SimpleStatement query = createAddTransactionQuery(size);
        //\TODO check if I am not shooting on my own foot
        executeQuorumQuery(index, query);
    }

    private SimpleStatement createAppendToRedoQuery(MriRow row){
        final UUID uuid = generateTimebasedUniqueKey();
        StringBuilder appendBuilder = new StringBuilder();
        appendBuilder.append("UPDATE ")
            .append(KEYSPACE2)
            .append(".")
            .append(MRI_TABLE_NAME)
            .append(" SET txredolog = txredolog +{")
            .append(uuid)
            .append("} WHERE rangeid = ")
            .append(row.mriId)
            .append(";");
        SimpleStatement statement = new SimpleStatement(appendBuilder.toString());
        return statement;
    }

    public void hardcodedBatchQuery(MriRow row, int txSize, int index){
        SimpleStatement redoStatement = createAppendToRedoQuery(row);
        SimpleStatement txStatement = createAddTransactionQuery(txSize);
        BatchStatement batchStatement = new BatchStatement();
        batchStatement.add(redoStatement);
        batchStatement.add(txStatement);
        executeCassandraBatchWriteQuery(batchStatement,index);
    }

    public void hardcodedAppendToRedo(MriRow row, Boolean useCritical, int index) {
        SimpleStatement query = createAppendToRedoQuery(row);
        executeCassandraWriteQuery(query,index);
    }

    private void createTable(String cql, String keyspace, String table) {
        executeCassandraTableQuery(cql,0);
    }


    private String createBaselineTableQuery(String keyspace,String table){
        String priKey = "id";
        StringBuilder fields = new StringBuilder();
        fields.append("id uuid, ");
        fields.append("counter int ");
        String cql = String.format("CREATE TABLE IF NOT EXISTS %s.%s (%s, PRIMARY KEY (%s));", keyspace,table,
            fields, priKey);
        return cql;
    }

    public void createBaselineTable(int version)
        throws RuntimeException {
        if(version>2 || version < 0){
            System.err.println("Invalid baseline query");
            System.exit(1);
        }
        String keyspace = (version==0)?BASELINE_KEYSPACE1:BASELINE_KEYSPACE2;
        String table = (version==0)?BASELINE_TABLE1:BASELINE_TABLE2;
        String cql = createBaselineTableQuery(keyspace,table);
        createTable(cql, keyspace, table);
    }



    private String createMusicTxDigestTableQuery(){
        String priKey = "txid";
        StringBuilder fields = new StringBuilder();
        fields.append("txid uuid, ");
        fields.append("compressed boolean, ");
        fields.append("transactiondigest blob ");//notice lack of ','
        String cql = String.format("CREATE TABLE IF NOT EXISTS %s.%s (%s, PRIMARY KEY (%s));", KEYSPACE1,MTD_TABLE_NAME,
            fields, priKey);
        return cql;
    }

    public void createMusicTxDigestTable()
        throws RuntimeException {
        String cql = createMusicTxDigestTableQuery();
        createTable(cql, KEYSPACE1, MTD_TABLE_NAME);
    }

    public String createMusicRangeInformationTableQuery() throws RuntimeException {
        String priKey = "rangeid";
        StringBuilder fields = new StringBuilder();
        fields.append("rangeid uuid, ");
        fields.append("keys set<text>, ");
        fields.append("ownerid text, ");
        fields.append("islatest boolean, ");
        fields.append("metricprocessid text, ");
        //TODO: Frozen is only needed for old versions of cassandra, please update correspondingly
        fields.append("txredolog set<uuid> ");
        String cql = String.format("CREATE TABLE IF NOT EXISTS %s.%s (%s, PRIMARY KEY (%s));",
            KEYSPACE2, MRI_TABLE_NAME, fields, priKey);
        return cql;
    }

    public void createMusicRangeInformationTable() throws RuntimeException {
        String cql = createMusicRangeInformationTableQuery();
        createTable(cql,KEYSPACE2,MRI_TABLE_NAME);
    }

    public UUID generateTimebasedUniqueKey() {return UUIDs.timeBased();}

    public MriRow createBasicRow(String table)
        throws RuntimeException {
        List<String> tables = new ArrayList<>();
        tables.add(table);
        final UUID uuid = generateTimebasedUniqueKey();
        String lockId =createMusicRangeInformation(uuid,tables);
        return new MriRow(uuid,lockId);
    }

    public String createMusicRangeInformation(UUID mriIndex, List<String> tables)
        throws RuntimeException {
        String lockId=null;
        createEmptyMriRow(KEYSPACE2,MRI_TABLE_NAME,mriIndex,"somethingHardcoded", lockId, tables,true);
        return lockId;
    }

    public SimpleStatement createEmptyMriRowQuery(UUID id, List<String> tables, String lockId, Boolean isLatest,
        String processId){
        StringBuilder insert = new StringBuilder("INSERT INTO ")
            .append(KEYSPACE2)
            .append('.')
            .append(MRI_TABLE_NAME)
            .append(" (rangeid,keys,ownerid,islatest,metricprocessid,txredolog) VALUES ")
            .append("(")
            .append(id)
            .append(",{");
        boolean first=true;
        for(String r: tables){
            if(first){ first=false; }
            else {
                insert.append(',');
            }
            insert.append("'").append(r).append("'");
        }
        insert.append("},'")
            .append((lockId==null)?"":lockId)
            .append("',")
            .append(isLatest)
            .append(",'")
            .append(processId)
            .append("',{});");
        return new SimpleStatement(insert.toString());
    }

    public UUID createEmptyMriRow(String musicNamespace, String mriTableName, UUID id, String processId,
        String lockId, List<String> tables, boolean isLatest)
        throws RuntimeException {
        SimpleStatement query = createEmptyMriRowQuery(id,tables,lockId,isLatest,processId);
        executeCassandraWriteQuery(query,0);
        return id;
    }

    private void executeCassandraBatchWriteQuery(BatchStatement batchQuery, int sessionIndex){
        batchQuery.setConsistencyLevel(ConsistencyLevel.QUORUM);
        if(USE_TRACING){
            batchQuery.enableTracing();
        }
        final ResultSet rs = sessions.get(sessionIndex).execute(batchQuery);
        if (!rs.wasApplied()) {
            System.err.println("Error executing query " + batchQuery.getStatements());
            System.exit(1);
        }
        else if(USE_TRACING){
            printInfo(rs);
        }
    }

    private void executeCassandraWriteQuery(SimpleStatement statement, int sessionIndex) {
        statement.setConsistencyLevel(ConsistencyLevel.QUORUM);
        if(USE_TRACING){
            statement.enableTracing();
        }
        final ResultSet rs = sessions.get(sessionIndex).execute(statement);
        if (!rs.wasApplied()) {
            System.err.println("Error executing query " + statement.getQueryString());
            System.exit(1);
        }
        else if(USE_TRACING){
            printInfo(rs);
        }
    }

    private void executeCassandraTableQuery(String cql, int sessionIndex){
        SimpleStatement statement;
        statement = new SimpleStatement(cql);
        statement.setConsistencyLevel(ConsistencyLevel.QUORUM);
        if(USE_TRACING){
            statement.enableTracing();
        }
        final ResultSet rs = sessions.get(sessionIndex).execute(statement);
        if (!rs.wasApplied() || ! rs.getExecutionInfo().isSchemaInAgreement()) {
            System.err.println("Error executing query " + cql);
            System.exit(1);
        }
        else if(USE_TRACING){
            printInfo(rs);
        }
    }

    synchronized public void printInfo(ResultSet results){
        try {
            Thread.sleep(5000);
            System.out.println("------------------------------------------------------------");
            ExecutionInfo executionInfo = results.getExecutionInfo();
            System.out.printf(
                "'%s' from %s-%s\n",
                executionInfo.getQueriedHost().getAddress(),
                executionInfo.getQueriedHost().getDatacenter(),
                executionInfo.getQueriedHost().getRack());
            final QueryTrace trace = executionInfo.getQueryTrace();
            System.out.println("Parameters for the query");
            for(Map.Entry<String,String> param: trace.getParameters().entrySet()){
               System.out.printf("\t[%s]: %s\n",param.getKey(),param.getValue());
            }
            System.out.printf(
                "'%s' to %s took %dμs - Timestamp[%d] %n",
                trace.getRequestType(), trace.getCoordinator(), trace.getDurationMicros(), trace.getStartedAt());
            for (QueryTrace.Event event : trace.getEvents()) {
                System.out.printf(
                    "  %d - %s - %s - Timestamp [%d]%n",
                    event.getSourceElapsedMicros(), event.getSource(), event.getDescription(),event.getTimestamp());
            }
            System.out.println("------------------------------------------------------------");
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("ERROR-------------------------------------------------------");
            System.err.println("Error print tracing information");
        }
    }

    public static void printResults(List<Long> values,Map<String,List<Long>> results){
        final LongSummaryStatistics longSummaryStatistics = values.stream().mapToLong((x) -> x).summaryStatistics();
        System.out.println("Operation\t\t\tMin\t\t\tAvg\t\t\tMax\t\t\t");
        printStats("Total",longSummaryStatistics);
        for(Map.Entry<String,List<Long>> e:results.entrySet()){
            LongSummaryStatistics longSummaryStatisticsTemp = e.getValue().stream().mapToLong((x) -> x).summaryStatistics();
            printStats(e.getKey(),longSummaryStatisticsTemp);
        }
    }

    private static void printStats(String operation, LongSummaryStatistics statistics) {
        System.out.print(operation);
        System.out.print("\t\t\t");
        System.out.print(statistics.getMin() + "ms");
        System.out.print("\t\t\t");
        System.out.print(statistics.getAverage() + "ms");
        System.out.print("\t\t\t");
        System.out.print(statistics.getMax() + "ms");
        System.out.print("\n");
    }
}
