/*
 * Copyright 2012 NGDATA nv
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
package org.lilyproject.indexer.model.indexerconf;


import com.google.common.base.Predicate;
import org.lilyproject.repository.api.RepositoryException;
import org.lilyproject.repository.api.Scope;
import org.lilyproject.util.repo.VTaggedRecord;

public interface MappingNode {

    /**
     * Returns true if the index can be affected by the given vtRecord
     */
    public abstract boolean isIndexAffectedByUpdate(VTaggedRecord vtRecord, Scope scope) throws InterruptedException,
            RepositoryException;

    /**
     * Traverse the MappingNode hierarchy and report context records and data to index to the indexUpdateBuilder.
     */
    public abstract void collectIndexUpdate(IndexUpdateBuilder indexUpdateBuilder) throws InterruptedException, RepositoryException;

    /**
     * Evaluate the predicate for this node. If predicate.apply returns true, descend into children
     */
    public abstract void visitAll(Predicate<MappingNode> predicate);

}