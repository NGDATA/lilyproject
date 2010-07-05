package org.lilycms.repository.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;

import org.apache.avro.ipc.AvroRemoteException;
import org.apache.avro.ipc.HttpTransceiver;
import org.apache.avro.specific.SpecificRequestor;
import org.apache.avro.util.Utf8;
import org.lilycms.repository.api.*;
import org.lilycms.repository.api.FieldTypeExistsException;
import org.lilycms.repository.api.FieldTypeNotFoundException;
import org.lilycms.repository.api.FieldTypeUpdateException;
import org.lilycms.repository.api.RecordTypeExistsException;
import org.lilycms.repository.api.RecordTypeNotFoundException;
import org.lilycms.repository.avro.*;

public class TypeManagerRemoteImpl extends AbstractTypeManager implements TypeManager {

    private AvroTypeManager typeManagerProxy;
    private AvroConverter converter;

    public TypeManagerRemoteImpl(InetSocketAddress address, AvroConverter converter, IdGenerator idGenerator)
            throws IOException {
        this.converter = converter;
        //TODO idGenerator should not be available or used in the remote implementation
        this.idGenerator = idGenerator;
        HttpTransceiver client = new HttpTransceiver(new URL("http://" + address.getHostName() + ":" + address.getPort() + "/"));

        typeManagerProxy = (AvroTypeManager) SpecificRequestor.getClient(AvroTypeManager.class, client);
        initialize();
    }
    
    public RecordType createRecordType(RecordType recordType) throws RecordTypeExistsException,
            RecordTypeNotFoundException, FieldTypeNotFoundException, TypeException {

        try {
            return converter.convert(typeManagerProxy.createRecordType(converter.convert(recordType)));
        } catch (AvroRecordTypeExistsException e) {
            throw converter.convert(e);
        } catch (AvroRecordTypeNotFoundException e) {
            throw converter.convert(e);
        } catch (AvroFieldTypeNotFoundException e) {
            throw converter.convert(e);
        } catch (AvroTypeException e) {
            throw converter.convert(e);
        } catch (AvroRemoteException e) {
            throw converter.convert(e);
        }
    }

    public RecordType getRecordType(String id, Long version) throws RecordTypeNotFoundException, TypeException {
        try {
            long avroVersion;
            if (version == null) {
                avroVersion = -1;
            } else {
                avroVersion = version;
            }
            return converter.convert(typeManagerProxy.getRecordType(new Utf8(id), avroVersion));
        } catch (AvroRecordTypeNotFoundException e) {
            throw converter.convert(e);
        } catch (AvroTypeException e) {
            throw converter.convert(e);
        } catch (AvroRemoteException e) {
            throw converter.convert(e);
        }
    }

    public RecordType updateRecordType(RecordType recordType) throws RecordTypeNotFoundException,
            FieldTypeNotFoundException, TypeException {

        try {
            return converter.convert(typeManagerProxy.updateRecordType(converter.convert(recordType)));
        } catch (AvroRecordTypeNotFoundException e) {
            throw converter.convert(e);
        } catch (AvroFieldTypeNotFoundException e) {
            throw converter.convert(e);
        } catch (AvroTypeException e) {
            throw converter.convert(e);
        } catch (AvroRemoteException e) {
            throw converter.convert(e);
        }
    }

    public FieldType createFieldType(FieldType fieldType) throws FieldTypeExistsException, TypeException {
        try {
            AvroFieldType avroFieldType = converter.convert(fieldType);
            AvroFieldType createFieldType = typeManagerProxy.createFieldType(avroFieldType);
            FieldType resultFieldType = converter.convert(createFieldType);
            return resultFieldType;
        } catch (AvroFieldTypeExistsException e) {
            throw converter.convert(e);
        } catch (AvroTypeException e) {
            throw converter.convert(e);
        } catch (AvroRemoteException e) {
            throw converter.convert(e);
        }
    }

    public FieldType updateFieldType(FieldType fieldType) throws FieldTypeNotFoundException, FieldTypeUpdateException,
            TypeException {

        try {
            return converter.convert(typeManagerProxy.updateFieldType(converter.convert(fieldType)));
        } catch (AvroFieldTypeNotFoundException e) {
            throw converter.convert(e);
        } catch (AvroFieldTypeUpdateException e) {
            throw converter.convert(e);
        } catch (AvroTypeException e) {
            throw converter.convert(e);
        } catch (AvroRemoteException e) {
            throw converter.convert(e);
        }
    }

    public FieldType getFieldTypeById(String id) throws FieldTypeNotFoundException, TypeException {
        try {
            return converter.convert(typeManagerProxy.getFieldTypeById(new Utf8(id)));
        } catch (AvroFieldTypeNotFoundException e) {
            throw converter.convert(e);
        } catch (AvroTypeException e) {
            throw converter.convert(e);
        } catch (AvroRemoteException e) {
            throw converter.convert(e);
        }
    }

    public FieldType getFieldTypeByName(QName name) throws FieldTypeNotFoundException, TypeException {
        try {
            return converter.convert(typeManagerProxy.getFieldTypeByName(converter.convert(name)));
        } catch (AvroFieldTypeNotFoundException e) {
            throw converter.convert(e);
        } catch (AvroTypeException e) {
            throw converter.convert(e);
        } catch (AvroRemoteException e) {
            throw converter.convert(e);
        }
    }

}
