package com.rashidmayes.clairvoyance.model;

import com.aerospike.client.Info;
import com.aerospike.client.cluster.Node;
import com.rashidmayes.clairvoyance.util.MapParser;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class NodeInfoMapper {

    public NodeInfo getNodeInfo(Node node) {
        var details = Info.request(null, node);
        var statistics = details.get("statistics");
        return NodeInfo.builder()
                .nodeId(details.get("node"))
                .build(details.get("build"))
                .edition(details.get("edition"))
                .version(details.get("version"))
                .statistics(MapParser.parseMap(statistics, ";"))
                .address(node.getAddress().toString())
                .host(node.getHost().toString())
                .name(node.getName())
                .namespaces(getNamespaceInfo(node))
                .build();
    }

    public List<NamespaceInfo> getNamespaceInfo(Node node) {
        var namespaces = new ArrayList<NamespaceInfo>();
        var info = Info.request(null, node, "namespaces");
        for (String namespace : StringUtils.split(info, ";")) {
            var oneStringNamespaceInfo = Info.request(null, node, "namespace/" + namespace);
            var properties = MapParser.parseMap(oneStringNamespaceInfo, ";");
            var namespaceInfo = NamespaceInfo.builder()
                    .name(namespace)
                    .properties(properties)
                    .sets(getSetInfo(node, namespace))
                    .objects(MapParser.getLong(properties, "objects"))
                    .type(MapParser.getString(properties, "type", "storage-engine"))
                    .proleObjects(MapParser.getLong(properties, "prole-objects", "prole_objects"))
                    .usedBytesMemory(MapParser.getLong(properties, "used-bytes-memory", "memory_used_bytes"))
                    .replicationFactor(MapParser.getLong(properties, "repl-factor"))
                    .usedBytesDisk(MapParser.getLong(properties, "used-bytes-disk", "device_used_bytes"))
                    .masterObjects(MapParser.getLong(properties, "master-objects", "master_objects"))
                    .totalBytesMemory(MapParser.getLong(properties, "total-bytes-memory", "memory-size"))
                    .totalBytesDisk(MapParser.getLong(properties, "total-bytes-disk", "device_total_bytes"))
                    .freeMemoryPercent(MapParser.getLong(properties, "free-pct-memory", "memory_free_pct"))
                    .freeDiskPercent(MapParser.getLong(properties, "free-pct-disk", "device_available_pct"))
                    .build();
            namespaces.add(namespaceInfo);
        }
        return namespaces;
    }

    public static List<SetInfo> getSetInfo(Node node, String namespace) {
        var sets = new ArrayList<SetInfo>();
        var oneStringSetInfo = Info.request(null, node, "sets/" + namespace);
        for (String set : StringUtils.split(oneStringSetInfo, ";")) {
            var map = MapParser.parseMap(set, ":");
            var setInfo = SetInfo.builder()
                    .namespace(MapParser.getString(map, "ns_name", "ns"))
                    .name(MapParser.getString(map, "set_name", "set"))
                    .objectCount(MapParser.getLong(map, "n_objects", "objects"))
                    .bytesMemory(MapParser.getLong(map, "n-bytes-memory", "memory_data_bytes"))
                    .properties(map)
                    .build();
            sets.add(setInfo);
        }
        return sets;
    }

}
