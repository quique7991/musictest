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
import java.nio.ByteBuffer;
import java.util.*;

import org.onap.music.datastore.Condition;
import org.onap.music.datastore.MusicDataStoreHandle;
import org.onap.music.datastore.PreparedQueryObject;
import org.onap.music.exceptions.MusicLockingException;
import org.onap.music.exceptions.MusicQueryException;
import org.onap.music.exceptions.MusicServiceException;
import org.onap.music.main.MusicCore;
import org.onap.music.main.MusicUtil;
import org.onap.music.main.ResultType;
import org.onap.music.main.ReturnType;

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

    TestUtils(int replicationFactor, Boolean useCassandra, int parallelSessions, Boolean useTracing) throws MusicServiceException {
        USE_CASSANDRA=useCassandra;
        USE_TRACING=useTracing;
        REPLICATION_FACTOR=replicationFactor;
        if(USE_CASSANDRA) {
            clusters = new ArrayList<>();
            sessions = new ArrayList<>();
            //Use this just to populate the required field for cassandra
            MusicDataStoreHandle.getDSHandle();
            for(int sessionId=0;sessionId<parallelSessions;sessionId++) {
                SocketOptions options = new SocketOptions();
                options.setReadTimeoutMillis(3000000).setConnectTimeoutMillis(3000000);
                options.setTcpNoDelay(true);
                Cluster cluster = Cluster.builder().withPort(9042)
                    .addContactPoint(MusicUtil.getMyCassaHost())
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

    private PreparedQueryObject createKeyspaceQuery(String keyspace){
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

        PreparedQueryObject queryObject = new PreparedQueryObject();
        queryObject.appendQueryString(
            "CREATE KEYSPACE IF NOT EXISTS " + keyspace +
                " WITH REPLICATION = " + replicationInfo.toString().replaceAll("=", ":"));
        return queryObject;
    }

    public void createKeyspace(String keyspace) throws RuntimeException {
        PreparedQueryObject queryObject = createKeyspaceQuery(keyspace);
        if(USE_CASSANDRA){
            executeCassandraWriteQuery(queryObject, 0);
        }
        else {
            try {
                MusicCore.nonKeyRelatedPut(queryObject, "eventual");
            } catch (MusicServiceException e) {
                if (!e.getMessage().equals("Keyspace " + keyspace + " already exists")) {
                    throw new RuntimeException(
                        "Error creating namespace: " + keyspace + ". Internal error:" + e.getErrorMessage(),
                        e);
                }
            }
        }
    }

    private PreparedQueryObject createBaselineQuery(int version){
        if(version>2 || version < 0){
            System.err.println("Invalid baseline query");
            System.exit(1);
        }
        String keyspace = (version==0)?BASELINE_KEYSPACE1:BASELINE_KEYSPACE2;
        String table = (version==0)?BASELINE_TABLE1:BASELINE_TABLE2;
        final UUID uuid = generateTimebasedUniqueKey();
        final int counter = 0;
        PreparedQueryObject query = new PreparedQueryObject();
        String cql = String.format("INSERT INTO %s.%s (id,counter) VALUES (?,?);",keyspace,
            table);
        query.appendQueryString(cql);
        query.addValue(uuid);
        query.addValue(counter);
        return query;
    }

    public void hardcodedBaselineQuery(int version, int index){
        PreparedQueryObject query = createBaselineQuery(version);
        //\TODO check if I am not shooting on my own foot
        executeQuorumQuery(index, query);
    }

    private void executeQuorumQuery(int index, PreparedQueryObject query) {
        if(USE_CASSANDRA) {
            executeCassandraWriteQuery(query,index);
        }
        else {
            try {
                MusicCore.nonKeyRelatedPut(query, "critical");
            } catch (MusicServiceException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private PreparedQueryObject createAddTransactionQuery(int size){
        final UUID uuid = generateTimebasedUniqueKey();
        ByteBuffer serializedTransactionDigest = ByteBuffer.allocate(size);
        for(int i=0;i<size;i++){
            serializedTransactionDigest.put((byte)i);
        }
        PreparedQueryObject query = new PreparedQueryObject();
        String cql = String.format("INSERT INTO %s.%s (txid,transactiondigest,compressed ) VALUES (?,?,?);",KEYSPACE1,
            MTD_TABLE_NAME);
        query.appendQueryString(cql);
        query.addValue(uuid);
        query.addValue(serializedTransactionDigest);
        query.addValue(false);
        return query;
    }

    public void hardcodedAddtransaction(int size, int index){
        PreparedQueryObject query = createAddTransactionQuery(size);
        //\TODO check if I am not shooting on my own foot
        executeQuorumQuery(index, query);
    }

    private PreparedQueryObject createAppendToRedoQuery(MriRow row){
        final UUID uuid = generateTimebasedUniqueKey();
        PreparedQueryObject query = new PreparedQueryObject();
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
        query.appendQueryString(appendBuilder.toString());
        return query;
    }

    public void hardcodedBatchQuery(MriRow row, int txSize, int index){
        PreparedQueryObject redoQuery = createAppendToRedoQuery(row);
        SimpleStatement redoStatement = new SimpleStatement(redoQuery.getQuery(), redoQuery.getValues().toArray());
        PreparedQueryObject txDigestQuery = createAddTransactionQuery(txSize);
        SimpleStatement txStatement = new SimpleStatement(txDigestQuery.getQuery(), txDigestQuery.getValues().toArray());
        BatchStatement batchStatement = new BatchStatement();
        batchStatement.add(redoStatement);
        batchStatement.add(txStatement);
        executeCassandraBatchWriteQuery(batchStatement,index);
    }

    public void hardcodedAppendToRedo(MriRow row, Boolean useCritical, int index) {
        PreparedQueryObject query = createAppendToRedoQuery(row);
        if (USE_CASSANDRA){
            executeCassandraWriteQuery(query,index);
        }
        else {
            if (useCritical) {
                ReturnType returnType = MusicCore.criticalPut(KEYSPACE2, MRI_TABLE_NAME, row.mriId.toString(),
                    query, row.lockId, null);
                if (returnType.getResult().compareTo(ResultType.SUCCESS) != 0) {
                    System.exit(1);
                }
            } else {
                try {
                    ResultType critical = MusicCore.nonKeyRelatedPut(query, "critical");
                    if (critical.compareTo(ResultType.SUCCESS) != 0) {
                        System.exit(1);
                    }
                } catch (MusicServiceException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }

    private void createTable(String cql, String keyspace, String table) {
        if (USE_CASSANDRA){
            executeCassandraTableQuery(cql,0);
        }
        else {
            try {
                executeMusicTableQuery(keyspace, table, cql);
            } catch (RuntimeException e) {
                System.err.println("Initialization error: Failure to create redo records table");
                throw (e);
            }
        }
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
        if(!USE_CASSANDRA) {
            String fullyQualifiedMriKey = KEYSPACE2 + "." + MRI_TABLE_NAME + "." + mriIndex.toString();
            int counter = 0;
            do {
                lockId = createAndAssignLock(fullyQualifiedMriKey);
                //TODO: fix this retry logic
            } while ((lockId == null || lockId.isEmpty()) && (counter++ < 3));
            if (lockId == null || lockId.isEmpty()) {
                throw new RuntimeException(
                    "Error initializing music range information, error creating a lock for a new row" +
                        "for key " + fullyQualifiedMriKey);
            }
        }
        createEmptyMriRow(KEYSPACE2,MRI_TABLE_NAME,mriIndex,"somethingHardcoded", lockId, tables,true);
        return lockId;
    }

    public PreparedQueryObject createEmptyMriRowQuery(UUID id, List<String> tables, String lockId, Boolean isLatest,
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
        PreparedQueryObject query = new PreparedQueryObject();
        query.appendQueryString(insert.toString());
        return query;
    }

    public UUID createEmptyMriRow(String musicNamespace, String mriTableName, UUID id, String processId,
        String lockId, List<String> tables, boolean isLatest)
        throws RuntimeException {
        PreparedQueryObject query = createEmptyMriRowQuery(id,tables,lockId,isLatest,processId);
        if(!USE_CASSANDRA) {
            try {
                executeMusicLockedPut(musicNamespace, mriTableName, id.toString(), query, lockId, null);
            } catch (RuntimeException e) {
                throw new RuntimeException("Initialization error:Failure to add new row to transaction information", e);
            }
        }
        else{
           executeCassandraWriteQuery(query,0);
        }
        return id;
    }

    protected String createAndAssignLock(String fullyQualifiedKey) throws RuntimeException{
        String lockId;
        lockId = MusicCore.createLockReference(fullyQualifiedKey);
        if(lockId==null) {
            throw new RuntimeException("lock reference is null");
        }
        ReturnType lockReturn;
        int counter=0;
        do {
            if(counter > 0){
                //TODO: Improve backoff
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    System.out.println("Error sleeping for acquiring the lock");
                }
                System.out.println("Error acquiring lock id: ["+lockId+"] for key: ["+fullyQualifiedKey+"]");
            }
            lockReturn = acquireLock(fullyQualifiedKey,lockId);
        }while((lockReturn == null||lockReturn.getResult().compareTo(ResultType.SUCCESS) != 0 )&&(counter++<3));
        if(lockReturn.getResult().compareTo(ResultType.SUCCESS) != 0 ) {
            System.err.println("Lock acquire returned invalid error: "+lockReturn.getResult().name());
            return null;
        }
        return lockId;
    }

    private void executeMusicLockedPut(String namespace, String tableName,
        String primaryKeyWithoutDomain, PreparedQueryObject queryObject, String lockId,
        Condition conditionInfo) throws RuntimeException{
        ReturnType rt ;
        if(lockId==null) {
            try {
                rt = MusicCore.atomicPut(namespace, tableName, primaryKeyWithoutDomain, queryObject, conditionInfo);
            } catch (MusicLockingException|MusicServiceException|MusicQueryException e) {
                System.err.println("Something bad happen performing a locked put");
                throw new RuntimeException("Music locked put failed", e);
            }
        }
        else {
            rt = MusicCore.criticalPut(namespace, tableName, primaryKeyWithoutDomain, queryObject, lockId, conditionInfo);
        }
        if (rt.getResult().getResult().toLowerCase().equals("failure")) {
            throw new RuntimeException("Music locked put failed");
        }
    }

    private ReturnType acquireLock(String fullyQualifiedKey, String lockId) throws RuntimeException{
        ReturnType lockReturn;
        //\TODO Handle better failures to acquire locks
        try {
            lockReturn = MusicCore.acquireLock(fullyQualifiedKey,lockId);
        } catch (MusicLockingException|MusicServiceException|MusicQueryException e) {
            System.err.println("Lock was not acquire correctly for key "+fullyQualifiedKey);
            throw new RuntimeException("Lock was not acquire correctly for key "+fullyQualifiedKey, e);
        }
        return lockReturn;
    }

    private void executeMusicTableQuery(String keyspace, String table, String cql)
        throws RuntimeException {
        PreparedQueryObject pQueryObject = new PreparedQueryObject();
        pQueryObject.appendQueryString(cql);
        ResultType rt = null;
        try {
            rt = MusicCore.createTable(keyspace,table,pQueryObject,"critical");
        } catch (MusicServiceException e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating table",e );
        }
        String result = rt.getResult();
        if (result==null || result.toLowerCase().equals("failure")) {
            throw new RuntimeException("Music eventual put failed");
        }
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

    private void executeCassandraWriteQuery(PreparedQueryObject queryObject, int sessionIndex) {
        SimpleStatement statement;
        statement = new SimpleStatement(queryObject.getQuery(), queryObject.getValues().toArray());
        statement.setConsistencyLevel(ConsistencyLevel.QUORUM);
        if(USE_TRACING){
            statement.enableTracing();
        }
        final ResultSet rs = sessions.get(sessionIndex).execute(statement);
        if (!rs.wasApplied()) {
            System.err.println("Error executing query " + queryObject.getQuery());
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
                    "[%s]  %d - %s - %s - Timestamp [%d]%n",
                    event.getThreadName(),event.getSourceElapsedMicros(), event.getSource(), event.getDescription(),event.getTimestamp());
            }
            System.out.println("------------------------------------------------------------");
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("ERROR-------------------------------------------------------");
            System.err.println("Error print tracing information");
        }
    }
    private static long Percentile(List<Long> latencies, double Percentile) {
        int Index = (int)Math.ceil((Percentile / (double)100) * (double)latencies.size());
        return latencies.get(Index-1);
    }

    public static void printResults(List<Long> values,Map<String,Queue<Long>> results){
        Collections.sort(values);
        final LongSummaryStatistics longSummaryStatistics = values.stream().mapToLong((x) -> x).summaryStatistics();
        System.out.println("Operation\t\t\tMin\t\t\tAvg\t\t\tMax\t\t\tP10\t\t\tP25\t\t\tP50\t\t\tP75\t\t\tP90\t\t\tP99");
        printStats("Total",longSummaryStatistics,values);
        for(Map.Entry<String,Queue<Long>> e:results.entrySet()){
            List<Long> valueList = new ArrayList(e.getValue());
            Collections.sort(valueList);
            LongSummaryStatistics longSummaryStatisticsTemp = e.getValue().stream().mapToLong((x) -> x).summaryStatistics();
            printStats(e.getKey(),longSummaryStatisticsTemp,valueList);
        }
    }

    private static void printStats(String operation, LongSummaryStatistics statistics, List<Long> sortedList) {
        System.out.print(operation);
        System.out.print("\t\t\t");
        System.out.print(statistics.getMin() + "ms");
        System.out.print("\t\t\t");
        System.out.print(statistics.getAverage() + "ms");
        System.out.print("\t\t\t");
        System.out.print(statistics.getMax() + "ms");
        System.out.print("\t\t\t");
        System.out.print(Percentile(sortedList,10.0) + "ms");
        System.out.print("\t\t\t");
        System.out.print(Percentile(sortedList,25.0) + "ms");
        System.out.print("\t\t\t");
        System.out.print(Percentile(sortedList,50.0) + "ms");
        System.out.print("\t\t\t");
        System.out.print(Percentile(sortedList,75.0) + "ms");
        System.out.print("\t\t\t");
        System.out.print(Percentile(sortedList,90.0) + "ms");
        System.out.print("\t\t\t");
        System.out.print(Percentile(sortedList,99.0) + "ms");
        System.out.print("\n");
        System.out.print("\n");
    }
}
