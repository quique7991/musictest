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


import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import org.onap.music.mdbc.TestUtils.MriRow;

public class ParallelTest {
    final int REPLICATION_FACTOR=3;
    final private TestUtils utils;
    final private MriRow row;

    public ParallelTest(String rangeTableName) {
        utils=new TestUtils(REPLICATION_FACTOR);
        utils.createMusicRangeInformationTable();
        utils.createMusicTxDigest();
        row = utils.createBasicRow(rangeTableName);
    }

    public void addTxDigest(int size){
        System.out.println("Starting tx digest");
        utils.hardcodedAddtransaction(size);
        System.out.println("Ending tx digest");
    }

    public void appendToRedo(){
        System.out.println("Starting redo append");
        utils.hardcodedAppendToRedo(row);
        System.out.println("Ending redo append");
    }

    public void testMethod() {
        Thread t1;
        Thread t2;

        final Runnable insertDigestCallable = () -> addTxDigest(110);

        t1 = new Thread(insertDigestCallable);
        t1.start();

        final Runnable appendCallable = () -> appendToRedo();

        t2 = new Thread(appendCallable);
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args){
        List<Long> values=new ArrayList<>();
        int iterations = 100;
        ParallelTest test = new ParallelTest("rangeTable");
        for(int iter=0;iter<iterations;iter++) {
            long time = System.nanoTime();
            test.testMethod();
            long nanosecondTime = System.nanoTime() - time;
            long millisecond = nanosecondTime / 1000000;
            values.add(millisecond);
        }
        final LongSummaryStatistics longSummaryStatistics = values.stream().mapToLong((x) -> x).summaryStatistics();
        System.out.println("Min:"+longSummaryStatistics.getMin() + "ms");
        System.out.println("Average:"+longSummaryStatistics.getAverage() + "ms");
        System.out.println("Max:"+longSummaryStatistics.getMax() + "ms");
    }

}
