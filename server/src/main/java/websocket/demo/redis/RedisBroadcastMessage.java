package websocket.demo.redis;

public class RedisBroadcastMessage {

    private String destination;
    private Object payload;

    public RedisBroadcastMessage() {
    }

    public RedisBroadcastMessage(String destination, Object payload) {
        this.destination = destination;
        this.payload = payload;
    }

    public String getDestination() {
        return destination;
    }

    public Object getPayload() {
        return payload;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
