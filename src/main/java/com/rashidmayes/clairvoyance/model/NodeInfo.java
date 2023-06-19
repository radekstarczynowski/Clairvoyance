package com.rashidmayes.clairvoyance.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class NodeInfo implements Identifiable {

    private final String nodeId;
    private final String build;
    private final String edition;
    private final String version;
    private final String name;
    private final String host;
    private final String address;
    private final List<NamespaceInfo> namespaces;
    private final Map<String, String> statistics;

    @Override
    public String getId() {
        return "$node." + nodeId;
    }
}
