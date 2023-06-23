package com.rashidmayes.clairvoyance.model;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.rashidmayes.clairvoyance.ClairvoyanceFxApplication;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Objects;

@Getter
@EqualsAndHashCode(of = "key")
@ToString
public class RecordRow {

    public static final Record NULL_RECORD = new Record(new HashMap<>(), 0, 0);
    public static final Record LOADING_RECORD = new Record(new HashMap<>(), 0, 0);

    private final int index;
    private final Key key;
    @Getter(AccessLevel.NONE)
    private SoftReference<Record> referent;

    public RecordRow(Key key, Record record, int index) {
        this.key = key;
        if (record != null) {
            referent = new SoftReference<>(record);
        }
        this.index = index;
    }

    public Record getRecord() {
        if (referent == null || referent.get() == null) {
            ClairvoyanceLogger.logger.warn("fetching record from server...");
            var client = ClairvoyanceFxApplication.getClient();
            var record = client.get(null, key);
            referent = new SoftReference<>(Objects.requireNonNullElse(record, NULL_RECORD));
        }
        return referent.get();
    }

}
