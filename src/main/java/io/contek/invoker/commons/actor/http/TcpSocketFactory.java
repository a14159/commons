package io.contek.invoker.commons.actor.http;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

public class TcpSocketFactory extends SocketFactory {

  private final SocketFactory delegate = SocketFactory.getDefault();

  private static Socket configure(Socket s) throws SocketException {
    s.setTcpNoDelay(true);      // Disable Nagle
    s.setKeepAlive(true);       // Enable TCP keepalive
    return s;
  }

  @Override
  public Socket createSocket() throws IOException {
    return configure(delegate.createSocket());
  }

  @Override
  public Socket createSocket(String host, int port) throws IOException {
    return configure(delegate.createSocket(host, port));
  }

  @Override
  public Socket createSocket(String host, int port,
                             InetAddress local, int localPort) throws IOException {
    return configure(delegate.createSocket(host, port, local, localPort));
  }

  @Override
  public Socket createSocket(InetAddress host, int port) throws IOException {
    return configure(delegate.createSocket(host, port));
  }

  @Override
  public Socket createSocket(InetAddress host, int port,
                             InetAddress local, int localPort) throws IOException {
    return configure(delegate.createSocket(host, port, local, localPort));
  }
}
