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

package org.onap.music.mdbc.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 * Interface exported by the server.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.19.0)",
    comments = "Source: test.proto")
public final class TestServiceGrpc {

  private TestServiceGrpc() {}

  public static final String SERVICE_NAME = "musictestproto.TestService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.onap.music.mdbc.proto.Empty,
      org.onap.music.mdbc.proto.Ack> getExecuteOnceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExecuteOnce",
      requestType = org.onap.music.mdbc.proto.Empty.class,
      responseType = org.onap.music.mdbc.proto.Ack.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.onap.music.mdbc.proto.Empty,
      org.onap.music.mdbc.proto.Ack> getExecuteOnceMethod() {
    io.grpc.MethodDescriptor<org.onap.music.mdbc.proto.Empty, org.onap.music.mdbc.proto.Ack> getExecuteOnceMethod;
    if ((getExecuteOnceMethod = TestServiceGrpc.getExecuteOnceMethod) == null) {
      synchronized (TestServiceGrpc.class) {
        if ((getExecuteOnceMethod = TestServiceGrpc.getExecuteOnceMethod) == null) {
          TestServiceGrpc.getExecuteOnceMethod = getExecuteOnceMethod = 
              io.grpc.MethodDescriptor.<org.onap.music.mdbc.proto.Empty, org.onap.music.mdbc.proto.Ack>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "musictestproto.TestService", "ExecuteOnce"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.onap.music.mdbc.proto.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.onap.music.mdbc.proto.Ack.getDefaultInstance()))
                  .setSchemaDescriptor(new TestServiceMethodDescriptorSupplier("ExecuteOnce"))
                  .build();
          }
        }
     }
     return getExecuteOnceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.onap.music.mdbc.proto.Iterations,
      org.onap.music.mdbc.proto.Ack> getExecuteManyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExecuteMany",
      requestType = org.onap.music.mdbc.proto.Iterations.class,
      responseType = org.onap.music.mdbc.proto.Ack.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.onap.music.mdbc.proto.Iterations,
      org.onap.music.mdbc.proto.Ack> getExecuteManyMethod() {
    io.grpc.MethodDescriptor<org.onap.music.mdbc.proto.Iterations, org.onap.music.mdbc.proto.Ack> getExecuteManyMethod;
    if ((getExecuteManyMethod = TestServiceGrpc.getExecuteManyMethod) == null) {
      synchronized (TestServiceGrpc.class) {
        if ((getExecuteManyMethod = TestServiceGrpc.getExecuteManyMethod) == null) {
          TestServiceGrpc.getExecuteManyMethod = getExecuteManyMethod = 
              io.grpc.MethodDescriptor.<org.onap.music.mdbc.proto.Iterations, org.onap.music.mdbc.proto.Ack>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "musictestproto.TestService", "ExecuteMany"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.onap.music.mdbc.proto.Iterations.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.onap.music.mdbc.proto.Ack.getDefaultInstance()))
                  .setSchemaDescriptor(new TestServiceMethodDescriptorSupplier("ExecuteMany"))
                  .build();
          }
        }
     }
     return getExecuteManyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.onap.music.mdbc.proto.Empty,
      org.onap.music.mdbc.proto.Empty> getEndMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "End",
      requestType = org.onap.music.mdbc.proto.Empty.class,
      responseType = org.onap.music.mdbc.proto.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.onap.music.mdbc.proto.Empty,
      org.onap.music.mdbc.proto.Empty> getEndMethod() {
    io.grpc.MethodDescriptor<org.onap.music.mdbc.proto.Empty, org.onap.music.mdbc.proto.Empty> getEndMethod;
    if ((getEndMethod = TestServiceGrpc.getEndMethod) == null) {
      synchronized (TestServiceGrpc.class) {
        if ((getEndMethod = TestServiceGrpc.getEndMethod) == null) {
          TestServiceGrpc.getEndMethod = getEndMethod = 
              io.grpc.MethodDescriptor.<org.onap.music.mdbc.proto.Empty, org.onap.music.mdbc.proto.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "musictestproto.TestService", "End"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.onap.music.mdbc.proto.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.onap.music.mdbc.proto.Empty.getDefaultInstance()))
                  .setSchemaDescriptor(new TestServiceMethodDescriptorSupplier("End"))
                  .build();
          }
        }
     }
     return getEndMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static TestServiceStub newStub(io.grpc.Channel channel) {
    return new TestServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static TestServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new TestServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static TestServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new TestServiceFutureStub(channel);
  }

  /**
   * <pre>
   * Interface exported by the server.
   * </pre>
   */
  public static abstract class TestServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void executeOnce(org.onap.music.mdbc.proto.Empty request,
        io.grpc.stub.StreamObserver<org.onap.music.mdbc.proto.Ack> responseObserver) {
      asyncUnimplementedUnaryCall(getExecuteOnceMethod(), responseObserver);
    }

    /**
     */
    public void executeMany(org.onap.music.mdbc.proto.Iterations request,
        io.grpc.stub.StreamObserver<org.onap.music.mdbc.proto.Ack> responseObserver) {
      asyncUnimplementedUnaryCall(getExecuteManyMethod(), responseObserver);
    }

    /**
     */
    public void end(org.onap.music.mdbc.proto.Empty request,
        io.grpc.stub.StreamObserver<org.onap.music.mdbc.proto.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getEndMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getExecuteOnceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.onap.music.mdbc.proto.Empty,
                org.onap.music.mdbc.proto.Ack>(
                  this, METHODID_EXECUTE_ONCE)))
          .addMethod(
            getExecuteManyMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.onap.music.mdbc.proto.Iterations,
                org.onap.music.mdbc.proto.Ack>(
                  this, METHODID_EXECUTE_MANY)))
          .addMethod(
            getEndMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.onap.music.mdbc.proto.Empty,
                org.onap.music.mdbc.proto.Empty>(
                  this, METHODID_END)))
          .build();
    }
  }

  /**
   * <pre>
   * Interface exported by the server.
   * </pre>
   */
  public static final class TestServiceStub extends io.grpc.stub.AbstractStub<TestServiceStub> {
    private TestServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private TestServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TestServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new TestServiceStub(channel, callOptions);
    }

    /**
     */
    public void executeOnce(org.onap.music.mdbc.proto.Empty request,
        io.grpc.stub.StreamObserver<org.onap.music.mdbc.proto.Ack> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExecuteOnceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void executeMany(org.onap.music.mdbc.proto.Iterations request,
        io.grpc.stub.StreamObserver<org.onap.music.mdbc.proto.Ack> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExecuteManyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void end(org.onap.music.mdbc.proto.Empty request,
        io.grpc.stub.StreamObserver<org.onap.music.mdbc.proto.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getEndMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * Interface exported by the server.
   * </pre>
   */
  public static final class TestServiceBlockingStub extends io.grpc.stub.AbstractStub<TestServiceBlockingStub> {
    private TestServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private TestServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TestServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new TestServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.onap.music.mdbc.proto.Ack executeOnce(org.onap.music.mdbc.proto.Empty request) {
      return blockingUnaryCall(
          getChannel(), getExecuteOnceMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.onap.music.mdbc.proto.Ack executeMany(org.onap.music.mdbc.proto.Iterations request) {
      return blockingUnaryCall(
          getChannel(), getExecuteManyMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.onap.music.mdbc.proto.Empty end(org.onap.music.mdbc.proto.Empty request) {
      return blockingUnaryCall(
          getChannel(), getEndMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Interface exported by the server.
   * </pre>
   */
  public static final class TestServiceFutureStub extends io.grpc.stub.AbstractStub<TestServiceFutureStub> {
    private TestServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private TestServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TestServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new TestServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.onap.music.mdbc.proto.Ack> executeOnce(
        org.onap.music.mdbc.proto.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getExecuteOnceMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.onap.music.mdbc.proto.Ack> executeMany(
        org.onap.music.mdbc.proto.Iterations request) {
      return futureUnaryCall(
          getChannel().newCall(getExecuteManyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.onap.music.mdbc.proto.Empty> end(
        org.onap.music.mdbc.proto.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getEndMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_EXECUTE_ONCE = 0;
  private static final int METHODID_EXECUTE_MANY = 1;
  private static final int METHODID_END = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final TestServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(TestServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_EXECUTE_ONCE:
          serviceImpl.executeOnce((org.onap.music.mdbc.proto.Empty) request,
              (io.grpc.stub.StreamObserver<org.onap.music.mdbc.proto.Ack>) responseObserver);
          break;
        case METHODID_EXECUTE_MANY:
          serviceImpl.executeMany((org.onap.music.mdbc.proto.Iterations) request,
              (io.grpc.stub.StreamObserver<org.onap.music.mdbc.proto.Ack>) responseObserver);
          break;
        case METHODID_END:
          serviceImpl.end((org.onap.music.mdbc.proto.Empty) request,
              (io.grpc.stub.StreamObserver<org.onap.music.mdbc.proto.Empty>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class TestServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    TestServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.onap.music.mdbc.proto.MusicTestProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("TestService");
    }
  }

  private static final class TestServiceFileDescriptorSupplier
      extends TestServiceBaseDescriptorSupplier {
    TestServiceFileDescriptorSupplier() {}
  }

  private static final class TestServiceMethodDescriptorSupplier
      extends TestServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    TestServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (TestServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new TestServiceFileDescriptorSupplier())
              .addMethod(getExecuteOnceMethod())
              .addMethod(getExecuteManyMethod())
              .addMethod(getEndMethod())
              .build();
        }
      }
    }
    return result;
  }
}
