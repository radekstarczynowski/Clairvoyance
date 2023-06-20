package com.rashidmayes.clairvoyance;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.ScanPolicy;
import com.rashidmayes.clairvoyance.model.ApplicationModel;
import com.rashidmayes.clairvoyance.model.RecordRow;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SetScanner {

    // safety net to prevent from too much of memory consumption
    public static final int MAX_RECORDS_FETCH = 1_000_000;

    public interface ScanCallbacks {

        void scanSuccessCallback(List<RecordRow> buffer);

        void scanTerminated(List<RecordRow> buffer);

    }

    private final String namespace;
    private final String set;
    private final ScanCallbacks scanCallbacks;

    @Getter
    private final List<RecordRow> buffer;
    private volatile boolean scanCancelled;

    public SetScanner(String namespace, String set, ScanCallbacks scanCallbacks) {
        this.namespace = namespace;
        this.set = set;
        this.buffer = Collections.synchronizedList(new LinkedList<>());
        this.scanCallbacks = scanCallbacks;
    }

    public void scan() {
        var internalBuffer = Collections.synchronizedList(new LinkedList<RecordRow>());
        var atomicIndex = new AtomicInteger();
        scanCancelled = false;
        try {
            var client = getAerospikeClient();
            client.scanAll(
                    createScanPolicy(),
                    namespace,
                    set,
                    (key, record) -> {
                        var index = atomicIndex.getAndIncrement();
                        onScanRecordReceived(key, record, index, internalBuffer);
                    }
            );
            scanCallbacks.scanSuccessCallback(internalBuffer);
            ClairvoyanceLogger.logger.info("submitted scan all request");

        } catch (AerospikeException.ScanTerminated exception) {
            ClairvoyanceLogger.logger.info(exception.getMessage());
            scanCallbacks.scanTerminated(internalBuffer);
        } catch (Exception exception) {
            ClairvoyanceLogger.logger.error(exception.getMessage(), exception);
            ClairvoyanceFxApplication.displayAlert("could not scan set: " + exception.getMessage());
        }
    }

    private void onScanRecordReceived(Key key, Record record, int index, List<RecordRow> internalBuffer) {
        if (scanCancelled) {
            throw new AerospikeException.ScanTerminated();
        }
        if (internalBuffer.size() < MAX_RECORDS_FETCH) {
            var recordRow = new RecordRow(key, record);
            recordRow.setIndex(index);
            internalBuffer.add(recordRow);
        } else {
            ClairvoyanceLogger.logger.error("internal buffer full");
            throw new AerospikeException.ScanTerminated();
        }
    }

    public boolean isScanCancelled() {
        return scanCancelled;
    }

    public void cancelScan() {
        this.scanCancelled = true;
    }

    private IAerospikeClient getAerospikeClient() {
        var clientResult = ApplicationModel.INSTANCE.getAerospikeClient();
        if (clientResult.hasError()) {
            throw new AerospikeException(clientResult.getError());
        }
        return clientResult.getData();
    }

    private ScanPolicy createScanPolicy() {
        var scanPolicy = new ScanPolicy();
        scanPolicy.totalTimeout = 4000;
        scanPolicy.maxRecords = MAX_RECORDS_FETCH;
        scanPolicy.sendKey = true;
        return scanPolicy;
    }

}
