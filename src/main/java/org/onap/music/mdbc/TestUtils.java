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

import com.datastax.driver.core.utils.UUIDs;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.onap.music.datastore.Condition;
import org.onap.music.datastore.PreparedQueryObject;
import org.onap.music.exceptions.MusicLockingException;
import org.onap.music.exceptions.MusicQueryException;
import org.onap.music.exceptions.MusicServiceException;
import org.onap.music.main.MusicCore;
import org.onap.music.main.ResultType;
import org.onap.music.main.ReturnType;

public class TestUtils {
    final String KEYSPACE = "metrictest";
    final String INTERNAL_KEYSPACE = "music_internal";
    final String MRI_TABLE_NAME = "musicrangeinformation";
    final String MTD_TABLE_NAME = "musictransactiondigest";
    final int REPLICATION_FACTOR;

    TestUtils(int replicationFactor){
        REPLICATION_FACTOR=replicationFactor;
        createKeyspace(KEYSPACE);
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

    public void createKeyspace(String keyspace) throws RuntimeException {
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

        try {
            MusicCore.nonKeyRelatedPut(queryObject, "eventual");
        } catch (MusicServiceException e) {
            if (!e.getMessage().equals("Keyspace "+keyspace+" already exists")) {
                throw new RuntimeException("Error creating namespace: "+keyspace+". Internal error:"+e.getErrorMessage(),
                    e);
            }
        }
    }

    public void hardcodedAddtransaction(int size){
        final UUID uuid = generateTimebasedUniqueKey();
        ByteBuffer serializedTransactionDigest = ByteBuffer.allocate(size);
        for(int i=0;i<size;i++){
            serializedTransactionDigest.put((byte)i);
        }
        PreparedQueryObject query = new PreparedQueryObject();
        String cql = String.format("INSERT INTO %s.%s (txid,transactiondigest,compressed ) VALUES (?,?,?);",KEYSPACE,
            MTD_TABLE_NAME);
        query.appendQueryString(cql);
        query.addValue(uuid);
        query.addValue(serializedTransactionDigest);
        query.addValue(false);
        //\TODO check if I am not shooting on my own foot
        try {
            MusicCore.nonKeyRelatedPut(query,"critical");
        } catch (MusicServiceException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void hardcodedAppendToRedo(MriRow row){
        final UUID uuid = generateTimebasedUniqueKey();
        PreparedQueryObject query = new PreparedQueryObject();
        StringBuilder appendBuilder = new StringBuilder();
        appendBuilder.append("UPDATE ")
            .append(KEYSPACE)
            .append(".")
            .append(MRI_TABLE_NAME)
            .append(" SET txredolog = txredolog +[('")
            .append(MTD_TABLE_NAME)
            .append("',")
            .append(uuid)
            .append(")] WHERE rangeid = ")
            .append(row.mriId)
            .append(";");
        query.appendQueryString(appendBuilder.toString());
        ReturnType returnType = MusicCore.criticalPut(KEYSPACE, MRI_TABLE_NAME, row.mriId.toString(),
            query, row.lockId, null);
        //returnType.getExecutionInfo()
        if (returnType.getResult().compareTo(ResultType.SUCCESS) != 0) {
            System.exit(1);
        }
    }

    public void createMusicTxDigest()
        throws RuntimeException {
        String priKey = "txid";
        StringBuilder fields = new StringBuilder();
        fields.append("txid uuid, ");
        fields.append("compressed boolean, ");
        fields.append("transactiondigest blob ");//notice lack of ','
        String cql = String.format("CREATE TABLE IF NOT EXISTS %s.%s (%s, PRIMARY KEY (%s));", KEYSPACE,MTD_TABLE_NAME,
            fields, priKey);
        try {
            executeMusicWriteQuery(KEYSPACE,MTD_TABLE_NAME,cql);
        } catch (RuntimeException e) {
            System.err.println("Initialization error: Failure to create redo records table");
            throw(e);
        }
    }

    public void createMusicRangeInformationTable() throws RuntimeException {
        String priKey = "rangeid";
        StringBuilder fields = new StringBuilder();
        fields.append("rangeid uuid, ");
        fields.append("keys set<text>, ");
        fields.append("ownerid text, ");
        fields.append("islatest boolean, ");
        fields.append("metricprocessid text, ");
        //TODO: Frozen is only needed for old versions of cassandra, please update correspondingly
        fields.append("txredolog list<frozen<tuple<text,uuid>>> ");
        String cql = String.format("CREATE TABLE IF NOT EXISTS %s.%s (%s, PRIMARY KEY (%s));",
            KEYSPACE, MRI_TABLE_NAME, fields, priKey);
        try {
            executeMusicWriteQuery(KEYSPACE,MRI_TABLE_NAME,cql);
        } catch (RuntimeException e) {
            System.err.println("Initialization error: Failure to create transaction information table");
            throw(e);
        }
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
        String fullyQualifiedMriKey = KEYSPACE+"."+ MRI_TABLE_NAME+"."+mriIndex.toString();
        String lockId;
        int counter=0;
        do {
            lockId = createAndAssignLock(fullyQualifiedMriKey);
            //TODO: fix this retry logic
        }while((lockId ==null||lockId.isEmpty())&&(counter++<3));
        if(lockId == null || lockId.isEmpty()){
            throw new RuntimeException("Error initializing music range information, error creating a lock for a new row" +
                "for key "+fullyQualifiedMriKey) ;
        }
        createEmptyMriRow(KEYSPACE,MRI_TABLE_NAME,mriIndex,"somethingHardcoded", lockId, tables,true);
        return lockId;
    }

    public UUID createEmptyMriRow(String musicNamespace, String mriTableName, UUID id, String processId,
        String lockId, List<String> tables, boolean isLatest)
        throws RuntimeException {
        StringBuilder insert = new StringBuilder("INSERT INTO ")
            .append(musicNamespace)
            .append('.')
            .append(mriTableName)
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
            .append("',[]);");
        PreparedQueryObject query = new PreparedQueryObject();
        query.appendQueryString(insert.toString());
        try {
            executeMusicLockedPut(musicNamespace,mriTableName,id.toString(),query,lockId,null);
        } catch (RuntimeException e) {
            throw new RuntimeException("Initialization error:Failure to add new row to transaction information", e);
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

    private void executeMusicWriteQuery(String keyspace, String table, String cql)
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
}
