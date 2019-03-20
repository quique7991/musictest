/* ============LICENSE_START====================================================
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

import io.grpc.stub.StreamObserver;
import org.onap.music.mdbc.proto.*;

public class RpcServer extends TestServiceGrpc.TestServiceImplBase {
    ParallelTest test;

    public RpcServer(ParallelTest test) {
        this.test=test;
    }

    private Ack executeOnce(){
        long time = System.nanoTime();
        test.testMethod();
        long nanosecondTime = System.nanoTime() - time;
        long millisecond = nanosecondTime / 1000000;
        return Ack.newBuilder().addLatency(millisecond).build();
    }

    private Ack executeMany(Iterations request){
        Ack.Builder builder = Ack.newBuilder();
        for(int iter=0;iter<request.getIters();iter++) {
            long time = System.nanoTime();
            test.testMethod();
            long nanosecondTime = System.nanoTime() - time;
            long millisecond = nanosecondTime / 1000000;
            builder.addLatency(millisecond);
        }
        return builder.build();
    }

    @Override
    public void executeOnce(Empty request, StreamObserver<Ack> responseObserver) {
        responseObserver.onNext(executeOnce());
        responseObserver.onCompleted();
    }

    @Override
    public void executeMany(Iterations request, StreamObserver<Ack> responseObserver) {
        responseObserver.onNext(executeMany(request));
        responseObserver.onCompleted();
    }

    @Override
    public void end(Empty request, StreamObserver<Empty> responseObserver) {
        try {
            this.finalize();
            System.exit(0);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.exit(1);
        }
    }


}
