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


import java.io.IOException;
import java.util.*;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.onap.music.exceptions.MusicServiceException;
import org.onap.music.mdbc.TestUtils.MriRow;

public class ParallelTest {
    final int REPLICATION_FACTOR=3;
    final private int PARALLEL_SESSIONS=2;
    Server server;

    static public Boolean USE_CRITICAL=true;
    static public Boolean PRINT=false;
    static public Boolean RUN_TX_DIGEST=true;
    static public Boolean RUN_REDO=true;
    static public Boolean USE_CASSANDRA=true;
    static public Boolean USE_TRACING=false;

    final private MriRow row;
    final private TestUtils utils;
    final public Map<String,List<Long>> results;
    final private String TX_DIGEST="DIGEST";
    final private String REDO_LOG="REDO";

    public ParallelTest(String rangeTableName) throws MusicServiceException {
        results = new HashMap<>();
        results.put(TX_DIGEST,new ArrayList<>());
        results.put(REDO_LOG,new ArrayList<>());
        utils=new TestUtils(REPLICATION_FACTOR,USE_CASSANDRA,PARALLEL_SESSIONS,USE_TRACING);
        utils.createMusicRangeInformationTable();
        utils.createMusicTxDigestTable();
        row = utils.createBasicRow(rangeTableName);
    }

    private void AddResultAndProcess(long time, String operType) {
        long nanosecondTime = System.nanoTime() - time;
        long millisecond = nanosecondTime / 1000000;
        if(USE_TRACING){
            System.out.print(millisecond);
            System.out.println(" ms");
        }
        results.get(operType).add(millisecond);
    }


    public void addTxDigest(int size,int index){
        long time = System.nanoTime();
        if(PRINT)
            System.out.println("Starting tx digest");
        utils.hardcodedAddtransaction(size,index);
        if(PRINT)
            System.out.println("Ending tx digest");
        AddResultAndProcess(time, TX_DIGEST);
    }


    public void appendToRedo(int index){
        long time = System.nanoTime();
        if(PRINT)
            System.out.println("Starting redo append");
        utils.hardcodedAppendToRedo(row,USE_CRITICAL,index);
        if(PRINT)
            System.out.println("Ending redo append");
        AddResultAndProcess(time, REDO_LOG);
    }

    public void testMethod() {
        Thread t1=null;
        Thread t2=null;

        final Runnable insertDigestCallable = () -> addTxDigest(110,0);

        if(RUN_TX_DIGEST) {
            t1 = new Thread(insertDigestCallable);
            t1.start();
        }

        final Runnable appendCallable = () -> appendToRedo(1);
        ///final Runnable appendCallable = () -> addTxDigest(110,1);

        if(RUN_REDO) {
            t2 = new Thread(appendCallable);
            t2.start();
        }

        try {
            if(RUN_TX_DIGEST)
                t1.join();
            if(RUN_REDO)
                t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void stopServer(){
        if (server != null) {
            server.shutdown();
        }
        List<Long> values=new ArrayList<>();
        TestUtils.printResults(values,results);
    }


    public static void main(String[] args) throws IOException {
        List<Long> values=new ArrayList<>();
        int iterations = Integer.parseInt(args[0]);
        if(args.length>1) {
            ParallelTest.RUN_TX_DIGEST = Boolean.parseBoolean(args[1]);
        }
        if(args.length>2) {
            ParallelTest.RUN_REDO = Boolean.parseBoolean(args[2]);
        }

        if(args.length>3) {
            ParallelTest.USE_CASSANDRA = Boolean.parseBoolean(args[3]);
        }

        if(args.length>4) {
            ParallelTest.USE_CRITICAL = Boolean.parseBoolean(args[4]);
        }
        if(args.length>5) {
            ParallelTest.PRINT = Boolean.parseBoolean(args[5]);
        }
        if(args.length>6) {
            ParallelTest.USE_TRACING = Boolean.parseBoolean(args[6]);
        }
        boolean useServer=false;
        if(args.length>7) {
            useServer  = Boolean.parseBoolean(args[6]);
        }
        int port=-1;
        if(args.length>8){
            port = Integer.parseInt(args[7]);
        }
        else if(useServer){
            System.err.println("Use server requires the port to be used");
            System.exit(1);
        }

        ParallelTest test = null;
        try {
            test = new ParallelTest("rangeTable");
        } catch (MusicServiceException e) {
            e.printStackTrace();
            System.exit(1);
        }
        if(useServer) {
            ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);
            test.server=serverBuilder.addService(new RpcServer(test)).build();
            test.server.start();
            ParallelTest finalTest = test;
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                    System.err.println("*** shutting down gRPC server since JVM is shutting down");
                    finalTest.stopServer();
                    System.err.println("*** server shut down");
                }
            });
        }
        else {
            for (int iter = 0; iter < iterations; iter++) {
                long time = System.nanoTime();
                test.testMethod();
                long nanosecondTime = System.nanoTime() - time;
                long millisecond = nanosecondTime / 1000000;
                values.add(millisecond);
            }
        }
        TestUtils.printResults(values,test.results);
        System.out.println("EXITING");
        System.exit(0);
    }

}
