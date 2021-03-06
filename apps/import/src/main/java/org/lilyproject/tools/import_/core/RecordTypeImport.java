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
package org.lilyproject.tools.import_.core;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lilyproject.repository.api.FieldTypeEntry;
import org.lilyproject.repository.api.QName;
import org.lilyproject.repository.api.RecordType;
import org.lilyproject.repository.api.RecordTypeExistsException;
import org.lilyproject.repository.api.RecordTypeNotFoundException;
import org.lilyproject.repository.api.RepositoryException;
import org.lilyproject.repository.api.SchemaId;
import org.lilyproject.repository.api.TypeManager;

import static org.lilyproject.tools.import_.core.ImportMode.CREATE;
import static org.lilyproject.tools.import_.core.ImportMode.CREATE_OR_UPDATE;
import static org.lilyproject.tools.import_.core.ImportMode.UPDATE;

public class RecordTypeImport {
    private RecordTypeImport() {
    }

    public static ImportResult<RecordType> importRecordType(RecordType newRecordType, ImportMode impMode,
            IdentificationMode idMode, QName identifyingName, boolean refreshSubtypes, TypeManager typeManager)
            throws RepositoryException, InterruptedException {

        if (idMode == IdentificationMode.ID && impMode == CREATE_OR_UPDATE) {
            throw new IllegalArgumentException("The combination of import mode " + CREATE_OR_UPDATE
                    + " and identification mode " + IdentificationMode.ID + " is not possible.");
        }

        int loopCount = 0;
        while (true) {
            if (loopCount > 1) {
                // We should never arrive here
                throw new RuntimeException("Unexpected situation: when we tried to update the record type, " +
                        "it did not exist, when we tried to create the record type, it exists, and then when we retry " +
                        "to update, it does not exist after all.");
            }

            if (impMode == UPDATE || impMode == CREATE_OR_UPDATE) {
                RecordType oldRecordType = null;
                try {
                    if (idMode == IdentificationMode.ID) {
                        oldRecordType = typeManager.getRecordTypeById(newRecordType.getId(), null);
                    } else {
                        oldRecordType = typeManager.getRecordTypeByName(identifyingName, null);
                    }
                } catch (RecordTypeNotFoundException e) {
                    if (impMode == UPDATE) {
                        return ImportResult.cannotUpdateDoesNotExist();
                    }
                }

                if (oldRecordType != null) {
                    boolean updated = false;

                    // Update field entries
                    Set<FieldTypeEntry> oldFieldTypeEntries = new HashSet<FieldTypeEntry>(oldRecordType.getFieldTypeEntries());
                    Set<FieldTypeEntry> newFieldTypeEntries = new HashSet<FieldTypeEntry>(newRecordType.getFieldTypeEntries());
                    if (!newFieldTypeEntries.equals(oldFieldTypeEntries)) {
                        updated = true;

                        oldRecordType.getFieldTypeEntries().clear();

                        for (FieldTypeEntry entry : newFieldTypeEntries) {
                            oldRecordType.addFieldTypeEntry(entry);
                        }
                    }

                    // Update supertypes
                    Map<SchemaId, Long> oldSupertypes = oldRecordType.getSupertypes();
                    Map<SchemaId, Long> newSupertypes = newRecordType.getSupertypes();

                    // Resolve any 'null' versions to actual version numbers, otherwise we are unable to compare
                    // with the old state.
                    for (Map.Entry<SchemaId, Long> entry : newSupertypes.entrySet()) {
                        if (entry.getValue() == null) {
                            entry.setValue(typeManager.getRecordTypeById(entry.getKey(), null).getVersion());
                        }
                    }

                    if (!oldSupertypes.equals(newSupertypes)) {
                        updated = true;

                        oldRecordType.getSupertypes().clear();

                        for (Map.Entry<SchemaId, Long> entry : newSupertypes.entrySet()) {
                            oldRecordType.addSupertype(entry.getKey(), entry.getValue());
                        }
                    }

                    // Update name
                    QName oldName = oldRecordType.getName();
                    QName newName = newRecordType.getName();
                    if (!oldName.equals(newName)) {
                        updated = true;
                        oldRecordType.setName(newName);
                    }

                    if (updated) {
                        oldRecordType = typeManager.updateRecordType(oldRecordType, refreshSubtypes);
                        return ImportResult.updated(oldRecordType);
                    } else {
                        return ImportResult.upToDate(oldRecordType);
                    }
                }
            }

            if (impMode == UPDATE) {
                // We should never arrive here, update is handled above
                throw new RuntimeException("Unexpected situation: in case of mode " + UPDATE + " we should not be here.");
            }

            try {
                RecordType createdRecordType = typeManager.createRecordType(newRecordType);
                return ImportResult.created(createdRecordType);
            } catch (RecordTypeExistsException e) {
                if (impMode == CREATE) {
                    return ImportResult.cannotCreateExists();
                }
                // and otherwise, the record type has been created since we last checked, so we now
                // loop again to the top to try to update it
            }

            loopCount++;
        }

    }
}
