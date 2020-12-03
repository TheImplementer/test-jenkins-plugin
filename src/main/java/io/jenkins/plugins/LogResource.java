package io.jenkins.plugins;

import io.jenkins.blueocean.commons.ServiceException;
import io.jenkins.blueocean.rest.impl.pipeline.FlowNodeWrapper;
import io.jenkins.blueocean.rest.impl.pipeline.PipelineNodeUtil;
import org.jenkinsci.plugins.workflow.actions.LogAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.kohsuke.stapler.AcceptHeader;
import org.kohsuke.stapler.Header;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.framework.io.CharSpool;
import org.kohsuke.stapler.framework.io.LineEndNormalizingWriter;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Simplified implementation based on io.jenkins.blueocean.rest.impl.pipeline.NodeLogResource
 * in https://github.com/jenkinsci/blueocean-plugin
 */
public class LogResource {

    private final List<FlowNodeWrapper> steps;

    public LogResource(List<FlowNodeWrapper> steps) {
        this.steps = steps;
    }

    public void doIndex(StaplerRequest req, StaplerResponse rsp, @Header("Accept") AcceptHeader accept) {
        String download = req.getParameter("download");

        if ("true".equalsIgnoreCase(download)) {
            rsp.setHeader("Content-Disposition", "attachment; filename=log.txt");
        }

        rsp.setContentType("text/plain;charset=UTF-8");
        rsp.setStatus(HttpServletResponse.SC_OK);

        long count = 0;
        try (CharSpool spool = new CharSpool()) {
            for (FlowNodeWrapper step : steps) {
                FlowNode node = step.getNode();
                if (PipelineNodeUtil.isLoggable.apply(node)) {
                    LogAction logAction = node.getAction(LogAction.class);
                    if (logAction != null) {
                        count += logAction.getLogText().writeLogTo(0, spool);
                    }
                }
            }
            Writer writer;
            if (count > 0) {
                writer = (count > 4096) ? rsp.getCompressedWriter(req) : rsp.getWriter();
                spool.flush();
                spool.writeTo(new LineEndNormalizingWriter(writer));
                rsp.addHeader("X-Text-Size", String.valueOf(count));
                writer.close();
            }
        } catch (IOException e) {
            throw new ServiceException.UnexpectedErrorException("Error reading log");
        }
    }
}
