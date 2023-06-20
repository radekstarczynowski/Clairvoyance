package com.rashidmayes.clairvoyance.model;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.ClientPolicy;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import com.rashidmayes.clairvoyance.util.Result;

public class AerospikeClientFactory {

    public Result<IAerospikeClient, String> create(ConnectionInfo connectionInfo) {
        try {
            var policy = createPolicy(connectionInfo);
            var client = new AerospikeClient(policy, connectionInfo.host(), connectionInfo.port());
            setDefaultConnectionParameters(client);
            ClairvoyanceLogger.logger.info(ClairvoyanceLogger.IN_APP_CONSOLE, "created new aerospike client");
            return Result.of(client);
        } catch (AerospikeException exception) {
            ClairvoyanceLogger.logger.error(ClairvoyanceLogger.IN_APP_CONSOLE, "could not create aerospike client", exception);
            return Result.error("could not connect to cluster: " + exception.getMessage());
        }
    }

    public ClientPolicy createPolicy(ConnectionInfo connectionInfo) {
        var policy = new ClientPolicy();
        policy.useServicesAlternate = connectionInfo.useServicesAlternate();
        policy.user = connectionInfo.username();
        policy.password = connectionInfo.password();
        return policy;
    }

    public void setDefaultConnectionParameters(IAerospikeClient client) {
        client.getReadPolicyDefault().totalTimeout = 4000;
        client.getQueryPolicyDefault().totalTimeout = 40_000;
        ClairvoyanceLogger.logger.debug("read policy timeout set to {}", client.getReadPolicyDefault().totalTimeout);
        ClairvoyanceLogger.logger.debug("query policy timeout set to {}", client.getQueryPolicyDefault().totalTimeout);
    }

}
