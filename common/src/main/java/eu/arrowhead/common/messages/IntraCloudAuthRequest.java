/*
 * Copyright (c) 2018 AITIA International Inc.
 *
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.arrowhead.common.database.ArrowheadService;
import eu.arrowhead.common.database.ArrowheadSystem;
import java.util.HashSet;
import java.util.Set;

public class IntraCloudAuthRequest {

  private ArrowheadSystem consumer;
  private Set<ArrowheadSystem> providers = new HashSet<>();
  private ArrowheadService service;

  public IntraCloudAuthRequest() {
  }

  public IntraCloudAuthRequest(ArrowheadSystem consumer, Set<ArrowheadSystem> providers, ArrowheadService service) {
    this.consumer = consumer;
    this.providers = providers;
    this.service = service;
  }

  public ArrowheadSystem getConsumer() {
    return consumer;
  }

  public void setConsumer(ArrowheadSystem consumer) {
    this.consumer = consumer;
  }

  public Set<ArrowheadSystem> getProviders() {
    return providers;
  }

  public void setProviders(Set<ArrowheadSystem> providers) {
    this.providers = providers;
  }

  public ArrowheadService getService() {
    return service;
  }

  public void setService(ArrowheadService service) {
    this.service = service;
  }

  @JsonIgnore
  public boolean isValid() {
    if (consumer == null || service == null || providers.isEmpty() || !consumer.isValidForDatabase() || !service.isValidForDatabase()) {
      return false;
    }
    for (ArrowheadSystem provider : providers) {
      if (!provider.isValidForDatabase()) {
        return false;
      }
    }
    return true;
  }

}