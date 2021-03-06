/*
 * Copyright 2011 Outerthought bvba
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
package org.lilyproject.repository.impl.valuetype;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lilyproject.bytes.api.DataInput;
import org.lilyproject.bytes.api.DataOutput;
import org.lilyproject.repository.api.IdentityRecordStack;
import org.lilyproject.repository.api.RepositoryException;
import org.lilyproject.repository.api.TypeManager;
import org.lilyproject.repository.api.ValueType;
import org.lilyproject.repository.api.ValueTypeFactory;
import org.lilyproject.util.ArgumentValidator;

public class ListValueType extends AbstractValueType implements ValueType {

    public final static String NAME = "LIST";

    private ValueType valueType;

    private final String fullName;

    public ListValueType(TypeManager typeManager, String typeParams) throws RepositoryException, InterruptedException {
        ArgumentValidator.notNull(typeParams, "typeParams");
        this.fullName = NAME+"<"+typeParams+">";
        this.valueType = typeManager.getValueType(typeParams);
    }

    public ListValueType(TypeManager typeManager, DataInput typeParamsDataInput) throws RepositoryException, InterruptedException {
        this(typeManager, typeParamsDataInput.readUTF());
    }

    @Override
    public String getBaseName() {
        return NAME;
    }

    @Override
    public String getName() {
        return fullName;
    }

    @Override
    public ValueType getDeepestValueType() {
        return valueType.getDeepestValueType();
    }

    @Override
    public ValueType getNestedValueType() {
        return valueType;
    }

    @Override
    public int getNestingLevel() {
        return 1 + valueType.getNestingLevel();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object> read(DataInput dataInput) throws RepositoryException, InterruptedException {
        int nrOfValues = dataInput.readInt();
        List<Object> result = new ArrayList<Object>(nrOfValues);
        for (int i = 0 ; i < nrOfValues; i++) {
            result.add(valueType.read(dataInput));
       }
        return result;
    }

    @Override
    public void write(Object value, DataOutput dataOutput, IdentityRecordStack parentRecords)
            throws RepositoryException, InterruptedException {
        List<Object> values = ((List<Object>) value);
        dataOutput.writeInt(values.size());
        for (Object element : values) {
            valueType.write(element, dataOutput, parentRecords);
        }
    }

    @Override
    public Class getType() {
        return List.class;
    }

    @Override
    public Comparator getComparator() {
        return null;
    }

    @Override
    public Set<Object> getValues(Object value) {
        Set<Object> result = new HashSet<Object>();
        for (Object element : ((List<Object>) value)) {
            result.addAll(valueType.getValues(element));
        }
        return result;
    }

    @Override
    public boolean isMultiValue() {
        return true;
    }

    @Override
    public boolean isHierarchical() {
        return valueType.isHierarchical();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + fullName.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return fullName.equals(((ListValueType) obj).fullName);
    }

    //
    // Factory
    //
    public static ValueTypeFactory factory(TypeManager typeManager) {
        return new ListValueTypeFactory(typeManager);
    }

    public static class ListValueTypeFactory implements ValueTypeFactory {

        private TypeManager typeManager;

        public ListValueTypeFactory(TypeManager typeManager) {
            this.typeManager = typeManager;
        }

        @Override
        public ValueType getValueType(String typeParams) throws RepositoryException, InterruptedException {
            return new ListValueType(typeManager, typeParams);
        }
    }
}
