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

// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: test.proto

package org.onap.music.mdbc.proto;

public final class MusicTestProto {
  private MusicTestProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_musictestproto_Empty_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_musictestproto_Empty_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_musictestproto_Ack_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_musictestproto_Ack_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_musictestproto_Iterations_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_musictestproto_Iterations_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\ntest.proto\022\016musictestproto\"\007\n\005Empty\"\026\n" +
      "\003Ack\022\017\n\007latency\030\001 \003(\003\"\033\n\nIterations\022\r\n\005i" +
      "ters\030\001 \001(\0052\303\001\n\013TestService\022;\n\013ExecuteOnc" +
      "e\022\025.musictestproto.Empty\032\023.musictestprot" +
      "o.Ack\"\000\022@\n\013ExecuteMany\022\032.musictestproto." +
      "Iterations\032\023.musictestproto.Ack\"\000\0225\n\003End" +
      "\022\025.musictestproto.Empty\032\025.musictestproto" +
      ".Empty\"\000B3\n\031org.onap.music.mdbc.protoB\016M" +
      "usicTestProtoP\001\242\002\003MTPb\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
    internal_static_musictestproto_Empty_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_musictestproto_Empty_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_musictestproto_Empty_descriptor,
        new java.lang.String[] { });
    internal_static_musictestproto_Ack_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_musictestproto_Ack_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_musictestproto_Ack_descriptor,
        new java.lang.String[] { "Latency", });
    internal_static_musictestproto_Iterations_descriptor =
      getDescriptor().getMessageTypes().get(2);
    internal_static_musictestproto_Iterations_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_musictestproto_Iterations_descriptor,
        new java.lang.String[] { "Iters", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
