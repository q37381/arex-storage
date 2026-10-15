package com.arextest.config.model.vo;

import lombok.Data;

/**
 * @author Owen_gan
 * @since 2023/7/17
 */
@Data
public class AgentStatusRequest {

  private String appId;
  private String host;
  private String agentStatus;
  /**
   * Record rate the agent actually applies right now - the agent decelerates on
   * its own when the report queue overflows or the storage service misbehaves,
   * so this differs from the configured sample rate.
   */
  private String currentRate;
  /**
   * Deceleration reason code reported by the agent, 0 means normal.
   */
  private Integer decelerateCode;
}
