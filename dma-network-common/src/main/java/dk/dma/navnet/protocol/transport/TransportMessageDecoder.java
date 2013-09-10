package dk.dma.navnet.protocol.transport;

import java.io.IOException;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import dk.dma.navnet.messages.TransportMessage;

public class TransportMessageDecoder implements Decoder.Text<TransportMessage> {

    public TransportMessage decode(String src) throws DecodeException {
        try {
            return TransportMessage.parseMessage(src);
        } catch (IOException e) {
            throw new DecodeException(src, "could not decode message", e);
        }
    }

    public boolean willDecode(String src) {
        return src.startsWith("{");
    }

    /** {@inheritDoc} */
    @Override
    public void init(EndpointConfig config) {}

    /** {@inheritDoc} */
    @Override
    public void destroy() {}
}
