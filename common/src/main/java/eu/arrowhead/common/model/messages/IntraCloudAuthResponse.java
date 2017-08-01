package eu.arrowhead.common.model.messages;

import eu.arrowhead.common.model.ArrowheadSystem;
import java.util.HashMap;

public class IntraCloudAuthResponse {

  private HashMap<ArrowheadSystem, Boolean> authorizationState = new HashMap<>();

  public IntraCloudAuthResponse() {
  }

  public IntraCloudAuthResponse(HashMap<ArrowheadSystem, Boolean> authorizationState) {
    this.authorizationState = authorizationState;
  }

  public HashMap<ArrowheadSystem, Boolean> getAuthorizationMap() {
    return authorizationState;
  }

  public void setAuthorizationMap(HashMap<ArrowheadSystem, Boolean> authorizationState) {
    this.authorizationState = authorizationState;
  }

  public boolean isPayloadUsable() {
    return !authorizationState.isEmpty();
  }

}
