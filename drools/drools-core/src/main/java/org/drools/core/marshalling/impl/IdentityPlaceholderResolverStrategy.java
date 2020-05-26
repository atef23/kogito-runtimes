/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
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
 */

package org.drools.core.marshalling.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kie.api.marshalling.MarshallingException;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.marshalling.ObjectMarshallingStrategyAcceptor;
import org.kie.api.marshalling.UnmarshallingException;

public class IdentityPlaceholderResolverStrategy
    implements
    ObjectMarshallingStrategy {

    private Map<Integer, Object> ids;
    private Map<Object, Integer> objects;

    private String name = IdentityPlaceholderResolverStrategy.class.getName();
    private ObjectMarshallingStrategyAcceptor acceptor;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    public IdentityPlaceholderResolverStrategy(ObjectMarshallingStrategyAcceptor acceptor) {
        this.acceptor = acceptor;
        this.ids = new HashMap<>();
        this.objects = new IdentityHashMap<>();
    }

    public IdentityPlaceholderResolverStrategy(String name, ObjectMarshallingStrategyAcceptor acceptor) {
    	this( acceptor );
    	this.name = name;
    }
    
    public IdentityPlaceholderResolverStrategy(ObjectMarshallingStrategyAcceptor acceptor, Map<Integer, Object> ids) {
        this(acceptor);
        setIds(ids);
    }
    
    public IdentityPlaceholderResolverStrategy(String name, ObjectMarshallingStrategyAcceptor acceptor, Map<Integer, Object> ids){
    	this( acceptor, ids );
    	this.name = name;
    }
    
    @Override
    public String getName(){
    	return this.name;
    }

    public Object read(ObjectInputStream os) throws IOException,
                                                       ClassNotFoundException {
        int id = os.readInt();
        return  ids.get( id );
    }

    public void write(ObjectOutputStream os,
                      Object object) throws IOException {
        Integer id = objects.get( object );
        if ( id == null ) {
            id = ids.size();
            ids.put( id, object );
            objects.put(  object, id );
        }
        os.writeInt( id );
    }

    public boolean accept(Object object) {
        return this.acceptor.accept( object );
    }

    public byte[] marshal(Context context,
                          ObjectOutputStream os,
                          Object object) {
        Integer id = objects.get( object );
        if ( id == null ) {
            id = ids.size();
            ids.put( id, object );
            objects.put(  object, id );
        }
        return intToByteArray( id );
    }

    public Object unmarshal(String dataType, 
                            Context context,
                            ObjectInputStream is,
                            byte[] object, 
                            ClassLoader classloader ) {
        return ids.get( byteArrayToInt( object ) );
    }
    
    private byte[] intToByteArray(int value) {
        return new byte[] {
                (byte) ((value >>> 24) & 0xFF),
                (byte) ((value >>> 16) & 0xFF),
                (byte) ((value >>> 8) & 0xFF),
                (byte) (value  & 0xFF) };
    }    
    
    private int byteArrayToInt(byte [] b) {
        return (b[0] << 24)
                + ((b[1] & 0xFF) << 16)
                + ((b[2] & 0xFF) << 8)
                + (b[3] & 0xFF);
    }
    
    public Context createContext() {
        // no need for context
        return null;
    }

    public Map<Integer, Object> getIds() {
        return ids;
    }

    public void setIds(Map<Integer, Object> ids) {
        this.ids = ids;
        objects.clear();
        for (Map.Entry<Integer, Object> idEntry : ids.entrySet()) {
            objects.put(idEntry.getValue(), idEntry.getKey());
        }
    }

    @Override
    public String marshalToJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new MarshallingException(e);
        }
    }

    @Override
    public Object unmarshalFromJson(String dataType, String json) {
        try {
            Class<?> loadClass = Thread.currentThread().getContextClassLoader().loadClass(dataType);
            return MAPPER.readValue(json, loadClass);
        } catch (ClassNotFoundException | JsonProcessingException e) {
            throw new UnmarshallingException(e);
        }
    }
}
