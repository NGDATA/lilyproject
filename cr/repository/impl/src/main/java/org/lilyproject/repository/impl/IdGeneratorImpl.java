/*
 * Copyright 2010 Outerthought bvba
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lilyproject.repository.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.lilyproject.bytes.api.DataInput;
import org.lilyproject.bytes.api.DataOutput;
import org.lilyproject.bytes.impl.DataInputImpl;
import org.lilyproject.bytes.impl.DataOutputImpl;
import org.lilyproject.repository.api.IdGenerator;
import org.lilyproject.repository.api.RecordId;
import org.lilyproject.repository.api.SchemaId;
import org.lilyproject.util.ArgumentValidator;


public class IdGeneratorImpl implements IdGenerator {
    
    private static enum IdIdentifier{ USER((byte)0), UUID((byte)1), VARIANT((byte)2);
    
        private final byte identifierByte;

        IdIdentifier(byte identifierByte) {
            this.identifierByte = identifierByte;
        }
        
        public byte getIdentifierByte() {
            return identifierByte;
        }
    }

    private static IdIdentifier[] IDENTIFIERS;
    static {
        IDENTIFIERS = new IdIdentifier[3];
        IDENTIFIERS[0] = IdIdentifier.USER;
        IDENTIFIERS[1] = IdIdentifier.UUID;
        IDENTIFIERS[2] = IdIdentifier.VARIANT;
    }

    public RecordId newRecordId() {
        return new UUIDRecordId(this);
    }

    public RecordId newRecordId(RecordId masterRecordId, Map<String, String> variantProperties) {
        ArgumentValidator.notNull(masterRecordId, "masterRecordId");
        ArgumentValidator.notNull(variantProperties, "variantProperties");

        if (!masterRecordId.isMaster())
            throw new IllegalArgumentException("Specified masterRecordId is a variant record ID.");

        if (variantProperties.isEmpty())
            return masterRecordId;

        checkReservedCharacters(variantProperties);
        
        return new VariantRecordId(masterRecordId, variantProperties, this);
    }

    public RecordId newRecordId(Map<String, String> variantProperties) {
        return newRecordId(newRecordId(), variantProperties);
    }

    public RecordId newRecordId(String userProvidedId) {
        ArgumentValidator.notNull(userProvidedId, "userProvidedId");
        checkIdString(userProvidedId, "record id");
        return new UserRecordId(userProvidedId, this);
    }
    
    public RecordId newRecordId(String userProvidedId, Map<String, String> variantProperties) {
        return newRecordId(newRecordId(userProvidedId), variantProperties);
    }

    // Bytes
    // An identifier byte is put behind the bytes provided by the RecordId itself
    
    protected byte[] toBytes(UUIDRecordId uuidRecordId) {
        DataOutput dataOutput = new DataOutputImpl(17);
        writeBytes(uuidRecordId, dataOutput);
        return dataOutput.toByteArray();
    }

    protected void writeBytes(UUIDRecordId uuidRecordId, DataOutput dataOutput) {
        uuidRecordId.writeBasicBytes(dataOutput);
        dataOutput.writeByte(IdIdentifier.UUID.getIdentifierByte());
    }
    
    protected byte[] toBytes(UserRecordId userRecordId) {
        DataOutput dataOutput = new DataOutputImpl();
        writeBytes(userRecordId, dataOutput);
        return dataOutput.toByteArray();
    }
    
    protected void writeBytes(UserRecordId userRecordId, DataOutput dataOutput) {
        userRecordId.writeBasicBytes(dataOutput);
        dataOutput.writeByte(IdIdentifier.USER.getIdentifierByte());
    }
    
    protected byte[] toBytes(VariantRecordId variantRecordId) {
        DataOutput dataOutput = new DataOutputImpl();
        writeBytes(variantRecordId, dataOutput);
        return dataOutput.toByteArray();
    }
    
    protected void writeBytes(VariantRecordId variantRecordId, DataOutput dataOutput) {
        variantRecordId.writeBasicBytes(dataOutput);
        dataOutput.writeByte(IdIdentifier.VARIANT.getIdentifierByte());
    }
    
    public RecordId fromBytes(byte[] bytes) {
        return fromBytes(new DataInputImpl(bytes));
    }

    public RecordId fromBytes(DataInput dataInput) {
        // Read the identifier byte at the end of the input
        int pos = dataInput.getPosition();
        int size = dataInput.getSize();
        dataInput.setPosition(size - 1);
        byte identifierByte = dataInput.readByte();
        dataInput.setPosition(pos);

        // Virtually remove the identifier byte from the input
        dataInput.setSize(size - 1);

        IdIdentifier idIdentifier = IDENTIFIERS[identifierByte];
        RecordId recordId = null;
        switch (idIdentifier) {
        case UUID:
            recordId = new UUIDRecordId(dataInput, this);
            break;

        case USER:
            recordId = new UserRecordId(dataInput, this);
            break;
            
        case VARIANT:
            recordId = new VariantRecordId(dataInput, this);
            break;
            
        default:
            // will already have failed on the IDENTIFIERS array lookup above
            break;
        }
        return recordId;
    }
    
    // Strings
    // The prefix string (e.g. "UUID.") is put before the string provided by the recordId itself
    
    protected String toString(UUIDRecordId uuidRecordId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(IdIdentifier.UUID.name());
        stringBuilder.append(".");
        stringBuilder.append(uuidRecordId.getBasicString());
        return stringBuilder.toString();
    }
    
    protected String toString(UserRecordId userRecordId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(IdIdentifier.USER.name());
        stringBuilder.append(".");
        stringBuilder.append(userRecordId.getBasicString());
        return stringBuilder.toString();
    }
    
    // The variantproperties are appended to the string of the master record
    protected String toString(VariantRecordId variantRecordId) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(variantRecordId.getMaster().toString());
        stringBuilder.append(".");

        boolean first = true;
        for (Map.Entry<String, String> entry : variantRecordId.getVariantProperties().entrySet()) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(",");
            }
            
            stringBuilder.append(entry.getKey()).append('=').append(entry.getValue());
        }

        return stringBuilder.toString();
    }
    
    public RecordId fromString(String recordIdString) {
        int firstDotPos = recordIdString.indexOf('.');

        if (firstDotPos == -1) {
            throw new IllegalArgumentException("Invalid record id, contains no dot: " + recordIdString);
        }

        String type = recordIdString.substring(0, firstDotPos).trim();
        String id;
        String variantString = null;

        int secondDotPos = recordIdString.indexOf('.', firstDotPos + 1);

        if (secondDotPos == -1) {
            id = recordIdString.substring(firstDotPos + 1).trim();
        } else {
            id = recordIdString.substring(firstDotPos + 1, secondDotPos).trim();
            variantString = recordIdString.substring(secondDotPos + 1);
        }

        RecordId masterRecordId;

        if (type.equals(IdIdentifier.UUID.name())) {
            masterRecordId = new UUIDRecordId(id, this);
        } else if (type.equals(IdIdentifier.USER.name())) {
            masterRecordId = new UserRecordId(id, this);
        } else {
            throw new IllegalArgumentException("Invalid record id: unknown type '" + type + "' in record id '" +
                    recordIdString + "'.");
        }

        if (variantString == null) {
            return masterRecordId;
        }

        // Parse the variant string
        Map<String, String> variantProps = new HashMap<String, String>();

        String[] variantStringParts = variantString.split(",");
        for (String part : variantStringParts) {
            int eqPos = part.indexOf('=');
            if (eqPos == -1) {
                throw new IllegalArgumentException("Invalid record id: " + recordIdString);
            }

            String name = part.substring(0, eqPos).trim();
            String value = part.substring(eqPos + 1).trim();

            variantProps.put(name, value);
        }

        return new VariantRecordId(masterRecordId, variantProps, this);
    }

    protected static void checkReservedCharacters(Map<String, String> props) {
        for (Map.Entry<String, String> entry : props.entrySet()) {
            checkVariantPropertyNameValue(entry.getKey());
            checkVariantPropertyNameValue(entry.getValue());
        }
    }

    protected static void checkVariantPropertyNameValue(String text) {
        checkIdString(text, "variant property name or value");
    }

    protected static void checkIdString(String text, String what) {
        if (text.length() == 0) {
            throw new IllegalArgumentException("Zero-length " + what + " is not allowed.");
        }

        if (Character.isWhitespace(text.charAt(0)) || Character.isWhitespace(text.charAt(text.length() - 1))) {
            throw new IllegalArgumentException(what + " should not start or end with whitespace: \"" + text + "\".");
        }

        checkReservedCharacters(text);
    }

    protected static void checkReservedCharacters(String text) {
        for (int i = 0; i < text.length(); i++) {
            switch (text.charAt(i)) {
                case '.':
                case '=':
                case ',':
                    throw new IllegalArgumentException("Reserved record id character (one of: . , =) in \"" +
                            text + "\".");
            }
        }
    }
    
    public SchemaId getSchemaId(byte[] id) {
        return new SchemaIdImpl(id);
    }

    public SchemaId getSchemaId(String id) {
        return new SchemaIdImpl(id);
    }
    
    public SchemaId getSchemaId(UUID id) {
        return new SchemaIdImpl(id);
    }

}