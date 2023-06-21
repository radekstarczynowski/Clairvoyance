package com.rashidmayes.clairvoyance.model;

import com.rashidmayes.clairvoyance.util.MapHelper;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class NamespaceInfo implements Identifiable {

    private final String name;
    private final Map<String, String> properties;
    private final List<SetInfo> sets;

    public long getObjects() {
        return MapHelper.getLong(properties, "objects");
    }

    public String getType() {
        return MapHelper.getString(properties, "type", "storage-engine");
    }

    public long getProleObjects() {
        return MapHelper.getLong(properties, "prole-objects", "prole_objects");
    }

    public long getUsedBytesMemory() {
        return MapHelper.getLong(properties, "used-bytes-memory", "memory_used_bytes");
    }

    public long getReplicationFactor() {
        return MapHelper.getLong(properties, "repl-factor");
    }

    public long getUsedBytesDisk() {
        return MapHelper.getLong(properties, "used-bytes-disk", "device_used_bytes");
    }

    public long getMasterObjects() {
        return MapHelper.getLong(properties, "master-objects", "master_objects");
    }

    public long getTotalBytesMemory() {
        return MapHelper.getLong(properties, "total-bytes-memory", "memory-size");
    }

    public long getTotalBytesDisk() {
        return MapHelper.getLong(properties, "total-bytes-disk", "device_total_bytes");
    }

    public long getFreeMemoryPercent() {
        return MapHelper.getLong(properties, "free-pct-memory", "memory_free_pct");
    }

    public long getFreeDiskPercent() {
        return MapHelper.getLong(properties, "free-pct-disk", "device_available_pct");
    }


    @Override
    public String getId() {
        return "$namespace." + name;
    }

}
