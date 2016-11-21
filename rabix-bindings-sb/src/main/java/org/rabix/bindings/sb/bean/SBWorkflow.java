package org.rabix.bindings.sb.bean;

import java.util.ArrayList;
import java.util.List;

import org.rabix.common.json.BeanPropertyView;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

public class SBWorkflow extends SBJobApp {

  @JsonProperty("steps")
  private List<SBStep> steps;

  @JsonProperty("dataLinks")
  @JsonView(BeanPropertyView.Full.class)
  private List<SBDataLink> dataLinks;

  public SBWorkflow() {
    this.steps = new ArrayList<>();
    this.dataLinks = new ArrayList<>();
  }

  public SBWorkflow(List<SBStep> steps, List<SBDataLink> dataLinks) {
    this.steps = steps;
    this.dataLinks = dataLinks;
  }

  @JsonIgnore
  public void addDataLink(SBDataLink dataLink) {
    this.dataLinks.add(dataLink);
  }

  @JsonIgnore
  public void addDataLinks(List<SBDataLink> dataLinks) {
    this.dataLinks.addAll(dataLinks);
  }

  public List<SBStep> getSteps() {
    return steps;
  }

  public List<SBDataLink> getDataLinks() {
    return dataLinks;
  }

  @Override
  public String toString() {
    return "Workflow [steps=" + steps + ", dataLinks=" + dataLinks + ", id=" + id + ", context=" + context
        + ", description=" + description + ", inputs=" + getInputs() + ", outputs=" + getOutputs() + ", requirements="
        + requirements + "]";
  }

  @Override
  @JsonIgnore
  public SBJobAppType getType() {
    return SBJobAppType.WORKFLOW;
  }

}
