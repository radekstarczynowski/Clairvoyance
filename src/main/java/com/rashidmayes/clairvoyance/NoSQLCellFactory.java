package com.rashidmayes.clairvoyance;

import com.aerospike.client.Record;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.rashidmayes.clairvoyance.model.RecordRow;
import com.rashidmayes.clairvoyance.util.ClairvoyanceObjectMapper;
import gnu.crypto.util.Base64;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.util.Callback;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class NoSQLCellFactory implements Callback<CellDataFeatures<RecordRow, String>, ObservableValue<String>> {

    private static final int MAX_BINARY_LENGTH = 512;

    private final String mBinName;

    public NoSQLCellFactory(String binName) {
        this.mBinName = binName;
    }

    @SneakyThrows
    @Override
    public ObservableValue<String> call(CellDataFeatures<RecordRow, String> param) {
        var recordRow = param.getValue();
        if (recordRow != null) {
            var record = recordRow.getRecord();
            if (record != null) {
                if (record == RecordRow.LOADING_RECORD) {
                    return new SimpleStringProperty("loading...");
                } else {
                    Map<String, Object> bins = record.bins;
                    if (bins != null) {
                        var value = bins.get(mBinName);
                        if (value != null) {
                            return getBinRepresentation(value);
                        }
                    }
                }
            }
        }
        return new SimpleStringProperty("");
    }

    private SimpleStringProperty getBinRepresentation(Object value) throws JsonProcessingException {
        if (value instanceof String || value instanceof Number) {
            return new SimpleStringProperty(value.toString());
        } else if (value instanceof byte[]) {
            return new SimpleStringProperty(StringUtils.abbreviate(Base64.encode((byte[]) value), MAX_BINARY_LENGTH));
        } else {
            return new SimpleStringProperty(ClairvoyanceObjectMapper.objectMapper.writeValueAsString(value));
        }
    }

}
