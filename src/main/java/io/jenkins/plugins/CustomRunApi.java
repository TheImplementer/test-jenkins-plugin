package io.jenkins.plugins;

import com.cloudbees.workflow.rest.AbstractWorkflowRunActionHandler;
import hudson.Extension;
import io.jenkins.blueocean.rest.impl.pipeline.FlowNodeWrapper;
import io.jenkins.blueocean.rest.impl.pipeline.PipelineNodeUtil;
import io.jenkins.blueocean.rest.impl.pipeline.PipelineStepVisitor;
import jenkins.security.stapler.StaplerDispatchable;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graphanalysis.DepthFirstScanner;
import org.jenkinsci.plugins.workflow.graphanalysis.ForkScanner;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.pipelinegraphanalysis.StageChunkFinder;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Extension
public class CustomRunApi extends AbstractWorkflowRunActionHandler {

    @Override
    public String getUrlName() {
        return "custom-api";
    }

    public HttpResponse doIndex() {
        return HttpResponses.text(target.getFullDisplayName());
    }

    @StaplerDispatchable
    public Object getNode(String id) {
        return new RunNode(getPipelineNodeSteps(target, id));
    }

    /**
     * Copied from io.jenkins.blueocean.rest.impl.pipeline.PipelineNodeGraphVisitor#getPipelineNodeSteps
     * in https://github.com/jenkinsci/blueocean-plugin
     */
    private List<FlowNodeWrapper> getPipelineNodeSteps(WorkflowRun run, String nodeId) {
        FlowExecution execution = run.getExecution();
        if (execution == null) {
            return Collections.emptyList();
        }
        DepthFirstScanner depthFirstScanner = new DepthFirstScanner();
        //If blocked scope, get the end node
        FlowNode n = depthFirstScanner
                .findFirstMatch(execution.getCurrentHeads(),
                        input -> (input != null
                                && input.getId().equals(nodeId)
                                && (PipelineNodeUtil.isStage(input) || PipelineNodeUtil.isParallelBranch(input))));

        if (n == null) { //if no node found or the node is not stage or parallel we return empty steps
            return Collections.emptyList();
        }
        PipelineStepVisitor visitor = new PipelineStepVisitor(run, n);
        ForkScanner.visitSimpleChunks(execution.getCurrentHeads(), visitor, new StageChunkFinder());
        return new ArrayList<>(visitor.getSteps());
    }
}
