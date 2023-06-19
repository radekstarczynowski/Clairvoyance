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

    @Override
    public String getId() {
        return "$namespace." + name;
    }

}
