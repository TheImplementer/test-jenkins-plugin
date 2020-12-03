package io.jenkins.plugins;

import io.jenkins.blueocean.rest.impl.pipeline.FlowNodeWrapper;
import jenkins.security.stapler.StaplerDispatchable;

import java.util.List;

public class RunNode {

    private final List<FlowNodeWrapper> steps;

    public RunNode(List<FlowNodeWrapper> steps) {
        this.steps = steps;
    }

    @StaplerDispatchable
    public Object getLog() {
        return new LogResource(steps);
    }
}
