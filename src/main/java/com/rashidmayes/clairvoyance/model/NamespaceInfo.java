package com.rashidmayes.clairvoyance.model;

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

    private final Long objects;
    private final String type;
    private final Long proleObjects;
    private final Long usedBytesMemory;
    private final Long replicationFactor;
    private final Long usedBytesDisk;
    private final Long masterObjects;
    private final Long totalBytesMemory;
    private final Long totalBytesDisk;
    private final Long freeMemoryPercent;
    private final Long freeDiskPercent;

    @Override
    public String getId() {
        return "$namespace." + name;
    }

}
