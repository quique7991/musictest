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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.json.*;
import org.onap.music.mdbc.proto.Ack;
import org.onap.music.mdbc.proto.Empty;
import org.onap.music.mdbc.proto.TestServiceGrpc;

public class Controller {

        public static void main(String[] args){
                Map<String,List<Long>> results = new HashMap<>();
                List<Long> values=new ArrayList<>();
                List<ManagedChannel> channels = new ArrayList<>();
                List<TestServiceGrpc.TestServiceBlockingStub> stubs = new ArrayList<>();
                int iterations = Integer.parseInt(args[0]);
                for(int i=1; i < args.length;i++){
                        JSONObject obj = new JSONObject(args[i]);
                        ManagedChannelBuilder<?> o = ManagedChannelBuilder.forAddress(obj.getString("url"), obj.getInt("port")).usePlaintext();
                        ManagedChannel chann = o.build();
                        channels.add(chann);
                        stubs.add(TestServiceGrpc.newBlockingStub(chann));
                }

                for(int iter = 0; iter<iterations;iter++){
                    for(TestServiceGrpc.TestServiceBlockingStub stub : stubs){
                            Ack ack = stub.executeOnce(Empty.newBuilder().build());
                            values.addAll(ack.getLatencyList());
                    }
                }
                for(TestServiceGrpc.TestServiceBlockingStub stub : stubs){
                        Empty empty = stub.end(Empty.newBuilder().build());
                }

                TestUtils.printResults(values,results);
                System.out.println("EXITING");
                System.exit(0);
        }
}
