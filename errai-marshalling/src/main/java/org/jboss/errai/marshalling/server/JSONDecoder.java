/*
 * Copyright 2010 JBoss, a divison Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.marshalling.server;

import org.jboss.errai.common.client.protocols.SerializationParts;
import org.jboss.errai.common.client.types.UHashMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static java.lang.Character.isDigit;
import static java.lang.Character.isJavaIdentifierPart;
import static org.jboss.errai.common.client.protocols.SerializationParts.ENCODED_TYPE;
import static org.mvel2.util.ParseTools.handleStringEscapes;
import static org.mvel2.util.ParseTools.subArray;

/**
 * Decodes a JSON string or character array, and provides a proper collection of elements
 */
public class JSONDecoder {
  public static Object decode(String o) {
    return new JSONStreamDecoder(new ByteArrayInputStream(o.getBytes())).parse();
  }
}