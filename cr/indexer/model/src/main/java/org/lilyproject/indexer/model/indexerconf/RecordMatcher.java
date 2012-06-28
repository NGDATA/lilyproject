package org.lilyproject.indexer.model.indexerconf;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.lilyproject.indexer.model.indexerconf.WildcardPattern;
import org.lilyproject.repository.api.FieldType;
import org.lilyproject.repository.api.QName;
import org.lilyproject.repository.api.Record;


/**
 * Matches records based on certain conditions, i.e. a predicate on record objects.
 */
public class RecordMatcher {
    private WildcardPattern recordTypeNamespace;
    private WildcardPattern recordTypeName;

    private FieldType fieldType;
    private Object fieldValue;

    /**
     * The variant properties the record should have. Evaluation rules: a key named
     * "*" (star symbol) is a wildcard meaning that any variant dimensions not specified
     * are accepted. Otherwise the variant dimension count should match exactly. The other
     * keys in the map are required variant dimensions. If their value is not null, the
     * values should match.
     */
    private final Map<String, String> variantPropsPattern;

    public RecordMatcher(WildcardPattern recordTypeNamespace, WildcardPattern recordTypeName, FieldType fieldType,
            Object fieldValue, Map<String, String> variantPropsPattern) {
        this.recordTypeNamespace = recordTypeNamespace;
        this.recordTypeName = recordTypeName;
        this.fieldType = fieldType;
        this.fieldValue = fieldValue;
        this.variantPropsPattern = variantPropsPattern;
    }

    public boolean matches(Record record) {
        QName recordTypeName = record.getRecordTypeName();
        Map<String, String> varProps = record.getId().getVariantProperties();

        // About "recordTypeName == null": normally record type name cannot be null, but it can
        // be in the case of IndexAwareMQFeeder
        if (this.recordTypeNamespace != null &&
                (recordTypeName == null || !this.recordTypeNamespace.lightMatch(recordTypeName.getNamespace()))) {
            return false;
        }

        if (this.recordTypeName != null
                && (recordTypeName == null || !this.recordTypeName.lightMatch(recordTypeName.getName()))) {
            return false;
        }

        if (variantPropsPattern.size() != varProps.size() && !variantPropsPattern.containsKey("*")) {
            return false;
        }

        for (Map.Entry<String, String> entry : variantPropsPattern.entrySet()) {
            if (entry.getKey().equals("*"))
                continue;

            String dimVal = varProps.get(entry.getKey());
            if (dimVal == null) {
                // this record does not have a required variant property
                return false;
            }

            if (entry.getValue() != null && !entry.getValue().equals(dimVal)) {
                // the variant property does not have the required value
                return false;
            }
        }

        if (fieldType != null) {
            return record.hasField(fieldType.getName()) && fieldValue.equals(record.getField(fieldType.getName()));
        }

        return true;

    }

    public Set<QName> getFieldDependencies() {
        return fieldType != null ? Collections.singleton(fieldType.getName()) : Collections.<QName>emptySet();
    }

    public boolean dependsOnRecordType() {
        return recordTypeName != null || recordTypeNamespace != null;
    }
}
